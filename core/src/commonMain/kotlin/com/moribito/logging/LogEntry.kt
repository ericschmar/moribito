package com.moribito.logging

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Represents a single log entry with timestamp, level, component, and message.
 */
data class LogEntry(
    val timestamp: Instant,
    val level: LogLevel,
    val component: String,
    val message: String,
    val throwable: Throwable? = null
) {
    /**
     * Formats the log entry as a string for file output.
     * Format: "2026-01-01 10:34:52.123 [DEBUG] [LdapClient] Message"
     */
    fun format(): String {
        val localDateTime = timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
        val formattedTime = String.format(
            "%04d-%02d-%02d %02d:%02d:%02d.%03d",
            localDateTime.year,
            localDateTime.monthNumber,
            localDateTime.dayOfMonth,
            localDateTime.hour,
            localDateTime.minute,
            localDateTime.second,
            localDateTime.nanosecond / 1_000_000
        )

        val baseMessage = "$formattedTime [${level.displayName}] [$component] $message"

        return if (throwable != null) {
            val stackTrace = throwable.stackTraceToString()
            "$baseMessage\n$stackTrace"
        } else {
            baseMessage
        }
    }

    companion object {
        /**
         * Parses a log line back into a LogEntry.
         * Returns null if the line cannot be parsed.
         */
        fun parse(line: String): LogEntry? {
            // Pattern: "2026-01-01 10:34:52.123 [DEBUG] [Component] Message"
            val regex = """(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) \[(\w+)] \[([^\]]+)] (.+)""".toRegex()
            val match = regex.matchEntire(line) ?: return null

            val (timestampStr, levelStr, component, message) = match.destructured

            // Parse timestamp (simplified - just use current time as fallback)
            val timestamp = Clock.System.now()
            val level = LogLevel.fromString(levelStr)

            return LogEntry(
                timestamp = timestamp,
                level = level,
                component = component,
                message = message,
                throwable = null
            )
        }
    }
}
