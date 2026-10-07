# ScootPit BMS

<img src="docs/assets/app-logo.svg" width="96" alt="ScootPit BMS App-Logo">

**Automatisches Fahrtenbuch für deinen E-Scooter – auch mit dem Handy in der Hosentasche.**

Das Highlight von ScootPit BMS ist die **automatische Fahrt-Erkennung**: Du startest einmal die Bereitschaft, und die App erkennt über ein kompatibles Bluetooth-BMS, wann dein Roller Strom entnimmt. Sie startet die Aufzeichnung, berücksichtigt Fahrtpausen und schließt die Fahrt beim bestätigten Entfernen vom Roller ab. Du musst nicht vor jeder Fahrt an einen Start- oder Stoppknopf denken.

**Die Aufzeichnung läuft auch mit ausgeschaltetem Bildschirm und gesperrtem Handy in der Hosentasche.** Bluetooth und Standort bleiben aktiv; Berechtigungen und Hintergrundbetrieb müssen eingerichtet sein. So entsteht dein Fahrtenbuch im Alltag, während du dich auf die Fahrt konzentrierst.

Wenn du das Handy am Lenker nutzt, wird ScootPit zusätzlich zum **persönlichen Scooter-Cockpit**: Geschwindigkeit, Strecke, Akkustand, Leistung, Verbrauch und geschätzte Restreichweite auf einen Blick.

## Was kann ScootPit BMS?

- **Fahrten automatisch aufzeichnen:** Fahrtstart erkennen, Pausen berücksichtigen und die Fahrt beim bestätigten Entfernen vom Roller abschließen. Strecke, Fahr- und Standzeit, Geschwindigkeit und Höhenwerte werden mitgeschrieben.
- **Scooter- und Akkuwerte live sehen:** GPS-Tacho, Kilometer, Akkustand, Spannung, Strom, Leistung, Energieverbrauch und Temperaturen in einer Ansicht. Weitere BMS-Daten, etwa Zellspannungen, sind je nach Gerät verfügbar.
- **Restreichweite besser einschätzen:** Die Schätzung berücksichtigt Akkureserve und Verbrauch. Während der Fahrt fließt der aktuelle Verbrauch ein; passende frühere Fahrten helfen bei der Startschätzung.
- **Das Cockpit selbst gestalten:** Kacheln verschieben und vergrößern, Zahlen, Balken oder Rundinstrumente wählen sowie Farben, Transparenz und Schriftgrößen anpassen. Separate Hoch- und Querformatlayouts, ein eigenes Rollerfoto und Freitext machen daraus dein persönliches Cockpit.
- **Fahrten behalten und weiterverwenden:** Das Fahrtenbuch wird lokal gespeichert. GPX-, CSV- und JSON-Dateien lassen sich in einen gewählten Ordner exportieren; GPX und CSV kannst du auch teilen.
- **Mehrere Scooter und Samsung-Routinen nutzen:** Eigene Profile trennen Roller, Einstellungen und Fahrtdaten. Auf Samsung-Handys können eingerichtete Routinen die Fahrtstart- und Fahrtende-Nachrichten nutzen, um den Energiesparmodus aus- und wieder einzuschalten.

## Was brauchst du dafür?

Ein Android-Handy ab **Android 8** und ein **kompatibles, auslesbares JBD/Jiabaida-Bluetooth-BMS** im Roller. ScootPit wurde für den **Joyor T6e Pro mit JBD SP14S004** entwickelt. Andere Roller und BMS-Versionen müssen auf Kompatibilität geprüft werden; Bluetooth allein genügt dafür nicht. Ein eigenständiger Aufzeichnungsmodus ohne kompatibles BMS ist derzeit nicht enthalten.

Für den Betrieb mit gesperrtem Handy müssen Bluetooth, Standort und die nötigen Berechtigungen aktiv sein. Die Bereitschaft wird bei geöffneter App gestartet. Auf Samsung-Handys sollte ScootPit von Akkuoptimierung und App-Standby ausgenommen sein; die Benachrichtigungen für Routine-Signale bleiben aktiviert. Die Kombination aus echtem BMS, Handy und Energiesparmodus bitte zunächst auf einer kurzen Fahrt prüfen. [Mehr zum Hintergrundbetrieb und zu Samsung-Routinen](docs/hintergrund-routinen.md).

## Loslegen

1. Die APK aus der [aktuellen Veröffentlichung](https://github.com/Tim-Gurke/ScootPit-BMS-Public/releases/latest) installieren und die Berechtigungen erlauben.
2. Das eigene BMS auswählen und die Akkuwerte prüfen.
3. **Bereitschaft starten** – danach das Cockpit nutzen oder das Handy sperren und einstecken.

Aktuelle Version: **6.3.1**. **ScootPit** verbindet **Scooter** und **Cockpit**; **BMS** steht für Batteriemanagementsystem.

## Anleitung

- [Installation, Ersteinrichtung, Profile und Speicherorte](docs/einrichtung.md)
- [BMS auswählen, optionale Daten, Kompatibilität und Wegfahrsperre](docs/bms.md)
- [Cockpit, Hoch-/Querformat, Farben und GPS-Messwerte](docs/cockpit.md)
- [Restreichweite und Temperaturlernen](docs/reichweite.md)
- [Bereitschaft, Fahrtpausen, Energiesparmodus und Samsung-Routinen](docs/hintergrund-routinen.md)
- [Entwicklung und dauerhafte APK-Signatur](docs/entwicklung.md)

## Änderungen in 6.3.1

Appkopf und Cockpit halten Abstand zu Statusleiste, Navigationsleiste und Kameraausschnitten, auch unter Android 15 und im Querformat.

Der Tacho hat ein voreingestelltes Skalenmaximum von **22 km/h**; gespeicherte eigene Maxima bleiben erhalten. **Akkuoptimierung** zeigt den aktuellen Status und öffnet die Android-Einstellungen oder die App-Info auch dann, wenn ScootPit BMS bereits ausgenommen ist.

Die Schriftgröße der Einheit (z. B. **km/h**) lässt sich pro Messwertkachel getrennt vom Zahlenwert einstellen. Die Einheit kann neben dem Wert, hochgestellt oder über dem Wert innerhalb des Rundinstruments stehen. Größe und Position werden mit dem Layout gesichert.

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
