package app.rvx.patches.youtube.player.action

import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.shared.litho.addLithoFilter
import app.rvx.patches.shared.litho.lithoFilterPatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.componentlist.hookElementList
import app.rvx.patches.youtube.utils.componentlist.lazilyConvertedElementHookPatch
import app.rvx.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.rvx.patches.youtube.utils.extension.Constants.PLAYER_PATH
import app.rvx.patches.youtube.utils.fix.hype.hypeButtonIconPatch
import app.rvx.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.rvx.patches.youtube.utils.patch.PatchList.HIDE_ACTION_BUTTONS
import app.rvx.patches.youtube.utils.request.buildRequestPatch
import app.rvx.patches.youtube.utils.request.hookBuildRequest
import app.rvx.patches.youtube.utils.request.hookInitPlaybackBuildRequest
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.patches.youtube.video.information.videoInformationPatch
import app.rvx.patches.youtube.video.videoid.videoIdPatch

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ActionButtonsFilter;"
private const val ACTION_BUTTONS_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/ActionButtonsPatch;"

@Suppress("unused")
val actionButtonsPatch = bytecodePatch(
    HIDE_ACTION_BUTTONS.displayTitle,
    HIDE_ACTION_BUTTONS.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        lazilyConvertedElementHookPatch,
        videoInformationPatch,
        videoIdPatch,
        buildRequestPatch,
        hypeButtonIconPatch,
    )

    apply {
        addLithoFilter(FILTER_CLASS_DESCRIPTOR)

        // region patch for hide action buttons by index

        hookBuildRequest("$ACTION_BUTTONS_CLASS_DESCRIPTOR->fetchRequest(Ljava/lang/String;Ljava/util/Map;)V")
        hookElementList("$ACTION_BUTTONS_CLASS_DESCRIPTOR->hideActionButtonByIndex")
        hookInitPlaybackBuildRequest("$ACTION_BUTTONS_CLASS_DESCRIPTOR->fetchRequest(Ljava/lang/String;Ljava/util/Map;)V")

        // endregion

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: HIDE_ACTION_BUTTONS"
            ),
            HIDE_ACTION_BUTTONS
        )

        // endregion

    }
}
