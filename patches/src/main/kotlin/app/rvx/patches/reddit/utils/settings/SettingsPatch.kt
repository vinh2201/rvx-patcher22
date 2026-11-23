package app.rvx.patches.reddit.utils.settings

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.rvx.patches.reddit.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.reddit.utils.compatibility.Constants.REDDIT_PACKAGE_NAME
import app.rvx.patches.reddit.utils.extension.Constants.EXTENSION_PATH
import app.rvx.patches.reddit.utils.extension.sharedExtensionPatch
import app.rvx.patches.reddit.utils.fix.signature.spoofSignaturePatch
import app.rvx.patches.reddit.utils.patch.PatchList
import app.rvx.patches.reddit.utils.patch.PatchList.SETTINGS_FOR_REDDIT
import app.rvx.patches.shared.extension.Constants.EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR
import app.rvx.patches.shared.sharedSettingFingerprint
import app.rvx.util.findMethodOrThrow
import app.rvx.util.fingerprint.matchOrThrow
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionOrThrow
import app.rvx.util.indexOfFirstInstructionReversedOrThrow
import app.rvx.util.indexOfFirstStringInstructionOrThrow
import app.rvx.util.valueOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import kotlin.io.path.exists

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$EXTENSION_PATH/settings/ActivityHook;->initialize(Landroid/app/Activity;)V"

private lateinit var acknowledgementsLabelBuilderMethod: MutableMethod
private lateinit var settingsStatusLoadMethod: MutableMethod

var is_2024_26_or_greater = false
    private set
var is_2024_41_or_greater = false
    private set
var is_2025_01_or_greater = false
    private set
var is_2025_05_or_greater = false
    private set
var is_2025_06_or_greater = false
    private set

private val settingsBytecodePatch = bytecodePatch(
    description = "settingsBytecodePatch"
) {

    apply {

        /**
         * Set version info
         */
        redditInternalFeaturesFingerprint.methodOrThrow().apply {
            val versionIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.CONST_STRING
                        && (this as? BuilderInstruction21c)?.reference.toString().startsWith("202")
            }

            val versionNumber =
                getInstruction<BuilderInstruction21c>(versionIndex).reference.toString()
                    .replace(".", "").toInt()

            is_2024_26_or_greater = 2024260 <= versionNumber
            is_2024_41_or_greater = 2024410 <= versionNumber
            is_2025_01_or_greater = 2025010 <= versionNumber
            is_2025_05_or_greater = 2025050 <= versionNumber
            is_2025_06_or_greater = 2025060 <= versionNumber
        }

        /**
         * Set SharedPrefCategory
         */
        sharedSettingFingerprint.methodOrThrow().apply {
            val stringIndex = indexOfFirstInstructionOrThrow(Opcode.CONST_STRING)
            val stringRegister = getInstruction<OneRegisterInstruction>(stringIndex).registerA

            replaceInstruction(
                stringIndex,
                "const-string v$stringRegister, \"reddit_revanced\""
            )
        }

        /**
         * Replace settings label
         */
        acknowledgementsLabelBuilderMethod =
            acknowledgementsLabelBuilderFingerprint.methodOrThrow()

        /**
         * Initialize settings activity
         */
        ossLicensesMenuActivityOnCreateFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.startIndex + 1

                addInstructions(
                    insertIndex, """
                        invoke-static {p0}, $EXTENSION_METHOD_DESCRIPTOR
                        return-void
                        """
                )
            }
        }

        settingsStatusLoadMethod = settingsStatusLoadFingerprint.methodOrThrow()

        findMethodOrThrow(EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR) {
            name == "setThemeColor"
        }.addInstruction(
            0,
            "invoke-static {}, $EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR->updateDarkModeStatus()V"
        )
    }
}

internal fun updateSettingsLabel(label: String) =
    acknowledgementsLabelBuilderMethod.apply {
        val stringIndex = indexOfFirstStringInstructionOrThrow("onboardingAnalytics")
        val insertIndex = indexOfFirstInstructionReversedOrThrow(stringIndex) {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.name == "getString"
        } + 2
        val insertRegister =
            getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

        addInstruction(
            insertIndex,
            "const-string v$insertRegister, \"$label\""
        )
    }

internal fun updatePatchStatus(description: String) =
    settingsStatusLoadMethod.addInstruction(
        0,
        "invoke-static {}, $EXTENSION_PATH/settings/SettingsStatus;->$description()V"
    )

internal fun updatePatchStatus(patch: PatchList) {
    patch.included = true
}

internal fun updatePatchStatus(
    description: String,
    patch: PatchList
) {
    updatePatchStatus(description)
    updatePatchStatus(patch)
}

private const val DEFAULT_LABEL = "RVX"

val settingsPatch = resourcePatch(
    SETTINGS_FOR_REDDIT.displayTitle,
    SETTINGS_FOR_REDDIT.summary,
) {
    compatibleWith(REDDIT_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        sharedExtensionPatch,
        settingsBytecodePatch,
        spoofSignaturePatch,
    )

    val rvxSettingsLabel = stringOption(
        default = DEFAULT_LABEL,
        values = mapOf(
            "ReVanced Extended" to "ReVanced Extended",
            "RVX" to DEFAULT_LABEL,
        ),
        name = "RVX settings menu name",
        description = "The name of the RVX settings menu.",
        required = true
    )

    apply {
        /**
         * Replace settings icon and label
         */
        val settingsLabel = rvxSettingsLabel
            .valueOrThrow()

        arrayOf(
            "preferences.xml",
            "preferences_logged_in.xml",
            "preferences_logged_in_old.xml",
        ).forEach { targetXML ->
            val resDirectory = get("res")
            val targetXml = resDirectory.resolve("xml").resolve(targetXML).toPath()

            if (targetXml.exists()) {
                val preference = get("res/xml/$targetXML")

                preference.writeText(
                    preference.readText()
                        .replace(
                            "\"@drawable/icon_text_post\" android:title=\"@string/label_acknowledgements\"",
                            "\"@drawable/icon_beta_planet\" android:title=\"$settingsLabel\""
                        )
                )
            }
        }

        updateSettingsLabel(settingsLabel)
        updatePatchStatus(SETTINGS_FOR_REDDIT)
    }
}
