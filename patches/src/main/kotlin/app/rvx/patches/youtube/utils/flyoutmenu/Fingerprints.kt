package app.rvx.patches.youtube.utils.flyoutmenu

import app.rvx.patches.youtube.utils.resourceid.videoQualityUnavailableAnnouncement
import app.rvx.util.fingerprint.legacyFingerprint
import app.rvx.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val videoQualityBottomSheetClassFingerprint = legacyFingerprint(
    name = "videoQualityBottomSheetClassFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Z"),
    literals = listOf(videoQualityUnavailableAnnouncement),
)
