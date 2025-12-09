import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    // Core module
    implementation(project(":core"))

    implementation("io.github.compose-fluent:fluent:v0.1.0")
    implementation("io.github.compose-fluent:fluent-icons-extended:v0.1.0") // If you want to use full fluent icons.

    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.mockk:mockk:1.13.9")
}

kotlin {
    jvmToolchain(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

compose.desktop {
    application {
        mainClass = "com.moribito.gui.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Moribito"
            packageVersion = "2.0.0"
            description = "LDAP Directory Explorer"
            vendor = "Moribito"

            macOS {
                iconFile.set(project.file("icons/icon.icns"))
                bundleID = "com.moribito.gui"
            }

            windows {
                iconFile.set(project.file("icons/icon.ico"))
                menuGroup = "Moribito"
            }

            linux {
                iconFile.set(project.file("icons/icon.png"))
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
