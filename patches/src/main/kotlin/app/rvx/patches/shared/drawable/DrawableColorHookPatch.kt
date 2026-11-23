package app.rvx.patches.shared.drawable

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private lateinit var insertMethod: MutableMethod
private var insertIndex: Int = 0
private var insertRegister: Int = 0
private var offset = 0

val drawableColorHookPatch = bytecodePatch(
    description = "drawableColorHookPatch"
) {
    apply {
        drawableColorFingerprint.methodOrThrow().apply {
            insertMethod = this
            insertIndex = indexOfFirstInstructionReversedOrThrow {
                getReference<MethodReference>()?.name == "setColor"
            }
            insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD
        }
    }
}

internal fun addDrawableColorHook(
    methodDescriptor: String,
    highPriority: Boolean = false
) {
    insertMethod.addInstructions(
        if (highPriority) insertIndex else insertIndex + offset,
        """
            invoke-static {v$insertRegister}, $methodDescriptor
            move-result v$insertRegister
            """
    )
    offset += 2
}

