package com.moribito.logging

/**
 * Logger interface for the Moribito logging system.
 * Provides methods for logging at different levels.
 */
interface Logger {
    /**
     * The component name for this logger.
     */
    val component: String

    /**
     * The minimum log level to record.
     */
    val minLevel: LogLevel

    /**
     * Logs a debug message.
     */
    fun debug(message: String, throwable: Throwable? = null)

    /**
     * Logs an info message.
     */
    fun info(message: String, throwable: Throwable? = null)

    /**
     * Logs a warning message.
     */
    fun warn(message: String, throwable: Throwable? = null)

    /**
     * Logs an error message.
     */
    fun error(message: String, throwable: Throwable? = null)

    /**
     * Logs a message at the specified level.
     */
    fun log(level: LogLevel, message: String, throwable: Throwable? = null)

    companion object {
        /**
         * Gets or creates a logger for the specified component.
         */
        fun get(component: String): Logger {
            return LoggerFactory.getLogger(component)
        }

        /**
         * Gets the current log file path.
         */
        fun getCurrentLogFile(): java.io.File {
            return LoggerFactory.getCurrentLogFile()
        }

        /**
         * Initializes the logging system.
         * Must be called once at application startup.
         */
        fun initialize(logDirectory: java.io.File, minLevel: LogLevel = LogLevel.DEBUG) {
            LoggerFactory.initialize(logDirectory, minLevel)
        }

        /**
         * Shuts down the logging system.
         * Should be called at application shutdown to flush remaining logs.
         */
        fun shutdown() {
            LoggerFactory.shutdown()
        }
    }
}

/**
 * Internal factory for creating loggers.
 * Platform-specific implementations will set the actual factory instance.
 */
expect object LoggerFactory {
    fun getLogger(component: String): Logger
    fun getCurrentLogFile(): java.io.File
    fun initialize(logDirectory: java.io.File, minLevel: LogLevel)
    fun shutdown()
}
