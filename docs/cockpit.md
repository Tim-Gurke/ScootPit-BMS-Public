# Cockpit, Layout und Messwerte

[← Übersicht](../README.md)

## Cockpit und visueller Editor

Zahnrad mit Blitz → **Cockpit bearbeiten**. Kachel lange drücken und ziehen; am Griff unten rechts Größe ändern. Antippen öffnet Beschriftung, Schriftgröße, Farben, Darstellung und genaue Rasterwerte. Aktionen sind während der Bearbeitung gesperrt. **Speichern** übernimmt den Entwurf, **Zurück** verwirft ihn. Entwürfe bleiben beim Drehen des Handys erhalten.

Das Raster hat 12 Spalten und 40-dp-Höheneinheiten: Breite 12 = ganze Zeile, Breite 6 = zwei gleiche Spalten, Breite 4 = drei. Hoch- und Querformat werden pro Scooter getrennt gespeichert. Ohne eigenes Querformatlayout wird das Hochformatlayout übernommen. Beim Drehen bleiben Layoutentwürfe erhalten. Hintergrund und Textfarbe jeder Kachel besitzen einen Transparenzregler (0 % = deckend, 100 % = unsichtbar). Die Aktionen Übernehmen, Zurück und Entfernen stehen fest in einer horizontalen Zeile.

Bis zu 100 Kacheln, individuelle Höhen von 80–480 dp. Bei Überlappung weichen betroffene Kacheln automatisch nach unten aus; ihre Breite und Höhe bleiben erhalten. **Lücken schließen** verdichtet das Layout. Bestehende Zeilenlayouts werden übernommen.

Messkacheln unterstützen **Zahl, Balken und Rundinstrument** mit einstellbarem Skalenmaximum. Geschwindigkeit ist standardmäßig rund, Akku mit Balken. Anordnung: automatisch, einzeilig oder untereinander. In flachen Kacheln hat der Messwert Vorrang; beispielsweise „Akku 75 %“ in einer Zeile. Überschrift und Zusatztext sind einzeln ausblendbar. Eigene Zeilenumbrüche und maximal 1–3 Überschriftzeilen werden berücksichtigt; Werte passen ihre Schrift an die verfügbare Fläche an. Bei zu kleinen Rundkacheln erscheint eine kompakte Balkenansicht.

Bereitschaft starten/beenden, manuelles Fahrtende, BMS-Lastausgang und Log sind ebenfalls verschiebbare Kacheln. Eigene Button-Beschriftungen erscheinen in der Ansicht. Start bleibt grün, Ende rot. Das zunächst eingeklappte Log besitzt einen eigenen Scrollbereich und zeigt maximal 60 Zustandswechsel.

## Messwerte

Fehlende, veraltete und inaktive Messwerte erscheinen deutlich gedimmt: Text, Balken und Rundinstrumente werden grau; auch individuell gefärbte Kachelhintergründe werden abgedunkelt und entsättigt. Bei frischen Daten kehren die gewählten Farben zurück. BMS-Werte gelten ohne laufende Bereitschaft/Verbindung oder nach 8 s als inaktiv, GPS-Geschwindigkeit/Höhe ohne laufende Fahrt oder nach 5 s ohne Fix. Wetterwerte werden nach 30 Minuten gedimmt; Fahrtstatistiken ohne laufende Fahrt ebenfalls. Gespeicherte Gesamtkilometer bleiben normal sichtbar. Ein gültiger Nullwert bleibt aktiv.

GPS-Geschwindigkeit, Fahrtstrecke, Gesamtkilometer, Fahr-/Standzeit (Bewegung ab 2 km/h), Durchschnitt während Bewegung, Höchstgeschwindigkeit, Höhe/Höhenmeter, Akku-Prozent, Spannung, Strom, elektrische Entladeleistung, maximale elektrische Fahrtleistung, verbrauchte Wh, Wh/km, Restreichweite, Temp1, Temp2 und Außentemperatur.

Gesamtkilometer können jederzeit korrigiert werden, auch bei laufender Fahrt. Nur akzeptierte GPS-Strecken werden addiert. Ungenaue, alte und zeitlich rückwärts laufende Punkte werden verworfen. Höhe/Höhenmeter bleiben GPS-Schätzungen. Maximale Leistung ist das Maximum der BMS-Messwerte bei etwa 2-s-Abfrage; sehr kurze Spitzen können fehlen.

Temp1 und Temp2 entsprechen der BMS-Sensorreihenfolge. SP14S004 besitzt laut Hersteller einen internen und einen externen NTC; deren numerische Zuordnung ist nicht sicher dokumentiert. Die Beschriftungen sind editierbar. Außentemperatur stammt von [Open-Meteo](https://open-meteo.com/) (CC BY 4.0), nicht von einem Rollersensor.

## Name und Appkopf

**ScootPit** verbindet **Scooter** und **Cockpit**. **BMS** steht für Batteriemanagementsystem und beschreibt die Quelle der Batteriedaten. Der Appkopf zeigt das vorhandene App-Logo; sein Name ist fest **ScootPit BMS** und seine Farbe unter **App-Farben → Appkopf-Farbe** unabhängig von der Akzentfarbe einstellbar.

## GPS-Ausreißer

Unplausible GPS-Geschwindigkeiten und Positionssprünge werden vor der Anzeige verworfen und verändern weder Höchstgeschwindigkeit noch Strecke oder Fahrtenbuch. Das Maximum ist pro Profil unter BMS und Fahrt-Erkennung editierbar (Vorgabe 45 km/h). Zusätzlich wird die Bewegung gegenüber vorherigen akzeptierten Punkten geprüft. GPS bleibt eine Messung mit Unsicherheit.

## Neue Kacheln und Farben ab 6.3

Ab 6.3.1 beträgt das voreingestellte Skalenmaximum des Tachos **22 km/h**. Ein selbst gespeichertes Maximum bleibt erhalten; ändern kannst du es im Kacheleditor unter **Skalenmaximum**.

Unter **Einheit: Schriftgröße** kannst du die Einheit jeder Messwertkachel unabhängig vom Zahlenwert auf **8–80** einstellen, etwa für **km/h**, **%**, **W** oder **°C**. Ein leeres Feld verwendet dieselbe Größe wie der Wert. Die Einstellung gilt für Zahl, Balken und Rundinstrument und wird im Layout mit Profilen und Sicherungen gespeichert. In schmalen Kacheln passt sich die gesamte Anzeige weiterhin an den verfügbaren Platz an.

**Einheit: Position** bietet **Neben dem Wert**, **Hochgestellt** und **Über dem Wert**. Beim Rundinstrument liegt **Über dem Wert** innerhalb der Skala oberhalb der Zahl; die Einheit beansprucht dann keinen zusätzlichen Platz neben der Zahl. Ohne eigene Einstellung bleibt die bisherige Position erhalten.

Im Kacheleditor sind **Instrument / Balken** und **Skala / Hintergrundbogen** unabhängig voneinander und je Kachel einstellbar. Fehlen eigene Instrumentfarben, verwendet die Kachel weiterhin die App-Akzentfarbe. Inaktive Messwerte bleiben gedimmt.

**+ Kachel → Bild** fügt eine Bildkachel hinzu. Antippen → **Bild auswählen** öffnet die Android-Dateiauswahl. Einpassen zeigt das gesamte Foto; Kachel füllen schneidet die Ränder bei Bedarf zu. Das Bild wird verkleinert, ohne Metadaten lokal in der Anordnung gespeichert und in Einstellungen sowie kopierten Profilen mitgesichert. Es wird nicht hochgeladen. Die Originaldatei bleibt unverändert. Die Kacheländerung mit **Übernehmen** und den gesamten Entwurf mit **Speichern** bestätigen.

**+ Kachel → Freitext** erlaubt bis zu 4000 Zeichen mit Groß-/Kleinschreibung und Zeilenumbrüchen. Schriftgröße, Hintergrund und Textfarbe einschließlich Transparenz bleiben einstellbar.

Die neue Grundeinstellung zeigt links einen großen, vollständig transparenten Tacho; rechts Akku 🔋, Restreichweite und Leistung. Darunter folgen Fahrzeit/Fahrtstrecke, Standzeit/Gesamtkilometer, maximale Fahrtleistung/BMS-Lastausgang, Wetter 🌞🌧️🌤️ und die beiden Temperatursensoren sowie An/Fahrt beenden/Aus. Das Log nimmt unten die gesamte Breite ein. Vorhandene eigene Anordnungen bleiben gespeichert; **Standardlayout → Zurücksetzen → Speichern** übernimmt die neue Anordnung.

Zurück aus einem Einstellungs-Untermenü führt zum Hauptmenü der Einstellungen. Beschriftungen von Schaltflächen behalten ihre Groß-/Kleinschreibung.
