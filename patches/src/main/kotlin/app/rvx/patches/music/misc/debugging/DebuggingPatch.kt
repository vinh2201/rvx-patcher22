package app.rvx.patches.music.misc.debugging

import app.revanced.patcher.patch.resourcePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.patch.PatchList.ENABLE_DEBUG_LOGGING
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.addSwitchPreference
import app.rvx.patches.music.utils.settings.settingsPatch

@Suppress("unused")
val debuggingPatch = resourcePatch(
    ENABLE_DEBUG_LOGGING.displayTitle,
    ENABLE_DEBUG_LOGGING.summary,
    // Unlike YouTube, YouTube Music's Litho components do not change very often.
    // That's why it seems better to selectively include patches for only those users who need them.
    false,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(settingsPatch)

    apply {
        addSwitchPreference(
            CategoryType.MISC,
            "revanced_debug",
            "false"
        )
        addSwitchPreference(
            CategoryType.MISC,
            "revanced_debug_protobuffer",
            "false",
            "revanced_debug"
        )
        addSwitchPreference(
            CategoryType.MISC,
            "revanced_debug_spannable",
            "false",
            "revanced_debug"
        )

        updatePatchStatus(ENABLE_DEBUG_LOGGING)

    }
}
