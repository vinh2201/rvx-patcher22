package app.rvx.patches.reddit.layout.communities

import app.rvx.util.fingerprint.legacyFingerprint
import app.rvx.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val communityRecommendationSectionFingerprint = legacyFingerprint(
    name = "communityRecommendationSectionFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("feedContext"),
)

internal val communityRecommendationSectionParentFingerprint = legacyFingerprint(
    name = "communityRecommendationSectionParentFingerprint",
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("community_recomendation_section_"),
    customFingerprint = { method, _ ->
        method.definingClass.startsWith("Lcom/reddit/onboardingfeedscomponents/communityrecommendation/impl/") &&
                method.name == "key"
    }
)