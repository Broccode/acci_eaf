# ADR-003: Auswahl eines Lizenzvalidierungsmechanismus

* **Status:** Akzeptiert
* **Datum:** 2025-04-26
* **Autoren:** [Compliance-Architekt]

## Kontext

Die ACCI EAF-Plattform basiert auf zahlreichen Open-Source-Bibliotheken und -Komponenten, die unter verschiedenen Lizenzen stehen, darunter MIT, Apache 2.0, ISC, BSD und GPL-Varianten. Die effektive Verwaltung von Open-Source-Lizenzen ist aus mehreren Gründen entscheidend:

1. **Rechtliche Compliance**: Einhaltung der rechtlichen Anforderungen jeder Lizenz, um Urheberrechtsverletzungen und rechtliche Risiken zu vermeiden.
2. **Risikomanagement**: Identifizierung und Minderung potenzieller Lizenzinkompatibilitäten oder restriktiver Bedingungen.
3. **Due Diligence**: Nachweis angemessener Sorgfalt bei der Softwareentwicklung und -verteilung.
4. **Berichtswesen**: Bereitstellung genauer Lizenzdaten für Compliance-Berichte und Audits.

Für Unternehmensanwendungen ist es entscheidend, Bibliotheken mit Lizenzen zu vermeiden, die das Copyleft-Prinzip zu streng umsetzen, wie etwa die GPL für kommerzielle Produkte. Das Framework benötigt einen automatisierten Mechanismus, um:

* Alle Projektabhängigkeiten zu scannen
* Lizenzinformationen aus verschiedenen Quellen zu extrahieren
* Lizenzen anhand vordefinierter Richtlinien zu validieren
* Warnungen bei potenziellen Compliance-Problemen zu generieren
* Die Lizenzdaten mit der SBOM zu verknüpfen

## Entscheidung

Wir werden eine mehrstufige Lizenzvalidierungsstrategie implementieren:

1. **Hauptwerkzeug: license-checker** - Eine Node.js-Bibliothek zur Extraktion von Lizenzinformationen aus npm-Paketen
2. **Sekundäres Werkzeug: licensee** - Ein Ruby-basiertes Tool für tiefgreifendere Lizenzanalysen
3. **Integrierte benutzerdefinierte Validierungslogik** - Eigene Implementierung zur Durchsetzung spezifischer Lizenzrichtlinien des Unternehmens
4. **SPDX-Lizenzidentifier** - Zur standardisierten Benennung und Kategorisierung aller identifizierten Lizenzen
5. **Speicherung der Lizenzinformationen in CycloneDX SBOM** - Zur einheitlichen Darstellung mit anderen Softwarekomponenten

## Begründung

Wir haben uns für diesen Ansatz aufgrund der folgenden Faktoren entschieden:

1. **Umfassende Abdeckung**: Die Kombination von license-checker (für npm) und licensee (für tiefergehende Analyse) bietet eine breite Abdeckung für die Lizenzidentifikation.

2. **Node.js-Kompatibilität**: license-checker ist nativ für die Node.js-Umgebung konzipiert und passt perfekt zu unserem Technologie-Stack.

3. **Flexible Richtliniendurchsetzung**: Die benutzerdefinierte Validierungslogik ermöglicht die Implementierung spezifischer Unternehmensrichtlinien und -präferenzen.

4. **Standardisierte Identifikation**: SPDX-Lizenzidentifier bieten eine einheitliche, branchenübliche Methode zur Lizenzbenennung.

5. **Integration mit SBOM**: Durch die Speicherung der Lizenzinformationen in der CycloneDX SBOM wird ein einheitlicher Ansatz für die Komponentenverfolgung gewährleistet.

6. **Automatisierungsfähigkeit**: Alle ausgewählten Tools unterstützen die Integration in CI/CD-Pipelines für automatisierte Validierung.

7. **Ausgereiftheit der Tools**: Sowohl license-checker als auch licensee sind etablierte Tools mit aktiver Wartung und breiter Community-Unterstützung.

8. **Fehlertoleranz**: Der mehrstufige Ansatz bietet Redundanz, falls ein Tool bei der Identifizierung bestimmter Lizenzen versagt.

## Konsequenzen

### Positiv

* Verbesserte Lizenz-Compliance durch automatisierte Validierung
* Frühzeitige Erkennung potenzieller Lizenzprobleme im Entwicklungszyklus
* Detaillierte Lizenzdokumentation für Audit- und Compliance-Zwecke
* Standardisierte Lizenzkategorisierung über SPDX
* Geringeres rechtliches Risiko durch proaktives Lizenzmanagement

### Negativ

* Erhöhte Komplexität in der Build-Pipeline durch mehrere Validierungstools
* Potenzielle falsch-positive Ergebnisse, die manuelle Überprüfung erfordern
* Leichter Leistungsoverhead während des Build-Prozesses
* Abhängigkeit von der Genauigkeit der Lizenzinformationen in Paketmetadaten
* Notwendigkeit, mit Lizenzen umzugehen, die nicht eindeutig identifiziert werden können

### Risikominderung

* Implementierung eines Override-Mechanismus für bekannte falsch-positive Ergebnisse
* Caching von Lizenzdaten, um wiederholte Analysen zu vermeiden
* Regelmäßige Aktualisierung der Lizenzvalidierungstools
* Dokumentierte Verfahren für die manuelle Überprüfung unklarer Lizenzen
* Schulung des Entwicklungsteams zu Lizenz-Compliance-Grundsätzen

## Implementierungshinweise

Die Implementation erfolgt in drei Phasen:

1. **Phase 1: Basisanalyse**
   * Integration von license-checker in Build-Skripte
   * Entwicklung einer Konfigurationsdatei für akzeptierte/abgelehnte Lizenzen
   * Implementierung der Basis-Validierungslogik

2. **Phase 2: Erweiterte Validierung**
   * Integration von licensee für tiefergehende Analyse
   * SPDX-Mapping für alle identifizierten Lizenzen
   * Verknüpfung mit CycloneDX SBOM

3. **Phase 3: Prozessintegration**
   * Einrichtung von Pre-Commit-Hooks für frühzeitige Warnung
   * Implementierung eines Dashboard für Lizenz-Compliance
   * Automatisierte Berichterstellung für Compliance-Zwecke

**Technische Details:**

* license-checker wird als npm-Skript implementiert
* licensee wird über Docker ausgeführt, um Ruby-Abhängigkeiten zu isolieren
* SPDX-Parsing wird mit der SPDX-Tools-Bibliothek implementiert
* Benutzerdefinierte Validierungslogik wird als Node.js-Modul implementiert
* Ein Caching-Layer wird hinzugefügt, um wiederholte Analysen zu vermeiden

## Betrachtete Alternativen

### Alleinige Verwendung von license-checker

* **Vorteile**: Einfachere Implementierung, geringere Komplexität
* **Nachteile**: Begrenzte Tiefe der Analyse, Abhängigkeit von npm-Metadaten

### Alleinige Verwendung von licensee

* **Vorteile**: Tiefere Analyse von Lizenztexten, bessere Erkennung unklarer Lizenzen
* **Nachteile**: Erforderliche Ruby-Umgebung, weniger nahtlose npm-Integration

### FOSSA oder andere kommerzielle Lösungen

* **Vorteile**: Umfassendere Funktionen, professioneller Support
* **Nachteile**: Kosten, potenzielle Vendor-Lock-ins, möglicherweise überflüssige Funktionen

### Manuelle Lizenzüberprüfung

* **Vorteile**: Höchste Genauigkeit für kritische Komponenten
* **Nachteile**: Nicht skalierbar, zeitaufwändig, fehleranfällig

### Benutzerdefinierte Implementierung von Grund auf

* **Vorteile**: Maximale Kontrolle über die Validierungslogik
* **Nachteile**: Hoher Entwicklungsaufwand, keine Community-Unterstützung

## Referenzen

* [license-checker auf GitHub](https://github.com/davglass/license-checker)
* [licensee auf GitHub](https://github.com/licensee/licensee)
* [SPDX-Lizenzliste](https://spdx.org/licenses/)
* [OpenChain-Spezifikation für Open-Source-Compliance](https://www.openchainproject.org/specification)
* [NIST-Richtlinien zum Open-Source-Risikomanagement](https://www.nist.gov/itl/ssd/software-quality-group/source-code-security-analyzers/secure-software-development-0)
* [CycloneDX-Lizenzkomponente](https://cyclonedx.org/capabilities/license-guidance/)
