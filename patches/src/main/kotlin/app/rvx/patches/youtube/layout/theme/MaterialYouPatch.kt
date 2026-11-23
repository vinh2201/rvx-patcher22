package app.rvx.patches.youtube.layout.theme

import app.revanced.patcher.patch.resourcePatch
import app.rvx.patches.shared.materialyou.baseMaterialYou
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.patch.PatchList.MATERIALYOU
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusTheme
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.util.copyXmlNode

@Suppress("unused")
val materialYouPatch = resourcePatch(
    MATERIALYOU.displayTitle,
    MATERIALYOU.summary,
    false,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        sharedThemePatch,
        settingsPatch,
    )

    apply {
        baseMaterialYou()

        copyXmlNode("youtube/materialyou/host", "values-v31/colors.xml", "resources")

        updatePatchStatusTheme("MaterialYou")

        addPreference(MATERIALYOU)

    }
}
