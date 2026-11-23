package app.rvx.patches.shared.dialog

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.rvx.util.fingerprint.matchOrThrow
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.getWalkerMethod
import app.rvx.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

fun baseViewerDiscretionDialogPatch(
    classDescriptor: String,
    isAgeVerified: Boolean = false
) = bytecodePatch(
    description = "baseViewerDiscretionDialogPatch"
) {
    apply {
        createDialogFingerprint
            .methodOrThrow()
            .invoke(classDescriptor, "confirmDialog")

        if (isAgeVerified) {
            ageVerifiedFingerprint.matchOrThrow().let {
                it.getWalkerMethod(it.patternMatch!!.endIndex - 1)
                    .invoke(classDescriptor, "confirmDialogAgeVerified")
            }
        }
    }
}

private fun MutableMethod.invoke(classDescriptor: String, methodName: String) {
    val showDialogIndex = indexOfFirstInstructionOrThrow {
        getReference<MethodReference>()?.name == "show"
    }
    val dialogRegister = getInstruction<FiveRegisterInstruction>(showDialogIndex).registerC

    addInstruction(
        showDialogIndex + 1,
        "invoke-static { v$dialogRegister }, $classDescriptor->$methodName(Landroid/app/AlertDialog;)V"
    )
}

