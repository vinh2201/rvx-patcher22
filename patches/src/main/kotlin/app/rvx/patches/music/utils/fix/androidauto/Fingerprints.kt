package app.rvx.patches.music.utils.fix.androidauto

import app.rvx.util.fingerprint.legacyFingerprint

internal val certificateCheckFingerprint = legacyFingerprint(
    name = "certificateCheckFingerprint",
    returnType = "Z",
    parameters = listOf("L"),
    strings = listOf("X509")
)