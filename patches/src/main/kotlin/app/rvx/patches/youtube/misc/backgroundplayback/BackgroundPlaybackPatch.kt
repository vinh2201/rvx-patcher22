package app.rvx.patches.youtube.misc.backgroundplayback

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.instructions
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.Constants.MISC_PATH
import app.rvx.patches.youtube.utils.patch.PatchList.REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS
import app.rvx.patches.youtube.utils.playertype.playerTypeHookPatch
import app.rvx.patches.youtube.utils.playservice.is_19_34_or_greater
import app.rvx.patches.youtube.utils.playservice.versionCheckPatch
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.util.addInstructionsAtControlFlowLabel
import app.rvx.util.findInstructionIndicesReversedOrThrow
import app.rvx.util.fingerprint.injectLiteralInstructionBooleanCall
import app.rvx.util.fingerprint.matchOrThrow
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.fingerprint.originalMethodOrThrow
import app.rvx.util.getReference
import app.rvx.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$MISC_PATH/BackgroundPlaybackPatch;"

@Suppress("unused")
val backgroundPlaybackPatch = bytecodePatch(
    REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS.displayTitle,
    REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        playerTypeHookPatch,
        settingsPatch,
        versionCheckPatch,
    )

    apply {

        arrayOf(
            backgroundPlaybackManagerFingerprint to "isBackgroundPlaybackAllowed",
            backgroundPlaybackManagerShortsFingerprint to "isBackgroundShortsPlaybackAllowed",
        ).forEach { (fingerprint, extensionsMethod) ->
            fingerprint.methodOrThrow().apply {
                findInstructionIndicesReversedOrThrow(Opcode.RETURN).forEach { index ->
                    val register = getInstruction<OneRegisterInstruction>(index).registerA

                    addInstructionsAtControlFlowLabel(
                        index,
                        """
                            invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->$extensionsMethod(Z)Z
                            move-result v$register 
                        """,
                    )
                }
            }
        }

        // Enable background playback option in YouTube settings
        backgroundPlaybackSettingsFingerprint.originalMethodOrThrow().apply {
            val booleanCalls = instructions.withIndex().filter {
                it.value.getReference<MethodReference>()?.returnType == "Z"
            }

            val settingsBooleanIndex = booleanCalls.elementAt(1).index
            val settingsBooleanMethod = navigate(this).to(settingsBooleanIndex).stop()

            settingsBooleanMethod.returnEarly(true)
        }

        // Force allowing background play for Shorts.
        shortsBackgroundPlaybackFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
            SHORTS_BACKGROUND_PLAYBACK_FEATURE_FLAG,
            "$EXTENSION_CLASS_DESCRIPTOR->isBackgroundShortsPlaybackAllowed(Z)Z"
        )

        // Fix PiP mode issue.
        if (is_19_34_or_greater) {
            arrayOf(
                backgroundPlaybackManagerCairoFragmentPrimaryFingerprint,
                backgroundPlaybackManagerCairoFragmentSecondaryFingerprint
            ).forEach { fingerprint ->
                fingerprint.matchOrThrow(backgroundPlaybackManagerCairoFragmentParentFingerprint)
                    .let {
                        it.method.apply {
                            val insertIndex = it.patternMatch!!.startIndex + 4
                            val insertRegister =
                                getInstruction<OneRegisterInstruction>(insertIndex).registerA

                            addInstruction(
                                insertIndex,
                                "const/4 v$insertRegister, 0x0"
                            )
                        }
                    }
            }

            pipInputConsumerFeatureFlagFingerprint.injectLiteralInstructionBooleanCall(
                PIP_INPUT_CONSUMER_FEATURE_FLAG,
                "0x0"
            )
        }

        // Force allowing background play for videos labeled for kids.
        kidsBackgroundPlaybackPolicyControllerFingerprint.methodOrThrow(
            kidsBackgroundPlaybackPolicyControllerParentFingerprint
        ).returnEarly()

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: SHORTS",
                "SETTINGS: DISABLE_SHORTS_BACKGROUND_PLAYBACK"
            ),
            REMOVE_BACKGROUND_PLAYBACK_RESTRICTIONS
        )

        // endregion

    }
}
