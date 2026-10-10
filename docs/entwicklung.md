# Entwicklung und dauerhafte Signierung

[← Übersicht](../README.md)

## Dauerhafte APK-Signatur

Für dieses Projekt ist ein dauerhafter Release-Schlüssel eingerichtet. Die private Schlüsselsicherung separat und sicher aufbewahren. Für alle kommenden Updates denselben Schlüssel verwenden; ein neuer Schlüssel würde die Installation als Update verhindern. Niemals Keystore oder Passwörter ins Repository oder als öffentliches Build-Artefakt legen. Unter **Settings → Secrets and variables → Actions** sind vier Repository-Secrets hinterlegt:

| Secret | Inhalt |
| --- | --- |
| `APP_KEYSTORE_BASE64` | Base64-Inhalt des eigenen Keystores |
| `APP_KEYSTORE_PASSWORD` | Keystore-Passwort |
| `APP_KEY_ALIAS` | Alias des Release-Schlüssels |
| `APP_KEY_PASSWORD` | Passwort dieses Schlüssels |

Base64 ist lediglich ein Transportformat. Der Workflow schreibt den Keystore nur vorübergehend auf den Runner, baut die signierte Release-APK und entfernt die Datei wieder. Für offizielle Veröffentlichungen müssen alle vier Secrets eingerichtet sein; fehlende oder unvollständige Signierung führt zu einem Fehler. Das Artefakt enthält den Signiermodus in `signing-mode.txt`. Release-APKs werden vor Veröffentlichung mit der bekannten Zertifikat-SHA-256 von 1.1.1 verglichen: `c58abf26de59344a21efbc0576e391a36b6179724a0b4fad27a35de03de184fb`. Eine anders signierte Debug-APK ist kein kompatibles Update.

## Offizielle Version

Die öffentliche Version heißt **1.5.5**. Android verwendet intern **versionCode 28**, damit das Update über bereits installierte Entwicklungsstände funktioniert. Die Versionsbezeichnung kann öffentlich neu beginnen; der interne Code muss bei jedem Update weiter steigen. App-ID und Signierschlüssel bleiben erhalten.

Build: Java 17, Gradle 8.9, Android SDK 35. Die Java-Tests decken Protokoll, Steuerung, Reichweite, Raster, Empfang, bestätigten Stillstand, Skalenfarbverläufe, umgekehrte Skalenrichtung, Verbrauchsfenster bis 1000 m, Statusposition im Querformatkopf, übernommene Layouts und scrollbare Untermenüs ab; Android Lint und Emulatorprüfungen unter Android 13/15 ergänzen die Prüfung. Reales BLE/GPS und Samsung-Routinen müssen am eigenen Roller geprüft werden.

