package app.rvx.patches.music.general.dialog

import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.rvx.patches.music.utils.patch.PatchList.REMOVE_VIEWER_DISCRETION_DIALOG
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.addSwitchPreference
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.patches.shared.dialog.baseViewerDiscretionDialogPatch

@Suppress("unused")
val viewerDiscretionDialogPatch = bytecodePatch(
    REMOVE_VIEWER_DISCRETION_DIALOG.displayTitle,
    REMOVE_VIEWER_DISCRETION_DIALOG.summary,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        baseViewerDiscretionDialogPatch(GENERAL_CLASS_DESCRIPTOR),
        settingsPatch,
    )

    apply {
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_remove_viewer_discretion_dialog",
            "false"
        )

        updatePatchStatus(REMOVE_VIEWER_DISCRETION_DIALOG)

    }
}
