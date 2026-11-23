package app.rvx.patches.shared.settingmenu

import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.shared.extension.Constants.PATCHES_PATH
import app.rvx.util.findMethodOrThrow
import app.rvx.util.fingerprint.methodCall

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/BaseSettingsMenuPatch;"

val settingsMenuPatch = bytecodePatch(
    description = "settingsMenuPatch",
) {
    apply {
        val findPreferenceMethodCall = findPreferenceFingerprint.methodCall()
        val removePreferenceMethodCall = removePreferenceFingerprint.methodCall()

        findMethodOrThrow(EXTENSION_CLASS_DESCRIPTOR) {
            name == "removePreference"
        }.addInstructionsWithLabels(
            0, """
                invoke-virtual {p0, p1}, $findPreferenceMethodCall
                move-result-object v0
                if-eqz v0, :ignore
                invoke-virtual {p0, v0}, $removePreferenceMethodCall
                :ignore
                return-void
                """
        )
    }
}

