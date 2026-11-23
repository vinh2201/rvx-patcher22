package app.rvx.patches.music.general.redirection

import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.extensions.ExternalLabel
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.rvx.patches.music.utils.patch.PatchList.DISABLE_DISLIKE_REDIRECTION
import app.rvx.patches.music.utils.playservice.is_7_29_or_greater
import app.rvx.patches.music.utils.playservice.versionCheckPatch
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.addSwitchPreference
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionOrThrow
import app.rvx.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

var onClickReference = ""

@Suppress("unused")
val dislikeRedirectionPatch = bytecodePatch(
    DISABLE_DISLIKE_REDIRECTION.displayTitle,
    DISABLE_DISLIKE_REDIRECTION.summary,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        settingsPatch,
        versionCheckPatch,
    )

    apply {

        notificationLikeButtonOnClickListenerFingerprint
            .methodOrThrow(notificationLikeButtonControllerFingerprint)
            .apply {
                val mapIndex = indexOfMapInstruction(this)
                val onClickIndex = indexOfFirstInstructionOrThrow(mapIndex) {
                    val reference = getReference<MethodReference>()

                    opcode == Opcode.INVOKE_INTERFACE &&
                            reference?.returnType == "V" &&
                            reference.parameterTypes.size == 1
                }
                onClickReference =
                    getInstruction<ReferenceInstruction>(onClickIndex).reference.toString()

                disableDislikeRedirection(onClickIndex)
            }

        if (is_7_29_or_greater) {
            dislikeButtonOnClickListenerFingerprint
                .methodOrThrow()
                .disableDislikeRedirection()
        } else {
            dislikeButtonOnClickListenerLegacyFingerprint
                .methodOrThrow()
                .disableDislikeRedirection()
        }

        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_disable_dislike_redirection",
            "false"
        )

        updatePatchStatus(DISABLE_DISLIKE_REDIRECTION)

    }
}

private fun MutableMethod.disableDislikeRedirection(startIndex: Int = 0) {
    val onClickIndex =
        if (startIndex == 0) {
            indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.toString() == onClickReference
            }
        } else {
            startIndex
        }
    val targetIndex = indexOfFirstInstructionReversedOrThrow(onClickIndex, Opcode.IF_EQZ)
    val insertRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

    addInstructionsWithLabels(
        targetIndex + 1, """
            invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->disableDislikeRedirection()Z
            move-result v$insertRegister
            if-nez v$insertRegister, :disable
            """, ExternalLabel("disable", getInstruction(onClickIndex + 1))
    )
}
