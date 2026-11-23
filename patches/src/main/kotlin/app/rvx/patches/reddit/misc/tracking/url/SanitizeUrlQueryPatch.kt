package app.rvx.patches.reddit.misc.tracking.url

import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.extensions.ExternalLabel
import app.rvx.patches.reddit.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.reddit.utils.compatibility.Constants.REDDIT_PACKAGE_NAME
import app.rvx.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.rvx.patches.reddit.utils.patch.PatchList.SANITIZE_SHARING_LINKS
import app.rvx.patches.reddit.utils.settings.settingsPatch
import app.rvx.patches.reddit.utils.settings.updatePatchStatus
import app.rvx.util.fingerprint.methodOrThrow

private const val SANITIZE_METHOD_DESCRIPTOR =
    "$PATCHES_PATH/SanitizeUrlQueryPatch;->stripQueryParameters()Z"

@Suppress("unused")
val sanitizeUrlQueryPatch = bytecodePatch(
    SANITIZE_SHARING_LINKS.displayTitle,
    SANITIZE_SHARING_LINKS.summary,
) {
    compatibleWith(REDDIT_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(settingsPatch)

    apply {
        shareLinkFormatterFingerprint.methodOrThrow().apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $SANITIZE_METHOD_DESCRIPTOR
                    move-result v0
                    if-eqz v0, :off
                    return-object p0
                    """, ExternalLabel("off", getInstruction(0))
            )
        }

        updatePatchStatus(
            "enableSanitizeUrlQuery",
            SANITIZE_SHARING_LINKS
        )
    }
}
