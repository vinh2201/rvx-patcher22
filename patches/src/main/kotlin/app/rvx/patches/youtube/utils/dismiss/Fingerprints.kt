package app.rvx.patches.youtube.utils.dismiss

import app.rvx.util.fingerprint.legacyFingerprint
import app.rvx.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal const val DISMISS_PLAYER_LITERAL = 34699L

internal val dismissPlayerOnClickListenerFingerprint = legacyFingerprint(
    name = "dismissPlayerOnClickListenerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    literals = listOf(DISMISS_PLAYER_LITERAL),
    customFingerprint = { method, _ ->
        method.name == "onClick"
    }
)
