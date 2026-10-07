# Installation und Ersteinrichtung

[← Übersicht](../README.md)

## Build, Tests und Installation

GitHub Actions prüft die App und baut bei vollständig eingerichteten Signier-Secrets eine Release-APK; ohne diese Secrets entsteht eine Test-APK. Artefakte: **ScootPit-BMS-APK** und **ScootPit-BMS-UI**. Erforderlich: Java 17, Gradle 8.9, Android SDK 35; minSdk 26. Protokoll-, Steuerungs-, Reichweiten- und Rastertests laufen als Java-Tests; Android Lint prüft die App. Android-13/15-Emulatortests prüfen Messwerte, Beschriftungen, Layoutänderungen, Temperatur-Startschätzung, Log, persistente Ordnerberechtigungen, Settings-Roundtrip, fehlgeschlagene Fahrtkopie mit Wiederholung und Bereitschaft bei ausgeschaltetem Bildschirm. Bildschirmaufnahmen werden archiviert.

Die App-ID bleibt **de.wortmonster.jbdtrigger**, damit Routines und der vorhandene App-Datenbereich bei kompatiblen Updates erhalten bleiben. Die APK benötigt für ein Update dieselbe Signatur. Unterschiedliche Debug-Schlüssel verschiedener CI-Läufe können eine Neuinstallation erfordern. **Vor einer Deinstallation Fahrten und Einstellungen sichern; Deinstallation löscht interne Daten und Ordnerberechtigungen.** Für die offizielle APK ist der dauerhafte Release-Schlüssel über GitHub Actions Secrets eingerichtet. Beim Wechsel von einer früheren Testsignatur kann einmalig eine Neuinstallation erforderlich sein. Danach können neue Versionen mit demselben Schlüssel als Update installiert werden.

ZIP entpacken und APK öffnen. Präzisen Standort, Bluetooth/Nearby Devices und Benachrichtigungen erlauben. Bereitschaft aus der sichtbaren App starten. Akkuoptimierung gegebenenfalls im Menü anpassen. Reale BMS-Steuerung, Sensorzuordnung, GPS, Routine-Auslösung und Hintergrundbetrieb sind am Roller zu prüfen.

## Ersteinrichtung und Standardlayout

Bei einer frischen Installation ist kein BMS vorbelegt. Über **BMS auswählen → Suche starten** das eigene Bluetooth-BMS auswählen. Erst danach kann die Bereitschaft gestartet werden. Eine bereits gespeicherte oder importierte gültige BMS-Auswahl bleibt erhalten. Die App enthält keine feste Geräteadresse. Auch ein direkter Dienststart ohne gültige Auswahl wird beendet.

Das Standardlayout zeigt links einen großen transparenten Tacho mit 22-km/h-Skala, rechts Akku, Restreichweite und Leistung. Darunter stehen Fahr-/Standzeit, Strecke, Gesamtkilometer, maximale Leistung, BMS-Lastausgang, Wetter und Temperaturen. Die Aktionen heißen An, Fahrt beenden und Aus. Der Appkopf bleibt ScootPit BMS. Tages- und Tourenkilometer können als weitere Kacheln ergänzt werden.

Bestehende Anordnungen werden beim Update nicht überschrieben. Das neue Standardlayout unter **Einstellungen → Cockpit bearbeiten → Standardlayout → Zurücksetzen → Speichern** übernehmen. Ein Import einer älteren Sicherung stellt deren enthaltene Anordnung wieder her.

## Mehrere Scooter

Über **Scooter: …** im Appkopf oder **Einstellungen → Scooter-Profile** auswählen, hinzufügen, umbenennen, kopieren und entfernen. Beim ersten Start wird das bisherige Setup als erstes Profil übernommen. Pro Profil getrennt: BMS, Akku-Konfiguration, Gesamt-/Tages-/Tourenkilometer, Verbrauchs-/Temperaturhistorie, Routine-Texte, Statistik-Auswahl, letzte Fahrt, Fahrtenordner, Layout, Appkopf und Farben. Manuelle Profilwechsel erfordern beendete Bereitschaft. Die Automatik kann gespeicherte BMS erkennen und vor Fahrtbeginn das zugehörige Profil wählen; während einer Fahrt bleibt die Zuordnung fest. Eine Designkopie übernimmt keine BMS-Zuordnung, Kilometer, Fahrten oder Verbrauchshistorie. Entfernen eines Profils löscht keine Fahrtdateien.

Neue Fahrten liegen lokal in einem eigenen Unterordner pro Profil; GPX-Name und JSON-Metadaten tragen Scooter-Name und Profil-ID. Bereits bestehende Dateien bleiben erhalten. Die Einstellungssicherung (Formatversion 2) enthält sämtliche Profile; alte Sicherungen der Version 1 können weiterhin in das aktive Profil geladen werden. Der Einstellungen-Ordner bleibt gemeinsam, Fahrtenordner sind pro Scooter einstellbar. Nach Neuinstallation müssen Zugriffsrechte für externe Ordner neu erteilt werden.

## Separate Speicherorte

**Einstellungen → Speicherorte**: Ordner für Einstellungen und Fahrtenbuch unabhängig mit Androids Ordnerauswahl bestimmen. Einstellungen bleiben lokal als Arbeitsstand und werden automatisch im gewählten Ordner gesichert. Die Datei **Joyor-Cockpit-settings.json** und ihre Formatkennung bleiben für bestehende Sicherungen kompatibel. Eine vorhandene Datei ausdrücklich laden oder überschreiben; Import wird vor Änderungen validiert. Kacheln für beide Ausrichtungen, Farben, Verbindungsparameter und Temperatur-Lerndaten sind enthalten. Android-Ordnerberechtigungen bleiben gerätebezogen; kein ungefragter Import beim Start.

Laufende Fahrten werden lokal gepuffert. Nach Abschluss werden GPX, CSV und JSON in den bei Fahrtbeginn gewählten Fahrtenordner kopiert. Bei Fehlern bleiben lokale Dateien und Kopieraufträge erhalten; erneuter Versuch unter Speicherorte. Lokale GPX-/CSV-Dateien lassen sich über **Letzte Fahrt / Export** teilen. Ordnerwechsel gilt ab der nächsten Fahrt.
