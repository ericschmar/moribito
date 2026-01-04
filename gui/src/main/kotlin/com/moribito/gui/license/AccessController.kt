package com.moribito.gui.license

import com.moribito.config.GeneralSettings

object AccessController {

    fun checkAccess(settings: GeneralSettings): AccessStatus {
        val ignoreLicense = settings.debugIgnoreLicense || System.getProperty("moribito.ignoreLicense") == "true"

        // 1. Check paid license first
        val licenseKey = settings.licenseKey
        if (!licenseKey.isNullOrBlank() && !ignoreLicense) {
            val result = LicenseVerifier.verify(licenseKey)
            when (result) {
                is LicenseResult.Success -> return AccessStatus.Licensed(result.userEmail, result.expiresAt, null)
                is LicenseResult.ExpiringSoon -> return AccessStatus.Licensed(result.userEmail, result.expiresAt, result.daysRemaining)
                is LicenseResult.Expired -> {
                    // Fall through to trial check if license expired
                }
                is LicenseResult.Invalid, is LicenseResult.Error -> {
                    // Fall through to trial check if license invalid
                }
            }
        }

        // 2. Check trial second
        val trialStatus = TrialManager.checkTrialStatus(settings)
        return when (trialStatus) {
            is TrialStatus.Active -> AccessStatus.TrialActive(trialStatus.daysRemaining)
            is TrialStatus.GracePeriod -> AccessStatus.GracePeriod(trialStatus.daysRemaining)
            is TrialStatus.Expired -> AccessStatus.Blocked("Trial expired")
            is TrialStatus.Tampered -> AccessStatus.Blocked("Trial data tampered")
            is TrialStatus.HardwareMismatch -> AccessStatus.Blocked("Hardware mismatch")
            is TrialStatus.Consumed -> AccessStatus.Blocked("Trial consumed") // Should have a license if consumed
            is TrialStatus.NotStarted -> AccessStatus.TrialAvailable
        }
    }

    fun activateTrial(settings: GeneralSettings): Pair<GeneralSettings, AccessStatus> {
        val updatedSettings = TrialManager.activateTrial(settings)
        val status = checkAccess(updatedSettings)
        return Pair(updatedSettings, status)
    }
}

sealed class AccessStatus {
    data class Licensed(val userEmail: String, val expiresAt: String, val daysRemaining: Int?) : AccessStatus()
    data class TrialActive(val daysRemaining: Int) : AccessStatus()
    data class GracePeriod(val daysRemaining: Int) : AccessStatus()
    object TrialAvailable : AccessStatus()
    data class Blocked(val reason: String) : AccessStatus()

    fun hasAccess(): Boolean = this is Licensed || this is TrialActive || this is GracePeriod
    fun needsWarning(): Boolean = (this is Licensed && daysRemaining != null) || this is GracePeriod
}
