package app.rvx.patches.youtube.utils.castbutton

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.rvx.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.rvx.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.rvx.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.rvx.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.rvx.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.rvx.util.findMethodOrThrow
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionOrThrow
import app.rvx.util.updatePatchStatus
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/CastButtonPatch;"

private lateinit var playerButtonMethod: MutableMethod
private lateinit var toolbarMenuItemInitializeMethod: MutableMethod
private lateinit var toolbarMenuItemVisibilityMethod: MutableMethod

val castButtonPatch = bytecodePatch(
    description = "castButtonPatch"
) {
    dependsOn(sharedResourceIdPatch)

    apply {
        toolbarMenuItemInitializeMethod = menuItemInitializeFingerprint.methodOrThrow()
        toolbarMenuItemVisibilityMethod =
            menuItemVisibilityFingerprint.methodOrThrow(menuItemInitializeFingerprint)

        playerButtonMethod = playerButtonFingerprint.methodOrThrow()

        findMethodOrThrow("Landroidx/mediarouter/app/MediaRouteButton;") {
            name == "setVisibility"
        }.addInstructions(
            0, """
                invoke-static {p1}, $EXTENSION_CLASS_DESCRIPTOR->hideCastButton(I)I
                move-result p1
                """
        )
    }
}

context(ctx: BytecodePatchContext)
internal fun hookPlayerCastButton() {
    playerButtonMethod.apply {
        val index = indexOfFirstInstructionOrThrow {
            getReference<MethodReference>()?.name == "setVisibility"
        }
        val instruction = getInstruction<FiveRegisterInstruction>(index)
        val viewRegister = instruction.registerC
        val visibilityRegister = instruction.registerD
        val reference = getInstruction<ReferenceInstruction>(index).reference

        addInstructions(
            index + 1, """
                    invoke-static {v$visibilityRegister}, $PLAYER_CLASS_DESCRIPTOR->hideCastButton(I)I
                    move-result v$visibilityRegister
                    invoke-virtual {v$viewRegister, v$visibilityRegister}, $reference
                    """
        )
        removeInstruction(index)
    }
    updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "PlayerButtons")
}

context(ctx: BytecodePatchContext)
internal fun hookToolBarCastButton() {
    toolbarMenuItemInitializeMethod.apply {
        val index = indexOfFirstInstructionOrThrow {
            getReference<MethodReference>()?.name == "setShowAsAction"
        } + 1
        addInstruction(
            index,
            "invoke-static {p1}, $GENERAL_CLASS_DESCRIPTOR->hideCastButton(Landroid/view/MenuItem;)V"
        )
    }
    toolbarMenuItemVisibilityMethod.addInstructions(
        0, """
                invoke-static {p1}, $GENERAL_CLASS_DESCRIPTOR->hideCastButton(Z)Z
                move-result p1
                """
    )
    updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "ToolBarComponents")
}
