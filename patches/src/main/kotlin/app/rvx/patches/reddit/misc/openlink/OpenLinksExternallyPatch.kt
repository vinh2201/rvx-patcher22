package app.rvx.patches.reddit.misc.openlink

import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.extensions.ExternalLabel
import app.rvx.patches.reddit.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.reddit.utils.compatibility.Constants.REDDIT_PACKAGE_NAME
import app.rvx.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.rvx.patches.reddit.utils.patch.PatchList.OPEN_LINKS_EXTERNALLY
import app.rvx.patches.reddit.utils.settings.settingsPatch
import app.rvx.patches.reddit.utils.settings.updatePatchStatus
import app.rvx.util.indexOfFirstStringInstructionOrThrow

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$PATCHES_PATH/OpenLinksExternallyPatch;" +
            "->" +
            "openLinksExternally(Landroid/app/Activity;Landroid/net/Uri;)Z"

@Suppress("unused")
val openLinksExternallyPatch = bytecodePatch(
    OPEN_LINKS_EXTERNALLY.displayTitle,
    OPEN_LINKS_EXTERNALLY.summary,
) {
    compatibleWith(REDDIT_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        settingsPatch,
        screenNavigatorMethodResolverPatch
    )

    apply {
        screenNavigatorMethod.apply {
            val insertIndex = indexOfFirstStringInstructionOrThrow("uri") + 2

            addInstructionsWithLabels(
                insertIndex, """
                    invoke-static {p1, p2}, $EXTENSION_METHOD_DESCRIPTOR
                    move-result v0
                    if-eqz v0, :dismiss
                    return-void
                    """, ExternalLabel("dismiss", getInstruction(insertIndex))
            )
        }

        updatePatchStatus(
            "enableOpenLinksExternally",
            OPEN_LINKS_EXTERNALLY
        )
    }
}
