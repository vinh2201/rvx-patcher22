package app.rvx.patches.music.misc.album

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.rvx.patches.music.utils.compatibility.Constants.COMPATIBLE_VERSIONS
import app.rvx.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.rvx.patches.music.utils.extension.Constants.MISC_PATH
import app.rvx.patches.music.utils.patch.PatchList.DISABLE_MUSIC_VIDEO_IN_ALBUM
import app.rvx.patches.music.utils.resourceid.sharedResourceIdPatch
import app.rvx.patches.music.utils.settings.CategoryType
import app.rvx.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.rvx.patches.music.utils.settings.addPreferenceWithIntent
import app.rvx.patches.music.utils.settings.addSwitchPreference
import app.rvx.patches.music.utils.settings.settingsPatch
import app.rvx.patches.music.video.information.videoIdHook
import app.rvx.patches.music.video.information.videoInformationPatch
import app.rvx.patches.music.video.playerresponse.Hook
import app.rvx.patches.music.video.playerresponse.addPlayerResponseMethodHook
import app.rvx.patches.music.video.playerresponse.playerResponseMethodHookPatch
import app.rvx.util.findMethodOrThrow
import app.rvx.util.fingerprint.methodOrThrow
import app.rvx.util.getReference
import app.rvx.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$MISC_PATH/AlbumMusicVideoPatch;"

@Suppress("unused")
val albumMusicVideoPatch = bytecodePatch(
    DISABLE_MUSIC_VIDEO_IN_ALBUM.displayTitle,
    DISABLE_MUSIC_VIDEO_IN_ALBUM.summary,
    false,
) {
    compatibleWith(YOUTUBE_MUSIC_PACKAGE_NAME(*COMPATIBLE_VERSIONS))

    dependsOn(
        settingsPatch,
        sharedResourceIdPatch,
        videoInformationPatch,
        playerResponseMethodHookPatch,
    )

    apply {

        // region hook player response

        addPlayerResponseMethodHook(
            Hook.VideoIdAndPlaylistId(
                "$EXTENSION_CLASS_DESCRIPTOR->newPlayerResponse(Ljava/lang/String;Ljava/lang/String;I)V"
            ),
        )

        // endregion

        // region hook video id

        videoIdHook("$EXTENSION_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")

        // endregion

        // region patch for hide snack bar

        snackBarFingerprint
            .methodOrThrow(snackBarAttributeFingerprint)
            .addInstructionsWithLabels(
                0, """
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->hideSnackBar()Z
                move-result v0
                if-eqz v0, :hide
                return-void
                :hide
                nop
                """
            )

        // endregion

        // region patch for setOnClick / setOnLongClick listener

        audioVideoSwitchToggleConstructorFingerprint.methodOrThrow().apply {
            val onClickListenerIndex = indexOfAudioVideoSwitchSetOnClickListenerInstruction(this)
            val viewRegister =
                getInstruction<FiveRegisterInstruction>(onClickListenerIndex).registerC

            addInstruction(
                onClickListenerIndex + 1,
                "invoke-static { v$viewRegister }, " +
                        "$EXTENSION_CLASS_DESCRIPTOR->setAudioVideoSwitchToggleOnLongClickListener(Landroid/view/View;)V"
            )

            val onClickListenerSyntheticIndex =
                indexOfFirstInstructionReversedOrThrow(onClickListenerIndex) {
                    opcode == Opcode.INVOKE_DIRECT &&
                            getReference<MethodReference>()?.name == "<init>"
                }
            val onClickListenerSyntheticClass =
                (getInstruction<ReferenceInstruction>(onClickListenerSyntheticIndex).reference as MethodReference).definingClass

            findMethodOrThrow(onClickListenerSyntheticClass) {
                name == "onClick"
            }.addInstructionsWithLabels(
                0, """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->openMusic()Z
                    move-result v0
                    if-eqz v0, :ignore
                    return-void
                    :ignore
                    nop
                    """
            )
        }

        // endregion

        addSwitchPreference(
            CategoryType.MISC,
            "revanced_disable_music_video_in_album",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_disable_music_video_in_album_redirect_type",
            "revanced_disable_music_video_in_album"
        )

        updatePatchStatus(DISABLE_MUSIC_VIDEO_IN_ALBUM)

    }
}
