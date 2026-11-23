package app.rvx.patches.youtube.general.formfactor

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.shared.CLIENT_INFO_CLASS_DESCRIPTOR
import app.rvx.patches.shared.createPlayerRequestBodyWithModelFingerprint
import app.rvx.patches.shared.spoof.guide.addClientInfoHook
import app.rvx.patches.shared.spoof.guide.spoofClientGuideEndpointPatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.rvx.patches.youtube.utils.navigation.navigationBarHookPatch
import app.rvx.patches.youtube.utils.patch.PatchList.CHANGE_FORM_FACTOR
import app.rvx.patches.youtube.utils.playertype.playerTypeHookPatch
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.util.fingerprint.definingClassOrThrow
import app.rvx.util.fingerprint.matchOrThrow
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/ChangeFormFactorPatch;"

@Suppress("unused")
val changeFormFactorPatch = bytecodePatch(
    CHANGE_FORM_FACTOR.displayTitle,
    CHANGE_FORM_FACTOR.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        settingsPatch,
        playerTypeHookPatch,
        navigationBarHookPatch,
        spoofClientGuideEndpointPatch,
    )

    apply {

        val formFactorEnumClass = formFactorEnumConstructorFingerprint
            .definingClassOrThrow()

        createPlayerRequestBodyWithModelFingerprint.methodOrThrow().apply {
            val ordinalIndex = indexOfFirstInstructionOrThrow {
                val reference = getReference<FieldReference>()
                opcode == Opcode.IGET &&
                        reference?.definingClass == formFactorEnumClass &&
                        reference.type == "I"
            }
            val ordinalRegister = getInstruction<TwoRegisterInstruction>(ordinalIndex).registerA

            // This patch changes the 'clientFormFactor' value to a different value
            addInstructions(
                ordinalIndex + 1, """
                    invoke-static {v$ordinalRegister}, $EXTENSION_CLASS_DESCRIPTOR->getFormFactor(I)I
                    move-result v$ordinalRegister
                    """
            )

            val clientFormFactorOrdinalIndex =
                indexOfFirstInstructionOrThrow(ordinalIndex - 1) {
                    val reference = getReference<FieldReference>()
                    opcode == Opcode.IPUT &&
                            reference?.type == "I" &&
                            reference.definingClass == CLIENT_INFO_CLASS_DESCRIPTOR
                }
            val clientFormFactorOrdinalReference =
                getInstruction<ReferenceInstruction>(clientFormFactorOrdinalIndex).reference

            // Changing 'clientFormFactor' in all requests will also affect the navigation bar
            // If 'clientFormFactor' is 'AUTOMOTIVE_FORM_FACTOR', the 'Shorts' button in the navigation bar will change to 'Explore'
            // To fix this side effect, requests to the '/guide' endpoint, which are related to navigation buttons, use the original 'clientFormFactor'
            addClientInfoHook(
                "patch_setClientFormFactor",
                """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->getFormFactor()I
                    move-result v2
                    iput v2, v1, $clientFormFactorOrdinalReference
                    """
            )
        }

        widthDpUIFingerprint.matchOrThrow().let {
            it.method.apply {
                val index = it.patternMatch!!.startIndex
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index, """
                        invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->getWidthDp(I)I
                        move-result v$register
                        """
                )
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: CHANGE_FORM_FACTOR"
            ),
            CHANGE_FORM_FACTOR
        )

        // endregion

    }
}
