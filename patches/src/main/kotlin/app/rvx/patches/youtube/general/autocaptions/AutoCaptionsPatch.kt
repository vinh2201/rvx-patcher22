package app.rvx.patches.youtube.general.autocaptions

import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.shared.captions.baseAutoCaptionsPatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.patch.PatchList.DISABLE_FORCED_AUTO_CAPTIONS
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val autoCaptionsPatch = bytecodePatch(
    DISABLE_FORCED_AUTO_CAPTIONS.displayTitle,
    DISABLE_FORCED_AUTO_CAPTIONS.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        baseAutoCaptionsPatch,
        settingsPatch,
    )

    apply {

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: DISABLE_AUTO_CAPTIONS"
            ),
            DISABLE_FORCED_AUTO_CAPTIONS
        )

        // endregion

    }
}
