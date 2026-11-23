package app.rvx.patches.youtube.utils.gms

import app.revanced.patcher.patch.Option
import app.rvx.patches.shared.gms.gmsCoreSupportPatch
import app.rvx.patches.shared.spoof.useragent.baseSpoofUserAgentPatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.sharedExtensionPatch
import app.rvx.patches.youtube.utils.fix.streamingdata.spoofStreamingDataPatch
import app.rvx.patches.youtube.utils.mainactivity.mainActivityFingerprint
import app.rvx.patches.youtube.utils.patch.PatchList.GMSCORE_SUPPORT
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.ResourceUtils.updateGmsCorePackageName
import app.rvx.patches.youtube.utils.settings.ResourceUtils.updatePackageName
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.util.valueOrThrow

@Suppress("unused")
val gmsCoreSupportPatch = gmsCoreSupportPatch(
    fromPackageName = YOUTUBE_PACKAGE_NAME,
    mainActivityOnCreateFingerprint = mainActivityFingerprint.second,
    extensionPatch = sharedExtensionPatch,
    gmsCoreSupportResourcePatchFactory = ::gmsCoreSupportResourcePatch,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))
}

private fun gmsCoreSupportResourcePatch(
    gmsCoreVendorGroupIdOption: Option<String>,
    packageNameYouTubeOption: Option<String>,
    packageNameYouTubeMusicOption: Option<String>,
) = app.rvx.patches.shared.gms.gmsCoreSupportResourcePatch(
    fromPackageName = YOUTUBE_PACKAGE_NAME,
    spoofedPackageSignature = "24bb24c05e47e0aefa68a58a766179d9b613a600",
    gmsCoreVendorGroupIdOption = gmsCoreVendorGroupIdOption,
    packageNameYouTubeOption = packageNameYouTubeOption,
    packageNameYouTubeMusicOption = packageNameYouTubeMusicOption,
    executeBlock = {
        updatePackageName(
            packageNameYouTubeOption.valueOrThrow()
        )
        updateGmsCorePackageName(
            "app.revanced",
            gmsCoreVendorGroupIdOption.valueOrThrow()
        )
        addPreference(
            arrayOf(
                "PREFERENCE: GMS_CORE_SETTINGS"
            ),
            GMSCORE_SUPPORT
        )
    },
) {
    dependsOn(
        baseSpoofUserAgentPatch(YOUTUBE_PACKAGE_NAME),
        spoofStreamingDataPatch,
        settingsPatch,
        accountCredentialsInvalidTextPatch,
    )
}
