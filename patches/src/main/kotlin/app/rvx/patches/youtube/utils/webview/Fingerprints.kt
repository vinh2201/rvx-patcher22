package app.rvx.patches.youtube.utils.webview

import app.rvx.util.fingerprint.legacyFingerprint

internal val vrWelcomeActivityOnCreateFingerprint = legacyFingerprint(
    name = "vrWelcomeActivityOnCreateFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { method, classDef ->
        classDef.type == "Lcom/google/android/libraries/youtube/player/features/gl/vr/VrWelcomeActivity;"
                && method.name == "onCreate"
    }
)
