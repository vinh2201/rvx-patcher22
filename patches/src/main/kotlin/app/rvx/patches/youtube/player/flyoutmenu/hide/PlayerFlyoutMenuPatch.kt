package app.rvx.patches.youtube.player.flyoutmenu.hide

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.shared.litho.addLithoFilter
import app.rvx.patches.shared.litho.lithoFilterPatch
import app.rvx.patches.youtube.utils.YOUTUBE_VIDEO_QUALITY_CLASS_TYPE
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.rvx.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.rvx.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.rvx.patches.youtube.utils.indexOfAddHeaderViewInstruction
import app.rvx.patches.youtube.utils.patch.PatchList.HIDE_PLAYER_FLYOUT_MENU
import app.rvx.patches.youtube.utils.playertype.playerTypeHookPatch
import app.rvx.patches.youtube.utils.playservice.is_18_39_or_greater
import app.rvx.patches.youtube.utils.playservice.is_19_30_or_greater
import app.rvx.patches.youtube.utils.playservice.versionCheckPatch
import app.rvx.patches.youtube.utils.qualityMenuViewInflateFingerprint
import app.rvx.patches.youtube.utils.resourceid.bottomSheetFooterText
import app.rvx.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.patches.youtube.video.information.videoInformationPatch
import app.rvx.util.REGISTER_TEMPLATE_REPLACEMENT
import app.rvx.util.fingerprint.injectLiteralInstructionBooleanCall
import app.rvx.util.fingerprint.injectLiteralInstructionViewCall
import app.rvx.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val PANELS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/PlayerFlyoutMenuFilter;"

@Suppress("unused")
val playerFlyoutMenuPatch = bytecodePatch(
    HIDE_PLAYER_FLYOUT_MENU.displayTitle,
    HIDE_PLAYER_FLYOUT_MENU.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        videoInformationPatch,
        versionCheckPatch
    )

    apply {
        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: PLAYER",
            "PREFERENCE_SCREENS: FLYOUT_MENU",
            "SETTINGS: HIDE_PLAYER_FLYOUT_MENU"
        )

        // region hide player flyout menu header, footer (non-litho)

        mapOf(
            advancedQualityBottomSheetFingerprint to "hidePlayerFlyoutMenuQualityFooter",
            captionsBottomSheetFingerprint to "hidePlayerFlyoutMenuCaptionsFooter",
            qualityMenuViewInflateFingerprint to "hidePlayerFlyoutMenuQualityFooter"
        ).forEach { (fingerprint, name) ->
            val smaliInstruction = """
                    invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $PLAYER_CLASS_DESCRIPTOR->$name(Landroid/view/View;)V
                    """
            fingerprint.injectLiteralInstructionViewCall(bottomSheetFooterText, smaliInstruction)
        }

        arrayOf(
            advancedQualityBottomSheetFingerprint,
            qualityMenuViewInflateFingerprint
        ).forEach { fingerprint ->
            fingerprint.methodOrThrow().apply {
                val insertIndex = indexOfAddHeaderViewInstruction(this)
                val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->hidePlayerFlyoutMenuQualityHeader(Landroid/view/View;)Landroid/view/View;
                        move-result-object v$insertRegister
                        """
                )
            }
        }

        // endregion

        // region patch for hide '1080p Premium' label

        currentVideoFormatConstructorFingerprint.methodOrThrow(
            currentVideoFormatToStringFingerprint
        ).apply {
            val videoQualitiesIndex =
                indexOfVideoQualitiesInstruction(this)
            val videoQualitiesRegister =
                getInstruction<TwoRegisterInstruction>(videoQualitiesIndex).registerA

            addInstructions(
                1, """
                    invoke-static/range { v$videoQualitiesRegister .. v$videoQualitiesRegister }, $PLAYER_CLASS_DESCRIPTOR->hidePlayerFlyoutMenuEnhancedBitrate([$YOUTUBE_VIDEO_QUALITY_CLASS_TYPE)[$YOUTUBE_VIDEO_QUALITY_CLASS_TYPE
                    move-result-object v$videoQualitiesRegister
                    """
            )
        }

        // endregion

        // region patch for hide pip mode menu

        if (is_18_39_or_greater) {
            pipModeConfigFingerprint.injectLiteralInstructionBooleanCall(
                45427407L,
                "$PLAYER_CLASS_DESCRIPTOR->hidePiPModeMenu(Z)Z"
            )
            settingArray += "SETTINGS: HIDE_PIP_MODE_MENU"
        }

        // endregion

        // region patch for hide sleep timer menu

        if (is_19_30_or_greater) {
            // Sleep timer menu in Additional settings (deprecated)
            // TODO: A patch will be implemented to assign this deprecated menu to another action.
            // mapOf(
            //     sleepTimerConstructorFingerprint to SLEEP_TIMER_CONSTRUCTOR_FEATURE_FLAG,
            //     sleepTimerFingerprint to SLEEP_TIMER_FEATURE_FLAG
            // ).forEach { (fingerprint, literal) ->
            //     fingerprint.injectLiteralInstructionBooleanCall(
            //         literal,
            //         "$PLAYER_CLASS_DESCRIPTOR->hideDeprecatedSleepTimerMenu(Z)Z"
            //     )
            // }
            settingArray += "SETTINGS: HIDE_SLEEP_TIMER_MENU"
        }

        // endregion

        addLithoFilter(PANELS_FILTER_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(settingArray, HIDE_PLAYER_FLYOUT_MENU)

        // endregion

    }
}
