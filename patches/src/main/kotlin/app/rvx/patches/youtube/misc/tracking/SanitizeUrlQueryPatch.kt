package app.rvx.patches.youtube.misc.tracking

import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.shared.tracking.baseSanitizeUrlQueryPatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.patch.PatchList.SANITIZE_SHARING_LINKS
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val sanitizeUrlQueryPatch = bytecodePatch(
    SANITIZE_SHARING_LINKS.displayTitle,
    SANITIZE_SHARING_LINKS.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        baseSanitizeUrlQueryPatch,
        settingsPatch,
    )

    apply {

        // region add settings

        addPreference(
            arrayOf(
                "SETTINGS: SANITIZE_SHARING_LINKS"
            ),
            SANITIZE_SHARING_LINKS
        )

        // endregion

    }
}
