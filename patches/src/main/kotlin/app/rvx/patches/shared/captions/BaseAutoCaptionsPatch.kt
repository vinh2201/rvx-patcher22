package app.rvx.patches.shared.captions

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.extensions.ExternalLabel
import app.rvx.patches.shared.extension.Constants.PATCHES_PATH
import app.rvx.patches.shared.startVideoInformerFingerprint
import app.rvx.util.fingerprint.methodOrThrow

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/AutoCaptionsPatch;"

val baseAutoCaptionsPatch = bytecodePatch(
    description = "baseAutoCaptionsPatch"
) {
    apply {
        subtitleTrackFingerprint.methodOrThrow().apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->disableAutoCaptions()Z
                    move-result v0
                    if-eqz v0, :disabled
                    const/4 v0, 0x1
                    return v0
                    """, ExternalLabel("disabled", getInstruction(0))
            )
        }

        mapOf(
            startVideoInformerFingerprint to 0,
            storyboardRendererDecoderRecommendedLevelFingerprint to 1
        ).forEach { (fingerprint, enabled) ->
            fingerprint.methodOrThrow().addInstructions(
                0, """
                    const/4 v0, 0x$enabled
                    invoke-static {v0}, $EXTENSION_CLASS_DESCRIPTOR->setCaptionsButtonStatus(Z)V
                    """
            )
        }
    }
}

