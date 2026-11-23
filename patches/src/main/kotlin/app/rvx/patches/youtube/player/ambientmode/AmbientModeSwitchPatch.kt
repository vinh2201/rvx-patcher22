package app.rvx.patches.youtube.player.ambientmode

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.rvx.patches.youtube.utils.patch.PatchList.AMBIENT_MODE_CONTROL
import app.rvx.patches.youtube.utils.playservice.is_19_34_or_greater
import app.rvx.patches.youtube.utils.playservice.is_19_41_or_greater
import app.rvx.patches.youtube.utils.playservice.versionCheckPatch
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.util.findMethodOrThrow
import app.rvx.util.fingerprint.injectLiteralInstructionBooleanCall
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionOrThrow
import app.rvx.util.indexOfFirstInstructionReversedOrThrow
import app.rvx.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val ambientModeSwitchPatch = bytecodePatch(
    AMBIENT_MODE_CONTROL.displayTitle,
    AMBIENT_MODE_CONTROL.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        settingsPatch,
        versionCheckPatch,
    )

    apply {
        // region patch for bypass ambient mode restrictions

        var syntheticClassList = emptyArray<String>()

        mapOf(
            powerSaveModeBroadcastReceiverFingerprint to false,
            powerSaveModeSyntheticFingerprint to true
        ).forEach { (fingerprint, reversed) ->
            fingerprint.methodOrThrow().apply {
                val stringIndex =
                    indexOfFirstStringInstructionOrThrow("android.os.action.POWER_SAVE_MODE_CHANGED")
                val targetIndex =
                    if (reversed)
                        indexOfFirstInstructionReversedOrThrow(stringIndex, Opcode.INVOKE_DIRECT)
                    else
                        indexOfFirstInstructionOrThrow(stringIndex, Opcode.INVOKE_DIRECT)
                val targetClass =
                    (getInstruction<ReferenceInstruction>(targetIndex).reference as MethodReference).definingClass

                syntheticClassList += targetClass
            }
        }

        syntheticClassList.distinct().forEach { className ->
            findMethodOrThrow(className) {
                name == "accept"
            }.apply {
                implementation!!.instructions
                    .withIndex()
                    .filter { (_, instruction) ->
                        val reference = (instruction as? ReferenceInstruction)?.reference
                        instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                                reference is MethodReference &&
                                reference.name == "isPowerSaveMode"
                    }
                    .map { (index, _) -> index }
                    .reversed()
                    .forEach { index ->
                        val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

                        addInstructions(
                            index + 2, """
                                invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->bypassAmbientModeRestrictions(Z)Z
                                move-result v$register
                                """
                        )
                    }
            }
        }

        // endregion

        // region patch for disable ambient mode in fullscreen

        if (!is_19_41_or_greater) {
            ambientModeInFullscreenFingerprint.injectLiteralInstructionBooleanCall(
                AMBIENT_MODE_IN_FULLSCREEN_FEATURE_FLAG,
                "$PLAYER_CLASS_DESCRIPTOR->disableAmbientModeInFullscreen()Z"
            )
        }

        if (is_19_34_or_greater) {
            setFullScreenBackgroundColorFingerprint.methodOrThrow().apply {
                val insertIndex = indexOfFirstInstructionReversedOrThrow {
                    getReference<MethodReference>()?.name == "setBackgroundColor"
                }
                val register = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                addInstructions(
                    insertIndex,
                    """
                        invoke-static { v$register }, $PLAYER_CLASS_DESCRIPTOR->getFullScreenBackgroundColor(I)I
                        move-result v$register
                        """,
                )
            }
        }

        // endregion

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: AMBIENT_MODE_CONTROLS"
            ),
            AMBIENT_MODE_CONTROL
        )

        // endregion

    }
}
