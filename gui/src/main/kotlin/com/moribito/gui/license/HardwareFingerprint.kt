package com.moribito.gui.license

import java.net.NetworkInterface
import java.net.InetAddress
import java.security.MessageDigest
import java.util.*

object HardwareFingerprint {

    fun getFingerprint(): String {
        val identifiers = mutableListOf<String>()

        // 1. System Username
        identifiers.add("user:${System.getProperty("user.name")}")

        // 2. OS Specific IDs
        getOsSpecificId()?.let { identifiers.add("os_id:$it") }

        val combined = identifiers.joinToString("|")
        return sha256(combined)
    }

    private fun getOsSpecificId(): String? {
        val os = System.getProperty("os.name").lowercase(Locale.getDefault())
        return when {
            os.contains("mac") -> getMacSerialNumber()
            os.contains("win") -> getWindowsMachineGuid()
            else -> null
        }
    }

    private fun getMacSerialNumber(): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("ioreg", "-rd1", "-c", "IOPlatformExpertDevice"))
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val marker = "\"IOPlatformSerialNumber\" = \""
            val start = output.indexOf(marker)
            if (start != -1) {
                val end = output.indexOf("\"", start + marker.length)
                if (end != -1) {
                    output.substring(start + marker.length, end)
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getWindowsMachineGuid(): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("reg", "query", "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid"))
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val marker = "MachineGuid    REG_SZ    "
            val start = output.indexOf(marker)
            if (start != -1) {
                output.substring(start + marker.length).trim()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
