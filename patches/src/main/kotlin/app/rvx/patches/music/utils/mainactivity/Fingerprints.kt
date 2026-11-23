package app.rvx.patches.music.utils.mainactivity

import app.rvx.util.fingerprint.legacyFingerprint

internal val mainActivityFingerprint = legacyFingerprint(
    name = "mainActivityFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    strings = listOf(
        "android.intent.action.MAIN",
        "FEmusic_home"
    ),
    customFingerprint = { method, classDef ->
        method.name == "onCreate" && classDef.endsWith("Activity;")
    }
)
