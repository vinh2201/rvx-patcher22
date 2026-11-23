package app.rvx.patches.youtube.layout.playerbuttonbg

import app.revanced.patcher.patch.resourcePatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.patch.PatchList.FORCE_HIDE_PLAYER_BUTTONS_BACKGROUND
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.util.doRecursively
import org.w3c.dom.Element

@Suppress("unused")
val playerButtonBackgroundPatch = resourcePatch(
    FORCE_HIDE_PLAYER_BUTTONS_BACKGROUND.displayTitle,
    FORCE_HIDE_PLAYER_BUTTONS_BACKGROUND.summary,
    false,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(settingsPatch)

    apply {
        document("res/drawable/player_button_circle_background.xml").use { document ->

            document.doRecursively node@{ node ->
                if (node !is Element) return@node

                node.getAttributeNode("android:color")?.let { attribute ->
                    attribute.textContent = "@android:color/transparent"
                }
            }
        }

        addPreference(FORCE_HIDE_PLAYER_BUTTONS_BACKGROUND)

    }
}
