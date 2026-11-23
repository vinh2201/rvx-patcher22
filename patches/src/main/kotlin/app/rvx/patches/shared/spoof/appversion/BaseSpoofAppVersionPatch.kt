package app.rvx.patches.shared.spoof.appversion

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.shared.createPlayerRequestBodyWithModelFingerprint
import app.rvx.patches.shared.indexOfReleaseInstruction
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

fun baseSpoofAppVersionPatch(
    descriptor: String,
) = bytecodePatch(
    description = "baseSpoofAppVersionPatch"
) {
    apply {
        createPlayerRequestBodyWithModelFingerprint.methodOrThrow().apply {
            val versionIndex = indexOfReleaseInstruction(this) + 1
            val insertIndex =
                indexOfFirstInstructionReversedOrThrow(versionIndex, Opcode.IPUT_OBJECT)
            val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex, """
                    invoke-static {v$insertRegister}, $descriptor
                    move-result-object v$insertRegister
                    """
            )
        }
    }
}
