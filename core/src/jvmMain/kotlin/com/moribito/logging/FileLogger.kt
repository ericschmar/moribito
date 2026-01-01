package com.moribito.logging

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.datetime.Clock
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

/**
 * JVM implementation of Logger that writes to files asynchronously.
 * Uses a coroutine channel to ensure non-blocking logging.
 */
class FileLogger(
    override val component: String,
    override val minLevel: LogLevel,
    private val logFile: File,
    private val enableConsole: Boolean = true
) : Logger {
    private val logChannel = Channel<LogEntry>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var writer: BufferedWriter? = null

    init {
        // Start the background logging coroutine
        scope.launch {
            writer = BufferedWriter(FileWriter(logFile, true))
            try {
                for (entry in logChannel) {
                    writeEntry(entry)
                }
            } finally {
                writer?.flush()
                writer?.close()
            }
        }
    }

    private fun writeEntry(entry: LogEntry) {
        try {
            val formattedMessage = entry.format()

            // Write to file
            writer?.write(formattedMessage)
            writer?.newLine()
            writer?.flush()

            // Also print to console if enabled
            if (enableConsole) {
                println(formattedMessage)
            }
        } catch (e: Exception) {
            // If logging fails, print to stderr but don't crash
            System.err.println("Failed to write log entry: ${e.message}")
        }
    }

    override fun debug(message: String, throwable: Throwable?) {
        log(LogLevel.DEBUG, message, throwable)
    }

    override fun info(message: String, throwable: Throwable?) {
        log(LogLevel.INFO, message, throwable)
    }

    override fun warn(message: String, throwable: Throwable?) {
        log(LogLevel.WARN, message, throwable)
    }

    override fun error(message: String, throwable: Throwable?) {
        log(LogLevel.ERROR, message, throwable)
    }

    override fun log(level: LogLevel, message: String, throwable: Throwable?) {
        // Only log if the level is at or above the minimum level
        if (level.priority >= minLevel.priority) {
            val entry = LogEntry(
                timestamp = Clock.System.now(),
                level = level,
                component = component,
                message = message,
                throwable = throwable
            )

            // Send to channel (non-blocking)
            scope.launch {
                logChannel.send(entry)
            }
        }
    }

    /**
     * Closes the logger and flushes remaining log entries.
     */
    fun close() {
        logChannel.close()
        runBlocking {
            // Wait for all pending entries to be written
            delay(100)
        }
        scope.cancel()
    }
}

/**
 * Actual implementation of the LoggerFactory for JVM.
 */
actual object LoggerFactory {
    private lateinit var logFileManager: LogFileManager
    private lateinit var currentLogFile: File
    private var minLevel: LogLevel = LogLevel.DEBUG
    private val loggers = mutableMapOf<String, FileLogger>()

    actual fun initialize(logDirectory: File, minLevel: LogLevel) {
        this.minLevel = minLevel
        logFileManager = LogFileManager(logDirectory)
        currentLogFile = logFileManager.createNewLogFile()
    }

    actual fun getLogger(component: String): Logger {
        // Return existing logger or create new one
        return loggers.getOrPut(component) {
            FileLogger(component, minLevel, currentLogFile)
        }
    }

    actual fun getCurrentLogFile(): File {
        return currentLogFile
    }

    actual fun shutdown() {
        // Close all loggers
        loggers.values.forEach { it.close() }
        loggers.clear()
    }
}
