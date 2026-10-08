# Automatik, Bereitschaft und Samsung-Routinen

[← Übersicht](../README.md)

## Automatische Scooter-Auswahl

Unter **Einstellungen → BMS und Fahrt-Erkennung** ist die automatische Auswahl gespeicherter Scooter standardmäßig aktiviert. Jeder Scooter benötigt ein eigenes Profil und eine zuvor geprüfte BMS-Zuordnung. Die Suche berücksichtigt nur gespeicherte Geräte. Dieselbe BMS-Adresse darf nicht mehreren Profilen zugeordnet sein; solche mehrdeutigen Geräte werden aus der Automatik ausgeschlossen.

Erreichbare BMS werden nacheinander verbunden und auf Stromentnahme geprüft. Bei mehreren gespeicherten Geräten wird ein Kandidat ohne Startsignal nach ungefähr 6 Sekunden verlassen. Während einer Fahrt wechselt die App das Profil nicht. Ohne Automatik wird nur das manuell ausgewählte Profil gesucht. Manuelle Profilwechsel und BMS-Änderungen erfordern beendete Bereitschaft.

## Start, Pause und Fahrtende

- **Bereitschaft starten:** Hintergrunddienst und BMS-Suche aktivieren. Bluetooth, präziser Standort und Berechtigungen müssen aktiv sein.
- **Fahrtstart:** Standardmäßig mindestens 0,30 A Entnahme für 1 Sekunde. GPS und Fahrtenbuch starten automatisch.
- **Ausrollen:** GPS wird auch ohne Stromentnahme weiter ausgewertet. Fehlende Motorleistung setzt den Tacho nicht auf null.
- **Stillstand:** Nach standardmäßig **45 Sekunden** zuverlässig erkanntem Stillstand pausiert die Fahrt. Die BMS-Verbindung bleibt erhalten. GPS-Ausfall oder schlechte Messungen gelten nicht als bestätigter Stillstand. **Fahrt pausieren nach Stillstand** ist je Scooter einstellbar (15–3600 Sekunden).
- **Weiterfahrt:** Bewegung oder bestätigte Stromentnahme setzt dieselbe Fahrt fort. Die Pause zählt dann zur Standzeit.
- **Lange Pause:** Nach standardmäßig **600 Sekunden** Pause wird die Fahrt rückwirkend zum Pausenbeginn abgeschlossen. Die BMS-Verbindung bleibt bestehen, solange das Gerät erreichbar ist. **Fahrtende nach langer Pause** ist von 60–7200 Sekunden einstellbar.
- **Verbindungsverlust in der Pause:** Die App sucht weiter. Ohne Wiederverbindung bis standardmäßig **120 Sekunden nach Pausenbeginn** wird die Pause als Fahrtende gespeichert. Dieses Zeitfenster ist von 0–7200 Sekunden einstellbar; das Limit für lange Pausen gilt weiterhin.
- **Entfernung:** Bestätigter Verlust der BMS-Verbindung kann die Fahrt ebenfalls beenden. Schwacher Empfang allein genügt bei frischen BMS-Daten nicht.
- **Fahrt beenden:** Manuell speichern; Bereitschaft bleibt aktiv. Erneuter Start erst nach Stromruhe, damit der Stop nicht sofort rückgängig gemacht wird.
- **Bereitschaft beenden:** Suche stoppen und laufende Fahrt speichern.

Nach automatisch erkanntem Fahrtende bleibt ein erreichbares BMS verbunden. Ist es nicht verbunden, beginnt die Suche nach ungefähr 1 Sekunde wieder. Fehlende BMS werden mit kurzen Suchpausen weiter gesucht; die früher lange Wartezeit zwischen Fahrten entfällt. Android kann BLE-Suchvorgänge begrenzen; Empfang und tatsächliche Startlatenz am eigenen Handy prüfen.

## Verbindungswerte

Vorgaben: Erstverbindung ab **−75 dBm**, mindestens 3 stabile Messungen über 3 Sekunden, Entfernungsschwelle **−95 dBm**. Suchpause bei fehlendem BMS **5 s**, schwachem Empfang **15 s**, gutem Empfang **2 s**, Wiederverbindung **2 s**. Scanfenster 10 s, bei Prüfung einer laufenden Fahrt 3 s. Entfernung wird nach 30 s ohne ausreichendes Signal bestätigt. BMS-Grundwerte werden ungefähr jede Sekunde abgefragt.

Werte sind pro Scooter editierbar und gelten nach Neustart der Bereitschaft. Vorher ausgelieferte Standardwerte werden einmalig an die neuen Vorgaben angepasst; andere gespeicherte Einstellungen bleiben erhalten. Empfangsstärke ist keine exakte Entfernungsmessung.

## Gesperrtes Handy

Die Bereitschaft wird aus der geöffneten App gestartet und bleibt als Vordergrunddienst aktiv. Bildschirm ausschalten und Handy einstecken beendet sie nicht. Die App hält einen begrenzten, erneuerten CPU-Wakelock. Android-/Samsung-Beschränkungen können trotzdem Bluetooth und GPS beeinflussen. Unter **Akkuoptimierung** Status prüfen, Ausnahme anfragen oder die App-Info öffnen. In Samsung-Einstellungen die App gegebenenfalls zu **Nie im Standby** hinzufügen.

## Routine-Nachrichten

Unter **Einstellungen → Routine-Nachrichten bearbeiten** Titel und Text für Fahrtstart und Fahrtende je Scooter einstellen. Standardtitel bleiben **T6E Fahrt gestartet** und **T6E Fahrt beendet**, damit bestehende Routinen weiter passen. Ein leerer Text verwendet die bisherige Startnachricht beziehungsweise Fahrtwerte. Platzhalter: **{scooter}**, **{km}**, **{wh}**.

Zwei getrennte Benachrichtigungskanäle: **Hintergrundbetrieb** darf separat ausgeblendet werden; **Routine-Signale** eingeschaltet lassen. Bei geänderten Titeln oder Texten auch die Suchbedingung der Samsung-Routine anpassen.

In Samsung **Modi und Routinen** zwei Routinen einrichten:

| Auslöser: Nachricht von ScootPit enthält | Aktion |
| --- | --- |
| `T6E Fahrt gestartet` oder eigener Starttext | Energiesparmodus aus |
| `T6E Fahrt beendet` oder eigener Endtext | Energiesparmodus an |

Die App kann den globalen Energiesparmodus nicht selbst schalten und richtet die Routinen nicht automatisch ein. Die Aktion stellt den Modus ausdrücklich auf an/aus und stellt keinen vorherigen Zustand wieder her. Eine Pause sendet kein Ende-Signal; erst der tatsächliche Fahrtabschluss nach längerer Pause, bestätigter Entfernung oder manuellem Beenden löst es aus.

Quellen: [Android: Energiesparmodus](https://source.android.com/docs/core/power/routine-battery-saver), [Samsung: Modi und Routinen](https://www.samsung.com/de/support/mobile-devices/modi-und-routinen/).
