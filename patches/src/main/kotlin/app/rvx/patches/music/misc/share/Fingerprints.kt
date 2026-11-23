package app.rvx.patches.music.misc.share

import app.rvx.patches.music.utils.resourceid.bottomSheetRecyclerView
import app.rvx.util.fingerprint.legacyFingerprint
import app.rvx.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val bottomSheetRecyclerViewFingerprint = legacyFingerprint(
    name = "bottomSheetRecyclerViewFingerprint",
    returnType = "Lj${'$'}/util/Optional;",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(bottomSheetRecyclerView),
)
