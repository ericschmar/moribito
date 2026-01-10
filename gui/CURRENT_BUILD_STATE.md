# Current GUI Build Configuration State

**Date:** 2026-01-09
**Task:** Backup and Verify Current Configuration (Task 1 of 10)
**Status:** Verified and Working

## Overview

This document captures the current state of the GUI module's build configuration before undertaking the Compose Multiplatform native distribution overhaul.

## Build Configuration Analysis

### File: `/Users/mmacbook/develop/moribito/gui/build.gradle.kts`

### Key Configuration Details

#### 1. Java/Kotlin Versions
- **JVM Toolchain:** Java 25
- **Source/Target Compatibility:** Java 25
- **Kotlin Version:** 2.3.0
- **Compose Version:** 1.8.0

#### 2. Dependencies

##### Duplicate Dependencies Identified
**Issue:** `compose.desktop.currentOs` is declared twice:
- **Line 29:** Within a configuration block that excludes Material:
  ```kotlin
  implementation(compose.desktop.currentOs) {
      exclude(group = "org.jetbrains.compose.material")
      // Also incorrectly nests other implementations here
      implementation("net.java.dev.jna:jna:5.14.0")
      implementation("net.java.dev.jna:jna-platform:5.14.0")
  }
  ```
- **Line 43:** Standalone declaration:
  ```kotlin
  implementation(compose.desktop.currentOs)
  ```

**Impact:** This duplication may cause:
- Unclear dependency resolution order
- Potential classpath conflicts
- Confusion about which Material exclusion is effective

##### Incorrectly Nested Dependencies
**Issue:** Lines 32-33 incorrectly nest JNA dependencies inside the `compose.desktop.currentOs` configuration block
```kotlin
implementation(compose.desktop.currentOs) {
    exclude(group = "org.jetbrains.compose.material")

    implementation("net.java.dev.jna:jna:5.14.0")           // WRONG: inside exclusion block
    implementation("net.java.dev.jna:jna-platform:5.14.0") // WRONG: inside exclusion block
}
```

**Correct approach:** These should be at the same level as other dependencies, not nested.

#### 3. Native Distribution Configuration

##### Current Runtime Bundling Approach
**Location:** Lines 104-117

**Method:** Custom `afterEvaluate` hook with `doLast` action

```kotlin
afterEvaluate {
    tasks.named("createDistributable").configure {
        doLast {
            val runtimeDir = file("build/compose/binaries/main/app/Moribito.app/Contents/runtime")
            if (runtimeDir.exists()) {
                println("Removing bundled JRE runtime to avoid macOS signing issues...")
                runtimeDir.deleteRecursively()
                println("Runtime removed. Users will need Java 21+ installed.")
            }
        }
    }
}
```

**Purpose:** Removes bundled JRE to avoid macOS 15 AMFI (Apple Mobile File Integrity) issues with adhoc-signed dynamic libraries (dylibs)

**Limitations:**
- Non-standard approach (workaround)
- Requires post-processing the distribution
- Comment mentions "Java 21+" but build requires Java 25
- Not aligned with official Compose Multiplatform best practices

##### Distribution Targets
- **macOS:** DMG format, bundle ID: `com.moribito.gui`
- **Windows:** MSI format
- **Linux:** DEB format

##### Package Metadata
- **Package Name:** "Moribito"
- **Package Version:** "2.0.0"
- **Description:** "LDAP Directory Explorer"
- **Vendor:** "Moribito"

#### 4. Icon Configuration
- **macOS:** `icons/moribito.icns`
- **Windows:** `icons/moribito.ico`
- **Linux:** `icons/moribito.png`

#### 5. Runtime Arguments
Custom dock icon configuration for macOS (lines 125-131):
```kotlin
tasks.withType<JavaExec> {
    val iconPath = file("icons/moribito.icns").absolutePath

    if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
        jvmArgs("-Xdock:icon=$iconPath", "-Xdock:name=Moribito")
    }
}
```

## Working Directory Structure

### Build Output Location
```
gui/build/compose/binaries/main/app/
└── Moribito.app/
    └── Contents/
        ├── Info.plist
        ├── MacOS/
        ├── PkgInfo
        ├── Resources/
        ├── _CodeSignature/
        ├── app/          (application jars and resources)
        └── runtime/      (REMOVED by afterEvaluate hook)
```

### Verification Results

#### Build Test
```bash
./gradlew :gui:jar -x test
```
**Result:** ✅ BUILD SUCCESSFUL

#### Packaging Test
```bash
./gradlew :gui:createDistributable
```
**Result:** ✅ BUILD SUCCESSFUL
**Output:** `/Users/mmacbook/develop/moribito/gui/build/compose/binaries/main/app/`
**Runtime Removal:** ✅ Confirmed - "Removing bundled JRE runtime to avoid macOS signing issues..."

## Issues to Address in Overhaul

1. **Duplicate Dependencies:** Remove duplicate `compose.desktop.currentOs` declaration
2. **Nested Dependencies:** Move JNA dependencies out of exclusion block
3. **Runtime Bundling:** Replace custom `afterEvaluate` workaround with official Compose configuration
4. **Version Mismatch:** Align Java version requirement comment (currently says "Java 21+" but requires Java 25)
5. **Material Exclusion:** Verify if Material exclusion is working as intended with duplicate declarations

## Next Steps

Following tasks will:
- Upgrade Compose Multiplatform to 1.8.1 (latest stable)
- Remove duplicate dependencies
- Implement proper `includeAllModules = false` configuration
- Replace custom runtime removal with official approach
- Add proper app launchers
- Improve native distribution configuration

## Backup Information

**Backup File:** `/Users/mmacbook/develop/moribito/gui/build.gradle.kts.backup`
**Created:** 2026-01-09
**Size:** 4,249 bytes
**Purpose:** Restore point before overhaul
