Add debug logging to all of our core ldap features. They should log to a file.

Add an action button in our bottom status bar to open up a new window that tails the log file. It should display all the logs from the file. A new log file is used every time the app starts up. The log file should be searchable.

Color code the log entries based on their severity.

The logs should be searchable.

Appendix:
**Search Bar** https://github.com/JetBrains/intellij-community/blob/master/platform/jewel/ui/src/main/kotlin/org/jetbrains/jewel/ui/component/SpeedSearchArea.kt