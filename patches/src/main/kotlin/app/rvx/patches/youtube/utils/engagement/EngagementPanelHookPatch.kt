package app.rvx.patches.youtube.utils.engagement

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.rvx.patches.youtube.utils.extension.Constants.SHARED_PATH
import app.rvx.patches.youtube.utils.extension.sharedExtensionPatch
import app.rvx.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.rvx.util.findMethodOrThrow
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstruction
import app.rvx.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$SHARED_PATH/EngagementPanel;"

internal lateinit var engagementPanelBuilderMethod: MutableMethod
internal var engagementPanelFreeRegister = 0
internal var engagementPanelIdIndex = 0
internal var engagementPanelIdRegister = 0

val engagementPanelHookPatch = bytecodePatch(
    description = "engagementPanelHookPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        sharedResourceIdPatch,
    )

    apply {
        fun Method.setFreeIndex(startIndex: Int) {
            val startRegister = engagementPanelIdRegister
            var index = startIndex
            var register = startRegister

            while (register == startRegister) {
                index = indexOfFirstInstruction(index + 1, Opcode.IGET_OBJECT)
                register = getInstruction<TwoRegisterInstruction>(index).registerA
            }

            engagementPanelFreeRegister = register
        }

        val engagementPanelInfoClass = engagementPanelLayoutFingerprint
            .methodOrThrow()
            .parameters[0]
            .toString()

        val (engagementPanelIdReference, engagementPanelObjectReference) =
            with(findMethodOrThrow(engagementPanelInfoClass)) {
                val engagementPanelIdIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>()?.type == "Ljava/lang/String;"
                }
                val engagementPanelObjectIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IPUT_OBJECT &&
                            getReference<FieldReference>()?.type != "Ljava/lang/String;"
                }
                Pair(
                    getInstruction<ReferenceInstruction>(engagementPanelIdIndex).reference.toString(),
                    getInstruction<ReferenceInstruction>(engagementPanelObjectIndex).reference.toString(),
                )
            }

        engagementPanelBuilderFingerprint.methodOrThrow().apply {
            val insertIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.IGET_OBJECT &&
                        getReference<FieldReference>()?.toString() == engagementPanelObjectReference
            }
            val insertInstruction = getInstruction<TwoRegisterInstruction>(insertIndex)
            val classRegister = insertInstruction.registerB
            engagementPanelIdRegister = insertInstruction.registerA

            setFreeIndex(insertIndex)

            addInstructions(
                insertIndex, """
                    iget-object v$engagementPanelIdRegister, v$classRegister, $engagementPanelIdReference
                    invoke-static {v$engagementPanelIdRegister}, $EXTENSION_CLASS_DESCRIPTOR->setId(Ljava/lang/String;)V
                    """
            )
            engagementPanelIdIndex = insertIndex + 1
            engagementPanelBuilderMethod = this
        }

        engagementPanelUpdateFingerprint
            .methodOrThrow(engagementPanelBuilderFingerprint)
            .addInstruction(
                0,
                "invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->hide()V"
            )
    }
}