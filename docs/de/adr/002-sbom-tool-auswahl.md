# ADR-002: Auswahl von CycloneDX für SBOM-Generierung

* **Status:** Akzeptiert
* **Datum:** 2025-04-25
* **Autoren:** [Sicherheitsarchitekt]

## Kontext

Software Bill of Materials (SBOM) wird zu einem wesentlichen Bestandteil der Softwareentwicklung, insbesondere für Unternehmensanwendungen, die Compliance mit Sicherheitsstandards wie ISO 27001 und SOC2 erfordern. Eine SBOM bietet ein umfassendes Inventar aller Komponenten, Bibliotheken und Module, die in der Software verwendet werden, was wertvoll ist für:

1. Schwachstellenmanagement
2. Lizenz-Compliance
3. Bewertung von Lieferkettenrisiken
4. Einhaltung gesetzlicher Vorschriften
5. Sicherheitsaudits

Das ACCI EAF muss die SBOM-Generierung in seinen Build-Prozess integrieren, um diese Anwendungsfälle zu unterstützen. Es existieren verschiedene SBOM-Standards und Tools:

* **CycloneDX**: Ein Projekt der OWASP Foundation, fokussiert auf Anwendungssicherheit.
* **SPDX**: Entwickelt von der Linux Foundation, stärker auf Lizenz-Compliance ausgerichtet.
* **SWID**: Software Identification Tags, ein internationaler Standard (ISO/IEC 19770-2).
* **Benutzerdefiniertes Format**: Entwicklung eines eigenen SBOM-Formats.

Für die Tooling wurden mehrere Optionen in Betracht gezogen:

* **@cyclonedx/bom**: Offizielles JavaScript/TypeScript SDK für CycloneDX
* **spdx-sbom-generator**: Linux Foundation Tool für SPDX
* **syft**: Ein SBOM-Generator mit Unterstützung für mehrere Formate
* **Eigene Implementierung**: Entwicklung eines eigenen SBOM-Generierungstools

## Entscheidung

Wir werden **CycloneDX** als SBOM-Format und **@cyclonedx/bom** als primäres Tool für die SBOM-Generierung im ACCI EAF verwenden.

## Begründung

CycloneDX wurde aus folgenden Gründen ausgewählt:

1. **Sicherheitsfokus**: CycloneDX wurde speziell mit Blick auf Anwendungssicherheit entwickelt, was mit unseren Sicherheitsanforderungen und Compliance-Zielen übereinstimmt.

2. **Umfang des Formats**: CycloneDX unterstützt alle Komponententypen, die wir dokumentieren müssen (Bibliotheken, Frameworks, Anwendungen) und enthält Felder für Schwachstellen, Lizenzen und andere Metadaten.

3. **JavaScript/TypeScript-Unterstützung**: Die offizielle @cyclonedx/bom-Bibliothek bietet native TypeScript-Unterstützung, die gut zu unserem Technologie-Stack passt.

4. **Aktive Entwicklung**: CycloneDX wird aktiv von der OWASP Foundation gepflegt, mit regelmäßigen Updates und Verbesserungen.

5. **Tool-Ökosystem**: Es gibt ein wachsendes Ökosystem von Tools, die das CycloneDX-Format unterstützen, einschließlich Schwachstellen-Scanner und SBOM-Analyseprogramme.

6. **Verbreitungsgrad**: CycloneDX ist in der Industrie weit verbreitet, besonders in Sektoren, die sich mit Sicherheits-Compliance befassen.

7. **Automatisierungsfähigkeiten**: Die @cyclonedx/bom-Bibliothek ist darauf ausgelegt, leicht in Build-Pipelines integriert zu werden, was unsere CI/CD-Anforderungen unterstützt.

8. **Standards-Compliance**: CycloneDX ist auf verschiedene Standards und Vorschriften im Zusammenhang mit der Sicherheit der Software-Lieferkette ausgerichtet.

Die anderen Optionen wurden aus folgenden Gründen abgelehnt:

* **SPDX**: Obwohl umfassend, liegt der Schwerpunkt stärker auf Lizenz-Compliance als auf Sicherheit, und es hat weniger robuste JavaScript/TypeScript-Tools.
* **SWID**: Geringere Akzeptanz in der modernen Webentwicklung und weniger verfügbare Tools für unseren spezifischen Technologie-Stack.
* **Benutzerdefiniertes Format**: Würde erheblichen Entwicklungsaufwand erfordern und wäre nicht kompatibel mit branchenüblichen Tools und Prozessen.

## Konsequenzen

### Positiv

* Optimierte Compliance mit Sicherheitsstandards und -vorschriften
* Bessere Transparenz bei Abhängigkeiten und potenziellen Schwachstellen
* Integration mit bestehenden Sicherheitstools, die CycloneDX unterstützen
* Reduzierter Aufwand bei der manuellen Bestandsverfolgung
* Verbesserte Sorgfaltspflicht bei Sicherheitsaudits

### Negativ

* Zusätzlicher Build-Zeit-Overhead für die SBOM-Generierung
* Notwendigkeit, die SBOM-Generierung als Teil der CI/CD-Pipeline zu pflegen
* Potenzielle Falschmeldungen in Schwachstellenberichten basierend auf SBOM-Daten
* Notwendigkeit, die SBOM-Tools aktuell zu halten

### Risikominderung

* Optimierung der SBOM-Generierung zur Minimierung der Auswirkungen auf die Build-Zeit
* Implementierung sorgfältiger Prüfprozesse für Schwachstellenberichte
* Automatisierung des Update-Prozesses für SBOM-Tools
* Bereitstellung klarer Dokumentation zur Interpretation und Nutzung von SBOM-Daten

## Implementierungshinweise

* Die SBOM-Generierung wird mit der @cyclonedx/bom-Bibliothek in den Build-Prozess integriert
* SBOMs werden sowohl auf Bibliotheksebene (für `libs/`) als auch auf Anwendungsebene (für `apps/`) generiert
* SBOM-Artefakte werden mit Build-Artefakten für die Rückverfolgbarkeit gespeichert
* Wir werden einen Prozess implementieren, um SBOMs regelmäßig gegen Schwachstellendatenbanken zu validieren
* Es wird eine Dokumentation bereitgestellt, wie SBOM-Daten für Sicherheitszwecke analysiert werden können

## Betrachtete Alternativen

### SPDX mit spdx-sbom-generator

SPDX ist ein etablierter Standard, insbesondere für Lizenz-Compliance. Jedoch:

* Die JavaScript/TypeScript-Tools sind weniger ausgereift im Vergleich zu CycloneDX
* Es ist stärker auf Lizenz-Compliance als auf Sicherheitslücken fokussiert
* Die Integration mit Sicherheitstools ist weniger entwickelt

### Syft

Syft ist ein leistungsstarker SBOM-Generator, der mehrere Formate unterstützt:

* Es ist primär für Container-Images konzipiert und nicht für Source-Code-Projekte
* Es würde eine zusätzliche externe Abhängigkeit einführen
* Es bietet nicht das gleiche Niveau nativer TypeScript-Integration

### Eigene Implementierung

Der Aufbau unseres eigenen SBOM-Generierungstools:

* Würde erheblichen Entwicklungsaufwand erfordern
* Wäre nicht kompatibel mit branchenüblichen Tools
* Würde Wartungsaufwand erzeugen
* Würde die Community-Validierung etablierter Standards vermissen

## Referenzen

* [CycloneDX Spezifikation](https://cyclonedx.org/specification/overview/)
* [CycloneDX JavaScript/TypeScript Bibliothek](https://github.com/CycloneDX/cyclonedx-node-module)
* [SPDX Spezifikation](https://spdx.dev/specifications/)
* [Software Supply Chain Security](https://www.cisa.gov/resources-tools/resources/software-supply-chain-best-practices)
* [ISO/IEC 19770-2 (SWID Tags)](https://www.iso.org/standard/65666.html)
