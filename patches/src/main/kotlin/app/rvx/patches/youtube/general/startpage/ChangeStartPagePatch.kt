package app.rvx.patches.youtube.general.startpage

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.rvx.patches.youtube.utils.patch.PatchList.CHANGE_START_PAGE
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/ChangeStartPagePatch;"

@Suppress("unused")
val changeStartPagePatch = bytecodePatch(
    CHANGE_START_PAGE.displayTitle,
    CHANGE_START_PAGE.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(settingsPatch)

    apply {

        // Hook browseId.
        browseIdFingerprint.methodOrThrow().apply {
            val browseIdIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.CONST_STRING &&
                        getReference<StringReference>()?.string == "FEwhat_to_watch"
            }
            val browseIdRegister = getInstruction<OneRegisterInstruction>(browseIdIndex).registerA

            addInstructions(
                browseIdIndex + 1, """
                    invoke-static { v$browseIdRegister }, $EXTENSION_CLASS_DESCRIPTOR->overrideBrowseId(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$browseIdRegister
                    """
            )
        }

        // There is no browseId assigned to Shorts and Search.
        // Just hook the Intent action.
        intentFingerprint.methodOrThrow().addInstruction(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->overrideIntent(Landroid/content/Intent;)V"
        )

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: CHANGE_START_PAGE"
            ),
            CHANGE_START_PAGE
        )

        // endregion

    }
}
