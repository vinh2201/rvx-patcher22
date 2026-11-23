package app.rvx.patches.reddit.misc.openlink

import app.rvx.util.fingerprint.methodOrNull
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getWalkerMethod

lateinit var screenNavigatorMethod: MutableMethod

val screenNavigatorMethodResolverPatch = bytecodePatch(
    description = "screenNavigatorMethodResolverPatch"
) {
    apply {
        screenNavigatorMethod =
                // ~ Reddit 2024.25.3
            screenNavigatorFingerprint.methodOrNull()
                    // Reddit 2024.26.1 ~
                ?: with(customReportsFingerprint.methodOrThrow()) {
                    getWalkerMethod(indexOfScreenNavigatorInstruction(this))
                }
    }
}
