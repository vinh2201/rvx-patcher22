package app.rvx.patches.music.general.spoofappversion

import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.rvx.patches.music.utils.patch.PatchList.SPOOF_APP_VERSION_FOR_LYRICS
import app.rvx.patches.music.utils.playservice.is_6_43_or_greater
import app.rvx.patches.music.utils.playservice.is_7_13_or_greater
import app.rvx.patches.music.utils.playservice.is_8_07_or_greater
import app.rvx.patches.music.utils.playservice.is_8_30_or_greater
import app.rvx.patches.music.utils.playservice.is_8_33_or_greater
import app.rvx.patches.music.utils.playservice.versionCheckPatch
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.addPreferenceWithIntent
import app.rvx.patches.music.utils.settings.addSwitchPreference
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.patches.shared.CLIENT_INFO_CLASS_DESCRIPTOR
import app.rvx.patches.shared.createPlayerRequestBodyWithModelFingerprint
import app.rvx.patches.shared.indexOfReleaseInstruction
import app.rvx.patches.shared.spoof.browse.addClientInfoHook
import app.rvx.patches.shared.spoof.browse.spoofClientBrowseEndpointPatch
import app.rvx.util.ResourceGroup
import app.rvx.util.Utils.printWarn
import app.rvx.util.appendAppVersion
import app.rvx.util.copyResources
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private val spoofAppVersionForLyricsBytecodePatch = bytecodePatch(
    description = "spoofAppVersionForLyricsBytecodePatch"
) {
    dependsOn(spoofClientBrowseEndpointPatch)

    apply patchApply@{
        createPlayerRequestBodyWithModelFingerprint.methodOrThrow().apply {
            val appVersionIndex = indexOfReleaseInstruction(this) + 1
            val appVersionFieldIndex =
                indexOfFirstInstructionReversedOrThrow(appVersionIndex) {
                    val reference = getReference<FieldReference>()
                    opcode == Opcode.IPUT_OBJECT &&
                            reference?.type == "Ljava/lang/String;" &&
                            reference.definingClass == CLIENT_INFO_CLASS_DESCRIPTOR
                }
            val appVersionField =
                getInstruction<ReferenceInstruction>(appVersionFieldIndex).reference

            addClientInfoHook(
                helperMethodName = "patch_setClientAppVersion",
                smaliInstructions = """
                    invoke-static {v3}, $GENERAL_CLASS_DESCRIPTOR->getLyricsVersionOverride(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v2
                    iput-object v2, v1, $appVersionField
                    """,
                insertLast = false
            )
        }
    }
}

@Suppress("unused")
val spoofAppVersionForLyricsPatch = resourcePatch(
    SPOOF_APP_VERSION_FOR_LYRICS.displayTitle,
    SPOOF_APP_VERSION_FOR_LYRICS.summary,
) {
    compatibleWith(
        YOUTUBE_MUSIC_PACKAGE_NAME(
            "6.51.53",
            "7.16.53",
            "7.25.53",
            "8.12.54",
            "8.28.54",
            "8.30.54",
        ),
    )

    dependsOn(
        spoofAppVersionForLyricsBytecodePatch,
        settingsPatch,
        versionCheckPatch,
    )

    apply patchApply@{
        if (!is_6_43_or_greater) {
            printWarn("\"${SPOOF_APP_VERSION_FOR_LYRICS.title}\" is not supported in this version. Use YouTube Music 6.51.53 or later.")
            return@patchApply
        }

        fun appendLyricsAppVersion(appVersion: String) =
            appendAppVersion(
                appVersion,
                "revanced_spoof_app_version_for_lyrics_target"
            )

        if (is_8_30_or_greater && !is_8_33_or_greater) {
            copyResources(
                "music/lyrics",
                ResourceGroup(
                    "drawable",
                    "yt_outline_experimental_translate_vd_theme_24.xml",
                    "yt_outline_translate_vd_theme_24.xml"
                )
            )

            appendLyricsAppVersion("8.33.54")
        }
        if (is_8_07_or_greater) {
            appendLyricsAppVersion("8.06.52")
        }
        if (is_7_13_or_greater) {
            appendLyricsAppVersion("7.12.52")
        }
        appendLyricsAppVersion("6.42.55")

        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_spoof_app_version_for_lyrics",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.GENERAL,
            "revanced_spoof_app_version_for_lyrics_target",
            "revanced_spoof_app_version_for_lyrics"
        )

        updatePatchStatus(SPOOF_APP_VERSION_FOR_LYRICS)

    }
}
