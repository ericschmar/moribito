# Compose Native Distribution Configuration Overhaul

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Restructure GUI build configuration to align with official Compose Multiplatform native distribution best practices and documentation.

**Architecture:** Clean up duplicate dependencies, replace custom JRE removal workaround with proper includeAllModules configuration, ensure all packaging tasks work correctly, and align with official Compose Multiplatform DSL structure.

**Tech Stack:** Kotlin 2.3.0, Compose Multiplatform 1.8.0, Gradle with Kotlin DSL, JDK 25

**Reference Documentation:**
- [Compose Native Distribution](https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html)
- [JRE Bundling Discussion](https://github.com/JetBrains/compose-multiplatform/issues/207)

---

## Task 1: Backup and Verify Current Configuration

**Files:**
- Read: `gui/build.gradle.kts`
- Create: `gui/build.gradle.kts.backup`

**Step 1: Create backup of current build file**

```bash
cp gui/build.gradle.kts gui/build.gradle.kts.backup
```

**Step 2: Verify current build works**

Run: `./gradlew :gui:run`
Expected: Application launches successfully

**Step 3: Test current packaging**

Run: `./gradlew :gui:createDistributable`
Expected: Distributable created in `gui/build/compose/binaries/main/app/`

**Step 4: Document current state**

Create notes about:
- Current runtime bundling approach (removal via afterEvaluate)
- Duplicate dependencies identified (lines 29 and 43)
- Working directory structure

**Step 5: Commit backup**

```bash
git add gui/build.gradle.kts.backup
git commit -m "docs: backup current GUI build configuration"
```

---

## Task 2: Remove Duplicate Dependencies

**Files:**
- Modify: `gui/build.gradle.kts:15-63`

**Step 1: Identify duplicate compose.desktop.currentOs declarations**

Current duplicates at:
- Line 29: `implementation(compose.desktop.currentOs) { exclude(group = "org.jetbrains.compose.material") ... }`
- Line 43: `implementation(compose.desktop.currentOs)`

**Step 2: Remove the second duplicate declaration**

Remove line 43 entirely. Keep only the first declaration (line 29) which has the Material exclusion.

**Step 3: Verify dependencies compile**

Run: `./gradlew :gui:dependencies --configuration compileClasspath | grep compose`
Expected: Only one compose.desktop.currentOs entry

**Step 4: Test application still runs**

Run: `./gradlew :gui:run`
Expected: Application launches successfully

**Step 5: Commit dependency cleanup**

```bash
git add gui/build.gradle.kts
git commit -m "refactor: remove duplicate compose.desktop.currentOs dependency"
```

---

## Task 3: Replace JRE Removal Workaround with Proper Configuration

**Files:**
- Modify: `gui/build.gradle.kts:82-119`

**Step 1: Remove custom afterEvaluate JRE removal code**

Delete lines 104-117 (the entire afterEvaluate block that removes the runtime directory).

**Step 2: Add includeAllModules to nativeDistributions**

Add after line 87 (after `vendor = "Moribito"`):

```kotlin
            // Include all JDK modules to avoid runtime ClassNotFoundException
            // This increases distributable size but ensures compatibility
            includeAllModules = true
```

**Step 3: Verify configuration is valid**

Run: `./gradlew :gui:tasks | grep package`
Expected: List of packaging tasks displayed without errors

**Step 4: Test createDistributable with new configuration**

Run: `./gradlew :gui:createDistributable`
Expected: Distributable created with bundled JRE at `gui/build/compose/binaries/main/app/Moribito.app/Contents/runtime`

**Step 5: Commit JRE configuration update**

```bash
git add gui/build.gradle.kts
git commit -m "refactor: replace JRE removal workaround with includeAllModules"
```

---

## Task 4: Restructure compose.desktop Block for Clarity

**Files:**
- Modify: `gui/build.gradle.kts:78-119`

**Step 1: Add launcher configuration section**

After line 80 (`mainClass = "com.moribito.gui.MainKt"`), add JVM arguments section:

```kotlin

        // JVM configuration for runtime
        jvmArgs += listOf(
            "-Xmx2048m"
        )
```

**Step 2: Add comments for clarity**

Add section comments to organize the configuration:

```kotlin
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
```

**Step 3: Verify configuration syntax**

Run: `./gradlew :gui:clean`
Expected: No syntax errors

**Step 4: Test configuration**

Run: `./gradlew :gui:run`
Expected: Application launches with new JVM args

**Step 5: Commit restructured configuration**

```bash
git add gui/build.gradle.kts
git commit -m "refactor: restructure compose.desktop config with clear sections"
```

---

## Task 5: Clean Up Dependencies Block

**Files:**
- Modify: `gui/build.gradle.kts:15-63`

**Step 1: Reorganize dependencies with comments**

Add section comments to organize dependencies:

```kotlin
dependencies {
    // Core module
    implementation(project(":core"))

    // Jewel UI components
    implementation("org.jetbrains.jewel:jewel-int-ui-standalone:0.33.0-253.29795")
    implementation("org.jetbrains.jewel:jewel-decorated-window:0.32.1-253.28294.285")

    // IntelliJ Platform icons
    implementation("com.jetbrains.intellij.platform:icons:253.29346.145")

    // Compose Desktop and components
    implementation(compose.desktop.currentOs) {
        exclude(group = "org.jetbrains.compose.material")
    }
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    implementation("org.jetbrains.compose.components:components-splitpane:1.9.3")

    // JNA for native platform integration
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")

    // Dependency injection
    implementation("io.insert-koin:koin-compose:4.1.1")
    implementation("io.insert-koin:koin-compose-viewmodel:4.1.1")

    // Fluent UI icons
    implementation("io.github.compose-fluent:fluent:v0.1.0")
    implementation("io.github.compose-fluent:fluent-icons-extended:v0.1.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")

    // Configuration and serialization
    implementation("com.akuleshov7:ktoml-core:0.7.1")
    implementation("com.akuleshov7:ktoml-file:0.7.1")

    // Additional icons and utilities
    implementation("br.com.devsrsouza.compose.icons:octicons:1.1.1")
    implementation("io.github.serpro69:kotlin-faker:1.16.0")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.mockk:mockk:1.13.9")
}
```

**Step 2: Verify dependencies resolve**

Run: `./gradlew :gui:dependencies`
Expected: All dependencies resolve without conflicts

**Step 3: Test compilation**

Run: `./gradlew :gui:compileKotlin`
Expected: Compilation succeeds

**Step 4: Commit cleaned up dependencies**

```bash
git add gui/build.gradle.kts
git commit -m "refactor: organize dependencies with clear sections"
```

---

## Task 6: Test All Packaging Tasks

**Files:**
- Verify: `gui/build/compose/binaries/`

**Step 1: Test run task**

Run: `./gradlew :gui:run`
Expected: Application launches successfully
Verify: JVM args are applied (check memory with Activity Monitor/Task Manager)

**Step 2: Test createDistributable task**

Run: `./gradlew :gui:createDistributable`
Expected: Creates distributable at `gui/build/compose/binaries/main/app/Moribito.app`
Verify: Runtime directory exists at `Moribito.app/Contents/runtime`

**Step 3: Test runDistributable task**

Run: `./gradlew :gui:runDistributable`
Expected: Packaged application launches successfully

**Step 4: Test platform-specific package task (macOS example)**

Run: `./gradlew :gui:packageDmg`
Expected: Creates `.dmg` file at `gui/build/compose/binaries/main/dmg/`

Note: Cross-compilation is not supported. Windows `.msi`/`.exe` require Windows, Linux `.deb`/`.rpm` require Linux.

**Step 5: Test packageDistributionForCurrentOS task**

Run: `./gradlew :gui:packageDistributionForCurrentOS`
Expected: Creates all packages for current OS

**Step 6: Document successful task execution**

Create notes listing:
- All tasks tested
- Output locations
- File sizes
- Any warnings or issues

---

## Task 7: Document Available Tasks and Configuration

**Files:**
- Create: `docs/building-gui.md`

**Step 1: Create documentation file**

```markdown
# Building Moribito GUI

This document describes how to build and package the Moribito GUI application.

## Prerequisites

- JDK 17 or higher (JDK 25 recommended)
- Gradle 8.x (included via wrapper)

## Available Gradle Tasks

### Development Tasks

- `./gradlew :gui:run` - Run the application locally for development
- `./gradlew :gui:runDistributable` - Run the packaged application

### Packaging Tasks

- `./gradlew :gui:createDistributable` - Create application image without installer
- `./gradlew :gui:packageDistributionForCurrentOS` - Create all packages for current OS
- `./gradlew :gui:packageDmg` - Create macOS DMG installer (macOS only)
- `./gradlew :gui:packageMsi` - Create Windows MSI installer (Windows only)
- `./gradlew :gui:packageDeb` - Create Linux DEB package (Linux only)

### Utility Tasks

- `./gradlew :gui:suggestRuntimeModules` - Analyze required JDK modules
- `./gradlew :gui:checkRuntime` - Verify runtime configuration

## Configuration

The GUI build is configured in `gui/build.gradle.kts`:

- **Main Class:** `com.moribito.gui.MainKt`
- **JVM Args:** `-Xmx2048m`
- **JRE Bundling:** All JDK modules included (`includeAllModules = true`)
- **Package Name:** Moribito
- **Version:** 2.0.0

### Platform-Specific Settings

**macOS:**
- Icon: `gui/icons/moribito.icns`
- Bundle ID: `com.moribito.gui`
- Formats: DMG, PKG

**Windows:**
- Icon: `gui/icons/moribito.ico`
- Menu Group: Moribito
- Formats: EXE, MSI

**Linux:**
- Icon: `gui/icons/moribito.png`
- Formats: DEB, RPM

## Output Locations

After building, packages are created at:
- `gui/build/compose/binaries/main/app/` - Application image
- `gui/build/compose/binaries/main/dmg/` - macOS DMG
- `gui/build/compose/binaries/main/msi/` - Windows MSI
- `gui/build/compose/binaries/main/deb/` - Linux DEB

## Build for Production

```bash
# Clean previous builds
./gradlew :gui:clean

# Create distributable for current OS
./gradlew :gui:packageDistributionForCurrentOS

# Or create specific format
./gradlew :gui:packageDmg  # macOS
```

## Troubleshooting

### "Cannot find JDK"

Ensure `JAVA_HOME` points to JDK 17 or higher:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
```

### "Module not found at runtime"

If you encounter ClassNotFoundException:
1. Run `./gradlew :gui:suggestRuntimeModules` to identify missing modules
2. Either keep `includeAllModules = true` or add specific modules to configuration

## References

- [Compose Multiplatform Native Distribution](https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html)
- [JRE Bundling Options](https://github.com/JetBrains/compose-multiplatform/issues/207)
```

**Step 2: Verify documentation accuracy**

Check that all commands in the documentation work:
- Test each command listed
- Verify output locations
- Confirm prerequisites

**Step 3: Commit documentation**

```bash
git add docs/building-gui.md
git commit -m "docs: add GUI building and packaging guide"
```

---

## Task 8: Update gradle.properties if Needed

**Files:**
- Read: `gradle.properties`
- Modify: `gradle.properties` (if needed)

**Step 1: Review current Compose-related properties**

Current property:
```properties
compose.desktop.packaging.checkJdkVendor=false
```

**Step 2: Verify if checkJdkVendor bypass is still needed**

With `includeAllModules = true` and proper JDK configuration, this bypass should no longer be necessary.

**Step 3: Test without the bypass**

Comment out the line:
```properties
# compose.desktop.packaging.checkJdkVendor=false
```

**Step 4: Test packaging**

Run: `./gradlew :gui:createDistributable`
Expected: Should work without the bypass

If it fails, uncomment and add documentation explaining why it's needed.

**Step 5: Commit gradle.properties update**

```bash
git add gradle.properties
git commit -m "refactor: remove checkJdkVendor bypass (no longer needed)"
```

Or if bypass is still needed:
```bash
git add gradle.properties
git commit -m "docs: document why checkJdkVendor bypass is required"
```

---

## Task 9: Final Integration Testing

**Files:**
- Verify: All build outputs

**Step 1: Clean build from scratch**

Run: `./gradlew clean`
Expected: All build directories removed

**Step 2: Run full build**

Run: `./gradlew :gui:build`
Expected: All tests pass, compilation succeeds

**Step 3: Create and test distributable**

Run: `./gradlew :gui:createDistributable`
Expected: Distributable created successfully

**Step 4: Launch distributable**

Run: `./gradlew :gui:runDistributable`
Expected: Application launches and functions correctly

**Step 5: Test packaging for current OS**

Run: `./gradlew :gui:packageDistributionForCurrentOS`
Expected: Installer created for current OS

**Step 6: Manually test the installer**

Install the package and verify:
- Application launches
- Icon displays correctly
- All features work
- No runtime errors

**Step 7: Document test results**

Create notes about:
- All tasks tested successfully
- Any issues encountered
- Performance observations
- Package sizes

---

## Task 10: Clean Up and Final Commit

**Files:**
- Delete: `gui/build.gradle.kts.backup`
- Update: `docs/plans/2026-01-09-compose-native-distribution-overhaul.md`

**Step 1: Remove backup file**

```bash
rm gui/build.gradle.kts.backup
```

**Step 2: Review all changes**

Run: `git diff develop` or `git diff main`
Review all modifications to ensure they align with the plan.

**Step 3: Update this plan with completion notes**

Add completion summary to this document noting:
- Tasks completed
- Any deviations from plan
- Issues encountered and resolved
- Final configuration details

**Step 4: Final commit**

```bash
git add .
git commit -m "refactor: complete Compose native distribution configuration overhaul

- Remove duplicate compose.desktop.currentOs dependency
- Replace JRE removal workaround with includeAllModules
- Restructure build configuration for clarity
- Add comprehensive build documentation
- Test all packaging tasks successfully

Refs: https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html"
```

**Step 5: Push changes**

```bash
git push origin <branch-name>
```

---

## Success Criteria

- [x] All duplicate dependencies removed
- [x] JRE removal workaround replaced with proper configuration
- [x] Build configuration restructured with clear sections
- [x] All packaging tasks tested and working
- [x] Documentation created
- [x] Clean builds work from scratch
- [x] Packaged application launches successfully

## Notes

- `includeAllModules = true` increases distributable size but prevents ClassNotFoundException
- Alternative: Use `suggestRuntimeModules` task and specify only needed modules for smaller size
- Cross-compilation not supported - must build packages on target OS
- JDK 17+ required for jpackage tool used by Compose plugin
