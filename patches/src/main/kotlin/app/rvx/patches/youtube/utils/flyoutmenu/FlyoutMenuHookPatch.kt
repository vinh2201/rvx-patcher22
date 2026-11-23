package app.rvx.patches.youtube.utils.flyoutmenu

import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.rvx.patches.youtube.utils.playbackRateBottomSheetBuilderFingerprint
import app.rvx.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.rvx.util.addStaticFieldToExtension
import app.rvx.util.fingerprint.methodOrThrow

private const val EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/utils/VideoUtils;"

val flyoutMenuHookPatch = bytecodePatch(
    description = "flyoutMenuHookPatch",
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(sharedResourceIdPatch)

    apply {
        playbackRateBottomSheetBuilderFingerprint.methodOrThrow().apply {
            val smaliInstructions =
                """
                    if-eqz v0, :ignore
                    invoke-virtual {v0}, $definingClass->$name()V
                    :ignore
                    return-void
                    """

            addStaticFieldToExtension(
                EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                "showYouTubeLegacyPlaybackSpeedFlyoutMenu",
                "playbackRateBottomSheetClass",
                definingClass,
                smaliInstructions
            )
        }

        videoQualityBottomSheetClassFingerprint.methodOrThrow().apply {
            val smaliInstructions =
                """
                    if-eqz v0, :ignore
                    const/4 v1, 0x1
                    invoke-virtual {v0, v1}, $definingClass->$name(Z)V
                    :ignore
                    return-void
                    """

            addStaticFieldToExtension(
                EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                "showYouTubeLegacyVideoQualityFlyoutMenu",
                "videoQualityBottomSheetClass",
                definingClass,
                smaliInstructions
            )
        }
    }
}
