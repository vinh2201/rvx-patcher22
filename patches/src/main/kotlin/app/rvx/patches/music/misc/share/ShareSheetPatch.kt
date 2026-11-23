package app.rvx.patches.music.misc.share

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.extension.Constants.COMPONENTS_PATH
import app.rvx.patches.music.utils.extension.Constants.MISC_PATH
import app.rvx.patches.music.utils.patch.PatchList.CHANGE_SHARE_SHEET
import app.rvx.patches.music.utils.resourceid.bottomSheetRecyclerView
import app.rvx.patches.music.utils.resourceid.sharedResourceIdPatch
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.addSwitchPreference
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.patches.shared.litho.addLithoFilter
import app.rvx.patches.shared.litho.lithoFilterPatch
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.indexOfFirstInstructionOrThrow
import app.rvx.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$MISC_PATH/ShareSheetPatch;"

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ShareSheetMenuFilter;"

@Suppress("unused")
val shareSheetPatch = bytecodePatch(
    CHANGE_SHARE_SHEET.displayTitle,
    CHANGE_SHARE_SHEET.summary,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        sharedResourceIdPatch
    )

    apply {
        bottomSheetRecyclerViewFingerprint.methodOrThrow().apply {
            val constIndex = indexOfFirstLiteralInstructionOrThrow(bottomSheetRecyclerView)
            val targetIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.CHECK_CAST)
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $EXTENSION_CLASS_DESCRIPTOR->onShareSheetMenuCreate(Landroid/support/v7/widget/RecyclerView;)V"
            )
        }

        addLithoFilter(FILTER_CLASS_DESCRIPTOR)

        addSwitchPreference(
            CategoryType.MISC,
            "revanced_change_share_sheet",
            "false"
        )

        updatePatchStatus(CHANGE_SHARE_SHEET)

    }
}
