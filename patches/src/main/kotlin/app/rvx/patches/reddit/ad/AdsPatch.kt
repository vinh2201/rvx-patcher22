package app.rvx.patches.reddit.ad

import app.rvx.util.fingerprint.methodOrNull
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.rvx.patches.reddit.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.reddit.utils.compatibility.Constants.REDDIT_PACKAGE_NAME
import app.rvx.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.rvx.patches.reddit.utils.patch.PatchList.HIDE_ADS
import app.rvx.patches.reddit.utils.settings.is_2025_06_or_greater
import app.rvx.patches.reddit.utils.settings.settingsPatch
import app.rvx.patches.reddit.utils.settings.updatePatchStatus
import app.rvx.util.findMutableMethodOf
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionOrThrow
import app.rvx.util.indexOfFirstStringInstruction
import app.rvx.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/GeneralAdsPatch;"

@Suppress("unused")
val adsPatch = bytecodePatch(
    HIDE_ADS.displayTitle,
    HIDE_ADS.summary,
) {
    compatibleWith(REDDIT_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(settingsPatch)

    apply {
        // region Filter promoted ads (does not work in popular or latest feed)
        adPostFingerprint.methodOrThrow().apply {
            val targetIndex = indexOfFirstInstructionOrThrow {
                getReference<FieldReference>()?.name == "children"
            }
            val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

            addInstructions(
                targetIndex, """
                    invoke-static {v$targetRegister}, $EXTENSION_CLASS_DESCRIPTOR->hideOldPostAds(Ljava/util/List;)Ljava/util/List;
                    move-result-object v$targetRegister
                    """
            )
        }

        // The new feeds work by inserting posts into lists.
        // AdElementConverter is conveniently responsible for inserting all feed ads.
        // By removing the appending instruction no ad posts gets appended to the feed.
        val newAdPostMethod = newAdPostFingerprint.methodOrNull()
            ?: newAdPostLegacyFingerprint.methodOrThrow()

        newAdPostMethod.apply {
            val startIndex =
                0.coerceAtLeast(indexOfFirstStringInstruction("android_feed_freeform_render_variant"))
            val targetIndex = indexOfAddArrayListInstruction(this, startIndex)
            val targetInstruction = getInstruction<FiveRegisterInstruction>(targetIndex)

            replaceInstruction(
                targetIndex,
                "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, " +
                        "$EXTENSION_CLASS_DESCRIPTOR->hideNewPostAds(Ljava/util/ArrayList;Ljava/lang/Object;)V"
            )
        }

        // region Filter comment ads
        fun MutableMethod.hook() =
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->hideCommentAds()Z
                    move-result v0
                    if-eqz v0, :show
                    return-void
                    :show
                    nop
                    """
            )
        if (is_2025_06_or_greater) {
            listOf(
                commentAdCommentScreenAdViewFingerprint,
                commentAdDetailListHeaderViewFingerprint,
                commentsViewModelFingerprint
            ).forEach { fingerprint ->
                fingerprint.methodOrThrow().hook()
            }
        } else {
            val isCommentAdsMethod: Method.() -> Boolean = {
                parameterTypes.size == 1 &&
                        parameterTypes.first().startsWith("Lcom/reddit/ads/conversation/") &&
                        accessFlags == AccessFlags.PUBLIC or AccessFlags.FINAL &&
                        returnType == "V" &&
                        indexOfFirstStringInstruction("ad") >= 0
            }

            classDefs.toList().forEach { classDef ->
                classDef.methods.forEach { method ->
                    if (method.isCommentAdsMethod()) {
                        classDefs.getOrReplaceMutable(classDef)
                            .findMutableMethodOf(method)
                            .hook()
                    }
                }
            }
        }

        updatePatchStatus(
            "enableGeneralAds",
            HIDE_ADS
        )
    }
}
