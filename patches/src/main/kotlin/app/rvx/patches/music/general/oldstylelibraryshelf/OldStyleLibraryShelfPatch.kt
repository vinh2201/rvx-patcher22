package app.rvx.patches.music.general.oldstylelibraryshelf

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.rvx.patches.music.utils.patch.PatchList.RESTORE_OLD_STYLE_LIBRARY_SHELF
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.addSwitchPreference
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.indexOfFirstInstructionReversedOrThrow
import app.rvx.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
val oldStyleLibraryShelfPatch = bytecodePatch(
    RESTORE_OLD_STYLE_LIBRARY_SHELF.displayTitle,
    RESTORE_OLD_STYLE_LIBRARY_SHELF.summary,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(settingsPatch)

    apply {

        browseIdFingerprint.methodOrThrow().apply {
            val stringIndex = indexOfFirstStringInstructionOrThrow("FEmusic_offline")
            val targetIndex =
                indexOfFirstInstructionReversedOrThrow(stringIndex, Opcode.IGET_OBJECT)
            val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

            addInstructions(
                targetIndex + 1, """
                    invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->restoreOldStyleLibraryShelf(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$targetRegister
                    """
            )
        }

        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_restore_old_style_library_shelf",
            "false"
        )

        updatePatchStatus(RESTORE_OLD_STYLE_LIBRARY_SHELF)

    }
}
