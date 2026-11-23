package app.rvx.patches.music.general.spoofappversion

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.rvx.patches.music.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.rvx.patches.music.utils.extension.sharedExtensionPatch
import app.rvx.patches.music.utils.patch.PatchList.SPOOF_APP_VERSION
import app.rvx.patches.music.utils.playservice.is_6_36_or_greater
import app.rvx.patches.music.utils.playservice.is_6_43_or_greater
import app.rvx.patches.music.utils.playservice.is_7_25_or_greater
import app.rvx.patches.music.utils.playservice.versionCheckPatch
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.addPreferenceWithIntent
import app.rvx.patches.music.utils.settings.addSwitchPreference
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.patches.shared.spoof.appversion.baseSpoofAppVersionPatch
import app.rvx.patches.shared.spoof.watchnext.spoofAppVersionWatchNextPatch
import app.rvx.util.Utils.printWarn
import app.rvx.util.appendAppVersion
import app.rvx.util.findMethodOrThrow
import app.rvx.util.returnEarly

private val spoofAppVersionWatchNextPatch = spoofAppVersionWatchNextPatch(
    block = {
        dependsOn(
            sharedExtensionPatch,
            versionCheckPatch
        )
    },
    patchRequired = {
        is_7_25_or_greater
    },
    availabilityDescriptor = "$GENERAL_CLASS_DESCRIPTOR->spoofWatchNextEndpointAppVersionEnabled()Z",
    appVersionDescriptor = "$GENERAL_CLASS_DESCRIPTOR->getWatchNextEndpointVersionOverride()Ljava/lang/String;"
)

private val spoofAppVersionBytecodePatch = bytecodePatch(
    description = "spoofAppVersionBytecodePatch"
) {
    dependsOn(
        baseSpoofAppVersionPatch("$GENERAL_CLASS_DESCRIPTOR->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"),
        versionCheckPatch,
    )

    apply patchApply@{
        if (!is_6_36_or_greater) {
            return@patchApply
        }

        val defaultVersionString = if (is_7_25_or_greater)
            "6.42.55" else "6.35.52"

        findMethodOrThrow(PATCH_STATUS_CLASS_DESCRIPTOR) {
            name == "SpoofAppVersionDefaultString"
        }.returnEarly(defaultVersionString)
    }
}

@Suppress("unused")
val spoofAppVersionPatch = resourcePatch(
    // SPOOF_APP_VERSION.title,
    // SPOOF_APP_VERSION.summary,
) {
    compatibleWith(
        YOUTUBE_MUSIC_PACKAGE_NAME(
            "6.42.55",
            "6.51.53",
            "7.16.53",
            "7.25.53",
            "8.12.54",
            "8.28.54",
            "8.30.54",
        ),
    )

    dependsOn(
        spoofAppVersionBytecodePatch,
        spoofAppVersionWatchNextPatch,
        settingsPatch,
        versionCheckPatch,
    )

    apply patchApply@{
        if (!is_6_36_or_greater) {
            printWarn("\"${SPOOF_APP_VERSION.title}\" is not supported in this version. Use YouTube Music 6.36.54 or later.")
            return@patchApply
        }
        if (is_7_25_or_greater) {
            appendAppVersion("7.17.52")
        }
        if (is_6_43_or_greater) {
            appendAppVersion("6.42.55")
        }
        if (!is_7_25_or_greater) {
            appendAppVersion("6.35.52")
        }

        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_spoof_app_version",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.GENERAL,
            "revanced_spoof_app_version_target",
            "revanced_spoof_app_version"
        )

        updatePatchStatus(SPOOF_APP_VERSION)

    }
}
