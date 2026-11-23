package app.rvx.patches.music.misc.watchhistory

import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.patch.PatchList.WATCH_HISTORY
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.addPreferenceWithIntent
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.patches.shared.trackingurlhook.hookWatchHistory
import app.rvx.patches.shared.trackingurlhook.trackingUrlHookPatch

@Suppress("unused")
val watchHistoryPatch = bytecodePatch(
    WATCH_HISTORY.displayTitle,
    WATCH_HISTORY.summary,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        settingsPatch,
        trackingUrlHookPatch,
    )

    apply {
        hookWatchHistory()

        addPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_watch_history_type"
        )
    }

}