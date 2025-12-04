plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "21"
        }
        withJava()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // LDAP client
                implementation("com.unboundid:unboundid-ldapsdk:6.0.11")

                // YAML configuration
                implementation("com.charleskorn.kaml:kaml:0.55.0")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

                // Datetime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
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

        val jvmTest by getting {
            dependencies {
                // UnboundID In-Memory Directory Server for integration tests
                implementation("com.unboundid:unboundid-ldapsdk:6.0.11")
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
