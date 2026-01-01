package com.moribito.logging

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Manages log file rotation and cleanup.
 * Creates new log files and removes old ones based on retention policy.
 */
class LogFileManager(
    private val logDirectory: File,
    private val maxFileCount: Int = 10,
    private val maxFileSizeMB: Int = 10
) {
    private val fileNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")

    init {
        // Ensure log directory exists
        if (!logDirectory.exists()) {
            logDirectory.mkdirs()
        }
    }

    /**
     * Creates a new log file for this session.
     * File name pattern: moribito-YYYY-MM-DD-HHmmss.log
     */
    fun createNewLogFile(): File {
        val timestamp = LocalDateTime.now().format(fileNameFormatter)
        val fileName = "moribito-$timestamp.log"
        val logFile = File(logDirectory, fileName)

        // Create the file
        logFile.createNewFile()

        // Clean up old log files
        cleanupOldLogFiles()

        return logFile
    }

    /**
     * Removes old log files, keeping only the most recent maxFileCount files.
     */
    private fun cleanupOldLogFiles() {
        val logFiles = logDirectory.listFiles { file ->
            file.isFile && file.name.startsWith("moribito-") && file.name.endsWith(".log")
        } ?: return

        // Sort by last modified time (newest first)
        val sortedFiles = logFiles.sortedByDescending { it.lastModified() }

        // Delete files beyond the retention limit
        sortedFiles.drop(maxFileCount).forEach { file ->
            try {
                file.delete()
            } catch (e: Exception) {
                // Ignore deletion errors
                println("Failed to delete old log file: ${file.absolutePath}: ${e.message}")
            }
        }
    }

    /**
     * Checks if a log file has exceeded the maximum size.
     */
    fun isFileTooLarge(file: File): Boolean {
        val fileSizeMB = file.length() / (1024 * 1024)
        return fileSizeMB >= maxFileSizeMB
    }

    /**
     * Gets all log files in the directory, sorted by modification time (newest first).
     */
    fun getAllLogFiles(): List<File> {
        val logFiles = logDirectory.listFiles { file ->
            file.isFile && file.name.startsWith("moribito-") && file.name.endsWith(".log")
        } ?: return emptyList()

        return logFiles.sortedByDescending { it.lastModified() }
    }
}
