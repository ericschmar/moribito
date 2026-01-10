import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization")
}
repositories {
    maven("https://packages.jetbrains.team/maven/p/kpm/public/")
    maven("https://www.jetbrains.com/intellij-repository/releases/")
    mavenCentral()
}

dependencies {
    // Core module
    implementation(project(":core"))

    implementation("org.jetbrains.jewel:jewel-int-ui-standalone:0.33.0-253.29795")

    // IntelliJ Platform icons for AllIconsKeys
    implementation("com.jetbrains.intellij.platform:icons:253.29346.145")

    // Optional, for custom decorated windows:
    implementation("org.jetbrains.jewel:jewel-decorated-window:0.32.1-253.28294.285")
    implementation("org.jetbrains.compose.components:components-splitpane:1.9.3")

    // Do not bring in Material (we use Jewel)
    implementation(compose.desktop.currentOs) {
        exclude(group = "org.jetbrains.compose.material")

        implementation("net.java.dev.jna:jna:5.14.0")
        implementation("net.java.dev.jna:jna-platform:5.14.0")
    }

    implementation("io.insert-koin:koin-compose:4.1.1")
    implementation("io.insert-koin:koin-compose-viewmodel:4.1.1")

    implementation("io.github.compose-fluent:fluent:v0.1.0")
    implementation("io.github.compose-fluent:fluent-icons-extended:v0.1.0") // If you want to use full fluent icons.

    // Compose Desktop
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")

    implementation("com.akuleshov7:ktoml-core:0.7.1")
    implementation("com.akuleshov7:ktoml-file:0.7.1")

    implementation("br.com.devsrsouza.compose.icons:octicons:1.1.1")
    implementation("io.github.serpro69:kotlin-faker:1.16.0")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.mockk:mockk:1.13.9")
}

kotlin {
    jvmToolchain(25)
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

compose.resources {
    publicResClass = true
}

compose.desktop {
    application {
        // Application entry point
        mainClass = "com.moribito.gui.MainKt"

        // JVM configuration for runtime
        jvmArgs += listOf(
            "-Xmx2048m"
        )

        nativeDistributions {
            // Package metadata
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Moribito"
            packageVersion = "2.0.0"
            description = "LDAP Directory Explorer"
            vendor = "Moribito"

            // Include all JDK modules to avoid runtime ClassNotFoundException
            // This increases distributable size but ensures compatibility
            includeAllModules = true

            // Platform-specific configurations
            macOS {
                iconFile.set(project.file("icons/moribito.icns"))
                bundleID = "com.moribito.gui"
            }

            windows {
                iconFile.set(project.file("icons/moribito.ico"))
                menuGroup = "Moribito"
            }

            linux {
                iconFile.set(project.file("icons/moribito.png"))
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaExec> {
    val iconPath = file("icons/moribito.icns").absolutePath

    if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
        jvmArgs("-Xdock:icon=$iconPath", "-Xdock:name=Moribito")
    }
}
