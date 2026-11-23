package app.rvx.patches.shared.spoof.browse

import app.rvx.util.fingerprint.Match
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.rvx.patches.shared.authenticationChangeListenerFingerprint
import app.rvx.patches.shared.clientTypeFingerprint
import app.rvx.patches.shared.indexOfClientInfoInstruction
import app.rvx.patches.shared.indexOfMessageLiteBuilderReference
import app.rvx.util.fingerprint.matchOrThrow
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private lateinit var insertMatch: Match
private lateinit var browseIdField: FieldReference
private lateinit var clientInfoField: FieldReference
private lateinit var messageLiteBuilderField: FieldReference
private lateinit var messageLiteBuilderMethod: MethodReference

val spoofClientBrowseEndpointPatch = bytecodePatch(
    description = "spoofClientBrowseEndpointPatch"
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

        browseEndpointRequestBodyFingerprint
            .matchOrThrow(browseEndpointConstructorFingerprint)
            .let {
                it.method.apply {
                    val browseIdIndex = it.patternMatch!!.startIndex

                    browseIdField =
                        getInstruction<ReferenceInstruction>(browseIdIndex).reference as FieldReference
                    insertMatch = it
                }
            }
    }
}

@Suppress("SameParameterValue")
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
                MutableMethodImplementation(7),
            ).toMutable().apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-virtual {p0}, $messageLiteBuilderMethod
                        move-result-object v0
                        iget-object v0, v0, $messageLiteBuilderField
                        check-cast v0, ${clientInfoField.definingClass}
                        iget-object v1, v0, $clientInfoField
                        if-eqz v1, :ignore
                        iget-object v3, p0, $browseIdField
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

