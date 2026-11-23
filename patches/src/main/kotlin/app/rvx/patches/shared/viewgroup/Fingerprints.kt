package app.rvx.patches.shared.viewgroup

import app.rvx.util.fingerprint.legacyFingerprint
import app.rvx.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val viewGroupMarginFingerprint = legacyFingerprint(
    name = "viewGroupMarginFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("Landroid/view/View;", "I", "I"),
)

internal val viewGroupMarginParentFingerprint = legacyFingerprint(
    name = "viewGroupMarginParentFingerprint",
    returnType = "Landroid/view/ViewGroup${'$'}LayoutParams;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("Ljava/lang/Class;", "Landroid/view/ViewGroup${'$'}LayoutParams;"),
    strings = listOf("SafeLayoutParams"),
)
