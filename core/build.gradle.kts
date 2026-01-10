import org.gradle.kotlin.dsl.support.kotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvmToolchain(25)

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // LDAP client - ldaptive
                implementation("org.ldaptive:ldaptive:2.3.2")

                // YAML configuration
                implementation("com.charleskorn.kaml:kaml:0.55.0")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

                // Datetime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

                implementation("com.akuleshov7:ktoml-core:0.7.1")
                implementation("com.akuleshov7:ktoml-file:0.7.1")

                // Faker
                implementation("io.github.serpro69:kotlin-faker:1.16.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.kotest:kotest-runner-junit5:5.8.0")
                implementation("io.kotest:kotest-assertions-core:5.8.0")
                implementation("io.kotest:kotest-property:5.8.0")
                implementation("io.mockk:mockk:1.13.9")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
            }
        }

        val jvmMain by getting {
            dependencies {
                // Additional JVM-specific dependencies if needed
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
