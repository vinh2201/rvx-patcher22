package app.rvx.patches.reddit.utils.resourceid

import app.revanced.patcher.patch.resourcePatch
import app.rvx.patches.shared.mapping.ResourceType.STRING
import app.rvx.patches.shared.mapping.getResourceId
import app.rvx.patches.shared.mapping.resourceMappingPatch

var nsfwDialogTitle = -1L
    private set

internal val sharedResourceIdPatch = resourcePatch(
    description = "sharedResourceIdPatch"
) {
    dependsOn(resourceMappingPatch)

    apply {
        nsfwDialogTitle = getResourceId(STRING, "nsfw_dialog_title")
    }
}