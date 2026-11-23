package app.rvx.patches.shared.spoof.useragent

import app.rvx.util.fingerprint.legacyFingerprint
import app.rvx.util.or
import com.android.tools.smali.dexlib2.AccessFlags

const val CLIENT_PACKAGE_NAME = "cbr"

internal val apiStatsFingerprint = legacyFingerprint(
    name = "apiStatsFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    strings = listOf(CLIENT_PACKAGE_NAME),
)
