# Lizenzvalidierung im ACCI EAF

Version: 1.0
Datum: 2025-05-07
Status: Veröffentlicht

## Einleitung

Das ACCI EAF beinhaltet einen Mechanismus zur Lizenzvalidierung, um die Nutzung von Software, die auf dem Framework basiert, zu kontrollieren, Geschäfts_anforderungen_ zu entsprechen und Compliance sicherzustellen. Dieses Dokument beschreibt den konzeptionellen Ansatz zur Lizenzvalidierung in V1, der primär durch ADR-003 geleitet wird.

## Kernziel

Bereitstellung einer zuverlässigen und hinreichend sicheren Methode zur Validierung, dass eine Instanz einer EAF-basierten Anwendung gemäß den Bedingungen ihrer Lizenz verwendet wird.

## Schlüsselkonzepte (ADR-003)

### 1. Hybrider Validierungsansatz

ACCI EAF V1 verwendet eine hybride Lizenzvalidierungsstrategie:

- **Offline-Dateiprüfung (Obligatorisch):**
  - Eine digital signierte Lizenzdatei (z.B. im JSON- oder XML-Format) wird dem Mandanten/Kunden zur Verfügung gestellt.
  - Diese Datei enthält Lizenzberechtigungen und -beschränkungen (z.B. `tenantId`, `expiresAt`, Feature-Flags, `maxCpuCores`).
  - Die Anwendung validiert die Signatur dieser Datei unter Verwendung eines öffentlichen Schlüssels, der in die Anwendung eingebettet ist oder sicher abgerufen wird.
  - Anschließend prüft sie die in der Datei definierten Beschränkungen gegen die aktuelle Umgebung und Nutzung.
  - Dies ist die primäre und obligatorische Prüfung, die sicherstellt, dass die Anwendung in Umgebungen ohne Internetzugang (air-gapped) funktionieren kann.

- **Online-Prüfung (Optional aber empfohlen):**
  - Die Anwendung kann optional eine Verbindung zu einem zentralen Axians Lizenzserver herstellen, um die Lizenz erneut zu validieren oder Aktualisierungen abzurufen.
  - Dies ermöglicht eine dynamischere Lizenzverwaltung, wie z.B. die Fern-Deaktivierung oder Aktualisierungen von Berechtigungen.
  - Die Einzelheiten der Online-Server-API und ihres Betriebs liegen außerhalb des Geltungsbereichs der EAF V1-Entwicklung, das Framework bietet jedoch Hooks für diese Interaktion.
  - Die Richtlinie bei Netzwerkausfällen während einer Online-Prüfung (Fail-Open vs. Fail-Closed) kann je nach Geschäftsanforderungen konfigurierbar sein.

### 2. Lizenzdatei

- **Format:** Typischerweise JSON oder XML, enthält Schlüssel-Wert-Paare für Lizenzattribute.
- **Inhalt:** Gängige Attribute sind:
  - `licenseId`: Eindeutiger Identifikator für die Lizenz.
  - `tenantId`: Der spezifische Mandant, für den diese Lizenz ausgestellt wurde (kritisch für mandantenfähige Setups).
  - `issuedAt`: Datum und Uhrzeit der Lizenzausstellung.
  - `expiresAt`: Ablaufdatum und -uhrzeit der Lizenz. Danach könnte die Anwendung je nach Richtlinie in einem eingeschränkten Modus arbeiten oder nicht mehr funktionieren.
  - `featureFlags`: Eine Liste oder Map von Funktionen, die durch diese Lizenz aktiviert werden.
  - `constraints`: Nutzungsgrenzen, z.B. `maxUsers`, `maxDataVolume`, `maxCpuCores` (die Messmethode für CPU-Kerne bedarf sorgfältiger Überlegung und kann von der Umgebung abhängen).
- **Digitale Signatur:** Die Lizenzdatei ist kryptographisch signiert (z.B. mittels RSA oder ECDSA), um ihre Integrität und Authentizität sicherzustellen. Die Anwendung besitzt den entsprechenden öffentlichen Schlüssel zur Überprüfung der Signatur.

### 3. Validierungslogik (`libs/licensing`)

- Ein dediziertes Modul/Dienst innerhalb von `libs/licensing` kapselt die Logik der Lizenzvalidierung.
- **Validierung beim Start:** Die Lizenzvalidierung wird typischerweise beim Start der Anwendung durchgeführt. Schlägt die Validierung fehl, startet die Anwendung möglicherweise nicht oder arbeitet in einem eingeschränkten Modus.
- **Periodische Neuvalidierung (Optional):** Die Anwendung kann so konfiguriert werden, dass die Lizenz während der Laufzeit periodisch neu validiert wird.
- **Durchsetzung von Beschränkungen:** Der Lizenzierungsdienst stellt Methoden für andere Teile der Anwendung bereit, um zu prüfen, ob bestimmte Funktionen aktiviert sind oder ob Nutzungsgrenzen eingehalten werden.

### 4. Sicherheitsaspekte

- **Manipulationssicherheit:** Die digitale Signatur schützt die Lizenzdatei vor unbefugten Änderungen.
- **Umgehungsprävention:** Die Validierungslogik sollte robust gegen einfache Umgehungsversuche sein. Code-Verschleierung oder andere Härtungstechniken könnten für das Lizenzierungsmodul in hochsensiblen Szenarien in Betracht gezogen werden, obwohl dies kein primärer Fokus für V1 ist.
- **Sichere Schlüsselverwaltung:** Der private Schlüssel zum Signieren von Lizenzen muss streng geheim gehalten werden. Der in die Anwendung eingebettete öffentliche Schlüssel muss vor einfachem Austausch geschützt werden.

## Integration in das Framework

- Der Lizenzierungsdienst kann in andere Dienste injiziert werden, die den Lizenzstatus oder die Aktivierung von Funktionen überprüfen müssen.
- Beispielsweise könnten spezifische Module oder API-Endpunkte basierend auf in der validierten Lizenz vorhandenen Feature-Flags geschützt werden.

## Graceful Degradation / Durchsetzungsrichtlinie

- Das Verhalten der Anwendung bei Lizenzablauf oder Validierungsfehler (z.B. harter Stopp, Nur-Lese-Modus, Warnmeldungen) ist eine Geschäftsentscheidung und sollte konfigurierbar oder klar definiert sein.
- Für V1 ist das primäre Ziel, den Validierungsmechanismus zu etablieren; anspruchsvolle Durchsetzungsrichtlinien können darauf aufgebaut werden.

## Zugehörige ADRs

- **ADR-003: Lizenzvalidierung:** Dies ist das primäre ADR, das den hybriden Ansatz und die Kernattribute der Lizenz definiert.

## Zukünftige Überlegungen

- Anspruchsvollere Online-Interaktionen (z.B. dynamische Feature-Bereitstellung).
- Floating Licenses oder Lizenzmodelle für gleichzeitige Benutzer.
- Detaillierte Nutzungsberichte an den Lizenzserver (falls implementiert).

Dieses Dokument bietet einen konzeptionellen Überblick über die Lizenzvalidierung im ACCI EAF. Für detaillierte Implementierungen siehe `libs/licensing` und ADR-003.
