package app.rvx.patches.youtube.general.dialog

import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.shared.dialog.baseViewerDiscretionDialogPatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.rvx.patches.youtube.utils.patch.PatchList.REMOVE_VIEWER_DISCRETION_DIALOG
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val viewerDiscretionDialogPatch = bytecodePatch(
    REMOVE_VIEWER_DISCRETION_DIALOG.displayTitle,
    REMOVE_VIEWER_DISCRETION_DIALOG.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        baseViewerDiscretionDialogPatch(
            GENERAL_CLASS_DESCRIPTOR,
            true
        ),
        settingsPatch,
    )

    apply {

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: REMOVE_VIEWER_DISCRETION_DIALOG"
            ),
            REMOVE_VIEWER_DISCRETION_DIALOG
        )

        // endregion

    }
}
