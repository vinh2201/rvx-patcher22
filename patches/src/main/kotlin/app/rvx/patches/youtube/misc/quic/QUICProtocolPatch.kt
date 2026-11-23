package app.rvx.patches.youtube.misc.quic

import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.shared.quic.baseQuicProtocolPatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.patch.PatchList.DISABLE_QUIC_PROTOCOL
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch

@Suppress("unused", "SpellCheckingInspection")
val quicProtocolPatch = bytecodePatch(
    DISABLE_QUIC_PROTOCOL.displayTitle,
    DISABLE_QUIC_PROTOCOL.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        settingsPatch,
        baseQuicProtocolPatch(),
    )

    apply {
        addPreference(
            arrayOf(
                "SETTINGS: DISABLE_QUIC_PROTOCOL"
            ),
            DISABLE_QUIC_PROTOCOL
        )
    }
}
