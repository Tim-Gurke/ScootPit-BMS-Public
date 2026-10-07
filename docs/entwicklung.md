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

Base64 ist lediglich ein Transportformat. Der Workflow schreibt den Keystore nur vorübergehend auf den Runner, baut die signierte Release-APK und entfernt die Datei wieder. Sind alle vier Secrets leer, liefert er eine Test-APK; eine unvollständige Konfiguration führt zu einem Fehler. Das Artefakt enthält den Signiermodus in `signing-mode.txt`. Für Updates denselben Release-Schlüssel behalten. Beim ersten Wechsel von einer bisherigen Debug-APK auf den Release-Schlüssel kann nach Datensicherung eine Neuinstallation erforderlich sein.

## Offizielle Version

Die öffentliche Version heißt **1.1.0**. Android verwendet intern **versionCode 14**, damit das Update über bereits installierte Entwicklungsstände funktioniert. Die Versionsbezeichnung kann öffentlich neu beginnen; der interne Code muss bei jedem Update weiter steigen. App-ID und Signierschlüssel bleiben erhalten.

Build: Java 17, Gradle 8.9, Android SDK 35. Die Java-Tests decken Protokoll, Steuerung, Reichweite, Raster, Empfang und bestätigten Stillstand ab; Android Lint und Emulatorprüfungen unter Android 13/15 ergänzen die Prüfung. Reales BLE/GPS und Samsung-Routinen müssen am eigenen Roller geprüft werden.
