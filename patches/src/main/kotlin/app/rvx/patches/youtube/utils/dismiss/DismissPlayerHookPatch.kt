package app.rvx.patches.youtube.utils.dismiss

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.rvx.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.rvx.patches.youtube.utils.extension.sharedExtensionPatch
import app.rvx.util.addStaticFieldToExtension
import app.rvx.util.findMethodOrThrow
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.getWalkerMethod
import app.rvx.util.indexOfFirstInstructionOrThrow
import app.rvx.util.indexOfFirstInstructionReversedOrThrow
import app.rvx.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/utils/VideoUtils;"

private lateinit var dismissMethod: MutableMethod

val dismissPlayerHookPatch = bytecodePatch(
    description = "dismissPlayerHookPatch"
) {
    dependsOn(sharedExtensionPatch)

    apply {
        dismissPlayerOnClickListenerFingerprint.methodOrThrow().apply {
            val literalIndex =
                indexOfFirstLiteralInstructionOrThrow(DISMISS_PLAYER_LITERAL)
            val dismissPlayerIndex = indexOfFirstInstructionOrThrow(literalIndex) {
                val reference = getReference<MethodReference>()
                opcode == Opcode.INVOKE_VIRTUAL &&
                        reference?.returnType == "V" &&
                        reference.parameterTypes.isEmpty()
            }

            getWalkerMethod(dismissPlayerIndex).apply {
                val jumpIndex = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.returnType == "V"
                }
                getWalkerMethod(jumpIndex).apply {
                    val jumpIndex = indexOfFirstInstructionReversedOrThrow {
                        val reference = getReference<MethodReference>()
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                reference?.returnType == "V" &&
                                reference.parameterTypes.firstOrNull() == "I"
                    }
                    dismissMethod = getWalkerMethod(jumpIndex)
                }
            }

            val dismissPlayerReference =
                getInstruction<ReferenceInstruction>(dismissPlayerIndex).reference as MethodReference
            val dismissPlayerClass = dismissPlayerReference.definingClass

            val fieldIndex =
                indexOfFirstInstructionReversedOrThrow(dismissPlayerIndex) {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.type == dismissPlayerClass
                }
            val fieldReference =
                getInstruction<ReferenceInstruction>(fieldIndex).reference as FieldReference

            findMethodOrThrow(fieldReference.definingClass).apply {
                val insertIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>() == fieldReference
                }
                val insertRegister =
                    getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "sput-object v$insertRegister, $EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR->dismissPlayerClass:$dismissPlayerClass"
                )

                val smaliInstructions =
                    """
                        if-eqz v0, :ignore
                        invoke-virtual {v0}, $dismissPlayerReference
                        :ignore
                        return-void
                        """

                addStaticFieldToExtension(
                    EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                    "dismissPlayer",
                    "dismissPlayerClass",
                    dismissPlayerClass,
                    smaliInstructions,
                    false
                )
            }
        }
    }
}

/**
 * This method is called when the video is closed.
 */
internal fun hookDismissObserver(descriptor: String) =
    dismissMethod.addInstruction(
        0,
        "invoke-static {p1}, $descriptor"
    )