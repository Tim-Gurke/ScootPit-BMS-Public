# ScootPit BMS

<img src="docs/assets/app-logo.svg" width="96" alt="ScootPit BMS App-Logo">

**Automatisches Fahrtenbuch für deinen E-Scooter – auch mit dem Handy in der Hosentasche.**

**ScootPit – für alle, die ihren Roller überwachen wollen, ohne selbst überwacht zu werden.**

**Version 1.3.2 · [APK herunterladen](https://github.com/Tim-Gurke/ScootPit-BMS-Public/releases/latest)**

Das Highlight ist die Automatik: **Bereitschaft einschalten, zum Scooter gehen und losfahren.** ScootPit erkennt ein gespeichertes Bluetooth-BMS in Reichweite, wählt das zugehörige Scooter-Profil und verbindet sich bei ausreichend stabilem Empfang automatisch. Die Fahrtaufzeichnung startet, sobald die eingestellten Bedingungen für die Stromentnahme erfüllt sind. Die Annäherung allein startet noch keine Fahrt.

Die Aufzeichnung läuft auch mit ausgeschaltetem Bildschirm und gesperrtem Handy. Beim Ausrollen wird GPS weiter ausgewertet. Nach **45 Sekunden zuverlässig erkanntem Stillstand** pausiert die Fahrt; die erreichbare BMS-Verbindung bleibt bestehen. Fährst du weiter, wird dieselbe Fahrt fortgesetzt. Nach **10 Minuten Pause** wird sie rückwirkend zum Pausenbeginn abgeschlossen. Geht die Verbindung während der Pause verloren, sucht die App weiter und wartet standardmäßig bis 2 Minuten nach Pausenbeginn auf eine Wiederverbindung. Die Zeiten sind je Scooter einstellbar. GPS-Ausfall allein gilt nicht als Stillstand.

Mehrere gespeicherte Scooter? Die App prüft erreichbare BMS nacheinander und übernimmt das passende Profil. Während einer laufenden Fahrt bleibt die Zuordnung fest. Unbekannte BMS werden nicht ungefragt übernommen; jedes Gerät wird bei der Einrichtung einmal seinem Profil zugeordnet.

Am Lenker wird ScootPit zum **persönlichen Energie-Cockpit**: Verbrauch der letzten 500 Meter und aktuelle elektrische Leistung stehen im Mittelpunkt. Akku, Restreichweite, Kilometer und Temperaturen ergänzen die Übersicht; GPS-Geschwindigkeit bleibt als kleinere Anzeige verfügbar.

![Aktives ScootPit-Cockpit mit Akku, Restreichweite, Temperaturen und BMS-Status](docs/assets/screenshots/cockpit-livewerte.jpg)

*Das Cockpit zeigt verbundene BMS-Werte, Bereitschaft und Statusmeldungen auf einen Blick.*

## Was bringt dir ScootPit?

- **Automatisches Tracking:** Fahrtstart und Fahrtende erkennen, Fahr- und Standzeit erfassen, auch mit gesperrtem Handy.
- **Live-Werte im Blick:** GPS-Tacho, Akkustand, Spannung, Strom, Leistung, Energie, Temperaturen und verfügbare Zusatzdaten des BMS.
- **Drei Kilometerzähler:** Gesamtkilometer, Tageskilometer mit täglichem Neustart und ein manuell zurücksetzbarer Tourenzähler – getrennt je Scooter.
- **Fahrtenbuch mit Karte:** Fahrten der letzten 7 oder 30 Tage auswählen, Route mit Start und Ziel anzeigen, zoomen, verschieben und GPX/CSV teilen.
- **Auswählbare Statistiken:** Per Häkchen Werte und Diagramme wählen. Geschwindigkeit, Leistung und GPS-Höhe im Zeitverlauf; Zeitraumübersicht, Tagesstrecken und Vergleich einzelner Fahrten.
- **Eigenes Cockpit:** Feines Raster mit 24 Spalten, überlappende Kacheln mit wählbarer Ebene, transparente Hintergründe, einzeln einstellbare Rahmen und Ecken. Zahl/Balken/Rundinstrument wählen, sichtbare Farben selbst mischen, Schrift und Einheitenposition anpassen. Eigene Bild- und Freitextkacheln sowie getrennte Hoch-/Querformatlayouts.
- **Restreichweite einschätzen:** Akkureserve und Verbrauch berücksichtigen; aktuelle Fahrtdaten und passende Verbrauchshistorie verbessern die Schätzung. Startwert: 20 Wh/km.
- **Samsung-Routinen nutzen:** Editierbare Fahrtstart-/Fahrtende-Nachrichten können eingerichtete Routinen zum Umschalten des Energiesparmodus auslösen. Die App richtet die Routinen nicht selbst ein.
- **Daten behalten:** Fahrten und Einstellungen lokal speichern, zusätzliche Sicherungsordner auswählen. Keine Anmeldung erforderlich.

## Neu in 1.3.2

Die Rundinstrumente zeigen die Skalenwerte in runden Schritten. Der Messwert lässt sich je Instrument ausblenden oder mittig, höher bzw. tiefer platzieren. Bewusst überlappende Kacheln behalten beim Bearbeiten anderer Kacheln ihre Position. Die erste Wertezeile zeigt zwei flache, echte **Halbkreisinstrumente**: Verbrauch der letzten 500 m in Wh/km und aktuelle Leistung in W. Der Kreisausschnitt lässt sich je Instrument von **90° bis 270°** ändern, ebenso Skalenmaximum und Farben. Die 500-m-Anzeige erscheint, sobald genügend zusammenhängende Messdaten vorliegen.

Die Gestaltung wird leichter: **Dunkel/Orange** und **Hell/Blau**, frei wählbarer Bildschirm-Farbverlauf und ein Farbmischer mit Vorschau. Kachelrahmen, Rahmenfarbe, Eckenradius, Hintergrund und Innenabstand sind separat anpassbar. Überlappungen sind optional; du bestimmst, welche Kachel oben liegt. Das Raster ist in beiden Richtungen doppelt so fein. **Gesamtzeit** ergänzt Fahr- und Standzeit; Temperaturkacheln lassen sich antippen, um eine Aktualisierung anzufragen.

Der Appkopf mit Profil, Fahrtenbuch, Zahnrad mit Blitz und grünem Bereitschaftspunkt bleibt erhalten. Auch das feste App-Logo und individuell gefärbte Kachelsymbole bleiben verfügbar. Beim ersten Öffnen je vorhandenem Profil wird das Hochformat auf die neue Anordnung umgestellt; Fotos und persönliche Inhalte werden übernommen, die vorherige Anordnung wird gesichert. Das Querformat wird auf das feinere Raster umgerechnet. [Gestaltung anpassen](docs/design.md).

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

Die App speichert Fahrtdateien, Profile und persönliche Bilder lokal und überträgt sie nicht an GitHub. Wetterdaten sind optional: Dafür werden gerundete Standortkoordinaten bei automatischen Aktualisierungen höchstens alle 15 Minuten an Open-Meteo gesendet; Antippen einer Temperaturkachel kann zusätzlich eine manuelle Abfrage auslösen. Beim Öffnen einer Karte werden die sichtbaren Hintergrundkacheln von OpenStreetMap geladen; der Anbieter erhält deine IP-Adresse und die angefragten Kartenausschnitte. Die Route selbst wird lokal gezeichnet, die Fahrtdatei wird nicht hochgeladen. Statistiken benötigen kein Internet.

Der Quellcode enthält keine persönlichen Geräteadressen, Fahrtdateien oder Signierschlüssel. **ScootPit** verbindet **Scooter** und **Cockpit**; **BMS** steht für Batteriemanagementsystem.

## Lizenz

Quellcode, Dokumentation und Originalgrafiken von ScootPit BMS stehen unter der [Apache-Lizenz 2.0](LICENSE). Marken und Inhalte Dritter bleiben Eigentum ihrer jeweiligen Rechteinhaber.
