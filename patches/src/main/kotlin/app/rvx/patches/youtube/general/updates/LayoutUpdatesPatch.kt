package app.rvx.patches.youtube.general.updates

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.rvx.patches.youtube.utils.patch.PatchList.DISABLE_LAYOUT_UPDATES
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.util.fingerprint.matchOrThrow

@Suppress("unused")
val layoutUpdatesPatch = bytecodePatch(
    DISABLE_LAYOUT_UPDATES.displayTitle,
    DISABLE_LAYOUT_UPDATES.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(settingsPatch)

    apply {

        cronetHeaderFingerprint.matchOrThrow().let {
            it.method.apply {
                val index = it.stringMatches!!.first().index

                addInstructions(
                    index, """
                        invoke-static {p1, p2}, $GENERAL_CLASS_DESCRIPTOR->disableLayoutUpdates(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                        move-result-object p2
                        """
                )
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: SPOOFING",
                "SETTINGS: DISABLE_LAYOUT_UPDATES"
            ),
            DISABLE_LAYOUT_UPDATES
        )

        // endregion

    }
}
