package app.rvx.patches.reddit.utils.compatibility

internal object Constants {
    internal const val REDDIT_PACKAGE_NAME = "com.reddit.frontpage"

    val COMPATIBLE_VERSIONS: Array<String> = arrayOf(
        "2024.17.0", // This is the last version that can be patched without anti-split.
        "2025.05.1", // This was the latest version supported by the previous RVX patch.
        "2025.12.1", // This is the latest version supported by the RVX patch.
    )

    val COMPATIBLE_PACKAGE: Pair<String, Set<String>?> =
        Pair(REDDIT_PACKAGE_NAME, COMPATIBLE_VERSIONS.toSet())
}