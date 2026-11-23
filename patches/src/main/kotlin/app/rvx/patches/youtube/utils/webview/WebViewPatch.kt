package app.rvx.patches.youtube.utils.webview

import app.rvx.patches.shared.webview.webViewPatch
import app.rvx.patches.youtube.utils.extension.sharedExtensionPatch

val webViewPatch = webViewPatch(
    block = {
        dependsOn(sharedExtensionPatch)
    },
    targetActivityFingerprint = vrWelcomeActivityOnCreateFingerprint,
)
