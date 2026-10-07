# ScootPit BMS

<img src="docs/assets/app-logo.svg" width="96" alt="ScootPit BMS App-Logo">

**Automatisches Fahrtenbuch für deinen E-Scooter – auch mit dem Handy in der Hosentasche.**

**Version 1.1.1 · [APK herunterladen](https://github.com/Tim-Gurke/ScootPit-BMS-Public/releases/latest)**

Das Highlight ist die Automatik: **Bereitschaft einschalten, zum Scooter gehen und losfahren.** ScootPit erkennt ein gespeichertes Bluetooth-BMS in Reichweite, wählt das zugehörige Scooter-Profil und verbindet sich bei ausreichend stabilem Empfang automatisch. Die Fahrtaufzeichnung startet, sobald die eingestellten Bedingungen für die Stromentnahme erfüllt sind. Die Annäherung allein startet noch keine Fahrt.

Die Aufzeichnung läuft auch mit ausgeschaltetem Bildschirm und gesperrtem Handy. Beim Ausrollen wird die GPS-Geschwindigkeit weiter ausgewertet. Nach **45 Sekunden zuverlässig erkanntem Stillstand** wird die Fahrt abgeschlossen – auch wenn das BMS noch verbunden ist. Die Bereitschaft bleibt aktiv und die Suche beginnt wieder zügig. Auch bestätigtes Entfernen vom Scooter beendet die Fahrt. Die Stillstandszeit ist einstellbar: Für längere Ampelphasen kannst du beispielsweise 90 Sekunden wählen.

Mehrere gespeicherte Scooter? Die App prüft erreichbare BMS nacheinander und übernimmt das passende Profil. Während einer laufenden Fahrt bleibt die Zuordnung fest. Unbekannte BMS werden nicht ungefragt übernommen; jedes Gerät wird bei der Einrichtung einmal seinem Profil zugeordnet.

Am Lenker wird ScootPit zum **persönlichen Cockpit**: Geschwindigkeit, Strecke, Akku, elektrische Leistung, Verbrauch und geschätzte Restreichweite auf einen Blick.

## Was bringt dir ScootPit?

- **Automatisches Tracking:** Fahrtstart und Fahrtende erkennen, Fahr- und Standzeit erfassen, auch mit gesperrtem Handy.
- **Live-Werte im Blick:** GPS-Tacho, Akkustand, Spannung, Strom, Leistung, Energie, Temperaturen und verfügbare Zusatzdaten des BMS.
- **Drei Kilometerzähler:** Gesamtkilometer, Tageskilometer mit täglichem Neustart und ein manuell zurücksetzbarer Tourenzähler – getrennt je Scooter.
- **Fahrtenbuch mit Karte:** Fahrten der letzten 7 oder 30 Tage auswählen, Route mit Start und Ziel anzeigen, zoomen, verschieben und GPX/CSV teilen.
- **Auswählbare Statistiken:** Per Häkchen Werte und Diagramme wählen. Geschwindigkeit, Leistung und GPS-Höhe im Zeitverlauf; Zeitraumübersicht, Tagesstrecken und Vergleich einzelner Fahrten.
- **Eigenes Cockpit:** Kacheln verschieben und vergrößern, Zahl/Balken/Rundinstrument wählen, Instrument- und Skalenfarben separat einstellen, Transparenz, Schrift und Einheitenposition anpassen. Eigene Bild- und Freitextkacheln sowie getrennte Hoch-/Querformatlayouts.
- **Restreichweite einschätzen:** Akkureserve und Verbrauch berücksichtigen; aktuelle Fahrtdaten und passende Verbrauchshistorie verbessern die Schätzung. Startwert: 20 Wh/km.
- **Samsung-Routinen nutzen:** Editierbare Fahrtstart-/Fahrtende-Nachrichten können eingerichtete Routinen zum Umschalten des Energiesparmodus auslösen. Die App richtet die Routinen nicht selbst ein.
- **Daten behalten:** Fahrten und Einstellungen lokal speichern, zusätzliche Sicherungsordner auswählen. Keine Anmeldung erforderlich.

## Neues Cockpit in 1.1.1

Die Anordnung folgt dem gewünschten Cockpit: großer transparenter Tacho links, Akku, Restreichweite und Leistung rechts; darunter drei Spalten mit Fahrwerten, Tages-/Tourenzähler, Scooterfoto und Temperaturen. Der Tacho hat feine Teilstriche, Zahlen und eine Fortschrittsmarkierung. Die Skala richtet sich nach deinem einstellbaren Maximum (Standard 22 km/h); die Einheit steht standardmäßig im Instrument unter dem Wert.

Der Appkopf integriert Profil, Fahrtenbuch und Zahnrad mit Blitz. Ein grüner Punkt zeigt die aktive Bereitschaft. Die grünen und roten Schaltflächen heißen **Bereit** und tragen schwarze Symbole. Kachelsymbole erhalten passende, einzeln anpassbare Farben; Symbol, Beschriftung, beides oder keines bleiben wählbar. Die anpassbaren Farbschemata **Schwarz/Orange** und **Weiß/Blau** sowie das feste App-Logo bleiben erhalten.

Beim ersten Öffnen je vorhandenem Profil wird das Hochformat neu angeordnet. Fotos, zusätzliche Kacheln, Messwerte und persönliche Kacheleinstellungen werden übernommen; die vorherige Anordnung wird als Sicherung in den Einstellungen gespeichert. Ein separat gespeichertes Querformat bleibt erhalten. [Gestaltung anpassen](docs/design.md).

## Was brauchst du?

Ein Handy ab **Android 8** und ein **kompatibles, auslesbares JBD/Jiabaida-Bluetooth-BMS** im Roller. Entwickelt für den **Joyor T6e Pro mit JBD SP14S004**. Andere BMS müssen auf Kompatibilität geprüft werden; Bluetooth allein genügt nicht. Ein Aufzeichnungsmodus ohne kompatibles BMS ist nicht enthalten.

Bluetooth, präziser Standort und die nötigen Berechtigungen müssen aktiv sein. Starte die Bereitschaft bei geöffneter App. Für zuverlässigen Hintergrundbetrieb ScootPit von Akkuoptimierung und App-Standby ausnehmen. Prüfe die Kombination aus Handy, BMS und Samsung-Energiesparmodus auf einer kurzen Fahrt. [Hintergrundbetrieb einrichten](docs/hintergrund-routinen.md).

## Loslegen

1. Die signierte APK aus der [offiziellen Veröffentlichung](https://github.com/Tim-Gurke/ScootPit-BMS-Public/releases/latest) installieren und Berechtigungen erlauben.
2. Je Scooter ein Profil anlegen, das eigene BMS auswählen und die Werte prüfen.
3. **Bereitschaft starten** – danach Cockpit nutzen oder Handy sperren und einstecken.
4. Nach der Fahrt das **Fahrtenbuch-Symbol** im Kopf antippen.

Vorhandene Profile, Kilometer, Layouts und Fahrten bleiben bei einem kompatiblen Update erhalten. Die App-ID und der dauerhafte Signierschlüssel bleiben gleich. Vor einer gegebenenfalls erforderlichen Neuinstallation Daten sichern.

## Anleitung

- [Installation, Ersteinrichtung, Profile und Speicherorte](docs/einrichtung.md)
- [BMS, optionale Daten, Kompatibilität und Lastausgang](docs/bms.md)
- [Cockpit, Kilometerzähler und Messwerte](docs/cockpit.md)
- [Fahrtenbuch, Karten, Statistiken und Diagramme](docs/fahrtenbuch.md)
- [Restreichweite und Temperaturlernen](docs/reichweite.md)
- [Automatik, Fahrtende und Samsung-Routinen](docs/hintergrund-routinen.md)
- [Entwicklung und APK-Signierung](docs/entwicklung.md)

## Datenschutz

Fahrtdateien, Profile und persönliche Bilder bleiben lokal und werden nicht an GitHub übertragen. Wetterdaten sind optional: Dafür werden gerundete Standortkoordinaten höchstens alle 15 Minuten an Open-Meteo gesendet. Beim Öffnen einer Karte werden die sichtbaren Hintergrundkacheln von OpenStreetMap geladen; der Anbieter erhält deine IP-Adresse und die angefragten Kartenausschnitte. Die Route selbst wird lokal gezeichnet, die Fahrtdatei wird nicht hochgeladen. Statistiken benötigen kein Internet.

Der Quellcode enthält keine persönlichen Geräteadressen, Fahrten oder Signierschlüssel. **ScootPit** verbindet **Scooter** und **Cockpit**; **BMS** steht für Batteriemanagementsystem.
