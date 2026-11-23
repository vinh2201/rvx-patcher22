package app.rvx.patches.music.utils.fix.timedlyrics

import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.music.utils.playservice.is_8_33_or_greater
import app.rvx.patches.music.utils.playservice.versionCheckPatch
import app.rvx.util.fingerprint.injectLiteralInstructionBooleanCall

val timedLyricsPatch = bytecodePatch(
    description = "timedLyricsPatch"
) {
    dependsOn(versionCheckPatch)

    apply patchApply@{
        if (!is_8_33_or_greater) {
            return@patchApply
        }

        /**
         * When these experimental flags are enabled, the real-time lyrics UI will break.
         */
        mapOf(
            timedLyricsPrimaryFingerprint to TIMED_LYRICS_PRIMARY_FEATURE_FLAG,
            timedLyricsSecondaryFingerprint to TIMED_LYRICS_SECONDARY_FEATURE_FLAG,
        ).forEach { (fingerprint, literal) ->
            fingerprint.injectLiteralInstructionBooleanCall(
                literal,
                "0x0"
            )
        }
    }
}
