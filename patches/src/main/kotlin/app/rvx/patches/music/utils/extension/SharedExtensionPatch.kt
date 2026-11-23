package app.rvx.patches.music.utils.extension

import app.rvx.patches.music.utils.extension.hooks.applicationInitHook
import app.rvx.patches.shared.extension.sharedExtensionPatch

val sharedExtensionPatch = sharedExtensionPatch(
    applicationInitHook,
)
