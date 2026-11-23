package app.rvx.patches.youtube.general.audiotracks

import app.rvx.patches.shared.audiotracks.audioTracksPatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.patch.PatchList.DISABLE_FORCED_AUTO_AUDIO_TRACKS
import app.rvx.patches.youtube.utils.playservice.is_20_07_or_greater
import app.rvx.patches.youtube.utils.playservice.versionCheckPatch
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val audioTracksPatch = audioTracksPatch(
    block = {
        compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

        dependsOn(
            settingsPatch,
            versionCheckPatch,
        )
    },
    executeBlock = {
        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: DISABLE_AUTO_AUDIO_TRACKS"
            ),
            DISABLE_FORCED_AUTO_AUDIO_TRACKS
        )

        // endregion
    },
    fixUseLocalizedAudioTrackFlag = is_20_07_or_greater
)
