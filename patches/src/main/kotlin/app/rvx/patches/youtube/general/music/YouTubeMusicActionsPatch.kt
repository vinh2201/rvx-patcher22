package app.rvx.patches.youtube.general.music

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.rvx.patches.youtube.utils.patch.PatchList.HOOK_YOUTUBE_MUSIC_ACTIONS
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.util.fingerprint.matchOrThrow
import app.rvx.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/YouTubeMusicActionsPatch;"

@Suppress("unused")
val youtubeMusicActionsPatch = bytecodePatch(
    HOOK_YOUTUBE_MUSIC_ACTIONS.displayTitle,
    HOOK_YOUTUBE_MUSIC_ACTIONS.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(settingsPatch)

    apply {

        appDeepLinkFingerprint.matchOrThrow().let {
            it.method.apply {
                val packageNameIndex = it.patternMatch!!.startIndex
                val packageNameField =
                    getInstruction<ReferenceInstruction>(packageNameIndex).reference.toString()

                implementation!!.instructions
                    .withIndex()
                    .filter { (_, instruction) ->
                        instruction.opcode == Opcode.IGET_OBJECT &&
                                instruction.getReference<FieldReference>()
                                    ?.toString() == packageNameField
                    }
                    .map { (index, _) -> index }
                    .reversed()
                    .forEach { index ->
                        val register = getInstruction<TwoRegisterInstruction>(index).registerA

                        addInstructions(
                            index + 1, """
                                invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->overridePackageName(Ljava/lang/String;)Ljava/lang/String;
                                move-result-object v$register
                                """
                        )
                    }
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: HOOK_BUTTONS",
                "SETTINGS: HOOK_YOUTUBE_MUSIC_ACTIONS"
            ),
            HOOK_YOUTUBE_MUSIC_ACTIONS
        )

        // endregion

    }
}
