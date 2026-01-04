package com.moribito.gui.license

import com.moribito.config.GeneralSettings
import kotlinx.datetime.*
import java.security.MessageDigest

object TrialManager {
    private const val TRIAL_DURATION_DAYS = 14
    private const val GRACE_PERIOD_DAYS = 3
    private const val SALT = "moribito-trial-salt-2026"

    fun activateTrial(settings: GeneralSettings): GeneralSettings {
        val startedAt = Clock.System.now().toString()
        val hardwareKey = HardwareFingerprint.getFingerprint()
        val hash = generateHash(startedAt, hardwareKey)
        
        return settings.copy(
            trialStartedAt = startedAt,
            trialHardwareKey = hardwareKey,
            trialConsumed = false,
            trialValidationHash = hash
        )
    }

    fun consumeTrial(settings: GeneralSettings): GeneralSettings {
        return settings.copy(trialConsumed = true)
    }

    fun checkTrialStatus(settings: GeneralSettings): TrialStatus {
        if (settings.trialConsumed) return TrialStatus.Consumed
        if (settings.trialStartedAt == null) return TrialStatus.NotStarted

        if (!settings.isTrialDataValid()) return TrialStatus.Tampered

        val currentHardwareKey = HardwareFingerprint.getFingerprint()
        if (settings.trialHardwareKey != currentHardwareKey) return TrialStatus.HardwareMismatch

        return try {
            val startedAt = Instant.parse(settings.trialStartedAt!!)
            val now = Clock.System.now()
            val duration = now - startedAt
            val daysPassed = duration.toComponents { days, _, _, _, _ -> days }.toInt()

            when {
                daysPassed < TRIAL_DURATION_DAYS -> {
                    TrialStatus.Active(TRIAL_DURATION_DAYS - daysPassed)
                }
                daysPassed < (TRIAL_DURATION_DAYS + GRACE_PERIOD_DAYS) -> {
                    TrialStatus.GracePeriod((TRIAL_DURATION_DAYS + GRACE_PERIOD_DAYS) - daysPassed)
                }
                else -> {
                    TrialStatus.Expired
                }
            }
        } catch (e: Exception) {
            TrialStatus.Tampered
        }
    }

    private fun generateHash(startedAt: String, hardwareKey: String): String {
        val input = startedAt + hardwareKey + SALT
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

sealed class TrialStatus {
    object NotStarted : TrialStatus()
    data class Active(val daysRemaining: Int) : TrialStatus()
    data class GracePeriod(val daysRemaining: Int) : TrialStatus()
    object Expired : TrialStatus()
    object Consumed : TrialStatus()
    object Tampered : TrialStatus()
    object HardwareMismatch : TrialStatus()
}
