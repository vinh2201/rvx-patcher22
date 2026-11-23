package app.rvx.patches.youtube.misc.debugging

import app.rvx.util.fingerprint.legacyFingerprint
import app.rvx.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val currentWatchNextResponseFingerprint = legacyFingerprint(
    name = "currentWatchNextResponseFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC or AccessFlags.FINAL,
    parameters = listOf("L"),
)

internal val currentWatchNextResponseParentFingerprint = legacyFingerprint(
    name = "currentWatchNextResponseParentFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("currentWatchNextResponse")
)
