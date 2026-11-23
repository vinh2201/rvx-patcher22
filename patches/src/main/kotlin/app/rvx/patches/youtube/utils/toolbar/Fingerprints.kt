package app.rvx.patches.youtube.utils.toolbar

import app.rvx.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.rvx.util.fingerprint.legacyFingerprint
import app.rvx.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val toolBarPatchFingerprint = legacyFingerprint(
    name = "toolBarPatchFingerprint",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    customFingerprint = { method, _ ->
        method.definingClass == "$UTILS_PATH/ToolBarPatch;"
                && method.name == "hookToolBar"
    }
)


