# Cockpit, Layout und Messwerte

In ScootPit BMS bleiben Appkopf und Bedienelemente außerhalb von Statusleiste, Navigationsleiste und Kameraausschnitten. Das gilt auch unter Android 15 und bei gedrehtem Bildschirm.

[← Übersicht](../README.md)

## Cockpit und visueller Editor

Zahnrad mit Blitz → **Cockpit bearbeiten**. Kachel lange drücken und ziehen; am Griff unten rechts Größe ändern. Antippen öffnet Beschriftung, Schriftgröße, Farben, Darstellung und genaue Rasterwerte. Aktionen sind während der Bearbeitung gesperrt. **Speichern** übernimmt den Entwurf, **Zurück** verwirft ihn. Entwürfe bleiben beim Drehen des Handys erhalten.

Das Raster hat **24 Spalten**: Im Hochformat sind die Höheneinheiten 20 dp, im Querformat ebenfalls 20 dp. Breite 24 = ganze Zeile, Breite 12 = zwei gleiche Spalten, Breite 8 = drei. Hoch- und Querformat werden pro Scooter getrennt gespeichert. Ohne eigenes Querformatlayout wird das Hochformatlayout übernommen. Hintergrund und Textfarbe jeder Kachel besitzen einen Transparenzregler (0 % = deckend, 100 % = unsichtbar). Rahmen, Rahmenfarbe, Eckenradius und Innenabstand sind separat einstellbar; 0 dp Innenabstand erlaubt Text nahe am Rand. Die Aktionen Übernehmen, Zurück und Entfernen stehen fest in einer horizontalen Zeile.

Bis zu 100 Kacheln, individuelle Höhen von 40–480 dp. Im Kacheleditor **Überlappung erlauben** aktivieren, um Kacheln gezielt übereinander anzuordnen. **Ebene (unten → oben)** legt die Zeichen- und Bedienreihenfolge fest. Ohne erlaubte Überlappung weichen betroffene Kacheln nach unten aus. Bestehende Überlappungen bleiben beim Bearbeiten einer anderen Kachel unverändert. **Lücken schließen** ordnet das Layout bewusst ohne Überlappungen neu.

Messkacheln unterstützen **Zahl, Balken und Rundinstrument** mit einstellbarem Skalenmaximum. Rundinstrumente verwenden einen echten Kreis mit wählbarem **Kreisausschnitt von 90°–270°**, standardmäßig 180° als Halbkreis. Der Zahlenwert lässt sich pro Instrument ausblenden und mittig, höher oder tiefer positionieren. Verbrauch und Leistung sind in der Voreinstellung rund, Akku mit Balken; Geschwindigkeit steht kompakt daneben. Anordnung: automatisch, einzeilig oder untereinander. Überschrift und Zusatztext sind einzeln ausblendbar. Eigene Zeilenumbrüche und maximal 1–3 Überschriftzeilen werden berücksichtigt; Werte passen ihre Schrift an die verfügbare Fläche an. Bei zu kleinen Rundkacheln erscheint eine kompakte Balkenansicht.

Start-/Endeaktionen, manuelles Fahrtende, BMS-Lastausgang und Log sind ebenfalls verschiebbare Kacheln. Die Bereitschaftsanzeige bleibt im Hochformat unter dem Kopf und sitzt im Querformat kompakt in der Kopfzeile. Eigene Button-Beschriftungen erscheinen in der Ansicht. Start bleibt grün, Ende rot. Das zunächst eingeklappte Log besitzt einen eigenen Scrollbereich und zeigt maximal 60 Zustandswechsel.
## Messwerte

Fehlende, veraltete und inaktive Messwerte erscheinen deutlich gedimmt: Text, Balken und Rundinstrumente werden grau; auch individuell gefärbte Kachelhintergründe werden abgedunkelt und entsättigt. Bei frischen Daten kehren die gewählten Farben zurück. BMS-Werte gelten ohne laufende Bereitschaft/Verbindung oder nach 8 s als inaktiv, GPS-Geschwindigkeit/Höhe ohne laufende Fahrt oder nach 8 s ohne Fix. Wetterwerte werden nach 30 Minuten gedimmt; Fahrtstatistiken ohne laufende Fahrt ebenfalls. Gespeicherte Gesamt-, Tages- und Tourenkilometer bleiben normal sichtbar. Ein gültiger Nullwert bleibt aktiv.

GPS-Geschwindigkeit, Fahrtstrecke, Gesamtkilometer, Fahr-/Standzeit (Bewegung ab 2 km/h), **Gesamtzeit** als Summe von Fahr- und Standzeit, Durchschnitt während Bewegung, Höchstgeschwindigkeit, Höhe/Höhenmeter, Akku-Prozent, Spannung, Strom, elektrische Entladeleistung, maximale elektrische Fahrtleistung, verbrauchte Wh, Wh/km, **Verbrauch der letzten 100–1000 m**, Restreichweite, Temp1, Temp2 und Außentemperatur.

Gesamtkilometer können jederzeit korrigiert werden, auch bei laufender Fahrt. Nur akzeptierte GPS-Strecken werden addiert. Ungenaue, alte und zeitlich rückwärts laufende Punkte werden verworfen. Höhe/Höhenmeter bleiben GPS-Schätzungen. Maximale Leistung ist das Maximum der BMS-Messwerte bei etwa 1-s-Abfrage; sehr kurze Spitzen können fehlen.

Temp1 und Temp2 entsprechen der BMS-Sensorreihenfolge. SP14S004 besitzt laut Hersteller einen internen und einen externen NTC; deren numerische Zuordnung ist nicht sicher dokumentiert. Die Beschriftungen sind editierbar. **Temperaturkachel antippen** fordert eine Aktualisierung an; BMS-Werte benötigen eine aktive Verbindung. Außentemperatur stammt von [Open-Meteo](https://open-meteo.com/) (CC BY 4.0), nicht von einem Rollersensor.

## Name und Appkopf

**ScootPit** verbindet **Scooter** und **Cockpit**. **BMS** steht für Batteriemanagementsystem und beschreibt die Quelle der Batteriedaten. Der Appkopf zeigt das vorhandene App-Logo; sein Name ist fest **ScootPit BMS** und seine Farbe unter **App-Farben → Appkopf-Farbe** unabhängig von der Akzentfarbe einstellbar.

## GPS-Ausreißer

Unplausible GPS-Geschwindigkeiten und Positionssprünge werden vor der Anzeige verworfen und verändern weder Höchstgeschwindigkeit noch Strecke oder Fahrtenbuch. Das Maximum ist pro Profil unter BMS und Fahrt-Erkennung editierbar (Vorgabe 45 km/h). Zusätzlich wird die Bewegung gegenüber vorherigen akzeptierten Punkten geprüft. GPS bleibt eine Messung mit Unsicherheit.

## Kacheln, Farben und Einheiten

Das voreingestellte Skalenmaximum des Tachos beträgt **22 km/h**. Ein selbst gespeichertes Maximum bleibt erhalten; ändern kannst du es im Kacheleditor unter **Skalenmaximum**.

### Skalenfarben und Verbrauchsfenster

Mit **Skalenrichtung umkehren** wird die Skala einer Balken- oder Rundinstrumentkachel umgedreht: Hohe Werte liegen am Skalenanfang, niedrige am Ende. Zahlen und Messwerte ändern sich nicht. Die Einstellung gilt für die einzelne Kachel und kann zum Beispiel beim Akku eine Entladung in der gewünschten Richtung zeigen.

Im Kacheleditor kannst du Balken und Rundinstrumente **einfarbig, zweifarbig oder dreifarbig** färben. Anfangsfarbe und Endfarbe lassen sich einzeln über **Farbe auswählen / mischen** einstellen; im Dreifarbenmodus ist auch die Mittelfarbe frei wählbar. Zwei Schieberegler legen die Schwellen fest: Bis zur Anfangsschwelle bleibt die Anfangsfarbe voll sichtbar, ab der Endschwelle die Endfarbe. Dazwischen geht der Farbton stufenlos über; bei drei Farben liegt die Mittelfarbe in der Mitte des Übergangs. Voreinstellung: Rot bis 20 %, Grün ab 80 %, dazwischen ein weicher Übergang. Die Einstellung gilt für die jeweilige Kachel und wird mit dem Scooter-Profil gesichert.

Die Verbrauchskachel besitzt im Editor einen Schieberegler für **100 bis 1000 m** in 10-m-Schritten; ihre Beschriftung zeigt das eingestellte Fenster. Messfenster und Beschriftung werden je Scooter gespeichert. Bei noch zu kurzer oder fehlender Aufzeichnung erscheint kein erfundener Wert.


Unter **Einheit: Schriftgröße** kannst du die Einheit jeder Messwertkachel unabhängig vom Zahlenwert auf **8–80** einstellen, etwa für **km/h**, **%**, **W** oder **°C**. Ein leeres Feld verwendet dieselbe Größe wie der Wert. Die Einstellung gilt für Zahl, Balken und Rundinstrument und wird im Layout mit Profilen und Sicherungen gespeichert. In schmalen Kacheln passt sich die gesamte Anzeige weiterhin an den verfügbaren Platz an.

**Einheit: Position** bietet **Neben dem Wert**, **Hochgestellt** und **Über dem Wert**. Beim Rundinstrument liegt **Über dem Wert** innerhalb der Skala oberhalb der Zahl; die Einheit beansprucht dann keinen zusätzlichen Platz neben der Zahl. Ohne eigene Einstellung bleibt die bisherige Position erhalten.

Im Kacheleditor sind **Instrument / Balken** und **Skala / Hintergrundbogen** unabhängig voneinander und je Kachel einstellbar. Fehlen eigene Instrumentfarben, verwendet die Kachel weiterhin die App-Akzentfarbe. Inaktive Messwerte bleiben gedimmt.

**+ Kachel → Bild** fügt eine Bildkachel hinzu. Antippen → **Bild auswählen** öffnet die Android-Dateiauswahl. Einpassen zeigt das gesamte Foto; Kachel füllen schneidet die Ränder bei Bedarf zu. Das Bild wird verkleinert, ohne Metadaten lokal in der Anordnung gespeichert und in Einstellungen sowie kopierten Profilen mitgesichert. Es wird nicht hochgeladen. Die Originaldatei bleibt unverändert. Die Kacheländerung mit **Übernehmen** und den gesamten Entwurf mit **Speichern** bestätigen.

**+ Kachel → Freitext** erlaubt bis zu 4000 Zeichen mit Groß-/Kleinschreibung und Zeilenumbrüchen. Schriftgröße, Hintergrund und Textfarbe einschließlich Transparenz bleiben einstellbar.

Das Hochformat zeigt die Rundinstrumente für Verbrauch, Leistung und Akkustand sowie die kompakte Restreichweite. Das Querformat ordnet die Instrumente oben und Fahrdaten, Kilometerzähler, Temperaturen, Zeiten und Aktionen darunter an; das Log bleibt am unteren Rand. Die bereitgestellte Querformat-Voreinstellung nutzt 19 Rasterzeilen und verzichtet auf leere Bild-, Spannungs- und Stromkacheln. **Standardlayout → Zurücksetzen → Speichern** übernimmt die jeweilige Anordnung für das aktuelle Format. Bereits gespeicherte Layouts bleiben bei einem Update erhalten.


Zurück aus einem Einstellungs-Untermenü führt zum Hauptmenü der Einstellungen. Beschriftungen von Schaltflächen behalten ihre Groß-/Kleinschreibung.

## Tages- und Tourenkilometer

Im Editor **+ Kachel → Tageskilometer** oder **Tourenzähler** wählen. Tageskilometer beginnen am nächsten Kalendertag nach der lokalen Handyzeit wieder bei 0. Der Tourenzähler läuft bis **Einstellungen → Tourenzähler zurücksetzen**. Ein Reset betrifft nur den aktiven Scooter und verändert weder Tageskilometer noch Gesamtstand. Die drei Zähler verwenden dieselben akzeptierten GPS-Strecken.

Das Einstellungen-Symbol zeigt Zahnrad und Blitz ohne Hintergrund; die Symbolgröße bleibt erhalten.
