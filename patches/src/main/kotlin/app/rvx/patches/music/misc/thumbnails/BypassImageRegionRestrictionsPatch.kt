package app.rvx.patches.music.misc.thumbnails

import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.patch.PatchList.BYPASS_IMAGE_REGION_RESTRICTIONS
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.addSwitchPreference
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.patches.shared.imageurl.addImageUrlHook
import app.rvx.patches.shared.imageurl.cronetImageUrlHookPatch

@Suppress("unused")
val bypassImageRegionRestrictionsPatch = bytecodePatch(
    BYPASS_IMAGE_REGION_RESTRICTIONS.displayTitle,
    BYPASS_IMAGE_REGION_RESTRICTIONS.summary,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        settingsPatch,
        cronetImageUrlHookPatch(false)
    )

    apply {
        addImageUrlHook()

        addSwitchPreference(
            CategoryType.MISC,
            "revanced_bypass_image_region_restrictions",
            "false"
        )

        updatePatchStatus(BYPASS_IMAGE_REGION_RESTRICTIONS)

    }
}