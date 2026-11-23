package app.rvx.patches.youtube.player.miniplayer.startup

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.Constants.PLAYER_PATH
import app.rvx.patches.youtube.utils.patch.PatchList.DISABLE_RESUMING_MINIPLAYER_ON_STARTUP
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.util.fingerprint.matchOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/MiniplayerPatch;"

// YT uses "Miniplayer" without a space between 'mini' and 'player: https://support.google.com/youtube/answer/9162927.
@Suppress("unused", "SpellCheckingInspection")
val resumingMiniplayerOnStartupPatch = bytecodePatch(
    DISABLE_RESUMING_MINIPLAYER_ON_STARTUP.displayTitle,
    DISABLE_RESUMING_MINIPLAYER_ON_STARTUP.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(settingsPatch)

    apply {

        showMiniplayerCommandFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.endIndex
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $EXTENSION_CLASS_DESCRIPTOR->disableResumingStartupMiniPlayer(Z)Z
                        move-result v$insertRegister
                        """
                )
            }
        }


        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "SETTINGS: MINIPLAYER_COMPONENTS",
                "SETTINGS: DISABLE_RESUMING_MINIPLAYER"
            ),
            DISABLE_RESUMING_MINIPLAYER_ON_STARTUP
        )

    }
}
