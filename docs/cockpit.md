# Cockpit, Layout und Messwerte


In ScootPit BMS bleiben Appkopf und Bedienelemente außerhalb von Statusleiste, Navigationsleiste und Kameraausschnitten. Das gilt auch unter Android 15 und bei gedrehtem Bildschirm.


[← Übersicht](../README.md)




## Cockpit und visueller Editor


Zahnrad mit Blitz → **Cockpit bearbeiten**. Kachel lange drücken und ziehen; am Griff unten rechts Größe ändern. Antippen öffnet Beschriftung, Schriftgröße, Farben, Darstellung und genaue Rasterwerte. Aktionen sind während der Bearbeitung gesperrt. **Speichern** übernimmt den Entwurf, **Zurück** verwirft ihn. Entwürfe bleiben beim Drehen des Handys erhalten.


Das Raster hat 12 Spalten und 40-dp-Höheneinheiten: Breite 12 = ganze Zeile, Breite 6 = zwei gleiche Spalten, Breite 4 = drei. Hoch- und Querformat werden pro Scooter getrennt gespeichert. Ohne eigenes Querformatlayout wird das Hochformatlayout übernommen. Beim Drehen bleiben Layoutentwürfe erhalten. Hintergrund und Textfarbe jeder Kachel besitzen einen Transparenzregler (0 % = deckend, 100 % = unsichtbar). Die Aktionen Übernehmen, Zurück und Entfernen stehen fest in einer horizontalen Zeile.


Bis zu 100 Kacheln, individuelle Höhen von 80–480 dp. Bei Überlappung weichen betroffene Kacheln automatisch nach unten aus; ihre Breite und Höhe bleiben erhalten. **Lücken schließen** verdichtet das Layout. Bestehende Zeilenlayouts werden übernommen.


Messkacheln unterstützen **Zahl, Balken und Rundinstrument** mit einstellbarem Skalenmaximum. Geschwindigkeit ist standardmäßig rund, Akku mit Balken. Anordnung: automatisch, einzeilig oder untereinander. In flachen Kacheln hat der Messwert Vorrang; beispielsweise „Akku 75 %“ in einer Zeile. Überschrift und Zusatztext sind einzeln ausblendbar. Eigene Zeilenumbrüche und maximal 1–3 Überschriftzeilen werden berücksichtigt; Werte passen ihre Schrift an die verfügbare Fläche an. Bei zu kleinen Rundkacheln erscheint eine kompakte Balkenansicht.


Bereitschaft starten/beenden, manuelles Fahrtende, BMS-Lastausgang und Log sind ebenfalls verschiebbare Kacheln. Eigene Button-Beschriftungen erscheinen in der Ansicht. Start bleibt grün, Ende rot. Das zunächst eingeklappte Log besitzt einen eigenen Scrollbereich und zeigt maximal 60 Zustandswechsel.




## Messwerte


Fehlende, veraltete und inaktive Messwerte erscheinen deutlich gedimmt: Text, Balken und Rundinstrumente werden grau; auch individuell gefärbte Kachelhintergründe werden abgedunkelt und entsättigt. Bei frischen Daten kehren die gewählten Farben zurück. BMS-Werte gelten ohne laufende Bereitschaft/Verbindung oder nach 8 s als inaktiv, GPS-Geschwindigkeit/Höhe ohne laufende Fahrt oder nach 8 s ohne Fix. Wetterwerte werden nach 30 Minuten gedimmt; Fahrtstatistiken ohne laufende Fahrt ebenfalls. Gespeicherte Gesamt-, Tages- und Tourenkilometer bleiben normal sichtbar. Ein gültiger Nullwert bleibt aktiv.


GPS-Geschwindigkeit, Fahrtstrecke, Gesamtkilometer, Fahr-/Standzeit (Bewegung ab 2 km/h), Durchschnitt während Bewegung, Höchstgeschwindigkeit, Höhe/Höhenmeter, Akku-Prozent, Spannung, Strom, elektrische Entladeleistung, maximale elektrische Fahrtleistung, verbrauchte Wh, Wh/km, Restreichweite, Temp1, Temp2 und Außentemperatur.


Gesamtkilometer können jederzeit korrigiert werden, auch bei laufender Fahrt. Nur akzeptierte GPS-Strecken werden addiert. Ungenaue, alte und zeitlich rückwärts laufende Punkte werden verworfen. Höhe/Höhenmeter bleiben GPS-Schätzungen. Maximale Leistung ist das Maximum der BMS-Messwerte bei etwa 1-s-Abfrage; sehr kurze Spitzen können fehlen.
