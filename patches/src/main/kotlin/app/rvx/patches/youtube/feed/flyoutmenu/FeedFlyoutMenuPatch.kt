package app.rvx.patches.youtube.feed.flyoutmenu

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.youtube.utils.bottomSheetMenuItemBuilderFingerprint
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.Constants.FEED_CLASS_DESCRIPTOR
import app.rvx.patches.youtube.utils.indexOfSpannedCharSequenceInstruction
import app.rvx.patches.youtube.utils.patch.PatchList.HIDE_FEED_FLYOUT_MENU
import app.rvx.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.util.fingerprint.matchOrThrow
import app.rvx.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val feedFlyoutMenuPatch = bytecodePatch(
    HIDE_FEED_FLYOUT_MENU.displayTitle,
    HIDE_FEED_FLYOUT_MENU.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        sharedResourceIdPatch,
        settingsPatch,
    )
    apply {

        // region patch for phone

        bottomSheetMenuItemBuilderFingerprint.methodOrThrow().apply {
            val insertIndex = indexOfSpannedCharSequenceInstruction(this) + 2
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

            addInstructions(
                insertIndex, """
                    invoke-static {v$insertRegister}, $FEED_CLASS_DESCRIPTOR->hideFlyoutMenu(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                    move-result-object v$insertRegister
                    """
            )
        }

        // endregion

        // region patch for tablet

        contextualMenuItemBuilderFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = it.patternMatch!!.startIndex + 1
                val targetInstruction = getInstruction<Instruction35c>(targetIndex)

                val targetReferenceName =
                    (targetInstruction.reference as MethodReference).name
                if (targetReferenceName != "setText")
                    throw PatchException("Method name did not match: $targetReferenceName")

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, " +
                            "$FEED_CLASS_DESCRIPTOR->hideFlyoutMenu(Landroid/widget/TextView;Ljava/lang/CharSequence;)V"
                )
            }
        }

        // endregion

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: FEED",
                "SETTINGS: HIDE_FEED_FLYOUT_MENU"
            ),
            HIDE_FEED_FLYOUT_MENU
        )

        // endregion

    }
}
