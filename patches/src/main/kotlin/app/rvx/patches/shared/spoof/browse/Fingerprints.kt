package app.rvx.patches.shared.spoof.browse

import app.rvx.util.fingerprint.legacyFingerprint
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionReversed
import app.rvx.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val browseEndpointConstructorFingerprint = legacyFingerprint(
    name = "browseEndpointConstructorFingerprint",
    returnType = "V",
    strings = listOf("browse"),
    customFingerprint = { method, _ ->
        method.name == "<init>" &&
                method.indexOfFirstInstructionReversed {
                    opcode == Opcode.INVOKE_STATIC &&
                            getReference<MethodReference>()?.toString() == "Ljava/util/Locale;->getDefault()Ljava/util/Locale;"
                } >= 0
    }
)

internal val browseEndpointRequestBodyFingerprint = legacyFingerprint(
    name = "browseEndpointRequestBodyFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
    ),
)

