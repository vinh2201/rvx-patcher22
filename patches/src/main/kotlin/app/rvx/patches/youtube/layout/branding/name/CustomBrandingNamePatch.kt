package app.rvx.patches.youtube.layout.branding.name

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.patch.PatchList.CUSTOM_BRANDING_NAME_FOR_YOUTUBE
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.util.removeStringsElements
import app.rvx.util.valueOrThrow

private const val APP_NAME = "RVX"

@Suppress("unused")
val customBrandingNamePatch = resourcePatch(
    CUSTOM_BRANDING_NAME_FOR_YOUTUBE.displayTitle,
    CUSTOM_BRANDING_NAME_FOR_YOUTUBE.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(settingsPatch)

    val appNameOption = stringOption(
        default = APP_NAME,
        values = mapOf(
            "ReVanced Extended" to "ReVanced Extended",
            "RVX" to APP_NAME,
            "YouTube RVX" to "YouTube RVX",
            "YouTube" to "YouTube",
        ),
        name = "App name",
        description = "The name of the app.",
        required = true,
    )

    apply {
        // Check patch options first.
        val appName = appNameOption
            .valueOrThrow()

        removeStringsElements(
            arrayOf("application_name")
        )

        document("res/values/strings.xml").use { document ->
            val stringElement = document.createElement("string")

            stringElement.setAttribute("name", "application_name")
            stringElement.textContent = appName

            document.getElementsByTagName("resources").item(0)
                .appendChild(stringElement)
        }

    }
}
