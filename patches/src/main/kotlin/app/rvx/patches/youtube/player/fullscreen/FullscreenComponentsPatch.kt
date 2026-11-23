package app.rvx.patches.youtube.player.fullscreen

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.extensions.ExternalLabel
import app.rvx.patches.shared.litho.addLithoFilter
import app.rvx.patches.shared.litho.lithoFilterPatch
import app.rvx.patches.shared.mainactivity.onConfigurationChangedMethod
import app.rvx.patches.shared.mainactivity.onStartMethod
import app.rvx.patches.shared.mainactivity.onStopMethod
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.rvx.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.rvx.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.rvx.patches.youtube.utils.extension.Constants.PLAYER_PATH
import app.rvx.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.rvx.patches.youtube.utils.fullscreen.fullscreenButtonHookPatch
import app.rvx.patches.youtube.utils.indexOfFocusableInTouchModeInstruction
import app.rvx.patches.youtube.utils.layoutConstructorFingerprint
import app.rvx.patches.youtube.utils.mainactivity.mainActivityResolvePatch
import app.rvx.patches.youtube.utils.patch.PatchList.FULLSCREEN_COMPONENTS
import app.rvx.patches.youtube.utils.playertype.playerTypeHookPatch
import app.rvx.patches.youtube.utils.playservice.is_18_42_or_greater
import app.rvx.patches.youtube.utils.playservice.is_19_41_or_greater
import app.rvx.patches.youtube.utils.playservice.versionCheckPatch
import app.rvx.patches.youtube.utils.resourceid.autoNavPreviewStub
import app.rvx.patches.youtube.utils.resourceid.fullScreenEngagementPanel
import app.rvx.patches.youtube.utils.resourceid.quickActionsElementContainer
import app.rvx.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.patches.youtube.utils.youtubeControlsOverlayFingerprint
import app.rvx.patches.youtube.video.information.hookBackgroundPlayVideoInformation
import app.rvx.patches.youtube.video.information.videoEndMethod
import app.rvx.patches.youtube.video.information.videoInformationPatch
import app.rvx.util.addInstructionsAtControlFlowLabel
import app.rvx.util.findMethodOrThrow
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.fingerprint.mutableClassOrThrow
import app.rvx.util.getReference
import app.rvx.util.getWalkerMethod
import app.rvx.util.indexOfFirstInstructionOrThrow
import app.rvx.util.indexOfFirstLiteralInstructionOrThrow
import app.rvx.util.indexOfFirstStringInstructionOrThrow
import app.rvx.util.updatePatchStatus
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/QuickActionFilter;"

private const val EXTENSION_ENTER_FULLSCREEN_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/EnterFullscreenPatch;"

private const val EXTENSION_EXIT_FULLSCREEN_CLASS_DESCRIPTOR =
    "$PLAYER_PATH/ExitFullscreenPatch;"

@Suppress("unused")
val fullscreenComponentsPatch = bytecodePatch(
    FULLSCREEN_COMPONENTS.displayTitle,
    FULLSCREEN_COMPONENTS.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        settingsPatch,
        playerTypeHookPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        mainActivityResolvePatch,
        fullscreenButtonHookPatch,
        videoInformationPatch,
        sharedResourceIdPatch,
        versionCheckPatch,
    )

    apply {

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: PLAYER",
            "SETTINGS: FULLSCREEN_COMPONENTS"
        )

        // region patch for disable engagement panel

        fullScreenEngagementPanelFingerprint.methodOrThrow().apply {
            val literalIndex =
                indexOfFirstLiteralInstructionOrThrow(fullScreenEngagementPanel)
            val targetIndex = indexOfFirstInstructionOrThrow(literalIndex, Opcode.CHECK_CAST)
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, " +
                        "$PLAYER_CLASS_DESCRIPTOR->disableEngagementPanels(Landroidx/coordinatorlayout/widget/CoordinatorLayout;)V"
            )
        }

        playerTitleViewFingerprint.methodOrThrow().apply {
            val insertIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "addView"
            }
            val insertReference =
                getInstruction<ReferenceInstruction>(insertIndex).reference.toString()
            if (!insertReference.startsWith("Landroid/widget/FrameLayout;"))
                throw PatchException("Reference does not match: $insertReference")
            val insertInstruction = getInstruction<FiveRegisterInstruction>(insertIndex)

            replaceInstruction(
                insertIndex,
                "invoke-static { v${insertInstruction.registerC}, v${insertInstruction.registerD} }, " +
                        "$PLAYER_CLASS_DESCRIPTOR->showVideoTitleSection(Landroid/widget/FrameLayout;Landroid/view/View;)V"
            )
        }

        // endregion

        // region patch for enter fullscreen

        mapOf(
            onStartMethod to "onAppForegrounded",
            onStopMethod to "onAppBackgrounded"
        ).forEach { (method, name) ->
            method.addInstruction(
                0,
                "invoke-static {}, $EXTENSION_ENTER_FULLSCREEN_CLASS_DESCRIPTOR->$name()V"
            )
        }

        hookBackgroundPlayVideoInformation("$EXTENSION_ENTER_FULLSCREEN_CLASS_DESCRIPTOR->enterFullscreen(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")

        // endregion

        // region patch for exit fullscreen

        videoEndMethod.apply {
            addInstructionsAtControlFlowLabel(
                implementation!!.instructions.lastIndex,
                "invoke-static {}, $EXTENSION_EXIT_FULLSCREEN_CLASS_DESCRIPTOR->endOfVideoReached()V",
            )
        }

        // endregion

        // region patch for hide autoplay preview

        layoutConstructorFingerprint.methodOrThrow().apply {
            val constIndex = indexOfFirstLiteralInstructionOrThrow(autoNavPreviewStub)
            val constRegister = getInstruction<OneRegisterInstruction>(constIndex).registerA
            val jumpIndex =
                indexOfFirstInstructionOrThrow(constIndex + 2, Opcode.INVOKE_VIRTUAL) + 1

            addInstructionsWithLabels(
                constIndex, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideAutoPlayPreview()Z
                    move-result v$constRegister
                    if-nez v$constRegister, :hidden
                    """, ExternalLabel("hidden", getInstruction(jumpIndex))
            )
        }

        // endregion

        // region patch for hide related video overlay

        relatedEndScreenResultsFingerprint.mutableClassOrThrow().let {
            it.methods.find { method -> method.parameters == listOf("I", "Z", "I") }
                ?.apply {
                    addInstructionsWithLabels(
                        0, """
                            invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideRelatedVideoOverlay()Z
                            move-result v0
                            if-eqz v0, :show
                            return-void
                            """, ExternalLabel("show", getInstruction(0))
                    )
                } ?: throw PatchException("Could not find targetMethod")
        }

        // endregion

        // region patch for quick actions

        quickActionsElementSyntheticFingerprint.methodOrThrow().apply {
            val containerCalls = implementation!!.instructions.withIndex()
                .filter { instruction ->
                    (instruction.value as? WideLiteralInstruction)?.wideLiteral == quickActionsElementContainer
                }
            val constIndex = containerCalls.elementAt(containerCalls.size - 1).index

            val checkCastIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.CHECK_CAST)
            val insertRegister =
                getInstruction<OneRegisterInstruction>(checkCastIndex).registerA

            addInstruction(
                checkCastIndex + 1,
                "invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->setQuickActionMargin(Landroid/view/View;)V"
            )

            addInstruction(
                checkCastIndex,
                "invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->hideQuickActions(Landroid/view/View;)V"
            )
        }

        updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "QuickActions")

        // endregion

        // region patch for compact control overlay

        youtubeControlsOverlayFingerprint.methodOrThrow().apply {
            val targetIndex = indexOfFocusableInTouchModeInstruction(this)
            val walkerIndex = indexOfFirstInstructionOrThrow(targetIndex) {
                val reference = getReference<MethodReference>()
                opcode == Opcode.INVOKE_STATIC &&
                        reference?.returnType == "Z" &&
                        reference.parameterTypes.size == 1
            }

            val walkerMethod = getWalkerMethod(walkerIndex)
            walkerMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->enableCompactControlsOverlay(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        }

        // endregion

        // region patch for disable landscape mode

        onConfigurationChangedMethod.apply {
            val walkerIndex = indexOfFirstInstructionOrThrow {
                val reference = getReference<MethodReference>()
                reference?.parameterTypes == listOf("Landroid/content/res/Configuration;") &&
                        reference.returnType == "V" &&
                        reference.name != "onConfigurationChanged"
            }

            val walkerMethod = getWalkerMethod(walkerIndex)
            val constructorMethod =
                findMethodOrThrow(walkerMethod.definingClass) {
                    name == "<init>" &&
                            parameterTypes == listOf("Landroid/app/Activity;")
                }

            arrayOf(
                walkerMethod,
                constructorMethod
            ).forEach { method ->
                method.apply {
                    val index = indexOfFirstInstructionOrThrow {
                        val reference = getReference<MethodReference>()
                        reference?.parameterTypes == listOf("Landroid/content/Context;") &&
                                reference.returnType == "Z"
                    } + 1
                    val register = getInstruction<OneRegisterInstruction>(index).registerA

                    addInstructions(
                        index + 1, """
                            invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->disableLandScapeMode(Z)Z
                            move-result v$register
                            """
                    )
                }
            }
        }

        // endregion

        // region patch for keep landscape mode

        if (is_18_42_or_greater && !is_19_41_or_greater) {
            landScapeModeConfigFingerprint.methodOrThrow().apply {
                val insertIndex = implementation!!.instructions.lastIndex
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->keepFullscreen(Z)Z
                        move-result v$insertRegister
                        """
                )
            }
            broadcastReceiverFingerprint.methodOrThrow().apply {
                val stringIndex =
                    indexOfFirstStringInstructionOrThrow("android.intent.action.SCREEN_ON")
                val insertIndex =
                    indexOfFirstInstructionOrThrow(stringIndex, Opcode.IF_EQZ) + 1

                addInstruction(
                    insertIndex,
                    "invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->setScreenOn()V"
                )
            }

            settingArray += "SETTINGS: KEEP_LANDSCAPE_MODE"
        }

        // endregion

        addLithoFilter(FILTER_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(settingArray, FULLSCREEN_COMPONENTS)

        // endregion

    }
}
