package app.rvx.patches.music.general.audiotracks

import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.patch.PatchList.DISABLE_FORCED_AUTO_AUDIO_TRACKS
import app.rvx.patches.music.utils.playservice.is_8_12_or_greater
import app.rvx.patches.music.utils.playservice.versionCheckPatch
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.addSwitchPreference
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.patches.shared.audiotracks.audioTracksPatch

@Suppress("unused")
val audioTracksPatch = audioTracksPatch(
    block = {
        compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

        dependsOn(
            settingsPatch,
            versionCheckPatch,
        )
    },
    executeBlock = {
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_disable_auto_audio_tracks",
            "true"
        )

        updatePatchStatus(DISABLE_FORCED_AUTO_AUDIO_TRACKS)
    },
    fixUseLocalizedAudioTrackFlag = is_8_12_or_greater
)
