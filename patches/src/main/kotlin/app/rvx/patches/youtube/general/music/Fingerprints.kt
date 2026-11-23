package app.rvx.patches.youtube.general.music

import app.rvx.util.fingerprint.legacyFingerprint
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstruction
import app.rvx.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal val appDeepLinkFingerprint = legacyFingerprint(
    name = "appDeepLinkFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "Ljava/util/Map;"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.CONST_STRING,
    ),
    strings = listOf("android.intent.action.VIEW"),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            getReference<FieldReference>()?.name == "appDeepLinkEndpoint"
        } >= 0
    }
)
