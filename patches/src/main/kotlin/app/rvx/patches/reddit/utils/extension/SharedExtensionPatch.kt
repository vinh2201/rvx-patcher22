package app.rvx.patches.reddit.utils.extension

import app.rvx.patches.reddit.utils.extension.hooks.applicationInitHook
import app.rvx.patches.shared.extension.sharedExtensionPatch

val sharedExtensionPatch = sharedExtensionPatch(applicationInitHook)
