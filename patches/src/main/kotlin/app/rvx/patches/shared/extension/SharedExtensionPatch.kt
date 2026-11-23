package app.rvx.patches.shared.extension

import app.rvx.util.fingerprint.Fingerprint
import app.rvx.util.fingerprint.methodOrThrow
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.com.android.tools.smali.dexlib2.iface.value.MutableLongEncodedValue
import app.rvx.patches.shared.extension.Constants.EXTENSION_PATCH_STATUS_CLASS_DESCRIPTOR
import app.rvx.patches.shared.extension.Constants.EXTENSION_UTILS_CLASS_DESCRIPTOR
import app.rvx.util.findMethodsOrThrow
import app.rvx.util.returnEarly
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.immutable.value.ImmutableLongEncodedValue
import java.util.jar.Manifest

fun sharedExtensionPatch(
    vararg hooks: ExtensionHook,
) = bytecodePatch(
    description = "sharedExtensionPatch"
) {
    extendWith("extensions/rvx-shared.rve")

    apply {
        if (classDefs.none { EXTENSION_UTILS_CLASS_DESCRIPTOR == it.type }) {
            throw PatchException(
                "Shared extension has not been merged yet. This patch can not succeed without merging it.",
            )
        }
        hooks.forEach { hook -> hook(EXTENSION_UTILS_CLASS_DESCRIPTOR) }
    }

    afterDependents {
        findMethodsOrThrow(EXTENSION_PATCH_STATUS_CLASS_DESCRIPTOR).apply {
            find { method -> method.name == "PatchedTime" }
                ?.replaceInstruction(
                    0,
                    "const-wide v0, ${MutableLongEncodedValue(ImmutableLongEncodedValue(System.currentTimeMillis()))}L"
                )

            find { method -> method.name == "PatchVersion" }
                ?.apply {
                    val manifest = object {}
                        .javaClass
                        .classLoader
                        .getResources("META-INF/MANIFEST.MF")

                    while (manifest.hasMoreElements()) {
                        Manifest(manifest.nextElement().openStream())
                            .mainAttributes
                            .getValue("Version")
                            ?.let {
                                returnEarly(it)
                                return@afterDependents
                            }
                    }
                }
        }
    }
}

class ExtensionHook internal constructor(
    val fingerprint: Pair<String, Fingerprint>,
    private val insertIndexResolver: ((Method) -> Int),
    private val contextRegisterResolver: (Method) -> String,
) {
    context(ctx: BytecodePatchContext)
    operator fun invoke(extensionClassDescriptor: String) {
        val method = fingerprint.methodOrThrow()
        val insertIndex = insertIndexResolver(method)
        val contextRegister = contextRegisterResolver(method)

        method.addInstruction(
            insertIndex,
            "invoke-static/range { $contextRegister .. $contextRegister }, " +
                    "$extensionClassDescriptor->setContext(Landroid/content/Context;)V",
        )
    }
}

fun extensionHook(
    insertIndexResolver: ((Method) -> Int) = { 0 },
    contextRegisterResolver: (Method) -> String = { "p0" },
    fingerprint: Pair<String, Fingerprint>,
) = ExtensionHook(
    fingerprint,
    insertIndexResolver,
    contextRegisterResolver
)
