rootProject.name = "moribito"

include(
    ":core",
    ":tui",
    ":gui",
    ":app-tui",
    ":app-gui"
)

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}