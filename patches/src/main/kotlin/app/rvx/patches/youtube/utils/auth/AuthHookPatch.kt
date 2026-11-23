package app.rvx.patches.youtube.utils.auth

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.shared.extension.Constants.EXTENSION_PATH
import app.rvx.patches.youtube.utils.extension.sharedExtensionPatch
import app.rvx.patches.youtube.utils.request.buildRequestPatch
import app.rvx.patches.youtube.utils.request.hookBuildRequest
import app.rvx.patches.youtube.utils.request.hookInitPlaybackBuildRequest
import app.rvx.util.fingerprint.methodOrThrow

private const val EXTENSION_AUTH_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/innertube/utils/AuthUtils;"

val authHookPatch = bytecodePatch(
    description = "authHookPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        buildRequestPatch,
    )

    apply {
        // Get incognito status and data sync id.
        accountIdentityFingerprint.methodOrThrow().addInstruction(
            1,
            "invoke-static {p3, p4}, $EXTENSION_AUTH_UTILS_CLASS_DESCRIPTOR->setAccountIdentity(Ljava/lang/String;Z)V"
        )

        // Get the header to use the auth token.
        hookBuildRequest("$EXTENSION_AUTH_UTILS_CLASS_DESCRIPTOR->setRequestHeaders(Ljava/lang/String;Ljava/util/Map;)V")
        hookInitPlaybackBuildRequest("$EXTENSION_AUTH_UTILS_CLASS_DESCRIPTOR->setRequestHeaders(Ljava/lang/String;Ljava/util/Map;)V")
    }
}