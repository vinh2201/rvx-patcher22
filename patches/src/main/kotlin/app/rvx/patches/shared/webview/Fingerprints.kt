package app.rvx.patches.shared.webview

import app.rvx.util.fingerprint.legacyFingerprint

internal val webViewHostActivityOnCreateFingerprint = legacyFingerprint(
    name = "webViewHostActivityOnCreateFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { method, classDef ->
        classDef.endsWith("/WebViewHostActivity;") && method.name == "onCreate"
    }
)