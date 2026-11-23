package app.rvx.patches.youtube.alternative.thumbnails

import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.shared.imageurl.addImageUrlHook
import app.rvx.patches.shared.imageurl.cronetImageUrlHookPatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.patch.PatchList.BYPASS_IMAGE_REGION_RESTRICTIONS
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val bypassImageRegionRestrictionsPatch = bytecodePatch(
    BYPASS_IMAGE_REGION_RESTRICTIONS.displayTitle,
    BYPASS_IMAGE_REGION_RESTRICTIONS.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        cronetImageUrlHookPatch(true),
        settingsPatch,
    )
    apply {

        addImageUrlHook()

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: ALTERNATIVE_THUMBNAILS",
                "SETTINGS: BYPASS_IMAGE_REGION_RESTRICTIONS"
            ),
            BYPASS_IMAGE_REGION_RESTRICTIONS
        )

        // endregion

    }
}
