package app.rvx.patches.youtube.utils.componentlist

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.rvx.patches.shared.conversionContextFingerprintToString
import app.rvx.patches.shared.litho.componentContextSubParserFingerprint
import app.rvx.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.rvx.patches.youtube.utils.extension.sharedExtensionPatch
import app.rvx.util.addInstructionsAtControlFlowLabel
import app.rvx.util.findFreeRegister
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionOrThrow
import app.rvx.util.indexOfFirstInstructionReversedOrThrow
import app.rvx.util.indexOfFirstStringInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/LazilyConvertedElementPatch;"

private lateinit var lazilyConvertedElementMethod: MutableMethod

val lazilyConvertedElementHookPatch = bytecodePatch(
    description = "lazilyConvertedElementHookPatch"
) {
    dependsOn(sharedExtensionPatch)

    apply {
        componentListFingerprint.methodOrThrow(componentContextSubParserFingerprint).apply {
            val identifierReference = with(conversionContextFingerprintToString.methodOrThrow()) {
                val identifierStringIndex =
                    indexOfFirstStringInstructionOrThrow(", identifierProperty=")
                val identifierStringAppendIndex =
                    indexOfFirstInstructionOrThrow(identifierStringIndex, Opcode.INVOKE_VIRTUAL)
                val identifierAppendIndex =
                    indexOfFirstInstructionOrThrow(
                        identifierStringAppendIndex + 1,
                        Opcode.INVOKE_VIRTUAL
                    )
                val identifierRegister =
                    getInstruction<FiveRegisterInstruction>(identifierAppendIndex).registerD
                val identifierIndex =
                    indexOfFirstInstructionReversedOrThrow(identifierAppendIndex) {
                        opcode == Opcode.IGET_OBJECT &&
                                getReference<FieldReference>()?.type == "Ljava/lang/String;" &&
                                (this as? TwoRegisterInstruction)?.registerA == identifierRegister
                    }
                getInstruction<ReferenceInstruction>(identifierIndex).reference
            }

            val listIndex = implementation!!.instructions.lastIndex
            val listRegister = getInstruction<OneRegisterInstruction>(listIndex).registerA
            val identifierRegister = findFreeRegister(listIndex, listRegister)

            addInstructionsAtControlFlowLabel(
                listIndex, """
                    move-object/from16 v$identifierRegister, p2
                    iget-object v$identifierRegister, v$identifierRegister, $identifierReference
                    invoke-static {v$listRegister, v$identifierRegister}, $EXTENSION_CLASS_DESCRIPTOR->hookElements(Ljava/util/List;Ljava/lang/String;)V
                    """
            )

            lazilyConvertedElementMethod = lazilyConvertedElementPatchFingerprint.methodOrThrow()
        }
    }
}

internal fun hookElementList(descriptor: String) =
    lazilyConvertedElementMethod.addInstruction(
        0,
        "invoke-static {p0, p1}, $descriptor(Ljava/util/List;Ljava/lang/String;)V"
    )
