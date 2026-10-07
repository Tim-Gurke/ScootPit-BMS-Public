# Bereitschaft, Pausen und Energiesparmodus

[← Übersicht](../README.md)

## Bereitschaft, Tracking und Routines

- **Bereitschaft starten**: Hintergrunddienst und adaptive BMS-Suche.
- **Fahrt beenden**: Fahrt speichern; Bereitschaft bleibt aktiv. Erneuter automatischer Start erst nach Stromruhe, damit der manuelle Stop nicht sofort rückgängig gemacht wird.
- **Bereitschaft beenden**: Suche stoppen und laufende Fahrt speichern; eine laufende Fahrt benötigt vorher Bestätigung.

Alle Verbindungswerte sind unter **BMS und Fahrt-Erkennung** pro Scooter editierbar. Vorgaben: Verbinden ab **−85 dBm**, Entfernungsschwelle **−95 dBm**, Suchpause bei fehlendem BMS **60 s**, schwachem Empfang **15 s**, gutem Empfang **5 s**, Wiederverbindung während einer Fahrtpause **5 s**, Entfernung bestätigen nach **30 s** ohne Daten/ausreichendes BMS-Signal. Scanfenster dauern 10 s, bei Wiederverbindung 3 s; die Suchpause folgt dem Scanfenster. Laufende Verbindung: Stromabfrage alle 2 s.

Start ab 0,30 A Entnahme für 3 s. Nach 5 s ohne Entnahme pausiert die Fahrt, die BMS-Verbindung bleibt erhalten. Schwacher Empfang allein beendet keine Fahrt mit frischen BMS-Daten. Veraltete Daten führen zur Wiederverbindung. Erneute Stromentnahme setzt dieselbe Fahrt fort. Ohne Stromentnahme wird nach 3 s keine GPS-Strecke mehr addiert; sehr kurzes Gehen direkt nach dem Abstellen und Rollen ohne Motor sind dadurch nicht vollkommen unterscheidbar. Bei Wiederaufnahme wird kein GPS-Sprung über die Pause addiert. Ohne Fahrtbeginn nach 90 s erneut suchen. Änderungen gelten ab dem nächsten Bereitschaftsstart.

Beim ersten Bereitschaftsstart mit 6.2 werden die bisherige Pausenvorgabe 20 s und der vorübergehende Testwert 120 s einmal pro Profil auf 5 s angepasst. Andere eigene Pausenwerte bleiben erhalten; danach bleiben alle Werte frei editierbar.

App-Symbol öffnet das Cockpit, ohne die Bereitschaft zu beenden. Der Dienst hält einen begrenzten und erneuerten CPU-Wakelock. Im Emulator wird Bereitschaft bei ausgeschaltetem Bildschirm geprüft; echtes GPS/BLE-Tracking und Samsung-Energiesparverhalten sind am Handy und Roller zu testen.

Zwei getrennte Benachrichtigungskanäle: **Hintergrundbetrieb** (`monitor`) ist über die Android-Kanaleinstellungen separat ausblendbar. **Routine-Signale** (`ride_events`) aktiviert lassen; Texte bleiben **T6E Fahrt gestartet** und **T6E Fahrt beendet**. Android kann weiterhin „Aktive Apps“ anzeigen. Nicht sämtliche App-Benachrichtigungen sperren.

## Energiesparmodus mit Samsung-Routinen

ScootPit BMS darf den globalen Android-Energiesparmodus als gewöhnliche App nicht direkt schalten. Sie gibt bereits zwei Ereignisbenachrichtigungen aus; Samsung **Modi und Routinen** übernimmt das Umschalten:

| Auslöser: Nachricht von ScootPit BMS enthält | Aktion |
| --- | --- |
| `T6E Fahrt gestartet` | Energiesparmodus aus |
| `T6E Fahrt beendet` | Energiesparmodus an |

In Samsung **Modi und Routinen → Routinen** zwei Routinen anlegen. Als Wenn-Bedingung eine empfangene Benachrichtigung von ScootPit BMS mit dem jeweiligen Text wählen; als Dann-Aktion Energiesparmodus aus bzw. an. Bezeichnungen und verfügbare Bedingungen hängen von One UI ab. Diese Routinen müssen vom Nutzer eingerichtet werden; ScootPit legt sie nicht automatisch an. Die Texte bleiben für bestehende Routinen unverändert.

**Routine-Signale** aktiviert lassen. Nur den getrennten Kanal **Hintergrundbetrieb** bei Bedarf ausblenden. Fahrtstart/-ende mit stehendem Roller prüfen und kontrollieren, dass sich der Energiesparmodus tatsächlich ändert. Eine Fahrtpause sendet kein Fahrtende-Signal; erst der tatsächliche Abschluss. Das Ende-Signal schaltet mit dieser Konfiguration ausdrücklich auf „an“, es stellt nicht automatisch einen zuvor gewählten Zustand wieder her.

Die Ausnahme von der **Akkuoptimierung für ScootPit** ist eine andere Einstellung als der globale Energiesparmodus. Beides kann den Hintergrundbetrieb beeinflussen; das Verhalten muss auf dem eigenen Handy geprüft werden.

**Einstellungen → Akkuoptimierung** zeigt, ob ScootPit BMS bereits von der Android-Akkuoptimierung ausgenommen ist. Wenn sie noch aktiv ist, öffnet **Freigabe anfragen** den Android-Dialog. Bei vorhandener Ausnahme öffnet **Android-Einstellungen** die Übersicht. **App-Info** führt zu ScootPit BMS in den Systemeinstellungen; dort kannst du zusätzliche Beschränkungen unter **Akku** prüfen. **Zurück** führt wieder ins Einstellungs-Hauptmenü.

Quellen: [Android: privilegierte POWER_SAVER-Berechtigung](https://source.android.com/docs/core/power/routine-battery-saver), [Samsung: Modi und Routinen](https://www.samsung.com/de/support/mobile-devices/modi-und-routinen/).

## Erstverbindung und laufende Fahrt ab 6.3

Vor der ersten Verbindung müssen mindestens drei Empfangsmessungen die Verbindungsschwelle erreichen und über die Bestätigungsdauer stabil bleiben. Schwächere Messungen oder eine Lücke über 2,5 Sekunden setzen die Bestätigung zurück. Neue Profile verwenden **−70 dBm / 3 Sekunden**; vorhandene Schwellen bleiben erhalten. Die Bestätigungsdauer ist unter **BMS und Fahrt-Erkennung** zwischen 1 und 15 Sekunden einstellbar.

Nach erkanntem Fahrtstart gilt bei einer Wiederverbindung die schwächere Entfernungsschwelle. Frische BMS-Antworten erhalten die bestehende Verbindung auch in einer Pause. Nach dem Fahrtende gilt erneut die strenge Erstverbindung. Empfangsstärke ist keine zuverlässige Entfernungsmessung: Die geeignete Schwelle am Roller und am Abstellort erproben.
