package app.rvx.patches.youtube.utils.bottomsheet

import app.rvx.patches.youtube.utils.resourceid.designBottomSheet
import app.rvx.util.fingerprint.legacyFingerprint

internal val bottomSheetBehaviorFingerprint = legacyFingerprint(
    name = "bottomSheetBehaviorFingerprint",
    returnType = "V",
    literals = listOf(designBottomSheet),
)
