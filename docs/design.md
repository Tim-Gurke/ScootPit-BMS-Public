# Cockpit gestalten · 1.4.1

[← Übersicht](../README.md)

## Verbrauch und Leistung im Mittelpunkt

Zwei transparente Halbkreisinstrumente zeigen den Verbrauch der letzten 500 m und die aktuelle elektrische Leistung. Im Editor sind Darstellung, Skalenmaximum, Farben und Kreisausschnitt von 90° bis 270° je Kachel einstellbar. Den Messwert kannst du je Rundinstrument ausblenden oder zwischen mittiger, höherer und tieferer Position wählen. Ein zusätzlicher Drehregler richtet den Bogen frei aus; die Messwerte bleiben waagerecht. 180° zeichnet einen echten Halbkreis; die Kreisgeometrie wird nicht zur Ellipse gestreckt. Die kleinere Geschwindigkeitskachel kann bei Bedarf ebenfalls als Instrument gestaltet werden.

## Frei anordnen und überlagern

**Einstellungen → Cockpit bearbeiten** öffnet das Raster mit 24 Spalten und 20-dp-Höheneinheiten. Kacheln lange drücken und ziehen, am Griff die Größe ändern oder genaue Position und Größe eingeben. **Überlappung erlauben** gibt die Platzierung über anderen Kacheln frei. **Ebene (unten → oben)** bestimmt, welche Kachel darüber dargestellt und bedient wird. Bewusste Überlappungen bleiben beim Bearbeiten anderer Kacheln unverändert. **Lücken schließen** ordnet auf Wunsch ohne Überlappungen neu.

Rahmen ein-/ausschalten, Rahmenfarbe, Eckenradius, Hintergrundfarbe und Transparenz sowie Innenabstand jeder Kachel separat einstellen. Ein Innenabstand von 0 erlaubt Schrift bis nahe an den Rahmen. Bild- und Freitextkacheln bleiben möglich. Farben werden sichtbar als Vorschau gezeigt; **Farbe auswählen / mischen** bietet Farbton, Sättigung und Helligkeit sowie optional einen Hex-Code.

## Helles und dunkles Design

**App-Farben** bietet Dunkel/Orange und Hell/Blau. **Nur App-Farben** erhält eigene Kachelfarben; **Alle Kacheln** übernimmt das Schema auch für Hoch- und Querformat. Transparente Kacheln, Anordnung, Bilder und Texte bleiben erhalten. Der Bildschirm kann einen frei wählbaren Verlauf zwischen zwei Farben erhalten. Alle Farben sind danach manuell anpassbar.

Der Appkopf integriert Appname und antippbaren Profilnamen, Fahrtenbuch und Zahnrad mit Blitz. Der grüne Punkt zeigt aktive Bereitschaft. Das feste App-Symbol bleibt beim Schemawechsel gleich. Alle Kacheln besitzen passende Symbole mit separat wählbarer Farbe; Symbol, Beschriftung, beides oder keines sind einstellbar.

![Individuell angeordnetes Cockpit im dunklen Farbschema](assets/screenshots/cockpit-dunkel.jpg)

*Dunkles Design mit frei angeordneten Anzeigen und klaren orangefarbenen Akzenten.*

![Dasselbe individuell angeordnete Cockpit im hellen Farbschema](assets/screenshots/cockpit-hell.jpg)

*Das helle Schema nutzt Blau als Akzentfarbe und lässt das persönliche Layout bestehen.*

## Vorhandene Layouts

Beim ersten Öffnen eines Profils wird das Hochformat auf 1.2.1 umgestellt; Fotos, Texte, persönliche Kacheleinstellungen und Zusatzkacheln werden übernommen. Die vorherige Anordnung steht als `cockpit_board_before_121` in der exportierbaren Einstellungssicherung. Ein gespeichertes Querformat wird auf das doppelt so feine Raster umgerechnet und ebenfalls vorher gesichert. Anschließend lässt sich alles frei ändern.

Das Antippen der Tourenzähler-Kachel öffnet im normalen Cockpit die Rückfrage zum Zurücksetzen. Temperaturkacheln fordern beim Antippen eine Aktualisierung an. Im Editor öffnet das Antippen jeweils die Kacheleinstellungen.


## Standardgestaltung ab 1.4.1

Bei einer Neuinstallation übernimmt ScootPit das dunkle Design mit orangefarbenen Akzenten sowie Hoch- und Querformatlayout aus der bereinigten Cockpit-Voreinstellung. Bestehende Installationen und Profileinstellungen werden dabei nicht überschrieben.
