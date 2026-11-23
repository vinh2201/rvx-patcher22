package app.rvx.patches.music.utils.fix.androidauto

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.patch.PatchList.CERTIFICATE_SPOOF
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.util.fingerprint.methodOrThrow

@Suppress("unused")
val androidAutoCertificatePatch = bytecodePatch(
    CERTIFICATE_SPOOF.displayTitle,
    CERTIFICATE_SPOOF.summary,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    apply {
        certificateCheckFingerprint.methodOrThrow().addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
                """,
        )

        updatePatchStatus(CERTIFICATE_SPOOF)

    }
}
