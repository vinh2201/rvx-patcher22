package app.rvx.patches.youtube.feed.flyoutmenu

import app.rvx.patches.youtube.utils.resourceid.posterArtWidthDefault
import app.rvx.util.fingerprint.legacyFingerprint
import app.rvx.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val contextualMenuItemBuilderFingerprint = legacyFingerprint(
    name = "contextualMenuItemBuilderFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.SYNTHETIC,
    parameters = listOf("L", "L"),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.ADD_INT_2ADDR
    ),
    literals = listOf(posterArtWidthDefault),
)

