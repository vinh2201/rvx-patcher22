package app.rvx.patches.reddit.layout.navigation

import app.rvx.util.fingerprint.matchOrNull
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.reddit.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.reddit.utils.compatibility.Constants.REDDIT_PACKAGE_NAME
import app.rvx.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.rvx.patches.reddit.utils.patch.PatchList.HIDE_NAVIGATION_BUTTONS
import app.rvx.patches.reddit.utils.settings.is_2024_26_or_greater
import app.rvx.patches.reddit.utils.settings.is_2025_06_or_greater
import app.rvx.patches.reddit.utils.settings.settingsPatch
import app.rvx.patches.reddit.utils.settings.updatePatchStatus
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.fingerprint.resolvable
import app.rvx.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/NavigationButtonsPatch;"

@Suppress("unused")
val navigationButtonsPatch = bytecodePatch(
    HIDE_NAVIGATION_BUTTONS.displayTitle,
    HIDE_NAVIGATION_BUTTONS.summary,
) {
    compatibleWith(REDDIT_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(settingsPatch)

    apply {

        if (is_2024_26_or_greater) {
            val fingerprints = mutableListOf(bottomNavScreenSetupBottomNavigationFingerprint)

            if (is_2025_06_or_greater) fingerprints += composeBottomNavScreenFingerprint

            fingerprints.forEach { fingerprint ->
                fingerprint.methodOrThrow().apply {
                    val arrayIndex = indexOfButtonsArrayInstruction(this)
                    val arrayRegister =
                        getInstruction<OneRegisterInstruction>(arrayIndex + 1).registerA

                    addInstructions(
                        arrayIndex + 2, """
                            invoke-static {v$arrayRegister}, $EXTENSION_CLASS_DESCRIPTOR->hideNavigationButtons([Ljava/lang/Object;)[Ljava/lang/Object;
                            move-result-object v$arrayRegister
                            """
                    )
                }
            }
        } else {
            if (bottomNavScreenFingerprint.resolvable()) {
                val bottomNavScreenMutableClass = with(bottomNavScreenFingerprint.methodOrThrow()) {
                    val startIndex = indexOfGetDimensionPixelSizeInstruction(this)
                    val targetIndex =
                        indexOfFirstInstructionOrThrow(startIndex, Opcode.NEW_INSTANCE)
                    val targetReference =
                        getInstruction<ReferenceInstruction>(targetIndex).reference.toString()

                    classDefs.firstOrNull { it.type == targetReference }
                        ?.let { classDefs.getOrReplaceMutable(it) }
                        ?: throw ClassNotFoundException("Failed to find class $targetReference")
                }

                bottomNavScreenOnGlobalLayoutFingerprint.matchOrNull(
                    bottomNavScreenMutableClass
                )
                    ?.let {
                        it.method.apply {
                            val startIndex = it.patternMatch!!.startIndex
                            val targetRegister =
                                getInstruction<FiveRegisterInstruction>(startIndex).registerC

                            addInstruction(
                                startIndex + 1,
                                "invoke-static {v$targetRegister}, $EXTENSION_CLASS_DESCRIPTOR->hideNavigationButtons(Landroid/view/ViewGroup;)V"
                            )
                        }
                    }
            } else {
                // Legacy method.
                bottomNavScreenHandlerFingerprint.methodOrThrow().apply {
                    val targetIndex = indexOfGetItemsInstruction(this) + 1
                    val targetRegister =
                        getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {v$targetRegister}, $EXTENSION_CLASS_DESCRIPTOR->hideNavigationButtons(Ljava/util/List;)Ljava/util/List;
                            move-result-object v$targetRegister
                            """
                    )
                }
            }
        }

        updatePatchStatus(
            "enableNavigationButtons",
            HIDE_NAVIGATION_BUTTONS
        )
    }
}
