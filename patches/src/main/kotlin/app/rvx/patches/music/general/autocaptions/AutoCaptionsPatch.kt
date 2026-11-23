package app.rvx.patches.music.general.autocaptions

import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.patch.PatchList.DISABLE_FORCED_AUTO_CAPTIONS
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.addSwitchPreference
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.patches.shared.captions.baseAutoCaptionsPatch

@Suppress("unused")
val autoCaptionsPatch = bytecodePatch(
    DISABLE_FORCED_AUTO_CAPTIONS.displayTitle,
    DISABLE_FORCED_AUTO_CAPTIONS.summary,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        baseAutoCaptionsPatch,
        settingsPatch
    )

    apply {
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_disable_auto_captions",
            "false"
        )

        updatePatchStatus(DISABLE_FORCED_AUTO_CAPTIONS)

    }
}