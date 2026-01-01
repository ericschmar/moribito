package com.moribito.logging

/**
 * Log levels for the Moribito logging system.
 * Priority determines which messages are logged based on minimum level.
 */
enum class LogLevel(val priority: Int, val displayName: String) {
    ERROR(40, "ERROR"),
    WARN(30, "WARN"),
    INFO(20, "INFO"),
    DEBUG(10, "DEBUG");

    companion object {
        /**
         * Parse a log level from a string (case-insensitive).
         * Returns DEBUG if the string doesn't match any level.
         */
        fun fromString(value: String): LogLevel {
            return values().find { it.displayName.equals(value, ignoreCase = true) } ?: DEBUG
        }
    }
}
