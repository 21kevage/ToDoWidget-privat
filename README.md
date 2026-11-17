# Meine Aufgaben

Eine Android-App mit Startbildschirm-Widget für offene Aufgaben. Im Widget kannst du über **+** eine Aufgabe erfassen und über das Kästchen abhaken. Falls mehr Aufgaben offen sind, als hineinpassen, zeigt das Widget die Anzahl der weiteren Aufgaben an. Ein Tipp auf die Überschrift öffnet die vollständige Liste. Dort kannst du Aufgaben bearbeiten, löschen und wieder öffnen.

Das Widget hat einen leicht transparenten Hintergrund. Der Android-Launcher lässt dich die Größe ändern. Die App speichert die Aufgaben lokal auf dem Gerät.

## Installation

1. Die APK auf einem Gerät mit Android 8.0 oder neuer öffnen und installieren.
2. Eine freie Stelle auf dem Startbildschirm länger antippen und **Widgets** öffnen.
3. **Meine Aufgaben** auswählen und das Widget platzieren.

## Build

In Android Studio den Projektordner öffnen oder mit JDK 17 oder neuer und installiertem Android SDK 35 aus dem Projektordner bauen.

```sh
./gradlew :app:assembleDebug :app:lintDebug
```

Die APK liegt anschließend unter `app/build/outputs/apk/debug/app-debug.apk`.
