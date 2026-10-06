# ScootPit BMS

**ScootPit** verbindet **Scooter** und **Cockpit**; **BMS** steht für Batteriemanagementsystem. Native Android-App mit anpassbarem GPS-Cockpit, JBD-Bluetooth-BMS, Scooter-Profilen, Fahrtenbuch und Samsung-Routines-Signalen. Aktuelle Version: **6.2**.

Entwickelt für den Joyor T6e Pro mit JBD SP14S004. Weitere Roller sind mögliche Testkandidaten, keine bestätigte Kompatibilitätsliste.

## Anleitung

- [Installation, Ersteinrichtung, Profile und Speicherorte](docs/einrichtung.md)
- [BMS auswählen, optionale Daten, Kompatibilität und Wegfahrsperre](docs/bms.md)
- [Cockpit, Hoch-/Querformat, Farben und GPS-Messwerte](docs/cockpit.md)
- [Restreichweite und Temperaturlernen](docs/reichweite.md)
- [Bereitschaft, Fahrtpausen, Energiesparmodus und Samsung-Routinen](docs/hintergrund-routinen.md)
- [Entwicklung und dauerhafte APK-Signatur](docs/entwicklung.md)

## Änderungen in 6.2

BMS-Verbindung bleibt in einer Fahrtpause erhalten; Wiederverbindung, Empfangsschwellen und Suchzeiten sind pro Scooter einstellbar. GPS-Ausreißer werden vor der Anzeige und Aufzeichnung geprüft. Separate Hoch-/Querformatlayouts, Transparenzregler für Kachelfarben und sichtbare Dialogaktionen. Der Appkopf heißt fest **ScootPit BMS**; seine Farbe bleibt anpassbar.

Die bestehende App-ID bleibt erhalten. Ein Update benötigt denselben Signierschlüssel wie die installierte App; [Einrichtung und Grenzen der Signierung](docs/entwicklung.md). Vor einer erforderlichen Neuinstallation Einstellungen und Fahrten sichern.

Screenshots werden später neu ergänzt.

## Datenschutz im Repository

Dieses Repository beginnt mit einem bereinigten Stand der Version 6.2. Persönliche Bluetooth-Adressen, Fahrtdaten, alte Bildschirmaufnahmen und private Signierdateien werden nicht mitgeliefert. Commits verwenden die GitHub-No-Reply-Adresse. Lokale Fahrtdaten werden durch die App nicht an GitHub übertragen.
