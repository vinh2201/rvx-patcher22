package app.rvx.patches.youtube.utils.lottie

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.rvx.util.findMethodOrThrow
import app.rvx.util.fingerprint.methodOrThrow

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/LottieAnimationViewPatch;"

val lottieAnimationViewHookPatch = bytecodePatch(
    description = "lottieAnimationViewHookPatch",
) {
    apply {

        findMethodOrThrow(EXTENSION_CLASS_DESCRIPTOR) {
            name == "setAnimation"
        }.addInstruction(
            0,
            "invoke-virtual {p0, p1}, " +
                    LOTTIE_ANIMATION_VIEW_CLASS_DESCRIPTOR +
                    "->" +
                    setAnimationFingerprint.methodOrThrow().name +
                    "(I)V"
        )

    }
}
