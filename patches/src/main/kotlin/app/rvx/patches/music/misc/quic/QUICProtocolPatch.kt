package app.rvx.patches.music.misc.quic

import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.patch.PatchList.DISABLE_QUIC_PROTOCOL
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.addSwitchPreference
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.patches.shared.quic.baseQuicProtocolPatch

@Suppress("unused", "SpellCheckingInspection")
val quicProtocolPatch = bytecodePatch(
    DISABLE_QUIC_PROTOCOL.displayTitle,
    DISABLE_QUIC_PROTOCOL.summary,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        settingsPatch,
        baseQuicProtocolPatch(),
    )

    apply {

        addSwitchPreference(
            CategoryType.MISC,
            "revanced_disable_quic_protocol",
            "false"
        )
        updatePatchStatus(DISABLE_QUIC_PROTOCOL)

    }
}
