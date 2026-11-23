package app.rvx.patches.music.misc.backgroundplayback

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.patch.PatchList.REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.util.fingerprint.matchOrThrow
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.fingerprint.resolvable
import app.rvx.util.getReference
import app.rvx.util.getWalkerMethod
import app.rvx.util.indexOfFirstInstructionOrThrow
import app.rvx.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val backgroundPlaybackPatch = bytecodePatch(
    REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS.displayTitle,
    REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS.summary,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(settingsPatch)

    apply {
        // region patch for background play

        backgroundPlaybackManagerFingerprint.methodOrThrow().addInstructions(
            0, """
                const/4 v0, 0x1
                return v0
                """
        )

        // endregion

        // region patch for exclusive audio playback

        // don't play music video
        musicBrowserServiceFingerprint.matchOrThrow().let {
            it.method.apply {
                val stringIndex = it.stringMatches!!.first().index
                val targetIndex = indexOfFirstInstructionOrThrow(stringIndex) {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.returnType == "Z" &&
                            reference.parameterTypes.size == 0
                }

                getWalkerMethod(targetIndex).addInstructions(
                    0, """
                        const/4 v0, 0x1
                        return v0
                        """
                )
            }
        }

        // don't play podcast videos
        // enable by default from YouTube Music 7.05.52+

        if (podCastConfigFingerprint.resolvable() &&
            dataSavingSettingsFragmentFingerprint.resolvable()
        ) {
            podCastConfigFingerprint.methodOrThrow().apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "const/4 v$targetRegister, 0x1"
                )
            }

            dataSavingSettingsFragmentFingerprint.methodOrThrow().apply {
                val insertIndex =
                    indexOfFirstStringInstructionOrThrow("pref_key_dont_play_nma_video") + 4
                val targetRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                addInstruction(
                    insertIndex,
                    "const/4 v$targetRegister, 0x1"
                )
            }
        }

        // endregion

        // region patch for minimized playback

        kidsBackgroundPlaybackPolicyControllerFingerprint.methodOrThrow().addInstruction(
            0, "return-void"
        )

        // endregion

        updatePatchStatus(REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS)

    }
}
