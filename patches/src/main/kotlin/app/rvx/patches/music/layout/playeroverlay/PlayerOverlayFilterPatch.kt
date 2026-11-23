package app.rvx.patches.music.layout.playeroverlay

import app.revanced.patcher.patch.resourcePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.patch.PatchList.HIDE_PLAYER_OVERLAY_FILTER
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.util.removeOverlayBackground

@Suppress("unused")
val playerOverlayFilterPatch = resourcePatch(
    HIDE_PLAYER_OVERLAY_FILTER.displayTitle,
    HIDE_PLAYER_OVERLAY_FILTER.summary,
    use = false,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    apply {
        removeOverlayBackground(
            arrayOf("music_controls_overlay.xml"),
            arrayOf("player_control_screen")
        )

        updatePatchStatus(HIDE_PLAYER_OVERLAY_FILTER)

    }
}

