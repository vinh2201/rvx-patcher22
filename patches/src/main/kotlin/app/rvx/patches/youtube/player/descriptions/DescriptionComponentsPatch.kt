package app.rvx.patches.youtube.player.descriptions

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.extensions.ExternalLabel
import app.rvx.patches.shared.litho.addLithoFilter
import app.rvx.patches.shared.litho.lithoFilterPatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.engagement.engagementPanelHookPatch
import app.rvx.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.rvx.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.rvx.patches.youtube.utils.fix.hype.hypeButtonIconPatch
import app.rvx.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.rvx.patches.youtube.utils.patch.PatchList.DESCRIPTION_COMPONENTS
import app.rvx.patches.youtube.utils.playertype.playerTypeHookPatch
import app.rvx.patches.youtube.utils.playservice.is_18_49_or_greater
import app.rvx.patches.youtube.utils.playservice.is_19_05_or_greater
import app.rvx.patches.youtube.utils.playservice.versionCheckPatch
import app.rvx.patches.youtube.utils.recyclerview.recyclerViewTreeObserverHook
import app.rvx.patches.youtube.utils.recyclerview.recyclerViewTreeObserverPatch
import app.rvx.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.rvx.patches.youtube.utils.rollingNumberTextViewAnimationUpdateFingerprint
import app.rvx.patches.youtube.utils.rollingNumberTextViewFingerprint
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.util.fingerprint.matchOrThrow
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/DescriptionsFilter;"

@Suppress("unused")
val descriptionComponentsPatch = bytecodePatch(
    DESCRIPTION_COMPONENTS.displayTitle,
    DESCRIPTION_COMPONENTS.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        playerTypeHookPatch,
        recyclerViewTreeObserverPatch,
        engagementPanelHookPatch,
        hypeButtonIconPatch,
        sharedResourceIdPatch,
        versionCheckPatch,
    )

    apply {

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: PLAYER",
            "SETTINGS: DESCRIPTION_COMPONENTS"
        )

        // region patch for disable rolling number animation

        // RollingNumber is applied to YouTube v18.49.37+.
        // In order to maintain compatibility with YouTube v18.48.39 or previous versions,
        // This patch is applied only to the version after YouTube v18.49.37.
        if (is_18_49_or_greater) {
            rollingNumberTextViewAnimationUpdateFingerprint.matchOrThrow(
                rollingNumberTextViewFingerprint
            ).let {
                it.method.apply {
                    val freeRegister = implementation!!.registerCount - parameters.size - 2
                    val imageSpanIndex = it.patternMatch!!.startIndex
                    val setTextIndex = indexOfFirstInstructionOrThrow {
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                getReference<MethodReference>()?.name == "setText"
                    }
                    addInstruction(setTextIndex, "nop")
                    addInstructionsWithLabels(
                        imageSpanIndex, """
                            invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->disableRollingNumberAnimations()Z
                            move-result v$freeRegister
                            if-nez v$freeRegister, :disable_animations
                            """, ExternalLabel("disable_animations", getInstruction(setTextIndex))
                    )
                }
            }

            settingArray += "SETTINGS: DISABLE_ROLLING_NUMBER_ANIMATIONS"
        }

        // endregion

        // region patch for disable video description interaction and expand video description

        if (is_19_05_or_greater) {
            textViewComponentFingerprint.methodOrThrow().apply {
                val insertIndex = indexOfTextIsSelectableInstruction(this)
                val insertInstruction = getInstruction<FiveRegisterInstruction>(insertIndex)

                replaceInstruction(
                    insertIndex,
                    "invoke-static {v${insertInstruction.registerC}, v${insertInstruction.registerD}}, " +
                            "$PLAYER_CLASS_DESCRIPTOR->disableVideoDescriptionInteraction(Landroid/widget/TextView;Z)V"
                )
            }

            recyclerViewTreeObserverHook("$PLAYER_CLASS_DESCRIPTOR->onVideoDescriptionCreate(Landroid/support/v7/widget/RecyclerView;)V")

            settingArray += "SETTINGS: DESCRIPTION_INTERACTION"
        }

        // endregion

        addLithoFilter(FILTER_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(settingArray, DESCRIPTION_COMPONENTS)

        // endregion

    }
}
