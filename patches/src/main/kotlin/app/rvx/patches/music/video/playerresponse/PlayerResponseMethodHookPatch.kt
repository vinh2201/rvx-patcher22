package app.rvx.patches.music.video.playerresponse

import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.rvx.patches.music.utils.extension.sharedExtensionPatch
import app.rvx.patches.music.utils.playservice.is_7_03_or_greater
import app.rvx.patches.music.utils.playservice.versionCheckPatch
import app.rvx.util.fingerprint.methodOrThrow

private val hooks = mutableSetOf<Hook>()

fun addPlayerResponseMethodHook(hook: Hook) {
    hooks += hook
}

private const val REGISTER_VIDEO_ID = "p1"
private const val REGISTER_PLAYER_PARAMETER = "p3"
private const val REGISTER_PLAYLIST_ID = "p4"
private const val REGISTER_PLAYLIST_INDEX = "p5"

private lateinit var playerResponseMethod: MutableMethod
private var numberOfInstructionsAdded = 0

val playerResponseMethodHookPatch = bytecodePatch(
    description = "playerResponseMethodHookPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        versionCheckPatch,
    )

    apply {
        playerResponseMethod = if (is_7_03_or_greater) {
            playerParameterBuilderFingerprint
        } else {
            playerParameterBuilderLegacyFingerprint
        }.methodOrThrow()
    }

    afterDependents {
        fun hookVideoId(hook: Hook) {
            playerResponseMethod.addInstruction(
                0,
                "invoke-static {$REGISTER_VIDEO_ID}, $hook",
            )
            numberOfInstructionsAdded++
        }

        fun hookVideoIdAndPlaylistId(hook: Hook) {
            playerResponseMethod.addInstruction(
                0,
                "invoke-static {$REGISTER_VIDEO_ID, $REGISTER_PLAYLIST_ID, $REGISTER_PLAYLIST_INDEX}, $hook",
            )
            numberOfInstructionsAdded++
        }

        fun hookPlayerParameter(hook: Hook) {
            playerResponseMethod.addInstructions(
                0,
                """
                    invoke-static {$REGISTER_VIDEO_ID, v0}, $hook
                    move-result-object v0
                    """,
            )
            numberOfInstructionsAdded += 2
        }

        // Reverse the order in order to preserve insertion order of the hooks.
        val beforeVideoIdHooks =
            hooks.filterIsInstance<Hook.PlayerParameterBeforeVideoId>().asReversed()
        val videoIdHooks = hooks.filterIsInstance<Hook.VideoId>().asReversed()
        val videoIdAndPlaylistIdHooks =
            hooks.filterIsInstance<Hook.VideoIdAndPlaylistId>().asReversed()
        val afterVideoIdHooks = hooks.filterIsInstance<Hook.PlayerParameter>().asReversed()

        // Add the hooks in this specific order as they insert instructions at the beginning of the method.
        afterVideoIdHooks.forEach(::hookPlayerParameter)
        videoIdAndPlaylistIdHooks.forEach(::hookVideoIdAndPlaylistId)
        videoIdHooks.forEach(::hookVideoId)
        beforeVideoIdHooks.forEach(::hookPlayerParameter)

        playerResponseMethod.apply {
            addInstruction(
                0,
                "move-object/from16 v0, $REGISTER_PLAYER_PARAMETER"
            )
            numberOfInstructionsAdded++

            // Move the modified register back.
            addInstruction(
                numberOfInstructionsAdded,
                "move-object/from16 $REGISTER_PLAYER_PARAMETER, v0"
            )
        }
    }
}

abstract class Hook(private val methodDescriptor: String) {
    class VideoId(methodDescriptor: String) : Hook(methodDescriptor)
    class VideoIdAndPlaylistId(methodDescriptor: String) : Hook(methodDescriptor)

    class PlayerParameter(methodDescriptor: String) : Hook(methodDescriptor)
    class PlayerParameterBeforeVideoId(methodDescriptor: String) : Hook(methodDescriptor)

    override fun toString() = methodDescriptor
}
