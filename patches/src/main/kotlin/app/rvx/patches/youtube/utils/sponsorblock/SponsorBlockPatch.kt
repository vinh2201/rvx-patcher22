package app.rvx.patches.youtube.utils.sponsorblock

import app.rvx.patches.youtube.player.overlaybuttons.overlayButtonsIconTypeOption
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.rvx.patches.youtube.player.overlaybuttons.overlayButtonsPatch
import app.rvx.patches.youtube.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.rvx.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.rvx.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.rvx.patches.youtube.utils.patch.PatchList.SPONSORBLOCK
import app.rvx.patches.youtube.utils.playercontrols.addTopControl
import app.rvx.patches.youtube.utils.playercontrols.injectControl
import app.rvx.patches.youtube.utils.playercontrols.playerControlsPatch
import app.rvx.patches.youtube.utils.resourceid.insetOverlayViewLayout
import app.rvx.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.rvx.patches.youtube.utils.seekbarFingerprint
import app.rvx.patches.youtube.utils.seekbarOnDrawFingerprint
import app.rvx.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.rvx.patches.youtube.utils.settings.settingsPatch
import app.rvx.patches.youtube.utils.totalTimeFingerprint
import app.rvx.patches.youtube.utils.youtubeControlsOverlayFingerprint
import app.rvx.patches.youtube.video.information.hookVideoInformation
import app.rvx.patches.youtube.video.information.onCreateHook
import app.rvx.patches.youtube.video.information.videoInformationPatch
import app.rvx.patches.youtube.video.information.videoTimeHook
import app.rvx.util.ResourceGroup
import app.rvx.util.copyResources
import app.rvx.util.fingerprint.matchOrThrow
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionOrThrow
import app.rvx.util.indexOfFirstInstructionReversedOrThrow
import app.rvx.util.indexOfFirstLiteralInstructionOrThrow
import app.rvx.util.lowerCaseOrThrow
import app.rvx.util.updatePatchStatus
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_SPONSOR_BLOCK_PATH =
    "$EXTENSION_PATH/sponsorblock"

private const val EXTENSION_SPONSOR_BLOCK_UI_PATH =
    "$EXTENSION_SPONSOR_BLOCK_PATH/ui"

private const val EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR =
    "$EXTENSION_SPONSOR_BLOCK_PATH/SegmentPlaybackController;"

private const val EXTENSION_SPONSOR_BLOCK_VIEW_CONTROLLER_CLASS_DESCRIPTOR =
    "$EXTENSION_SPONSOR_BLOCK_UI_PATH/SponsorBlockViewController;"

val sponsorBlockBytecodePatch = bytecodePatch(
    description = "sponsorBlockBytecodePatch"
) {
    dependsOn(
        sharedResourceIdPatch,
        videoInformationPatch,
    )

    apply {
        // Hook the video time method
        videoTimeHook(
            EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR,
            "setVideoTime"
        )
        // Initialize the player controller
        onCreateHook(
            EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR,
            "initialize"
        )


        seekbarOnDrawFingerprint.methodOrThrow(seekbarFingerprint).apply {
            // Get left and right of seekbar rectangle
            val moveObjectIndex = indexOfFirstInstructionOrThrow(Opcode.MOVE_OBJECT_FROM16)

            addInstruction(
                moveObjectIndex + 1,
                "invoke-static/range {p0 .. p0}, " +
                        "$EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarRect(Ljava/lang/Object;)V"
            )

            // Set seekbar thickness
            val roundIndex = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.name == "round"
            } + 1
            val roundRegister = getInstruction<OneRegisterInstruction>(roundIndex).registerA

            addInstruction(
                roundIndex + 1,
                "invoke-static {v$roundRegister}, " +
                        "$EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->setSponsorBarThickness(I)V"
            )

            // Draw segment
            val drawCircleIndex = indexOfFirstInstructionReversedOrThrow {
                getReference<MethodReference>()?.name == "drawCircle"
            }
            val drawCircleInstruction = getInstruction<FiveRegisterInstruction>(drawCircleIndex)
            addInstruction(
                drawCircleIndex,
                "invoke-static {v${drawCircleInstruction.registerC}, v${drawCircleInstruction.registerE}}, " +
                        "$EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->drawSponsorTimeBars(Landroid/graphics/Canvas;F)V"
            )
        }

        // Voting & Shield button
        setOf(
            "CreateSegmentButton",
            "VotingButton"
        ).forEach { className ->
            injectControl("$EXTENSION_SPONSOR_BLOCK_UI_PATH/${className};")
        }

        // Skip button
        injectControl(
            descriptor = EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR,
            topControl = false,
            initialize = false
        )

        // Append timestamp
        totalTimeFingerprint.methodOrThrow().apply {
            val targetIndex = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.name == "getString"
            } + 1
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstructions(
                targetIndex + 1, """
                        invoke-static {v$targetRegister}, $EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->appendTimeWithoutSegments(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$targetRegister
                        """
            )
        }

        // Initialize the SponsorBlock view
        youtubeControlsOverlayFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex =
                    indexOfFirstLiteralInstructionOrThrow(insetOverlayViewLayout)
                val checkCastIndex = indexOfFirstInstructionOrThrow(targetIndex, Opcode.CHECK_CAST)
                val targetRegister =
                    getInstruction<OneRegisterInstruction>(checkCastIndex).registerA

                addInstruction(
                    checkCastIndex + 1,
                    "invoke-static {v$targetRegister}, $EXTENSION_SPONSOR_BLOCK_VIEW_CONTROLLER_CLASS_DESCRIPTOR->initialize(Landroid/view/ViewGroup;)V"
                )
            }
        }

        // Replace strings
        rectangleFieldInvalidatorFingerprint.methodOrThrow(seekbarFingerprint).apply {
            val invalidateIndex = indexOfInvalidateInstruction(this)
            val rectangleIndex = indexOfFirstInstructionReversedOrThrow(invalidateIndex + 1) {
                getReference<FieldReference>()?.type == "Landroid/graphics/Rect;"
            }
            val rectangleFieldName =
                (getInstruction<ReferenceInstruction>(rectangleIndex).reference as FieldReference).name

            segmentPlaybackControllerFingerprint.matchOrThrow().let {
                it.method.apply {
                    val replaceIndex = it.patternMatch!!.startIndex
                    val replaceRegister =
                        getInstruction<OneRegisterInstruction>(replaceIndex).registerA

                    replaceInstruction(
                        replaceIndex,
                        "const-string v$replaceRegister, \"$rectangleFieldName\""
                    )
                }
            }
        }

        // Set current video id
        hookVideoInformation("$EXTENSION_SEGMENT_PLAYBACK_CONTROLLER_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")

        updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "SponsorBlock")
    }
}

@Suppress("unused")
val sponsorBlockPatch = resourcePatch(
    SPONSORBLOCK.displayTitle,
    SPONSORBLOCK.summary,
) {
    compatibleWith(YOUTUBE_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        playerControlsPatch,
        sponsorBlockBytecodePatch,
        settingsPatch,
    )

    apply {
        /**
         * merge SponsorBlock drawables to main drawables
         */
        arrayOf(
            ResourceGroup(
                "layout",
                "revanced_sb_inline_sponsor_overlay.xml",
                "revanced_sb_skip_sponsor_button.xml"
            )
        ).forEach { resourceGroup ->
            copyResources("youtube/sponsorblock/shared", resourceGroup)
        }

        val iconType = overlayButtonsIconTypeOption
            .lowerCaseOrThrow()
        val outlineIcon = iconType == "thin"

        if (outlineIcon) {
            arrayOf(
                ResourceGroup(
                    "layout",
                    "revanced_sb_new_segment.xml"
                ),
                ResourceGroup(
                    "drawable",
                    "revanced_sb_adjust.xml",
                    "revanced_sb_backward.xml",
                    "revanced_sb_compare.xml",
                    "revanced_sb_edit.xml",
                    "revanced_sb_forward.xml",
                    "revanced_sb_logo.xml",
                    "revanced_sb_publish.xml",
                    "revanced_sb_voting.xml"
                )
            ).forEach { resourceGroup ->
                copyResources("youtube/sponsorblock/outline", resourceGroup)
            }
        } else {
            arrayOf(
                ResourceGroup(
                    "layout",
                    "revanced_sb_new_segment.xml"
                ),
                ResourceGroup(
                    "drawable",
                    "revanced_sb_adjust.xml",
                    "revanced_sb_compare.xml",
                    "revanced_sb_edit.xml",
                    "revanced_sb_logo.xml",
                    "revanced_sb_publish.xml",
                    "revanced_sb_voting.xml"
                )
            ).forEach { resourceGroup ->
                copyResources("youtube/sponsorblock/default", resourceGroup)
            }
        }

        /**
         * merge xml nodes from the host to their real xml files
         */
        addTopControl(
            "youtube/sponsorblock/shared",
            "@+id/revanced_sb_voting_button",
            "@+id/revanced_sb_create_segment_button"
        )

        /**
         * Add settings
         */
        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: SPONSOR_BLOCK"
            ),
            SPONSORBLOCK
        )
    }
}
