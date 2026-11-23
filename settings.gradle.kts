rootProject.name = "revanced-patches"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        // TODO: Remove once https://github.com/google/protobuf-gradle-plugin/pull/797 is merged.
        maven { setUrl("https://jitpack.io") }
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/revanced/revanced-patches-gradle-plugin")
            credentials(PasswordCredentials::class)
        }
    }
    // TODO: Remove once https://github.com/google/protobuf-gradle-plugin/pull/797 is merged.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.google.protobuf") {
                useModule("com.github.ReVanced:protobuf-gradle-plugin:${requested.version}")
            }
        }
    }
}

plugins {
    id("app.revanced.patches") version "1.0.0-dev.10"
}

// Folders left over from before the rename to app.rvx.* / rvx-shared.
// If they are still present (for example when new files were copied over an old checkout)
// they would be compiled and shipped next to the new ones, so remove them.
listOf(
    "patches/src/main/kotlin/app/revanced/patches",
    "patches/src/main/kotlin/app/revanced/util",
).forEach { path ->
    settingsDir.resolve(path).takeIf { it.exists() }?.deleteRecursively()
}
if (settingsDir.resolve("extensions/rvx-shared").exists()) {
    settingsDir.resolve("extensions/shared").takeIf { it.exists() }?.deleteRecursively()
}

dependencyResolutionManagement {
    repositories {
        // J2V8 (extensions:shared).
        maven { url = uri("https://jitpack.io") }
    }
}

settings {
    extensions {
        defaultNamespace = "app.revanced.extension"

        // Must resolve to an absolute path (not relative),
        // otherwise the extensions in subfolders will fail to find the proguard config.
        proguardFiles(rootProject.projectDir.resolve("extensions/proguard-rules.pro").toString())
    }
}
