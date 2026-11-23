package app.rvx.patches.music.navigation.components

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.rvx.patches.music.general.startpage.changeStartPagePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.extension.Constants.NAVIGATION_CLASS_DESCRIPTOR
import app.rvx.patches.music.utils.patch.PatchList.NAVIGATION_BAR_COMPONENTS
import app.rvx.patches.music.utils.playservice.is_6_27_or_greater
import app.rvx.patches.music.utils.playservice.is_8_29_or_greater
import app.rvx.patches.music.utils.playservice.versionCheckPatch
import app.rvx.patches.music.utils.resourceid.colorGrey
import app.rvx.patches.music.utils.resourceid.sharedResourceIdPatch
import app.rvx.patches.music.utils.resourceid.text1
import app.rvx.patches.music.utils.resourceid.ytFillSamples
import app.rvx.patches.music.utils.resourceid.ytFillYouTubeMusic
import app.rvx.patches.music.utils.resourceid.ytOutlineSamples
import app.rvx.patches.music.utils.resourceid.ytOutlineYouTubeMusic
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.addPreferenceWithIntent
import app.rvx.patches.music.utils.settings.addSwitchPreference
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.util.REGISTER_TEMPLATE_REPLACEMENT
import app.rvx.util.Utils.printWarn
import app.rvx.util.fingerprint.matchOrThrow
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionOrThrow
import app.rvx.util.indexOfFirstInstructionReversedOrThrow
import app.rvx.util.indexOfFirstLiteralInstructionOrThrow
import app.rvx.util.replaceLiteralInstructionCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val FLAG = "android:layout_weight"
private const val RESOURCE_FILE_PATH = "res/layout/image_with_text_tab.xml"

private val navigationBarComponentsResourcePatch = resourcePatch(
    description = "navigationBarComponentsResourcePatch"
) {
    apply {
        document(RESOURCE_FILE_PATH).use { document ->
            with(document.getElementsByTagName("ImageView").item(0)) {
                if (attributes.getNamedItem(FLAG) != null)
                    return@with

                document.createAttribute(FLAG)
                    .apply { value = "0.5" }
                    .let(attributes::setNamedItem)
            }
        }
    }
}

@Suppress("unused")
val navigationBarComponentsPatch = bytecodePatch(
    NAVIGATION_BAR_COMPONENTS.displayTitle,
    NAVIGATION_BAR_COMPONENTS.summary,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        changeStartPagePatch,
        navigationBarComponentsResourcePatch,
        sharedResourceIdPatch,
        settingsPatch,
        versionCheckPatch,
    )

    apply {
        /**
         * Enable custom navigation bar color
         */
        tabLayoutFingerprint.methodOrThrow().apply {
            val constIndex = indexOfFirstLiteralInstructionOrThrow(colorGrey)
            val insertIndex = indexOfFirstInstructionOrThrow(constIndex) {
                opcode == Opcode.INVOKE_VIRTUAL
                        && getReference<MethodReference>()?.name == "setBackgroundColor"
            }
            val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

            addInstructions(
                insertIndex, """
                    invoke-static {}, $NAVIGATION_CLASS_DESCRIPTOR->enableCustomNavigationBarColor()I
                    move-result v$insertRegister
                    """
            )
        }

        /**
         * Hide navigation labels
         */
        tabLayoutTextFingerprint.methodOrThrow().apply {
            val constIndex =
                indexOfFirstLiteralInstructionOrThrow(text1)
            val targetIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.CHECK_CAST)
            val targetParameter = getInstruction<ReferenceInstruction>(targetIndex).reference
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            if (!targetParameter.toString().endsWith("Landroid/widget/TextView;"))
                throw PatchException("Method signature parameter did not match: $targetParameter")

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $NAVIGATION_CLASS_DESCRIPTOR->hideNavigationLabel(Landroid/widget/TextView;)V"
            )
        }

        /**
         * Hide navigation bar & buttons
         */
        tabLayoutTextFingerprint.matchOrThrow().let {
            it.method.apply {
                val mapIndex = indexOfMapInstruction(this)
                val browseIdRegister =
                    getInstruction<FiveRegisterInstruction>(mapIndex).registerD
                val browseIdIndex = indexOfFirstInstructionReversedOrThrow(mapIndex + 1) {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.type == "Ljava/lang/String;" &&
                            (this as TwoRegisterInstruction).registerA == browseIdRegister
                }
                val browseIdClassRegister =
                    getInstruction<TwoRegisterInstruction>(browseIdIndex).registerB
                val browseIdFieldName =
                    (getInstruction<ReferenceInstruction>(browseIdIndex).reference as FieldReference).name

                val enumIndex = it.patternMatch!!.startIndex + 3
                val enumRegister = getInstruction<OneRegisterInstruction>(enumIndex).registerA
                val insertEnumIndex = indexOfFirstInstructionOrThrow(Opcode.AND_INT_LIT8) - 2

                val pivotTabIndex = indexOfGetVisibilityInstruction(this)
                val pivotTabRegister =
                    getInstruction<FiveRegisterInstruction>(pivotTabIndex).registerC

                val spannedIndex = indexOfSetTextInstruction(this)
                val spannedRegister =
                    getInstruction<FiveRegisterInstruction>(spannedIndex).registerD

                addInstruction(
                    pivotTabIndex,
                    "invoke-static {v$pivotTabRegister}, $NAVIGATION_CLASS_DESCRIPTOR->hideNavigationButton(Landroid/view/View;)V"
                )

                addInstructions(
                    mapIndex, """
                        const-string v$enumRegister, "$browseIdFieldName"
                        invoke-static {v$browseIdClassRegister, v$browseIdRegister, v$enumRegister}, $NAVIGATION_CLASS_DESCRIPTOR->replaceBrowseId(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$browseIdRegister
                        """
                )

                addInstructions(
                    spannedIndex, """
                        invoke-static {v$spannedRegister}, $NAVIGATION_CLASS_DESCRIPTOR->replaceNavigationLabel(Landroid/text/Spanned;)Landroid/text/Spanned;
                        move-result-object v$spannedRegister
                        """
                )

                addInstruction(
                    insertEnumIndex,
                    "invoke-static {v$enumRegister}, $NAVIGATION_CLASS_DESCRIPTOR->setLastAppNavigationEnum(Ljava/lang/Enum;)V"
                )
            }
        }

        val smaliInstruction = """
            invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $NAVIGATION_CLASS_DESCRIPTOR->replaceNavigationIcon(I)I
            move-result v$REGISTER_TEMPLATE_REPLACEMENT
            """

        arrayOf(
            ytFillSamples,
            ytFillYouTubeMusic,
            ytOutlineSamples,
            ytOutlineYouTubeMusic,
        ).forEach { literal ->
            replaceLiteralInstructionCall(literal, smaliInstruction)
        }

        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_enable_custom_navigation_bar_color",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.NAVIGATION,
            "revanced_custom_navigation_bar_color_value",
            "revanced_enable_custom_navigation_bar_color"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_home_button",
            "false"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_samples_button",
            "false"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_explore_button",
            "false"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_library_button",
            "false"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_upgrade_button",
            "true"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_bar",
            "false"
        )
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_hide_navigation_label",
            "false"
        )
        if (is_6_27_or_greater && !is_8_29_or_greater) {
            addSwitchPreference(
                CategoryType.NAVIGATION,
                "revanced_replace_navigation_samples_button",
                "false"
            )
        } else {
            printWarn("\"Replace Samples button\" is not supported in this version. Use YouTube Music 6.29.59 - 8.28.54.")
        }
        addSwitchPreference(
            CategoryType.NAVIGATION,
            "revanced_replace_navigation_upgrade_button",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.NAVIGATION,
            "revanced_replace_navigation_button_about"
        )

        updatePatchStatus(NAVIGATION_BAR_COMPONENTS)

    }
}
