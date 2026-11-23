package app.rvx.patches.music.general.splash

import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.ExternalLabel
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.extension.Constants.GENERAL_PATH
import app.rvx.patches.music.utils.patch.PatchList.DISABLE_CAIRO_SPLASH_ANIMATION
import app.rvx.patches.music.utils.playservice.is_7_16_or_greater
import app.rvx.patches.music.utils.playservice.is_7_20_or_greater
import app.rvx.patches.music.utils.playservice.versionCheckPatch
import app.rvx.patches.music.utils.resourceid.mainActivityLaunchAnimation
import app.rvx.patches.music.utils.resourceid.sharedResourceIdPatch
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.addSwitchPreference
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.util.Utils.printWarn
import app.rvx.util.findFreeRegister
import app.rvx.util.fingerprint.injectLiteralInstructionBooleanCall
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionOrThrow
import app.rvx.util.indexOfFirstLiteralInstructionOrThrow
import app.rvx.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$GENERAL_PATH/CairoSplashAnimationPatch;->disableCairoSplashAnimation(Z)Z"

@Suppress("unused")
val cairoSplashAnimationPatch = bytecodePatch(
    DISABLE_CAIRO_SPLASH_ANIMATION.displayTitle,
    DISABLE_CAIRO_SPLASH_ANIMATION.summary,
) {
    compatibleWith(
        YOUTUBE_MUSIC_PACKAGE_NAME(
            "7.16.53",
            "7.25.53",
            "8.12.54",
            "8.28.54",
            "8.30.54",
        ),
    )

    dependsOn(
        settingsPatch,
        sharedResourceIdPatch,
        versionCheckPatch,
    )

    apply patchApply@{
        if (!is_7_16_or_greater) {
            printWarn("\"${DISABLE_CAIRO_SPLASH_ANIMATION.title}\" is not supported in this version. Use YouTube Music 7.16.53 or later.")
            return@patchApply
        }

        fun MutableMethod.getMoveInstructions(
            startIndex: Int,
            endIndex: Int
        ): String {
            var smaliInstructions = ""
            val moveOpcodes = setOf(
                Opcode.MOVE, Opcode.MOVE_16,
                Opcode.MOVE_OBJECT, Opcode.MOVE_OBJECT_FROM16,
                Opcode.MOVE_WIDE, Opcode.MOVE_WIDE_16,
                Opcode.MOVE_WIDE_FROM16, Opcode.MOVE_FROM16
            )
            for (index in startIndex..endIndex) {
                val opcode = getInstruction(index).opcode
                if (moveOpcodes.contains(opcode)) {
                    val opcodeName = opcode.name
                    val instruction = getInstruction<TwoRegisterInstruction>(index)

                    val line = """
                        ${opcode.name} v${instruction.registerA}, v${instruction.registerB}
                        
                        """.trimIndent()

                    smaliInstructions += line
                }
            }

            return smaliInstructions
        }

        if (!is_7_20_or_greater) {
            cairoSplashAnimationConfigFingerprint.injectLiteralInstructionBooleanCall(
                CAIRO_SPLASH_ANIMATION_FEATURE_FLAG,
                EXTENSION_METHOD_DESCRIPTOR
            )
        } else {
            cairoSplashAnimationConfigFingerprint.methodOrThrow().apply {
                val stringIndex = indexOfFirstStringInstructionOrThrow("sa_e")
                val gotoIndex = indexOfFirstInstructionOrThrow(stringIndex) {
                    opcode == Opcode.GOTO || opcode == Opcode.GOTO_16
                }
                val literalIndex = indexOfFirstLiteralInstructionOrThrow(
                    mainActivityLaunchAnimation
                )
                val insertIndex =
                    indexOfSetContentViewInstruction(this, literalIndex) + 1
                val freeRegister = findFreeRegister(insertIndex, false)
                val jumpIndex = indexOfFirstInstructionOrThrow(insertIndex) {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.parameterTypes?.firstOrNull() == "Ljava/lang/Runnable;"
                } + 1

                addInstructionsWithLabels(
                    insertIndex,
                    getMoveInstructions(stringIndex, gotoIndex) + """
                        const/4 v$freeRegister, 0x1
                        invoke-static {v$freeRegister}, $EXTENSION_METHOD_DESCRIPTOR
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :skip
                        """, ExternalLabel("skip", getInstruction(jumpIndex))
                )
            }
        }

        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_disable_cairo_splash_animation",
            "false"
        )

        updatePatchStatus(DISABLE_CAIRO_SPLASH_ANIMATION)

    }
}
