package app.rvx.patches.all.misc.signature

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.stringOption

@Suppress("unused")
val spoofSignaturePatch = bytecodePatch(
    name = "Spoof signature (rvx)",
    description = "Spoofs the signature of the app.",
    use = false,
) {
    val packageName by stringOption(
        default = "",
        name = "Package name",
        description = "Package name.",
    )

    val certificateData by stringOption(
        default = "",
        name = "Certificate data",
        description = "Base64-encoded string of the certificate.",
    )

    dependsOn(
        baseSpoofSignaturePatch {
            AppInfo(
                packageName,
                certificateData,
            )
        },
    )
}
