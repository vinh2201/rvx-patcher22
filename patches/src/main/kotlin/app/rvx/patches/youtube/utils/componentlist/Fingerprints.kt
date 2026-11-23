package app.rvx.patches.youtube.utils.componentlist

import app.rvx.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.rvx.util.fingerprint.legacyFingerprint
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstruction
import app.rvx.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val componentListFingerprint = legacyFingerprint(
    name = "componentListFingerprint",
    returnType = "Ljava/util/List;",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_STATIC &&
                    getReference<MethodReference>()?.name == "nCopies"
        } >= 0
    }
)

internal val lazilyConvertedElementPatchFingerprint = legacyFingerprint(
    name = "lazilyConvertedElementPatchFingerprint",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    customFingerprint = { method, _ ->
        method.definingClass == "$UTILS_PATH/LazilyConvertedElementPatch;"
                && method.name == "hookElementList"
    }
)


