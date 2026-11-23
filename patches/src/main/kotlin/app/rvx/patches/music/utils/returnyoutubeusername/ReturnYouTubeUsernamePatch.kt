package app.rvx.patches.music.utils.returnyoutubeusername

import app.revanced.patcher.patch.resourcePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.patch.PatchList.RETURN_YOUTUBE_USERNAME
import app.rvx.patches.music.utils.playservice.is_6_42_or_greater
import app.rvx.patches.music.utils.playservice.versionCheckPatch
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.addPreferenceWithIntent
import app.rvx.patches.music.utils.settings.addSwitchPreference
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.patches.shared.returnyoutubeusername.baseReturnYouTubeUsernamePatch

@Suppress("unused")
val returnYouTubeUsernamePatch = resourcePatch(
    RETURN_YOUTUBE_USERNAME.displayTitle,
    RETURN_YOUTUBE_USERNAME.summary,
    use = false,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        baseReturnYouTubeUsernamePatch,
        settingsPatch,
        versionCheckPatch,
    )

    apply {
        addSwitchPreference(
            CategoryType.RETURN_YOUTUBE_USERNAME,
            "revanced_return_youtube_username_enabled",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.RETURN_YOUTUBE_USERNAME,
            "revanced_return_youtube_username_display_format",
            "revanced_return_youtube_username_enabled"
        )
        addPreferenceWithIntent(
            CategoryType.RETURN_YOUTUBE_USERNAME,
            "revanced_return_youtube_username_youtube_data_api_v3_developer_key",
            "revanced_return_youtube_username_enabled"
        )
        if (is_6_42_or_greater) {
            addPreferenceWithIntent(
                CategoryType.RETURN_YOUTUBE_USERNAME,
                "revanced_return_youtube_username_youtube_data_api_v3_about"
            )
        }

        updatePatchStatus(RETURN_YOUTUBE_USERNAME)

    }
}