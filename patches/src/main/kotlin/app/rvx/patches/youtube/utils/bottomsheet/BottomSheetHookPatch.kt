package app.rvx.patches.youtube.utils.bottomsheet

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.rvx.util.findMethodOrThrow
import app.rvx.util.fingerprint.definingClassOrThrow

private const val EXTENSION_BOTTOM_SHEET_HOOK_CLASS_DESCRIPTOR =
    "$UTILS_PATH/BottomSheetHookPatch;"

val bottomSheetHookPatch = bytecodePatch(
    description = "bottomSheetHookPatch"
) {
    apply {
        val bottomSheetClass =
            bottomSheetBehaviorFingerprint.definingClassOrThrow()

        arrayOf(
            "onAttachedToWindow",
            "onDetachedFromWindow"
        ).forEach { methodName ->
            findMethodOrThrow(bottomSheetClass) {
                name == methodName
            }.addInstruction(
                1,
                "invoke-static {}, $EXTENSION_BOTTOM_SHEET_HOOK_CLASS_DESCRIPTOR->$methodName()V"
            )
        }
    }
}
