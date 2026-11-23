package app.rvx.patches.shared.spoof.guide

import app.rvx.util.fingerprint.Match
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.rvx.patches.shared.ANDROID_AUTOMOTIVE_STRING
import app.rvx.patches.shared.CLIENT_INFO_CLASS_DESCRIPTOR
import app.rvx.patches.shared.authenticationChangeListenerFingerprint
import app.rvx.patches.shared.autoMotiveFingerprint
import app.rvx.patches.shared.clientTypeFingerprint
import app.rvx.patches.shared.createPlayerRequestBodyWithModelFingerprint
import app.rvx.patches.shared.indexOfClientInfoInstruction
import app.rvx.patches.shared.indexOfMessageLiteBuilderReference
import app.rvx.util.fingerprint.matchOrThrow
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionOrThrow
import app.rvx.util.indexOfFirstInstructionReversedOrThrow
import app.rvx.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private lateinit var insertMatch: Match
private lateinit var clientInfoField: FieldReference
private lateinit var messageLiteBuilderField: FieldReference
private lateinit var messageLiteBuilderMethod: MethodReference

val spoofClientGuideEndpointPatch = bytecodePatch(
    description = "spoofClientGuideEndpointPatch"
) {
    apply {
        clientTypeFingerprint.methodOrThrow().apply {
            val clientInfoIndex = indexOfClientInfoInstruction(this)
            val messageLiteBuilderIndex =
                indexOfFirstInstructionReversedOrThrow(clientInfoIndex) {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.name == "instance"
                }

            clientInfoField =
                getInstruction<ReferenceInstruction>(clientInfoIndex).reference as FieldReference
            messageLiteBuilderField =
                getInstruction<ReferenceInstruction>(messageLiteBuilderIndex).reference as FieldReference
        }

        authenticationChangeListenerFingerprint.methodOrThrow().apply {
            val messageLiteBuilderIndex =
                indexOfMessageLiteBuilderReference(this, messageLiteBuilderField.definingClass)

            messageLiteBuilderMethod =
                getInstruction<ReferenceInstruction>(messageLiteBuilderIndex).reference as MethodReference
        }

        insertMatch = guideEndpointRequestBodyFingerprint
            .matchOrThrow(guideEndpointConstructorFingerprint)
    }
}

internal fun addClientInfoHook(
    helperMethodName: String,
    smaliInstructions: String,
    insertLast: Boolean = false,
) = insertMatch.let {
    it.method.apply {
        it.classDef.methods.add(
            ImmutableMethod(
                definingClass,
                helperMethodName,
                emptyList(),
                "V",
                AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                annotations,
                null,
                MutableMethodImplementation(5),
            ).toMutable().apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-virtual {p0}, $messageLiteBuilderMethod
                        move-result-object v0
                        iget-object v0, v0, $messageLiteBuilderField
                        check-cast v0, ${clientInfoField.definingClass}
                        iget-object v1, v0, $clientInfoField
                        if-eqz v1, :ignore
                        """ + smaliInstructions + """
                        :ignore
                        return-void
                        """,
                )
            }
        )

        addInstruction(
            if (insertLast) implementation!!.instructions.lastIndex else 0,
            "invoke-direct/range { p0 .. p0 }, $definingClass->$helperMethodName()V"
        )
    }
}

context(ctx: BytecodePatchContext)
internal fun addClientOSVersionHook(
    helperMethodName: String,
    descriptor: String,
    isObject: Boolean = false,
    insertLast: Boolean = true,
) {
    val osNameLocalField = with (autoMotiveFingerprint.methodOrThrow()) {
        val stringIndex = indexOfFirstStringInstructionOrThrow(ANDROID_AUTOMOTIVE_STRING)
        val fieldType = if (isObject) "Ljava/lang/Object;" else "Ljava/lang/String;"
        val osNameFieldIndex = indexOfFirstInstructionOrThrow(stringIndex) {
            val reference = getReference<FieldReference>()
            opcode == Opcode.IPUT_OBJECT &&
                    reference?.type == fieldType &&
                    reference.definingClass == definingClass
        }

        getInstruction<ReferenceInstruction>(osNameFieldIndex).reference as FieldReference
    }

    createPlayerRequestBodyWithModelFingerprint.methodOrThrow().apply {
        val osNameLocalFieldIndex = indexOfFirstInstructionOrThrow {
            opcode == Opcode.IGET_OBJECT &&
                    getReference<FieldReference>() == osNameLocalField
        }
        val osNameIndex =
            indexOfFirstInstructionOrThrow(osNameLocalFieldIndex - 1) {
                val reference = getReference<FieldReference>()
                opcode == Opcode.IPUT_OBJECT &&
                        reference?.type == "Ljava/lang/String;" &&
                        reference.definingClass == CLIENT_INFO_CLASS_DESCRIPTOR
            }
        val osNameReference =
            getInstruction<ReferenceInstruction>(osNameIndex).reference

        addClientInfoHook(
            helperMethodName,
            """
                invoke-static {}, $descriptor
                move-result-object v2
                iput-object v2, v1, $osNameReference
                """,
            insertLast
        )
    }
}

