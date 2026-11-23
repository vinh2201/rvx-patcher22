package app.rvx.patches.music.utils.extension.hooks

import app.rvx.patches.shared.extension.extensionHook
import app.rvx.util.fingerprint.legacyFingerprint
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val applicationInitHook = extensionHook(
    fingerprint = legacyFingerprint(
        name = "applicationInitHook",
        returnType = "V",
        parameters = listOf(),
        strings = listOf("activity"),
        customFingerprint = { method, _ ->
            method.name == "onCreate" &&
                    method.indexOfFirstInstruction {
                        opcode == Opcode.INVOKE_VIRTUAL
                                && getReference<MethodReference>()?.name == "getRunningAppProcesses"
                    } >= 0
        }
    )
)
