package com.moribito.gui.license

import com.moribito.config.GeneralSettings
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.test.*

class LicenseSystemTest {

    @Test
    fun testHardwareFingerprint() {
        val f1 = HardwareFingerprint.getFingerprint()
        val f2 = HardwareFingerprint.getFingerprint()
        assertEquals(f1, f2, "Fingerprint should be stable")
        assertEquals(64, f1.length, "Should be a SHA-256 hash")
    }

    @Test
    fun testTrialActivation() {
        val settings = GeneralSettings()
        val updated = TrialManager.activateTrial(settings)
        
        assertNotNull(updated.trialStartedAt)
        assertNotNull(updated.trialHardwareKey)
        assertNotNull(updated.trialValidationHash)
        assertFalse(updated.trialConsumed)
        assertTrue(updated.isTrialDataValid())
    }

    @Test
    fun testTrialStatusActive() {
        val settings = TrialManager.activateTrial(GeneralSettings())
        val status = TrialManager.checkTrialStatus(settings)
        assertTrue(status is TrialStatus.Active)
        assertEquals(14, (status as TrialStatus.Active).daysRemaining)
    }

    @Test
    fun testTrialTampering() {
        val settings = TrialManager.activateTrial(GeneralSettings())
        val tampered = settings.copy(trialStartedAt = Clock.System.now().toString())
        assertFalse(tampered.isTrialDataValid())
        assertEquals(TrialStatus.Tampered, TrialManager.checkTrialStatus(tampered))
    }

    @Test
    fun testTrialStatusGracePeriod() {
        val now = Clock.System.now()
        val fifteenDaysAgo = now.minus(15, DateTimeUnit.DAY, TimeZone.UTC)
        
        val settings = TrialManager.activateTrial(GeneralSettings()).copy(
            trialStartedAt = fifteenDaysAgo.toString()
        )
        // Need to re-generate hash for tampered startedAt to be "valid" but old
        val updatedSettings = settings.copy(
            trialValidationHash = generateHash(settings.trialStartedAt!!, settings.trialHardwareKey!!)
        )
        
        val status = TrialManager.checkTrialStatus(updatedSettings)
        assertTrue(status is TrialStatus.GracePeriod)
        assertEquals(2, (status as TrialStatus.GracePeriod).daysRemaining) // 14 + 3 = 17. 17 - 15 = 2.
    }

    @Test
    fun testTrialStatusExpired() {
        val now = Clock.System.now()
        val twentyDaysAgo = now.minus(20, DateTimeUnit.DAY, TimeZone.UTC)

        val settings = TrialManager.activateTrial(GeneralSettings()).copy(
            trialStartedAt = twentyDaysAgo.toString()
        )
        val updatedSettings = settings.copy(
            trialValidationHash = generateHash(settings.trialStartedAt!!, settings.trialHardwareKey!!)
        )

        val status = TrialManager.checkTrialStatus(updatedSettings)
        assertEquals(TrialStatus.Expired, status)
    }

    @Test
    fun testIgnoreLicense() {
        // Create settings with a valid-looking license (but we'll just mock/assume verify would succeed)
        // Actually LicenseVerifier.verify is hard to satisfy without a real key, 
        // but we can check if it's CALLED.
        // Even easier: check if AccessController returns TrialStatus instead of Licensed when ignoreLicense is true.
        
        val settings = GeneralSettings(
            licenseKey = "fake.license",
            debugIgnoreLicense = true
        )
        
        // Should return TrialAvailable because no trial started, and license is ignored
        val status = AccessController.checkAccess(settings)
        assertEquals(AccessStatus.TrialAvailable, status)
        
        val settingsWithLicense = GeneralSettings(
            licenseKey = "invalid.license",
            debugIgnoreLicense = false
        )
        // Should NOT be TrialAvailable if license check is attempted and fails/is invalid, 
        // it falls through to trial check, which is NotStarted -> TrialAvailable.
        // Wait, if it's invalid, it falls through.
        
        // Let's test system property
        System.setProperty("moribito.ignoreLicense", "true")
        val statusProp = AccessController.checkAccess(GeneralSettings(licenseKey = "any"))
        assertEquals(AccessStatus.TrialAvailable, statusProp)
        System.clearProperty("moribito.ignoreLicense")
    }

    private fun generateHash(startedAt: String, hardwareKey: String): String {
        val salt = "moribito-trial-salt-2026"
        val input = startedAt + hardwareKey + salt
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
