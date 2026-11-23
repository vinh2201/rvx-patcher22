package app.rvx.patches.youtube.layout.translations

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.rvx.patches.shared.translations.APP_LANGUAGES
import app.rvx.patches.shared.translations.baseTranslationsPatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.patch.PatchList.TRANSLATIONS_FOR_YOUTUBE
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch

// Array of supported translations, each represented by its language code.
private val SUPPORTED_TRANSLATIONS = setOf(
    "ar", "bg-rBG", "de-rDE", "el-rGR", "es-rES", "fr-rFR", "hu-rHU", "id-rID", "in", "it-rIT", "ja-rJP",
    "ko-rKR", "pl-rPL", "pt-rBR", "ru-rRU", "tr-rTR", "uk-rUA", "vi-rVN", "zh-rCN", "zh-rTW"
)

@Suppress("unused")
val translationsPatch = resourcePatch(
    TRANSLATIONS_FOR_YOUTUBE.displayTitle,
    TRANSLATIONS_FOR_YOUTUBE.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(settingsPatch)

    val customTranslations by stringOption(
        default = "",
        name = "Custom translations",
        description = """
            The path to the 'strings.xml' file.
            Please note that applying the 'strings.xml' file will overwrite all existing translations.
            """.trimIndent(),
        required = true,
    )

    val selectedTranslations by stringOption(
        default = SUPPORTED_TRANSLATIONS.joinToString(", "),
        name = "Translations to add",
        description = "A list of translations to be added for the RVX settings, separated by commas.",
        required = true,
    )

    val selectedStringResources by stringOption(
        default = APP_LANGUAGES.joinToString(", "),
        name = "String resources to keep",
        description = """
            A list of string resources to be kept, separated by commas.
            String resources not in the list will be removed from the app.

            Default string resource, English, is not removed.
            """.trimIndent(),
        required = true,
    )

    apply {
        baseTranslationsPatch(
            customTranslations, selectedTranslations, selectedStringResources,
            SUPPORTED_TRANSLATIONS, "youtube"
        )

        addPreference(TRANSLATIONS_FOR_YOUTUBE)

    }
}
