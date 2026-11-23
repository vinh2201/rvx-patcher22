package app.rvx.patches.reddit.layout.branding.name

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.rvx.patches.reddit.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.reddit.utils.compatibility.Constants.REDDIT_PACKAGE_NAME
import app.rvx.patches.reddit.utils.fix.signature.spoofSignaturePatch
import app.rvx.patches.reddit.utils.patch.PatchList.CUSTOM_BRANDING_NAME_FOR_REDDIT
import app.rvx.patches.reddit.utils.settings.updatePatchStatus
import app.rvx.util.Utils.printInfo
import app.rvx.util.valueOrThrow
import java.io.FileWriter
import java.nio.file.Files

private const val ORIGINAL_APP_NAME = "Reddit"
private const val APP_NAME = "RVX Reddit"

@Suppress("unused")
val customBrandingNamePatch = resourcePatch(
    CUSTOM_BRANDING_NAME_FOR_REDDIT.displayTitle,
    CUSTOM_BRANDING_NAME_FOR_REDDIT.summary,
    false,
) {
    compatibleWith(REDDIT_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(spoofSignaturePatch)

    val appNameOption = stringOption(
        default = ORIGINAL_APP_NAME,
        values = mapOf(
            "Default" to APP_NAME,
            "Original" to ORIGINAL_APP_NAME,
        ),
        name = "App name",
        description = "The name of the app.",
        required = true
    )

    apply patchApply@{
        val appName = appNameOption
            .valueOrThrow()

        if (appName == ORIGINAL_APP_NAME) {
            printInfo("App name will remain unchanged as it matches the original.")
            return@patchApply
        }

        val resDirectory = get("res")

        val valuesV24Directory = resDirectory.resolve("values-v24")
        if (!valuesV24Directory.isDirectory)
            Files.createDirectories(valuesV24Directory.toPath())

        val stringsXml = valuesV24Directory.resolve("strings.xml")

        if (!stringsXml.exists()) {
            FileWriter(stringsXml).use {
                it.write("<?xml version=\"1.0\" encoding=\"utf-8\"?><resources></resources>")
            }
        }

        document("res/values-v24/strings.xml").use { document ->
            mapOf(
                "app_name" to appName
            ).forEach { (k, v) ->
                val stringElement = document.createElement("string")

                stringElement.setAttribute("name", k)
                stringElement.textContent = v

                document.getElementsByTagName("resources").item(0).appendChild(stringElement)
            }
        }

        updatePatchStatus(CUSTOM_BRANDING_NAME_FOR_REDDIT)
    }
}
