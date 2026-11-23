package app.rvx.patches.music.utils.playertype

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.music.utils.extension.Constants.UTILS_PATH
import app.rvx.util.fingerprint.methodOrThrow

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/PlayerTypeHookPatch;"

@Suppress("unused")
val playerTypeHookPatch = bytecodePatch(
    description = "playerTypeHookPatch"
) {

    apply {

        playerTypeFingerprint.methodOrThrow().addInstruction(
            0,
            "invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->setPlayerType(Ljava/lang/Enum;)V"
        )

    }
}
