package app.rvx.patches.youtube.misc.watchhistory

import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.shared.trackingurlhook.hookWatchHistory
import app.rvx.patches.shared.trackingurlhook.trackingUrlHookPatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.patch.PatchList.WATCH_HISTORY
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val watchHistoryPatch = bytecodePatch(
    WATCH_HISTORY.displayTitle,
    WATCH_HISTORY.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        settingsPatch,
        trackingUrlHookPatch,
    )

    apply {

        hookWatchHistory()

        // region add settings

        addPreference(
            arrayOf(
                "SETTINGS: WATCH_HISTORY"
            ),
            WATCH_HISTORY
        )

        // endregion

    }
}
