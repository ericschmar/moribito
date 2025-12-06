plugins {
    kotlin("jvm")
}

dependencies {
    // Core module
    implementation(project(":core"))

    // Doodle framework
    implementation("io.nacular.doodle:core:0.11.0")
    implementation("io.nacular.doodle:browser:0.11.0")
    implementation("io.nacular.doodle:controls:0.11.0")
    implementation("io.nacular.doodle:themes:0.11.0")
    implementation("io.nacular.doodle:animation:0.11.0")

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

tasks.withType<Test> {
    useJUnitPlatform()
}
