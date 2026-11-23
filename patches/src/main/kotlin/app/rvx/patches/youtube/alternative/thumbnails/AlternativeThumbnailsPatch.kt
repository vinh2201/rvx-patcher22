package app.rvx.patches.youtube.alternative.thumbnails

import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.shared.imageurl.addImageUrlErrorCallbackHook
import app.rvx.patches.shared.imageurl.addImageUrlHook
import app.rvx.patches.shared.imageurl.addImageUrlSuccessCallbackHook
import app.rvx.patches.shared.imageurl.cronetImageUrlHookPatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.Constants.ALTERNATIVE_THUMBNAILS_CLASS_DESCRIPTOR
import app.rvx.patches.youtube.utils.navigation.navigationBarHookPatch
import app.rvx.patches.youtube.utils.patch.PatchList.ALTERNATIVE_THUMBNAILS
import app.rvx.patches.youtube.utils.playertype.playerTypeHookPatch
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val alternativeThumbnailsPatch = bytecodePatch(
    ALTERNATIVE_THUMBNAILS.displayTitle,
    ALTERNATIVE_THUMBNAILS.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        cronetImageUrlHookPatch(true),
        navigationBarHookPatch,
        playerTypeHookPatch,
        settingsPatch,
    )
    apply {

        addImageUrlHook(ALTERNATIVE_THUMBNAILS_CLASS_DESCRIPTOR)
        addImageUrlSuccessCallbackHook(ALTERNATIVE_THUMBNAILS_CLASS_DESCRIPTOR)
        addImageUrlErrorCallbackHook(ALTERNATIVE_THUMBNAILS_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: ALTERNATIVE_THUMBNAILS",
                "SETTINGS: ALTERNATIVE_THUMBNAILS"
            ),
            ALTERNATIVE_THUMBNAILS
        )

        // endregion

    }
}
