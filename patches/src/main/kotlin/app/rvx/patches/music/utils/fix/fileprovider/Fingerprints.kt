package app.rvx.patches.music.utils.fix.fileprovider

import app.rvx.util.fingerprint.legacyFingerprint

internal val fileProviderResolverFingerprint = legacyFingerprint(
    name = "fileProviderResolverFingerprint",
    returnType = "L",
    strings = listOf(
        "android.support.FILE_PROVIDER_PATHS",
        "Name must not be empty"
    )
)