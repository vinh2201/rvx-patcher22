package app.rvx.patches.reddit.misc.openlink

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.reddit.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.reddit.utils.compatibility.Constants.REDDIT_PACKAGE_NAME
import app.rvx.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.rvx.patches.reddit.utils.patch.PatchList.OPEN_LINKS_DIRECTLY
import app.rvx.patches.reddit.utils.settings.settingsPatch
import app.rvx.patches.reddit.utils.settings.updatePatchStatus

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$PATCHES_PATH/OpenLinksDirectlyPatch;" +
            "->" +
            "parseRedirectUri(Landroid/net/Uri;)Landroid/net/Uri;"

@Suppress("unused")
val openLinksDirectlyPatch = bytecodePatch(
    OPEN_LINKS_DIRECTLY.displayTitle,
    OPEN_LINKS_DIRECTLY.summary,
) {
    compatibleWith(REDDIT_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        settingsPatch,
        screenNavigatorMethodResolverPatch
    )

    apply {
        screenNavigatorMethod.addInstructions(
            0, """
                invoke-static {p2}, $EXTENSION_METHOD_DESCRIPTOR
                move-result-object p2
                """
        )

        updatePatchStatus(
            "enableOpenLinksDirectly",
            OPEN_LINKS_DIRECTLY
        )
    }
}
