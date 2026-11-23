package app.rvx.patches.youtube.utils.extension.hooks

import app.rvx.patches.shared.extension.extensionHook
import app.rvx.util.fingerprint.legacyFingerprint

/**
 * Hooks the context when the app is launched as a regular application (and is not an embedded video playback).
 */
// Extension context is the Activity itself.
internal val applicationInitHook = extensionHook(
    fingerprint = legacyFingerprint(
        name = "applicationInitHook",
        strings = listOf("Application creation", "Application.onCreate")
    )
)
