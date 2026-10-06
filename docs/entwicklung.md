# Entwicklung und dauerhafte Signierung

[← Übersicht](../README.md)

## Dauerhafte APK-Signatur

Für dieses Projekt ist ab Version 6.2 ein dauerhafter Release-Schlüssel eingerichtet. Die private Schlüsselsicherung separat und sicher aufbewahren. Für alle kommenden Updates denselben Schlüssel verwenden; ein neuer Schlüssel würde die Installation als Update verhindern. Niemals Keystore oder Passwörter ins Repository oder als öffentliches Build-Artefakt legen. Unter **Settings → Secrets and variables → Actions** sind vier Repository-Secrets hinterlegt:

| Secret | Inhalt |
| --- | --- |
| `APP_KEYSTORE_BASE64` | Base64-Inhalt des eigenen Keystores |
| `APP_KEYSTORE_PASSWORD` | Keystore-Passwort |
| `APP_KEY_ALIAS` | Alias des Release-Schlüssels |
| `APP_KEY_PASSWORD` | Passwort dieses Schlüssels |

Base64 ist lediglich ein Transportformat. Der Workflow schreibt den Keystore nur vorübergehend auf den Runner, baut die signierte Release-APK und entfernt die Datei wieder. Sind alle vier Secrets leer, liefert er eine Test-APK; eine unvollständige Konfiguration führt zu einem Fehler. Das Artefakt enthält den Signiermodus in `signing-mode.txt`. Für Updates denselben Release-Schlüssel behalten. Beim ersten Wechsel von einer bisherigen Debug-APK auf den Release-Schlüssel kann nach Datensicherung eine Neuinstallation erforderlich sein.
