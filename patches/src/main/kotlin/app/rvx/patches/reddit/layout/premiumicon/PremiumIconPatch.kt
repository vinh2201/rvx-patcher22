package app.rvx.patches.reddit.layout.premiumicon

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.reddit.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.reddit.utils.compatibility.Constants.REDDIT_PACKAGE_NAME
import app.rvx.patches.reddit.utils.fix.signature.spoofSignaturePatch
import app.rvx.patches.reddit.utils.patch.PatchList.PREMIUM_ICON
import app.rvx.patches.reddit.utils.settings.updatePatchStatus
import app.rvx.util.fingerprint.methodOrThrow

@Suppress("unused")
val premiumIconPatch = bytecodePatch(
    PREMIUM_ICON.displayTitle,
    PREMIUM_ICON.summary,
) {
    compatibleWith(REDDIT_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(spoofSignaturePatch)

    apply {
        premiumIconFingerprint.methodOrThrow().addInstructions(
            0, """
                const/4 v0, 0x1
                return v0
                """
        )

        updatePatchStatus(PREMIUM_ICON)
    }
}
