rootProject.name = "moribito"

include(
    ":core",
    ":tui",
    ":gui",
    ":app-tui",
    ":app-gui"
)

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
