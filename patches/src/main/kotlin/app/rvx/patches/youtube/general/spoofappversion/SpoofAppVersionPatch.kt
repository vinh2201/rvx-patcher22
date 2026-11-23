package app.rvx.patches.youtube.general.spoofappversion

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.extensions.ExternalLabel
import app.rvx.patches.shared.clientParameterFingerprint
import app.rvx.patches.shared.spoof.appversion.baseSpoofAppVersionPatch
import app.rvx.patches.youtube.utils.CAIRO_FRAGMENT_FEATURE_FLAG
import app.rvx.patches.youtube.utils.cairoFragmentConfigFingerprint
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.rvx.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.rvx.patches.youtube.utils.indexOfGetDrawableInstruction
import app.rvx.patches.youtube.utils.patch.PatchList.SPOOF_APP_VERSION
import app.rvx.patches.youtube.utils.playservice.is_19_26_or_greater
import app.rvx.patches.youtube.utils.playservice.is_19_34_or_greater
import app.rvx.patches.youtube.utils.playservice.versionCheckPatch
import app.rvx.patches.youtube.utils.resourceid.settingsFragment
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.cairoFragmentDisabled
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.patches.youtube.utils.settingsFragmentSyntheticFingerprint
import app.rvx.patches.youtube.utils.toolBarButtonFingerprint
import app.rvx.util.Utils.printWarn
import app.rvx.util.findMethodOrThrow
import app.rvx.util.fingerprint.injectLiteralInstructionBooleanCall
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionOrThrow
import app.rvx.util.indexOfFirstInstructionReversedOrThrow
import app.rvx.util.indexOfFirstLiteralInstructionOrThrow
import app.rvx.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private val spoofAppVersionBytecodePatch = bytecodePatch(
    description = "spoofAppVersionBytecodePatch"
) {

    dependsOn(
        settingsPatch,
        versionCheckPatch
    )

    apply {
        // Spoof parameters (cver, cbrver) in URL
        clientParameterFingerprint.methodOrThrow().apply {
            addInstructions(
                0,
                """
                invoke-static {p2}, $GENERAL_CLASS_DESCRIPTOR->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p2
                """
            )
        }

        findMethodOrThrow(PATCH_STATUS_CLASS_DESCRIPTOR) {
            name == "SpoofAppVersion"
        }.returnEarly(true)

        /**
         * When spoofing the app version to YouTube 19.20.xx or earlier via Spoof app version on YouTube 19.23.xx+, the Library tab will crash.
         * As a temporary workaround, do not set an image in the toolbar when the enum name is UNKNOWN.
         */
        toolBarButtonFingerprint.methodOrThrow().apply {
            val getDrawableIndex = indexOfGetDrawableInstruction(this)
            val enumOrdinalIndex = indexOfFirstInstructionReversedOrThrow(getDrawableIndex) {
                opcode == Opcode.INVOKE_INTERFACE &&
                        getReference<MethodReference>()?.returnType == "I"
            }
            val insertIndex = enumOrdinalIndex + 2
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA
            val jumpIndex = indexOfFirstInstructionOrThrow(insertIndex) {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "setImageDrawable"
            } + 1

            addInstructionsWithLabels(
                insertIndex, """
                    if-eqz v$insertRegister, :ignore
                    """, ExternalLabel("ignore", getInstruction(jumpIndex))
            )
        }

        /**
         * RVX does not use CairoFragment, and uses a different method to restore the 'Playback' setting.
         * If the app version is spoofed to 19.30 or earlier, the 'Playback' setting will be broken.
         * Add a setting to fix this.
         */
        if (is_19_34_or_greater && cairoFragmentDisabled) {
            cairoFragmentConfigFingerprint.injectLiteralInstructionBooleanCall(
                CAIRO_FRAGMENT_FEATURE_FLAG,
                "$GENERAL_CLASS_DESCRIPTOR->disableCairoFragment(Z)Z"
            )

            settingsFragmentSyntheticFingerprint.methodOrThrow().apply {
                val index = indexOfFirstLiteralInstructionOrThrow(settingsFragment)
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index + 1, """
                        invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->useLegacyFragment(I)I
                        move-result v$register
                        """
                )
            }
        }
    }

}

@Suppress("unused")
val spoofAppVersionPatch = resourcePatch(
    SPOOF_APP_VERSION.displayTitle,
    SPOOF_APP_VERSION.summary,
) {

    dependsOn(
        baseSpoofAppVersionPatch("$GENERAL_CLASS_DESCRIPTOR->getVersionOverride(Ljava/lang/String;)Ljava/lang/String;"),
        spoofAppVersionBytecodePatch,
        settingsPatch,
        versionCheckPatch,
    )

    apply {
        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: SPOOFING",
            "SETTINGS: SPOOF_APP_VERSION"
        )

        if (is_19_34_or_greater && cairoFragmentDisabled) {
            settingArray += "SETTINGS: FIX_SPOOF_APP_VERSION_SIDE_EFFECT"
        }

        addPreference(
            settingArray,
            SPOOF_APP_VERSION
        )
    }
}
