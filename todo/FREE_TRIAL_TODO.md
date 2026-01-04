Free Trial System Implementation Plan

Overview

Implement a 14-day free trial system with
hardware-based tracking to prevent reinstall
workarounds, plus fix the existing license expiry
validation bug.

Requirements Summary

- Trial Duration: 14 days
- Grace Period: 3 days after expiry with warnings
  before hard block
- Hardware Tracking: Use MAC address + email to
  generate stable fingerprint
- Priority: Valid License → Active Trial → Grace Period
  → Hard Block
- Also Fix: License expiry dates currently ignored
  (line 34-35 in Verifier.kt)

Architecture

Access Control Priority

1. Check paid license first - if valid, grant full
   access
2. Check trial second - if no valid license, check
   trial status
3. Grace period - show warnings but maintain access
4. Hard block - prevent app usage after grace expires

Anti-Tampering Strategy

- Store trial metadata in config.toml with SHA-256
  validation hash
- Hardware fingerprint ensures trial tied to specific
  machine
- Validation hash = SHA256(startedAt + hardwareKey +
  salt)

Implementation Steps

Phase 1: Hardware Fingerprinting

New File: gui/src/main/kotlin/com/moribito/gui/license/
HardwareFingerprint.kt

Generate stable hardware fingerprint using:
- MAC address (primary, most stable)
- System username (user-specific)
- Hostname (machine-specific)
- OS-specific IDs (Mac serial via ioreg, Windows
  MachineGuid via registry)
- SHA-256 hash of combined identifiers

Why: Multi-factor approach provides stability across
reboots while preventing simple reinstall workarounds.

Phase 2: Trial Data Model

Modify: core/src/commonMain/kotlin/com/moribito/config/
Config.kt

Extend GeneralSettings (currently lines 50-54) with:
@SerialName("trial_started_at") val trialStartedAt:
String? = null  // ISO-8601
@SerialName("trial_hardware_key") val trialHardwareKey:
String? = null
@SerialName("trial_consumed") val trialConsumed:
Boolean = false
@SerialName("trial_validation_hash") val
trialValidationHash: String? = null

Add validation method:
fun isTrialDataValid(): Boolean  // Verifies SHA-256
hash

Why: Extends existing TOML-based config system with
anti-tamper protection.

Phase 3: Fix License Expiry Bug

Modify: gui/src/main/kotlin/com/moribito/gui/license/Ve
rifier.kt

Current Bug (lines 34-35): License data contains
email|expiry but only email is extracted.

Fix:
1. Expand LicenseResult sealed class (lines 46-49):
- Add data class Expired(userEmail: String,
  expiredAt: String)
- Add data class ExpiringSoon(userEmail: String,
  expiresAt: String, daysRemaining: Int)
- Modify Success to include expiresAt: String
2. Update verify() method (lines 12-43):
- Parse expiry date from license data
- Use kotlinx.datetime.LocalDate.parse() to parse
  YYYY-MM-DD format
- Calculate days until expiry
- Return Expired if past date, ExpiringSoon if ≤14
  days, Success otherwise

Why: Fixes critical bug where licenses with expiry
dates aren't validated.

Phase 4: Trial Management

New File: gui/src/main/kotlin/com/moribito/gui/license/
TrialManager.kt

Implement TrialManager object with:
- activateTrial(settings): Creates trial metadata,
  returns updated settings
- checkTrialStatus(settings): Returns TrialStatus enum
- consumeTrial(settings): Marks trial consumed when
  license activated

TrialStatus sealed class:
- NotStarted - No trial data exists
- Active(daysRemaining) - Trial active, show days left
- GracePeriod(daysRemaining) - Past 14 days, in 7-day
  grace
- Expired - Hard block
- Consumed - User purchased license
- Tampered - Validation hash mismatch
- HardwareMismatch - Fingerprint changed

Why: Encapsulates all trial logic in single responsible
component.

Phase 5: Unified Access Control

New File: gui/src/main/kotlin/com/moribito/gui/license/
AccessController.kt

Implement AccessController object with:
- checkAccess(settings): Returns AccessStatus by
  checking license → trial priority
- activateTrial(settings): Combines activation + status
  check

AccessStatus sealed class:
- Licensed(userEmail, expiresAt, daysRemaining?) -
  Valid license
- TrialActive(daysRemaining) - Trial active
- GracePeriod(daysRemaining) - Grace period active
- TrialAvailable - Can activate trial
- Blocked(reason) - No access

Helper methods:
- hasAccess(): Boolean - True for
  Licensed/TrialActive/GracePeriod
- needsWarning(): Boolean - True for expiring licenses
  or grace period

Why: Single source of truth for access decisions,
simplifies UI logic.

Phase 6: ViewModel Integration

Modify: gui/src/main/kotlin/com/moribito/gui/viewmodel/
ConfigViewModel.kt

Add methods:
- checkAccessStatus(): Returns current access status
- activateTrial(): Activates trial, saves config,
  updates state
- refreshAccessStatus(): Re-checks access (for periodic
  refresh)

Update init block:
- Replace license verification with
  AccessController.checkAccess()
- Log access status on startup

Update verifyLicense() method:
- Add trial consumption when license verified
- Handle new LicenseResult variants (Expired,
  ExpiringSoon)
- Call refreshAccessStatus() after verification

Why: Integrates trial system into existing ViewModel
architecture.

Phase 7: State Management

Modify: gui/src/main/kotlin/com/moribito/gui/viewmodel/
AppState.kt

Replace line 102:
// OLD: val verificationResult: LicenseResult? = null
// NEW: val accessStatus: AccessStatus? = null

Impact: All code referencing verificationResult must
update to accessStatus.

Why: Unified state model for both license and trial
status.

Phase 8: UI Updates

Modify: gui/src/main/kotlin/com/moribito/gui/ui/screens
/StartScreen.kt

Update left panel (branding section) to show:
- Licensed: "Licensed to: {email}" + expiry warning if
  needed
- TrialActive: "Trial Active: X days remaining"
- GracePeriod: "GRACE PERIOD: X days to purchase"
  (warning color)
- TrialAvailable: "Start 14-Day Free Trial" button
- Blocked: Error message with reason

Update right panel:
- Enable "Manage Connections" and connection cards
  based on accessStatus.hasAccess()
- All buttons disabled when hasAccess() == false

Keep existing license input/verification UI for users
who have keys.

Why: Clear visual feedback for all access states,
maintains existing UI structure.

Critical Files to Modify

New Files (3)

1. gui/src/main/kotlin/com/moribito/gui/license/Hardwar
   eFingerprint.kt
2. gui/src/main/kotlin/com/moribito/gui/license/TrialMa
   nager.kt
3. gui/src/main/kotlin/com/moribito/gui/license/AccessC
   ontroller.kt

Modified Files (5)

1. core/src/commonMain/kotlin/com/moribito/config/Confi
   g.kt - Lines 50-54
2. gui/src/main/kotlin/com/moribito/gui/license/Verifie
   r.kt - Lines 34-35, 46-49
3. gui/src/main/kotlin/com/moribito/gui/viewmodel/Confi
   gViewModel.kt - Add methods, update init
4. gui/src/main/kotlin/com/moribito/gui/viewmodel/AppSt
   ate.kt - Line 102
5. gui/src/main/kotlin/com/moribito/gui/ui/screens/Star
   tScreen.kt - UI state handling

Implementation Order

1. HardwareFingerprint.kt - No dependencies, foundation
   layer
2. Config.kt - Data model extension
3. Verifier.kt - Critical bug fix, no trial
   dependencies
4. TrialManager.kt - Depends on Config,
   HardwareFingerprint
5. AccessController.kt - Depends on TrialManager,
   Verifier
6. AppState.kt - State model update
7. ConfigViewModel.kt - Business logic orchestration
8. StartScreen.kt - UI layer (depends on all above)

Edge Cases Handled

| Scenario                       | Behavior
|
|--------------------------------|---------------------
-----------------------|
| Fresh install                  | Show "Start 14-Day
Free Trial" button      |
| Trial active                   | Show days remaining,
full app access       |
| Trial expired                  | Show grace period
warning, maintain access |
| Grace expired                  | Hard block with
purchase CTA               |
| License purchased during trial | Trial consumed, full
access via license    |
| License expires                | Fall back to trial
status (if available)   |
| Hardware change                | Hardware mismatch,
trial invalid           |
| Config tampering               | Tampered state,
block access               |
| Clock manipulation             | Accepted limitation
(offline-first design) |
| VM snapshot                    | MAC address changes,
trial won't transfer  |

Dependencies

All required dependencies already present:
- kotlinx-datetime:0.5.0 - For date parsing/comparison
- net.java.dev.jna:jna:5.14.0 - For hardware ID access
- ktoml-core / ktoml-file - For config serialization

Testing Strategy

Unit Tests

- HardwareFingerprint: Stability, fallbacks, hash
  format
- TrialManager: Activation, expiry calculations,
  tampering detection
- LicenseVerifier: Valid/expired/expiring licenses,
  signature validation
- AccessController: Priority logic, state transitions

Integration Tests

- Config persistence with trial metadata
- Full flow: trial → expiry → license purchase
- Tampering detection across save/load cycles

Manual Testing

- Fresh install trial activation
- Daily countdown verification
- Grace period warnings
- Hard block after grace
- License verification replacing trial
- Config file tampering
- Reinstall on same hardware (should preserve trial)

Success Criteria

✅ Trial system activates on fresh install
✅ Hardware fingerprint prevents reinstall workaround
✅ 14-day countdown works correctly
✅ 7-day grace period shows warnings
✅ Hard block prevents app usage after grace
✅ Valid license bypasses trial completely
✅ License expiry dates now validated (bug fixed)
✅ Config tampering detected and blocked
✅ UI clearly shows all access states
✅ Smooth transition from trial to paid license
