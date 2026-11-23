package app.rvx.patches.reddit.utils.extension.hooks

import app.rvx.patches.shared.extension.extensionHook
import app.rvx.util.fingerprint.legacyFingerprint

internal val applicationInitHook = extensionHook(
    fingerprint = legacyFingerprint(
        name = "applicationInitHook",
        customFingerprint = { method, _ ->
            method.definingClass.endsWith("/FrontpageApplication;") &&
                    method.name == "onCreate"
        }
    )
)
