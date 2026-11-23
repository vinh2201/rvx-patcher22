package app.rvx.patches.youtube.utils.returnyoutubeusername

import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.shared.returnyoutubeusername.baseReturnYouTubeUsernamePatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.patch.PatchList.RETURN_YOUTUBE_USERNAME
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val returnYouTubeUsernamePatch = bytecodePatch(
    RETURN_YOUTUBE_USERNAME.displayTitle,
    RETURN_YOUTUBE_USERNAME.summary,
    false,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        baseReturnYouTubeUsernamePatch,
        settingsPatch,
    )

    apply {

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: RETURN_YOUTUBE_USERNAME"
            ),
            RETURN_YOUTUBE_USERNAME
        )

        // endregion
    }
}
