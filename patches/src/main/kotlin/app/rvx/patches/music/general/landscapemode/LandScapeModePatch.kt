package app.rvx.patches.music.general.landscapemode

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.rvx.patches.music.utils.patch.PatchList.ENABLE_LANDSCAPE_MODE
import app.rvx.patches.music.utils.resourceid.sharedResourceIdPatch
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.addSwitchPreference
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.util.fingerprint.matchOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val landScapeModePatch = bytecodePatch(
    ENABLE_LANDSCAPE_MODE.displayTitle,
    ENABLE_LANDSCAPE_MODE.summary,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        sharedResourceIdPatch,
        settingsPatch,
    )

    apply {
        tabletIdentifierFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = it.patternMatch!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->enableLandScapeMode(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        }

        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_enable_landscape_mode",
            "false"
        )

        updatePatchStatus(ENABLE_LANDSCAPE_MODE)

    }
}
