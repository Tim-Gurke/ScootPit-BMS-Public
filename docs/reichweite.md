# Restreichweite und Temperatur

[← Übersicht](../README.md)

## Restreichweite und Temperaturlernen

Restreichweite = nutzbare Restenergie nach Reserve / geschätzter Verbrauch. Kapazität und Rest-Ah kommen bevorzugt vom BMS; Ersatzwerte sind **26 Ah und 48 V**. Reserve standardmäßig 10 %, anfänglicher Verbrauch 15 Wh/km; Werte sind konfigurierbar.

Bis 250 m dient Startwert/erlernter Verbrauch als Schätzung. Danach werden der jüngste Verbrauch über ungefähr 800 m zu 75 % und der Durchschnitt der laufenden Fahrt zu 25 % gewichtet. Energie wird aus BMS-Leistung zeitlich integriert. Nach einer Messlücke über 8 s wird das Verbrauchsfenster neu aufgebaut; fehlende Energie wird nicht erfunden. BMS-Werte werden nach 8 s bzw. Verbindungsverlust als veraltet dargestellt. Reichweite bleibt eine Schätzung.

Neue CSV-Dateien speichern Außentemperatur, Temp1 und Temp2; JSON-Zusammenfassungen enthalten deren Mittelwerte. Fehlende oder veraltete Temperaturen bleiben leer/null. Für die temperaturbezogene Startschätzung werden bis zu 100 Fahrten mit mindestens 1 km und plausiblem Verbrauch gespeichert. Erforderlich sind mindestens 3 passende Fahrten und gewichtete 5 km innerhalb ±5 °C. Ohne passende Daten gilt der bisherige Startwert; es gibt keinen pauschalen Kältefaktor und keine Extrapolation. Während der Fahrt hat der aktuelle gemessene Verbrauch Vorrang, damit Temperatur nicht doppelt berücksichtigt wird. Frühere Dateien ohne Temperaturen erlauben keine rückwirkende Kalibrierung. Das Lernmodell beschreibt beobachteten Fahrtverbrauch, nicht eine isoliert gemessene Temperaturwirkung.

Während aufgezeichneter Fahrten werden für Wetterabfragen auf drei Dezimalstellen gerundete Koordinaten per HTTPS an Open-Meteo übertragen, höchstens alle 15 Minuten; Fehlerwiederholung frühestens nach 5 Minuten. Wetter ist im Menü deaktivierbar. Nach 30 Minuten wird der Wert als veraltet markiert. Keine Wetterabfragen in reiner Bereitschaft.
