package app.rvx.patches.music.layout.overlayfilter

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.rvx.patches.music.utils.patch.PatchList.HIDE_OVERLAY_FILTER
import app.rvx.patches.music.utils.resourceid.sharedResourceIdPatch
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.util.fingerprint.matchOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private val overlayFilterBytecodePatch = bytecodePatch(
    description = "overlayFilterBytecodePatch"
) {
    dependsOn(sharedResourceIdPatch)

    apply {
        designBottomSheetDialogFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.endIndex - 1
                val freeRegister = getInstruction<OneRegisterInstruction>(insertIndex + 1).registerA

                addInstructions(
                    insertIndex, """
                        invoke-virtual {p0}, $definingClass->getWindow()Landroid/view/Window;
                        move-result-object v$freeRegister
                        invoke-static {v$freeRegister}, $GENERAL_CLASS_DESCRIPTOR->disableDimBehind(Landroid/view/Window;)V
                        """
                )
            }
        }

    }
}

@Suppress("unused")
val overlayFilterPatch = resourcePatch(
    HIDE_OVERLAY_FILTER.displayTitle,
    HIDE_OVERLAY_FILTER.summary,
    use = false,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        settingsPatch,
        overlayFilterBytecodePatch,
    )

    apply {
        val styleFile = get("res/values/styles.xml")

        styleFile.writeText(
            styleFile.readText()
                .replace(
                    "ytOverlayBackgroundMedium\">@color/yt_black_pure_opacity60",
                    "ytOverlayBackgroundMedium\">@android:color/transparent"
                )
        )

        updatePatchStatus(HIDE_OVERLAY_FILTER)

    }
}
