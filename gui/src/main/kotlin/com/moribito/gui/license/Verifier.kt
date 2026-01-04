package com.moribito.gui.license

import java.util.Base64
import java.security.Signature
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import kotlinx.datetime.*

object LicenseVerifier {
    // Your Ed25519 Public Key (Raw hex or Base64)
    private const val PUBLIC_KEY_B64 = "MCowBQYDK2VwAyEAbHHhrI5lplUS23qIU/lUArTBUBIbXJ8409cMlP1YEVI="

    fun verify(licenseKey: String): LicenseResult {
        return try {
            val parts = licenseKey.split(".")
            val encodedData = parts[0]
            val encodedSig = parts[1]

            val dataBytes = Base64.getUrlDecoder().decode(encodedData)
            val sigBytes = Base64.getUrlDecoder().decode(encodedSig)
            println("[DEBUG_LOG] Decoded data: ${String(dataBytes)}")
            println("[DEBUG_LOG] Signature (hex): ${sigBytes.joinToString("") { "%02x".format(it) }}")

            // Verify Logic
            val pubKeyBytes = Base64.getDecoder().decode(PUBLIC_KEY_B64)
            println("[DEBUG_LOG] Public Key (hex): ${pubKeyBytes.takeLast(32).joinToString("") { "%02x".format(it) }}")
            val pubKey = KeyFactory.getInstance("Ed25519")
                .generatePublic(X509EncodedKeySpec(pubKeyBytes))

            val s = Signature.getInstance("Ed25519")
            s.initVerify(pubKey)
            s.update(dataBytes)

            if (s.verify(sigBytes)) {
                val dataString = String(dataBytes) // "email|expiry"
                val split = dataString.split("|")
                val email = split[0]
                val expiryStr = split.getOrNull(1)

                if (expiryStr != null) {
                    val expiryDate = LocalDate.parse(expiryStr)
                    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                    val daysUntil = today.daysUntil(expiryDate)

                    when {
                        daysUntil < 0 -> LicenseResult.Expired(email, expiryStr)
                        daysUntil <= 14 -> LicenseResult.ExpiringSoon(email, expiryStr, daysUntil)
                        else -> LicenseResult.Success(email, expiryStr)
                    }
                } else {
                    // No expiry date, treat as lifetime
                    LicenseResult.Success(email, "Lifetime")
                }
            } else {
                LicenseResult.Invalid
            }
        } catch (e: Exception) {
            println(e.cause)
            LicenseResult.Error(e.message ?: "Unknown error")
        }
    }
}

sealed class LicenseResult {
    data class Success(val userEmail: String, val expiresAt: String) : LicenseResult()
    data class Expired(val userEmail: String, val expiredAt: String) : LicenseResult()
    data class ExpiringSoon(val userEmail: String, val expiresAt: String, val daysRemaining: Int) : LicenseResult()
    object Invalid : LicenseResult()
    data class Error(val msg: String) : LicenseResult()
}