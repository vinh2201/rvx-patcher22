package app.rvx.patches.music.misc.tracking

import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.patch.PatchList.SANITIZE_SHARING_LINKS
import app.rvx.patches.music.utils.playservice.is_8_05_or_greater
import app.rvx.patches.music.utils.playservice.versionCheckPatch
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.addSwitchPreference
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.patches.shared.tracking.baseSanitizeUrlQueryPatch
import app.rvx.patches.shared.tracking.hookQueryParameters
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionOrThrow
import app.rvx.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val sanitizeUrlQueryPatch = bytecodePatch(
    SANITIZE_SHARING_LINKS.displayTitle,
    SANITIZE_SHARING_LINKS.summary,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        baseSanitizeUrlQueryPatch,
        settingsPatch,
        versionCheckPatch,
    )

    apply {

        if (is_8_05_or_greater) {
            imageShareLinkFormatterFingerprint.methodOrThrow().apply {
                val stringIndex = indexOfFirstStringInstructionOrThrow("android.intent.extra.TEXT")
                val insertIndex = indexOfFirstInstructionOrThrow(stringIndex) {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.name == "putExtra" &&
                            reference.definingClass == "Landroid/content/Intent;"
                }

                hookQueryParameters(insertIndex)
            }
        }

        addSwitchPreference(
            CategoryType.MISC,
            "revanced_sanitize_sharing_links",
            "true"
        )

        updatePatchStatus(SANITIZE_SHARING_LINKS)

    }
}