package app.rvx.util.fingerprint

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableClassDef
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.firstMethod
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.PatchException
import app.rvx.util.containsLiteralInstruction
import app.rvx.util.indexOfFirstInstructionOrThrow
import app.rvx.util.indexOfFirstLiteralInstruction
import app.rvx.util.injectLiteralInstructionViewCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private val String.exception
    get() = PatchException("Failed to resolve $this")

/**
 * The result of resolving a [Fingerprint].
 */
class Match internal constructor(
    private val context: BytecodePatchContext,
    val originalClassDef: ClassDef,
    val originalMethod: Method,
    val patternMatch: PatternMatch?,
    val stringMatches: List<StringMatch>?,
) {
    class PatternMatch(val startIndex: Int, val endIndex: Int)

    class StringMatch(val string: String, val index: Int)

    /**
     * The mutable method that was matched.
     */
    val method: MutableMethod by lazy {
        with(context) { firstMethod(originalMethod) }
    }

    /**
     * The mutable class that contains the matched method.
     */
    val classDef: MutableClassDef by lazy {
        with(context) {
            // The class may already have been replaced by a mutable copy, so look it up by type.
            val current = classDefs.firstOrNull { it.type == originalClassDef.type } ?: originalClassDef
            classDefs.getOrReplaceMutable(current)
        }
    }
}

/**
 * A fingerprint that finds a single method.
 * Matching rules:
 * - [accessFlags] must be equal.
 * - [returnType] and every entry of [parameters] are compared with 'startsWith'.
 * - Every entry of [strings] must be contained in a string constant of the method.
 * - [opcodes] is searched as a pattern (null is a wildcard), [fuzzyPatternScanThreshold] mismatches are allowed.
 */
class Fingerprint internal constructor(
    private val fuzzyPatternScanThreshold: Int,
    private val accessFlags: Int?,
    private val returnType: String?,
    private val parameters: List<String>?,
    private val opcodes: List<Opcode?>?,
    private val strings: List<String>?,
    private val custom: ((method: Method, classDef: ClassDef) -> Boolean)?,
) {
    private var resolvedContext: BytecodePatchContext? = null
    private var resolvedMatch: Match? = null

    /**
     * Strings are matched in two passes, like the previous patcher did:
     * first the first string must be equal to a string constant (the others only need to be contained),
     * and only if nothing matched, every string only needs to be contained in a string constant.
     */
    private val stringPasses = if (strings.isNullOrEmpty()) listOf(false) else listOf(true, false)

    private fun matchMethod(
        context: BytecodePatchContext,
        method: Method,
        classDef: ClassDef,
        exactFirstString: Boolean,
    ): Match? {
        if (accessFlags != null && accessFlags != method.accessFlags) return null

        if (returnType != null && !method.returnType.startsWith(returnType)) return null

        if (parameters != null) {
            val actual = method.parameterTypes
            if (actual.size != parameters.size) return null
            for (i in parameters.indices) {
                if (!actual[i].startsWith(parameters[i])) return null
            }
        }

        if (custom != null && !custom.invoke(method, classDef)) return null

        var stringMatches: List<Match.StringMatch>? = null
        var patternMatch: Match.PatternMatch? = null

        if (strings != null || opcodes != null) {
            val implementation = method.implementation ?: return null

            if (strings != null) {
                // Single pass over the instructions, no allocation.
                val foundIndices = IntArray(strings.size) { -1 }
                var remaining = strings.size
                var index = 0
                for (instruction in implementation.instructions) {
                    val opcode = instruction.opcode
                    if (opcode == Opcode.CONST_STRING || opcode == Opcode.CONST_STRING_JUMBO) {
                        val value = ((instruction as ReferenceInstruction).reference as StringReference).string
                        for (i in strings.indices) {
                            val equal = if (exactFirstString && i == 0) value == strings[i] else value.contains(strings[i])
                            if (foundIndices[i] < 0 && equal) {
                                foundIndices[i] = index
                                remaining--
                            }
                        }
                        if (remaining == 0) break
                    }
                    index++
                }
                if (remaining > 0) return null

                stringMatches = strings.mapIndexed { i, expected ->
                    Match.StringMatch(expected, foundIndices[i])
                }
            }

            if (opcodes != null) {
                val instructions = implementation.instructions.toList()
                val size = opcodes.size
                if (instructions.size < size) return null

                var found = -1
                for (start in 0..instructions.size - size) {
                    var mismatches = 0
                    var ok = true
                    for (i in 0 until size) {
                        val expected = opcodes[i] ?: continue
                        if (instructions[start + i].opcode != expected) {
                            mismatches++
                            if (mismatches > fuzzyPatternScanThreshold) {
                                ok = false
                                break
                            }
                        }
                    }
                    if (ok) {
                        found = start
                        break
                    }
                }
                if (found < 0) return null
                patternMatch = Match.PatternMatch(found, found + size - 1)
            }
        }

        return Match(context, classDef, method, patternMatch, stringMatches)
    }

    /**
     * Match within a single class.
     */
    fun matchOrNull(context: BytecodePatchContext, classDef: ClassDef): Match? {
        for (exactFirstString in stringPasses) {
            for (method in classDef.methods) {
                matchMethod(context, method, classDef, exactFirstString)?.let { return it }
            }
        }
        return null
    }

    /**
     * Match within the whole app. The result is cached for the current context.
     */
    fun matchOrNull(context: BytecodePatchContext): Match? {
        if (resolvedContext === context) return resolvedMatch

        var result: Match? = null
        val classes = with(context) { classDefs }
        search@ for (exactFirstString in stringPasses) {
            for (classDef in classes) {
                for (method in classDef.methods) {
                    val match = matchMethod(context, method, classDef, exactFirstString)
                    if (match != null) {
                        result = match
                        break@search
                    }
                }
            }
        }

        // Only cache successful matches. A fingerprint that failed may match later,
        // after other patches have changed the code.
        if (result != null) {
            resolvedContext = context
            resolvedMatch = result
        }
        return result
    }
}

internal fun legacyFingerprint(
    name: String,
    fuzzyPatternScanThreshold: Int = 0,
    accessFlags: Int? = null,
    returnType: String? = null,
    parameters: List<String>? = null,
    opcodes: List<Opcode?>? = null,
    strings: List<String>? = null,
    literals: List<Long>? = null,
    customFingerprint: ((methodDef: Method, classDef: ClassDef) -> Boolean)? = null,
): Pair<String, Fingerprint> {
    val custom: ((Method, ClassDef) -> Boolean)? =
        if (literals == null && customFingerprint == null) {
            null
        } else {
            { method, classDef ->
                (literals == null || literals.all { method.containsLiteralInstruction(it) }) &&
                        (customFingerprint == null || customFingerprint(method, classDef))
            }
        }

    return Pair(
        name,
        Fingerprint(
            fuzzyPatternScanThreshold,
            accessFlags,
            returnType,
            parameters,
            opcodes,
            strings,
            custom,
        )
    )
}

context(ctx: BytecodePatchContext)
internal fun Fingerprint.methodOrThrow(): MutableMethod =
    matchOrNull(ctx)?.method ?: throw PatchException("Failed to resolve fingerprint")

context(ctx: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.resolvable(): Boolean =
    second.matchOrNull(ctx) != null

context(ctx: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.definingClassOrThrow(): String =
    second.matchOrNull(ctx)?.originalClassDef?.type ?: throw first.exception

context(ctx: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.matchOrThrow(): Match =
    second.matchOrNull(ctx) ?: throw first.exception

context(ctx: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.matchOrThrow(parentFingerprint: Pair<String, Fingerprint>): Match {
    val parentClassDef = parentFingerprint.second.matchOrNull(ctx)?.originalClassDef
        ?: throw parentFingerprint.first.exception
    return second.matchOrNull(ctx, parentClassDef)
        ?: throw first.exception
}

context(ctx: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.matchOrNull(): Match? =
    second.matchOrNull(ctx)

context(ctx: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.matchOrNull(parentFingerprint: Pair<String, Fingerprint>): Match? =
    parentFingerprint.second.matchOrNull(ctx)?.originalClassDef?.let { parentClassDef ->
        second.matchOrNull(ctx, parentClassDef)
    }

context(ctx: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.matchOrNull(classDef: ClassDef): Match? =
    second.matchOrNull(ctx, classDef)

context(ctx: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.methodOrNull(): MutableMethod? =
    second.matchOrNull(ctx)?.method

context(ctx: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.methodOrThrow(): MutableMethod =
    matchOrThrow().method

context(ctx: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.methodOrThrow(parentFingerprint: Pair<String, Fingerprint>): MutableMethod =
    matchOrThrow(parentFingerprint).method

context(ctx: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.originalMethodOrThrow(): Method =
    matchOrThrow().originalMethod

context(ctx: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.originalMethodOrThrow(parentFingerprint: Pair<String, Fingerprint>): Method =
    matchOrThrow(parentFingerprint).originalMethod

context(ctx: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.mutableClassOrThrow(): MutableClassDef =
    matchOrThrow().classDef

context(ctx: BytecodePatchContext)
internal fun Pair<String, Fingerprint>.methodCall() =
    methodOrThrow().methodCall()

internal fun MutableMethod.methodCall(): String {
    var methodCall = "$definingClass->$name("
    for (i in 0 until parameters.size) {
        methodCall += parameterTypes[i]
    }
    methodCall += ")$returnType"
    return methodCall
}

context(ctx: BytecodePatchContext)
fun Pair<String, Fingerprint>.injectLiteralInstructionBooleanCall(
    literal: Long,
    descriptor: String
) {
    methodOrThrow().apply {
        val literalIndex = indexOfFirstLiteralInstruction(literal)
        val index = indexOfFirstInstructionOrThrow(literalIndex, Opcode.MOVE_RESULT)
        val register = getInstruction<OneRegisterInstruction>(index).registerA

        val smaliInstruction =
            if (descriptor.startsWith("0x")) """
                const/16 v$register, $descriptor
                """
            else if (descriptor.endsWith("(Z)Z")) """
                invoke-static/range { v$register .. v$register }, $descriptor
                move-result v$register
                """
            else """
                invoke-static {}, $descriptor
                move-result v$register
                """

        addInstructions(
            index + 1,
            smaliInstruction
        )
    }
}

context(ctx: BytecodePatchContext)
fun Pair<String, Fingerprint>.injectLiteralInstructionViewCall(
    literal: Long,
    smaliInstruction: String
) {
    val method = methodOrThrow()
    method.injectLiteralInstructionViewCall(literal, smaliInstruction)
}
