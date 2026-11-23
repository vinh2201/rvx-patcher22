package app.rvx.patches.youtube.general.updatescreen

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.youtube.general.components.appBlockingCheckResultToStringFingerprint
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.patch.PatchList.DISABLE_UPDATE_SCREEN
import app.rvx.util.fingerprint.mutableClassOrThrow
import com.android.tools.smali.dexlib2.util.MethodUtil

val disableUpdateScreen = bytecodePatch(
    DISABLE_UPDATE_SCREEN.displayTitle,
    DISABLE_UPDATE_SCREEN.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    apply {
        appBlockingCheckResultToStringFingerprint.mutableClassOrThrow().methods.first { method ->
            MethodUtil.isConstructor(method) &&
                    method.parameters == listOf("Landroid/content/Intent;", "Z")
        }.addInstructions(
            1,
            "const/4 p1, 0x0"
        )
    }
}
