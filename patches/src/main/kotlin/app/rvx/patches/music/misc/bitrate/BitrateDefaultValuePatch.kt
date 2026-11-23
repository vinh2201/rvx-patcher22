package app.rvx.patches.music.misc.bitrate

import app.revanced.patcher.patch.resourcePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.patch.PatchList.BITRATE_DEFAULT_VALUE
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.settingsPatch

@Suppress("unused")
val bitrateDefaultValuePatch = resourcePatch(
    BITRATE_DEFAULT_VALUE.displayTitle,
    BITRATE_DEFAULT_VALUE.summary,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(settingsPatch)

    apply {
        document("res/xml/data_saving_settings.xml").use { document ->
            document.getElementsByTagName("com.google.android.apps.youtube.music.ui.preference.PreferenceCategoryCompat")
                .item(0).childNodes.apply {
                    arrayOf("BitrateAudioMobile", "BitrateAudioWiFi").forEach {
                        for (i in 1 until length) {
                            val view = item(i)
                            if (
                                view.hasAttributes() &&
                                view.attributes.getNamedItem("android:key").nodeValue.endsWith(it)
                            ) {
                                view.attributes.getNamedItem("android:defaultValue").nodeValue =
                                    "Always High"
                                break
                            }
                        }
                    }
                }
        }

        updatePatchStatus(BITRATE_DEFAULT_VALUE)

    }
}