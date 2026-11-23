package app.rvx.patches.youtube.player.hapticfeedback

import app.rvx.util.fingerprint.Fingerprint
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.extensions.ExternalLabel
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.rvx.patches.youtube.utils.patch.PatchList.DISABLE_HAPTIC_FEEDBACK
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.util.fingerprint.matchOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val hapticFeedbackPatch = bytecodePatch(
    DISABLE_HAPTIC_FEEDBACK.displayTitle,
    DISABLE_HAPTIC_FEEDBACK.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(settingsPatch)

    apply {
        fun Pair<String, Fingerprint>.hookHapticFeedback(methodName: String) =
            matchOrThrow().let {
                it.method.apply {
                    var index = 0
                    var register = 0

                    if (name == "run") {
                        val stringIndex = it.stringMatches!!.first().index
                        index = indexOfFirstInstructionReversedOrThrow(stringIndex) {
                            opcode == Opcode.SGET &&
                                    getReference<FieldReference>()?.toString() == "Landroid/os/Build${'$'}VERSION;->SDK_INT:I"
                        }
                        register = getInstruction<OneRegisterInstruction>(index).registerA
                    }

                    addInstructionsWithLabels(
                        index, """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->$methodName()Z
                        move-result v$register
                        if-eqz v$register, :vibrate
                        return-void
                        """, ExternalLabel("vibrate", getInstruction(index))
                    )
                }
            }

        arrayOf(
            seekHapticsFingerprint to "disableSeekVibrate",
            seekUndoHapticsFingerprint to "disableSeekUndoVibrate",
            scrubbingHapticsFingerprint to "disableScrubbingVibrate",
            markerHapticsFingerprint to "disableChapterVibrate",
            zoomHapticsFingerprint to "disableZoomVibrate"
        ).map { (fingerprint, methodName) ->
            fingerprint.hookHapticFeedback(methodName)
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: DISABLE_HAPTIC_FEEDBACK"
            ),
            DISABLE_HAPTIC_FEEDBACK
        )

        // endregion

    }
}
