package app.rvx.patches.all.misc.signature

import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.util.Utils.printWarn
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.fingerprint.mutableClassOrThrow
import app.rvx.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

class AppInfo(
    val packageName: String? = "",
    val certificateData: String? = "",
)

fun baseSpoofSignaturePatch(appInfoSupplier: () -> AppInfo) = bytecodePatch {
    // Lazy, so that patch options above are initialized before they are accessed.
    val replacements by lazy {
        with(appInfoSupplier()) {
            buildMap {
                if (!packageName.isNullOrEmpty()) put(PACKAGE_NAME, "\"$packageName\"")
                if (!certificateData.isNullOrEmpty()) put(CERTIFICATE_BASE64, "\"$certificateData\"")
            }
        }
    }

    extendWith("extensions/spoof-signature.rve")

    apply patchApply@{
        if (replacements.size != 2) {
            printWarn("Invalid package name or certificate data, skipping patch")

            return@patchApply
        }

        spoofSignatureFingerprint.methodOrThrow().apply {
            replacements.forEach { (k, v) ->
                val index = indexOfFirstStringInstructionOrThrow(k)
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                replaceInstruction(index, "const-string v$register, $v")
            }

            applicationFingerprint.mutableClassOrThrow().setSuperClass(definingClass)
        }
    }
}
