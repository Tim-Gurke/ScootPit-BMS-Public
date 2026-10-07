# ScootPit BMS

<img src="docs/assets/app-logo.svg" width="96" alt="ScootPit BMS App-Logo">

**ScootPit** verbindet **Scooter** und **Cockpit**; **BMS** steht für Batteriemanagementsystem. Native Android-App mit anpassbarem GPS-Cockpit, JBD-Bluetooth-BMS, Scooter-Profilen, Fahrtenbuch und Samsung-Routines-Signalen. Aktuelle Version: **6.3.1**.

Entwickelt für den Joyor T6e Pro mit JBD SP14S004. Weitere Roller sind mögliche Testkandidaten, keine bestätigte Kompatibilitätsliste.

## Anleitung

- [Installation, Ersteinrichtung, Profile und Speicherorte](docs/einrichtung.md)
- [BMS auswählen, optionale Daten, Kompatibilität und Wegfahrsperre](docs/bms.md)
- [Cockpit, Hoch-/Querformat, Farben und GPS-Messwerte](docs/cockpit.md)
- [Restreichweite und Temperaturlernen](docs/reichweite.md)
- [Bereitschaft, Fahrtpausen, Energiesparmodus und Samsung-Routinen](docs/hintergrund-routinen.md)
- [Entwicklung und dauerhafte APK-Signatur](docs/entwicklung.md)

## Änderungen in 6.3.1

Der Tacho hat ein voreingestelltes Skalenmaximum von **22 km/h**; gespeicherte eigene Maxima bleiben erhalten. **Akkuoptimierung** zeigt den aktuellen Status und öffnet die Android-Einstellungen oder die App-Info auch dann, wenn ScootPit BMS bereits ausgenommen ist.

## Änderungen in 6.3

- Erstverbindung erst nach mindestens drei starken, frischen Empfangsmessungen über eine editierbare Bestätigungsdauer. Vorgabe für neue Profile: −70 dBm und 3 Sekunden; laufende Fahrten und Pausen bleiben tolerant.
- Neue Standardanordnung mit großem transparentem Tacho und Akku-/Wettersymbolen. Eigene Layouts bleiben erhalten; das neue Layout lässt sich im Editor mit **Standardlayout → Zurücksetzen → Speichern** übernehmen.
- Instrumentfarbe und Skalenfarbe separat je Kachel; zusätzliche Bild- und Freitextkacheln, einschließlich Profil- und Einstellungssicherung.
- Zurück aus Einstellungs-Untermenüs führt ins Einstellungs-Hauptmenü. Zahnrad mit Blitz; keine erzwungenen Großbuchstaben auf Schaltflächen.
- Restreichweite: Startwert standardmäßig **20 Wh/km**. Gespeicherte Werte werden nicht überschrieben.

Die bestehende App-ID bleibt erhalten. Ein Update benötigt denselben Signierschlüssel wie die installierte App; [Einrichtung und Grenzen der Signierung](docs/entwicklung.md). Vor einer erforderlichen Neuinstallation Einstellungen und Fahrten sichern.

Screenshots werden später neu ergänzt.

## Datenschutz im Repository

Dieses Repository beginnt mit einem bereinigten Stand der Version 6.2. Persönliche Bluetooth-Adressen, Fahrtdaten, alte Bildschirmaufnahmen und private Signierdateien werden nicht mitgeliefert. Commits verwenden die GitHub-No-Reply-Adresse. Lokale Fahrtdaten werden durch die App nicht an GitHub übertragen.
