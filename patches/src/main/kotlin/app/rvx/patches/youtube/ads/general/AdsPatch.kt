package app.rvx.patches.youtube.ads.general

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.extensions.ExternalLabel
import app.rvx.patches.shared.ANDROID_AUTOMOTIVE_STRING
import app.rvx.patches.shared.ads.adsPatch
import app.rvx.patches.shared.autoMotiveFingerprint
import app.rvx.patches.shared.litho.addLithoFilter
import app.rvx.patches.shared.litho.lithoFilterPatch
import app.rvx.patches.shared.spoof.guide.addClientOSVersionHook
import app.rvx.patches.shared.spoof.guide.spoofClientGuideEndpointPatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.Constants.ADS_CLASS_DESCRIPTOR
import app.rvx.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.rvx.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.rvx.patches.youtube.utils.patch.PatchList.HIDE_ADS
import app.rvx.patches.youtube.utils.playservice.is_20_06_or_greater
import app.rvx.patches.youtube.utils.playservice.versionCheckPatch
import app.rvx.patches.youtube.utils.resourceid.adAttribution
import app.rvx.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.util.findMutableMethodOf
import app.rvx.util.fingerprint.matchOrThrow
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.indexOfFirstStringInstructionOrThrow
import app.rvx.util.injectHideViewCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31i
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

private const val ADS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/AdsFilter;"

@Suppress("unused")
val adsPatch = adsPatch(
    block = {
        compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

        dependsOn(
            settingsPatch,
            lithoFilterPatch,
            lithoLayoutPatch,
            sharedResourceIdPatch,
            spoofClientGuideEndpointPatch,
            versionCheckPatch,
        )
    },
    classDescriptor = ADS_CLASS_DESCRIPTOR,
    methodDescriptor = "hideVideoAds",
    executeBlock = {
        addLithoFilter(ADS_FILTER_CLASS_DESCRIPTOR)

        // region patch for hide general ads

        classDefs.toList().forEach { classDef ->
            classDef.methods.forEach { method ->
                method.implementation.apply {
                    this?.instructions?.forEachIndexed { index, instruction ->
                        if (instruction.opcode != Opcode.CONST)
                            return@forEachIndexed
                        // Instruction to store the id adAttribution into a register
                        if ((instruction as Instruction31i).wideLiteral != adAttribution)
                            return@forEachIndexed

                        val insertIndex = index + 1

                        // Call to get the view with the id adAttribution
                        (instructions.elementAt(insertIndex)).apply {
                            if (opcode != Opcode.INVOKE_VIRTUAL)
                                return@forEachIndexed

                            // Hide the view
                            val viewRegister = (this as Instruction35c).registerC
                            classDefs.getOrReplaceMutable(classDef)
                                .findMutableMethodOf(method)
                                .injectHideViewCall(
                                    insertIndex,
                                    viewRegister,
                                    ADS_CLASS_DESCRIPTOR,
                                    "hideAdAttributionView"
                                )
                        }
                    }
                }
            }
        }

        // endregion

        // region patch for hide get premium

        compactYpcOfferModuleViewFingerprint.matchOrThrow().let {
            it.method.apply {
                val startIndex = it.patternMatch!!.startIndex
                val measuredWidthRegister =
                    getInstruction<TwoRegisterInstruction>(startIndex).registerA
                val measuredHeightInstruction =
                    getInstruction<TwoRegisterInstruction>(startIndex + 1)
                val measuredHeightRegister = measuredHeightInstruction.registerA
                val tempRegister = measuredHeightInstruction.registerB

                addInstructionsWithLabels(
                    startIndex + 2, """
                        invoke-static {}, $ADS_CLASS_DESCRIPTOR->hideGetPremium()Z
                        move-result v$tempRegister
                        if-eqz v$tempRegister, :show
                        const/4 v$measuredWidthRegister, 0x0
                        const/4 v$measuredHeightRegister, 0x0
                        """, ExternalLabel("show", getInstruction(startIndex + 2))
                )
            }
        }

        // endregion

        // region patch for hide end screen store banner

        fullScreenEngagementAdContainerFingerprint.methodOrThrow().apply {
            val addListIndex = indexOfAddListInstruction(this)
            val addListInstruction =
                getInstruction<FiveRegisterInstruction>(addListIndex)
            val listRegister = addListInstruction.registerC
            val objectRegister = addListInstruction.registerD

            replaceInstruction(
                addListIndex,
                "invoke-static { v$listRegister, v$objectRegister }, " +
                        "$ADS_CLASS_DESCRIPTOR->hideEndScreenStoreBanner(Ljava/util/List;Ljava/lang/Object;)V"
            )
        }

        // endregion

        // region patch for hide shorts ad

        // Hide Shorts ads by changing 'OSName' to 'Android Automotive'
        autoMotiveFingerprint.methodOrThrow().apply {
            val insertIndex = indexOfFirstStringInstructionOrThrow(ANDROID_AUTOMOTIVE_STRING) - 1
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex, """
                    invoke-static {v$insertRegister}, $ADS_CLASS_DESCRIPTOR->hideShortsAds(Z)Z
                    move-result v$insertRegister
                    """
            )
        }

        // If 'OSName' is changed to 'Android Automotive' in all requests, a Notification button will appear in the navigation bar
        // To fix this side effect, requests to the '/guide' endpoint, which are related to navigation buttons, use the original 'OSName'
        addClientOSVersionHook(
            "patch_setClientOSNameByAdsPatch",
            "$ADS_CLASS_DESCRIPTOR->overrideOSName()Ljava/lang/String;",
            is_20_06_or_greater
        )

        // endregion

        // region patch for hide paid promotion label in Shorts (non-litho)

        shortsPaidPromotionFingerprint.methodOrThrow().apply {
            when (returnType) {
                "Landroid/widget/TextView;" -> {
                    val insertIndex = implementation!!.instructions.lastIndex
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex + 1, """
                            invoke-static {v$insertRegister}, $ADS_CLASS_DESCRIPTOR->hideShortsPaidPromotionLabel(Landroid/widget/TextView;)V
                            return-object v$insertRegister
                            """
                    )
                    removeInstruction(insertIndex)
                }

                "V" -> {
                    addInstructionsWithLabels(
                        0, """
                            invoke-static {}, $ADS_CLASS_DESCRIPTOR->hideShortsPaidPromotionLabel()Z
                            move-result v0
                            if-eqz v0, :show
                            return-void
                            """, ExternalLabel("show", getInstruction(0))
                    )
                }

                else -> {
                    throw PatchException("Unknown returnType: $returnType")
                }
            }
        }

        // endregion

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: ADS"
            ),
            HIDE_ADS
        )

        // endregion
    }
)