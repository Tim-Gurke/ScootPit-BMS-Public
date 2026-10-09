# Fahrtenbuch, Karte und Statistiken

[← Übersicht](../README.md)

## Fahrt auswählen

Im Cockpit **Fahrtenbuch** oder unter Einstellungen **Fahrtenbuch / Karte / Statistiken** öffnen. Zwischen **7 Tage**, **30 Tage** und **Alle** wählen. Die Liste enthält abgeschlossene, lokal gespeicherte Fahrten des aktiven Scooter-Profils mit Datum, Startzeit, Strecke und Dauer. Auf eine Fahrt tippen und **Statistiken**, **Karte** oder **GPX und CSV teilen** wählen.

Es gibt keine automatische Löschung nach 30 Tagen; die 7- und 30-Tage-Ansichten begrenzen nur den Zeitraum. **Alle** zeigt den kompletten Bestand des aktiven Scooter-Profils. Bereits vorhandene lokale Aufzeichnungen werden anhand ihrer JSON-Zusammenfassung und CSV-Datei gelesen.
## Fahrten importieren

Für eine einzelne Route **Einzelne Fahrt aus CSV oder GPX importieren** wählen. Unterstützt werden ScootPit-CSV-Dateien sowie CSV-Dateien mit Zeitstempel und GPS-Koordinaten (Breite/Länge oder latitude/longitude) und GPX-Tracks mit Zeitangaben. Die Fahrt wird lokal dem aktiven Scooter-Profil hinzugefügt und erscheint in der Liste, sobald sie im gewählten Zeitraum liegt. Der Import wird auf 32 MB und 200.000 GPS-Punkte begrenzt; dieselbe Datei kann nicht doppelt importiert werden. Eine Fahrt benötigt mindestens zwei gültige GPS-Punkte.

Für den vollständigen Bestand **Komplettes Fahrtenbuch aus Ordner importieren** wählen und danach den Scooter-Fahrtenordner auswählen. ScootPit liest Metadaten (`.json`), CSV-Aufzeichnungen und vorhandene GPX-Dateien; Unterordner werden mit durchsucht. Der Ordner kann direkt aus der von ScootPit verwendeten Fahrtenbuch-Sicherung stammen. Die importierten Fahrten werden dem gerade aktiven Scooter-Profil zugeordnet. Bestehende Fahrten werden nicht ersetzt; bereits vorhandene Fahrten werden anhand von Startzeit, Endzeit und CSV-Inhalt erkannt und übersprungen. Unterstützt werden bis zu 5.000 Dateien, höchstens 32 MB je Datei und 512 MB insgesamt. Danach **Alle** öffnen, um auch ältere importierte Fahrten anzuzeigen.

![Fahrtenbuch mit Import des kompletten Ordners und einzelner CSV-/GPX-Fahrten](assets/screenshots/fahrtenbuch-ordnerimport.jpg)

<em>Die Ordnerauswahl übernimmt den vollständigen Fahrtenbestand; über <strong>Alle</strong> werden auch ältere Fahrten sichtbar.</em>

## Karte

Start ist grün mit S, Ziel rot mit Z. Verschieben mit dem Finger, zoomen per Geste oder +/−, mit **Route** wieder die gesamte Strecke einpassen. Die Route wird auf dem Handy gezeichnet. Nur sichtbare OpenStreetMap-Hintergrundkacheln werden aus dem Internet geladen und zwischengespeichert. Bei fehlendem Internet bleibt die Route auf einem einfachen Hintergrund sichtbar; bereits vorhandene Kartenkacheln können aus dem Cache erscheinen. Es gibt keinen Download ganzer Offline-Gebiete.

![Fahrtkarte mit Route, grünem Startpunkt und rotem Zielpunkt](assets/screenshots/fahrtenbuch-route.jpg)

<em>Die Karte markiert den Start grün und das Ziel rot. Die Fahrtdatei bleibt auf dem Handy; online werden nur sichtbare Kartenkacheln angefragt.</em>

OpenStreetMap erhält IP-Adresse und angefragte Kartenausschnitte, keine Fahrtdatei. [OpenStreetMap-Lizenz und Mitwirkende](https://www.openstreetmap.org/copyright), [Kachelnutzung](https://operations.osmfoundation.org/policies/tiles/).
## Statistiken auswählen

**Angezeigte Werte auswählen** öffnet Häkchen für Strecke, Fahr-/Standzeit, Geschwindigkeit, Energie/Verbrauch, elektrische Leistung, Höhenmeter, Temperaturen sowie die drei Diagramme. Auswahl wird pro Scooter gespeichert. Fehlende Daten werden als **Nicht aufgezeichnet** angezeigt.

Durchschnittsgeschwindigkeit bezieht sich auf die Fahrzeit in Bewegung. Verbrauch = Wh je gefahrenem Kilometer. Leistung ist elektrisch am BMS gemessen. Temperaturen sind gespeicherte Fahrtmittelwerte; Höhe und Anstieg bleiben GPS-Schätzungen. Bei alten Aufzeichnungen ohne gespeicherte Zeitaufteilung kann sie für die Einzelansicht aus vorhandenen CSV-Punkten angenähert werden.

Die Diagramme zeigen Geschwindigkeit, elektrische Leistung und GPS-Höhe über die Zeit seit Fahrtbeginn. Eine fehlende Messung wird nicht als erfundener Nullwert gezeichnet. Lange Aufzeichnungen werden für die Darstellung ausgedünnt.

![Fahrtstatistik mit Strecke, Fahr- und Standzeit, Verbrauch, Leistung und Temperaturen](assets/screenshots/fahrtenbuch-statistik.jpg)

<em>Die Einzelansicht fasst die aufgezeichneten Kennwerte der Fahrt zusammen.</em>
## Zeitraum auswerten

**Zeitraum auswerten / Fahrten vergleichen** zeigt Anzahl, Summen und passende ausgewählte Kennwerte. Kilometer pro Tag werden mit Balken visualisiert. Einzelne Fahrten sind darunter mit Strecke, Dauer und – je nach Auswahl – Verbrauch und Höchstgeschwindigkeit vergleichbar. Anklicken öffnet wieder die Fahrtansicht. Statistiken funktionieren ohne Internet.

