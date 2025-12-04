plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    application
}

application {
    mainClass.set("com.moribito.MainGuiKt")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":gui"))

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

kotlin {
    jvmToolchain(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks {
    shadowJar {
        archiveBaseName.set("moribito-gui")
        archiveClassifier.set("")
        archiveVersion.set(project.version.toString())

        manifest {
            attributes["Main-Class"] = "com.moribito.MainGuiKt"
            attributes["Implementation-Version"] = project.version
        }

        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }
}
