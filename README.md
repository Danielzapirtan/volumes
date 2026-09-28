# Volumes

Volumes is a native Android app for setting the daily music volume at chosen times.
Create multiple daily schedules, choose a time and a percentage, and enable or
disable each schedule independently. Schedules continue when the app is closed
and are restored after a device restart or a clock/time-zone change.

## Build and run

Open this repository in Android Studio and run the `app` configuration on an
Android device or emulator (Android 6.0/API 23 or newer). The project uses the
Android Gradle Plugin 8.7.3 and Java 17.

On Android 12 and newer, allow **Alarms & reminders** access in the app if you
want schedule changes to happen at their selected times. Without that access,
Android may deliver them later; the app shows a settings shortcut and still
registers an inexact alarm. Scheduled percentages apply to the device's media
(music) volume stream and are mapped to the device's available volume steps.
