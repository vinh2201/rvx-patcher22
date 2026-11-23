package app.rvx.patches.music.misc.tracking

import app.rvx.util.fingerprint.legacyFingerprint

/**
 * Sharing panel of Lyrics
 */
internal val imageShareLinkFormatterFingerprint = legacyFingerprint(
    name = "imageShareLinkFormatterFingerprint",
    returnType = "V",
    strings = listOf(
        "android.intent.extra.TEXT",
        "Image Uri is null.",
    )
)