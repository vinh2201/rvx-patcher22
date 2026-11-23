package app.rvx.patches.shared.drc

import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.BytecodePatchBuilder
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod.Companion.toMutable
import app.rvx.patches.shared.extension.Constants.PATCHES_PATH
import app.rvx.patches.shared.formatStreamModelConstructorFingerprint
import app.rvx.util.addInstructionsAtControlFlowLabel
import app.rvx.util.fingerprint.injectLiteralInstructionBooleanCall
import app.rvx.util.fingerprint.matchOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

internal const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/DrcAudioPatch;"

fun drcAudioPatch(
    useLatestFingerprint: BytecodePatchBuilder.() -> Boolean = { false },
) = bytecodePatch(
    description = "drcAudioPatch",
) {
    apply {
        val fingerprint = if (useLatestFingerprint()) {
            compressionRatioFingerprint
        } else {
            compressionRatioLegacyFingerprint
        }

        val (formatFieldReference, loudnessDbFieldReference) =
            fingerprint.matchOrThrow(formatStreamModelConstructorFingerprint).let {
                with(it.method) {
                    val loudnessDbIndex = it.patternMatch!!.startIndex + 1
                    val loudnessDbFieldReference =
                        getInstruction<ReferenceInstruction>(loudnessDbIndex).reference as FieldReference
                    val formatClass = loudnessDbFieldReference.definingClass
                    val formatField = it.classDef.fields.find { field ->
                        field.type == formatClass
                    } ?: throw PatchException("Failed to find format field")

                    Pair(formatField, loudnessDbFieldReference)
                }
            }

        formatStreamModelConstructorFingerprint.matchOrThrow().let {
            it.method.apply {
                val helperMethodName = "patch_setLoudnessDb"

                it.classDef.methods.add(
                    ImmutableMethod(
                        it.classDef.type,
                        helperMethodName,
                        emptyList(),
                        "V",
                        AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                        annotations,
                        null,
                        MutableMethodImplementation(3),
                    ).toMutable().apply {
                        addInstructionsWithLabels(
                            0,
                            """
                            invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->disableDrcAudio()Z
                            move-result v0
                            if-eqz v0, :exit
                            iget-object v0, p0, $formatFieldReference
                            const/4 v1, 0x0
                            iput v1, v0, $loudnessDbFieldReference
                            iput-object v0, p0, $formatFieldReference
                            :exit
                            return-void
                            """,
                        )
                    }
                )

                val insertIndex = implementation!!.instructions.lastIndex

                addInstructionsAtControlFlowLabel(
                    insertIndex,
                    "invoke-direct/range { p0 .. p0 }, $definingClass->$helperMethodName()V"
                )
            }
        }

        volumeNormalizationConfigFingerprint.injectLiteralInstructionBooleanCall(
            VOLUME_NORMALIZATION_EXPERIMENTAL_FEATURE_FLAG,
            "$EXTENSION_CLASS_DESCRIPTOR->disableDrcAudioFeatureFlag(Z)Z"
        )
    }
}