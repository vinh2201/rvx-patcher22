package app.rvx.patches.youtube.utils.request

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.rvx.patches.shared.buildRequestFingerprint
import app.rvx.patches.shared.buildRequestParentFingerprint
import app.rvx.patches.shared.indexOfEntrySetInstruction
import app.rvx.patches.shared.indexOfNewUrlRequestBuilderInstruction
import app.rvx.patches.youtube.utils.extension.sharedExtensionPatch
import app.rvx.util.findFreeRegister
import app.rvx.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private lateinit var buildInitPlaybackRequestMethod: MutableMethod
private var initPlaybackUrlRegister = 0
private var initPlaybackMapRegister = 0

private lateinit var buildRequestMethod: MutableMethod
private var urlRegister = 0
private var mapRegister = 0
private var offSet = 0

val buildRequestPatch = bytecodePatch(
    description = "buildRequestPatch",
) {
    dependsOn(sharedExtensionPatch)

    apply {
        buildInitPlaybackRequestFingerprint.methodOrThrow().apply {
            buildInitPlaybackRequestMethod = this
            val mapIndex = indexOfMapInstruction(this)
            val mapField = getInstruction<ReferenceInstruction>(mapIndex).reference

            val uriIndex = indexOfUriToStringInstruction(this) + 1
            initPlaybackUrlRegister =
                getInstruction<OneRegisterInstruction>(uriIndex).registerA

            initPlaybackMapRegister =
                findFreeRegister(uriIndex, false, initPlaybackUrlRegister)

            addInstruction(
                0,
                "iget-object v$initPlaybackMapRegister, p1, $mapField"
            )
        }

        buildRequestFingerprint.methodOrThrow(buildRequestParentFingerprint).apply {
            buildRequestMethod = this

            val newRequestBuilderIndex = indexOfNewUrlRequestBuilderInstruction(this)
            urlRegister =
                getInstruction<FiveRegisterInstruction>(newRequestBuilderIndex).registerD

            val entrySetIndex = indexOfEntrySetInstruction(this)
            val isLegacyTarget = entrySetIndex < 0
            mapRegister = if (isLegacyTarget)
                urlRegister + 1
            else
                getInstruction<FiveRegisterInstruction>(entrySetIndex).registerC

            if (isLegacyTarget) {
                addInstructions(
                    newRequestBuilderIndex + 2,
                    "move-object/from16 v$mapRegister, p1"
                )
                offSet++
            }
        }
    }
}

internal fun hookBuildRequest(descriptor: String) {
    buildRequestMethod.apply {
        val insertIndex = indexOfNewUrlRequestBuilderInstruction(this) + 2 + offSet

        addInstructions(
            insertIndex,
            "invoke-static { v$urlRegister, v$mapRegister }, $descriptor"
        )
    }
}

internal fun hookBuildRequestUrl(descriptor: String) {
    buildRequestMethod.apply {
        val insertIndex = indexOfNewUrlRequestBuilderInstruction(this)

        addInstructions(
            insertIndex, """
                invoke-static { v$urlRegister }, $descriptor
                move-result-object v$urlRegister
                """
        )
    }
}

internal fun hookInitPlaybackBuildRequest(descriptor: String) {
    buildInitPlaybackRequestMethod.apply {
        val insertIndex = indexOfUriToStringInstruction(this) + 2

        addInstructions(
            insertIndex,
            "invoke-static { v$initPlaybackUrlRegister, v$initPlaybackMapRegister }, $descriptor"
        )
    }
}
