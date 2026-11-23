package app.rvx.patches.shared.audiotracks

import app.rvx.util.containsLiteralInstruction
import app.rvx.util.fingerprint.legacyFingerprint
import app.rvx.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal const val AUDIO_STREAM_IGNORE_DEFAULT_FEATURE_FLAG = 45666189L

internal val selectAudioStreamFingerprint = legacyFingerprint(
    name = "selectAudioStreamFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    returnType = "L",
    customFingerprint = { method, _ ->
        method.parameters.size > 2 // Method has a large number of parameters and may change.
                && method.containsLiteralInstruction(AUDIO_STREAM_IGNORE_DEFAULT_FEATURE_FLAG)
    }
)
