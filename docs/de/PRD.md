# Product Requirements Document: ACCI EAF

* **Version:** 1.0 Draft
* **Datum:** 2025-04-20
* **Autor:** Coding-Assistent (basierend auf User-Input)
* **Status:** Draft

## 1. Einführung & Vision

Dieses Dokument beschreibt die Anforderungen an das **ACCI EAF** (Axians Competence Center Infrastructure Enterprise Application Framework). Das ACCI EAF ist ein internes Software-Framework, das als Grundlage für die Entwicklung robuster, skalierbarer, sicherer, mandantenfähiger und wartbarer Enterprise-Anwendungen dient, die von Axians entwickelt und an Kunden lizenziert werden.

Es kombiniert moderne Technologien (TypeScript, NestJS) mit bewährten Architekturmustern (Hexagonale Architektur, CQRS/Event Sourcing, Multi-Tenancy via RLS, RBAC/ABAC) und Best Practices (Observability, Security by Design, Testbarkeit, i18n, SBOM), um die Entwicklungsgeschwindigkeit zu erhöhen, die Codequalität zu verbessern, die technische Konsistenz sicherzustellen und die Voraussetzungen für Industriestandards und Zertifizierungen (z.B. ISO 27001, SOC2) zu schaffen. Als ORM wird **MikroORM** eingesetzt.

## 2. Ziele & Motivation

Die Entwicklung von Enterprise-Anwendungen für Kunden stellt hohe Anforderungen an Sicherheit, Skalierbarkeit, Anpassbarkeit und Wartbarkeit. Häufige Herausforderungen sind:

* Inkonsistente Architekturen und Sicherheitsniveaus.
* Hoher Entwicklungsaufwand für Basisfunktionalitäten (Mandantenfähigkeit, AuthN/AuthZ, Auditing, Lizenzierung).
* Schwierigkeiten bei der Skalierung, Wartung und Weiterentwicklung.
* Mangelnde Testbarkeit und Beobachtbarkeit (Observability).
* Fehlende Nachvollziehbarkeit von Datenänderungen und Aktionen.
* Notwendigkeit der Unterstützung eines Lizenzmodells.
* Notwendigkeit, Compliance-Anforderungen (ISO 27001, SOC2) zu unterstützen.

Das ACCI EAF zielt darauf ab, diese Herausforderungen zu adressieren durch:

* **Beschleunigung der Entwicklung:** Bereitstellung einer soliden Grundlage, wiederverwendbarer Komponenten und klarer Muster.
* **Förderung von Best Practices:** Implementierung von Hexagonaler Architektur, CQRS/ES, Multi-Tenancy, RBAC/ABAC, i18n, SBOM, Security by Design als Standard.
* **Verbesserung der Qualität:** Hohe Testabdeckung durch definierte Strategien (Unit/Integration/E2E) und Tools (`suites`, Testcontainers, MikroORM Testing-Features).
* **Erhöhung der Skalierbarkeit:** Design für horizontale Skalierbarkeit durch CQRS und zustandslose Komponenten wo möglich.
* **Gewährleistung der Nachvollziehbarkeit:** Durch Event Sourcing und Vorbereitung für einen dedizierten Audit Trail.
* **Steigerung der Wartbarkeit:** Klare Trennung von Belangen, Modularität, standardisierte Prozesse (ADRs).
* **Ermöglichung von Flexibilität:** Durch ein Plugin-System (mit MikroORM Entity Discovery) und Konfigurierbarkeit (z.B. AuthN-Methoden pro Mandant).
* **Unterstützung des Geschäftsmodells:** Integration eines Mechanismus zur Lizenzvalidierung.
* **Compliance-Unterstützung:** Bereitstellung von Features und Dokumentation, die bei Zertifizierungsprozessen (ISO 27001, SOC2) helfen.

## 3. Zielgruppe

* **Primär:** Softwareentwicklungsteams bei Axians, die Enterprise-Anwendungen für Kunden entwickeln.
* **Sekundär:** Technische Architekten bei Axians, die Lösungsdesigns entwerfen und technische Standards definieren. Security & Compliance Officer bei Axians.

## 4. Umfang (Scope)

Das ACCI EAF wird phasenweise entwickelt.

### 4.1. Version 1.0 (V1) - Das Fundament

* **In Scope (V1):**
  * **Kernarchitektur:** Hexagonale Architektur, CQRS/ES-Basismechanismen, Plugin-System-Grundlagen.
  * **Multi-Tenancy:** Kernfunktionalität via Row-Level Security (RLS) mit `tenant_id`-Spalte (umgesetzt über MikroORM Filter); Mandantenkontext-Ermittlung (Token/Header) und -Weitergabe (via `AsyncLocalStorage`).
  * **Control Plane API:** Separate API (im Monorepo) für System-Administratoren zum Verwalten von Mandanten (CRUD-Operationen für Basis-Mandantenattribute).
  * **Standard-Adapter:** PostgreSQL-Adapter (MikroORM für Event Store Schema & Read Models), Redis-Adapter (für Caching).
  * **Internationalisierung (i18n):** Basis-Integration von `nestjs-i18n` für API-Antworten (Fehler, Validierung) und Locale-Ermittlung.
  * **Lizenzvalidierung:** Kernmechanismus zur Validierung einer bereitgestellten Lizenz (Details zur Implementierung TBD, siehe Offene Fragen).
  * **Observability Basics:** Hooks/Struktur für strukturiertes Logging; Health Check Endpunkte (`@nestjs/terminus`).
  * **Security Basics:** Standard-Security-Header (`helmet`), Rate Limiting (`@nestjs/throttler`), Basis-Authentifizierung (JWT-Validierung, lokale Passwort-Strategie), Basis-Autorisierung (RBAC Core-Logik und Enforcement Guards [Empfehlung: `casl`], Basic Ownership Check für `ownerUserId`), OWASP Top 10 Berücksichtigung im Design.
  * **Reliability Basics:** Hooks für Graceful Shutdown.
  * **Testing Framework:** Definierte Strategie und Tools (`suites` für Unit-Tests von DI-Komponenten, Testcontainers für Integrationstests mit MikroORM, `@nestjs/testing`+`supertest` für E2E-Tests).
  * **API Standards:** Empfehlung und Basiskonfiguration zur Nutzung von OpenAPI.
  * **SBOM Generation:** Standardisierter Prozess zur Generierung von Software Bill of Materials (Tooling z.B. `@cyclonedx/bom`, Format z.B. CycloneDX).
  * **Entwicklungspraktiken:** Monorepo-Setup (Empfehlung: Nx), Nutzung von Architecture Decision Records (ADRs), MikroORM Entity Discovery über Glob-Patterns.
  * **Basis-Dokumentation:** Setup-Guide, Architektur-Überblick, Kernkonzepte, ADRs.
  * **Projekt-Template:** Basis-Template zur Erstellung neuer Anwendungen mit dem EAF.

* **Out of Scope (für V1):**
  * Anwendungsspezifische Geschäftslogik oder Domänenimplementierungen.
  * UI Frameworks, Frontend für die Control Plane, Frontend für mandantenspezifische RBAC-Verwaltung.
  * Vollständige, produktionsreife CI/CD-Pipeline-Vorlagen.
  * Umfangreiche Bibliothek von vorgefertigten Plugins.
  * **Advanced Observability:** Metrik-Export (Prometheus), Distributed Tracing (OpenTelemetry).
  * **Advanced AuthN:** OIDC-, LDAP/AD-Integration.
  * **Dedizierter Audit Trail:** Separater Service/Modul für unveränderbare Audit Logs.
  * **Advanced Reliability:** Retry-Mechanismen, Circuit Breaker Patterns.
  * **Advanced Configuration:** Dynamic Configuration, Feature Flag System.
  * **Background Job System:** Integration mit Task Queues (z.B. BullMQ).
  * **User Groups:** Unterstützung für Benutzergruppen im RBAC/ABAC-Modell.
  * **Vollständige ISO 27001 / SOC2 Support-Werkzeuge:** Dedizierte Compliance Reports, vollautomatisierte Kontrollnachweise (V1 *ermöglicht* dies).
  * **CLI Enhancements:** Benutzerdefinierte Schematics für `nest g ...`.
  * **Verwaltungs-UIs:** Für Control Plane oder mandantenspezifische Administration (z.B. RBAC).

### 4.2. Zukünftige Versionen (Roadmap)

* Implementierung der unter "Out of Scope (for V1)" genannten Features basierend auf Priorisierung und Bedarf.
* Vertiefung der Unterstützung für Zertifizierungen.
* Performance-Optimierungen.
* Erweiterung der Plugin-Möglichkeiten.
* Unterstützung weiterer Datenbanken/ Technologien (optional).

## 5. Funktionale Anforderungen (FR) - V1

*(Hinweis: Detaillierte Aufschlüsselung folgt in separater Spezifikation, hier eine thematische Übersicht)*

* **Framework Core (FR-CORE):** Monorepo-Struktur, Basis-Bibliotheken, CQRS-Busse, ES-Basis, Plugin-Loader.
* **Multi-Tenancy (FR-MT):** `tenant_id`-Ermittlung/Kontext, RLS-Implementierung via MikroORM Filters.
* **Control Plane API (FR-CP):** Separate API, Mandanten-CRUD, Admin-AuthZ.
* **Persistence Adapters (FR-PA):** MikroORM Setup für PG (tenant-aware Entities via Discovery, RLS via Filters), Redis Cache Adapter.
* **Authentifizierung (FR-AUTHN):** AuthN-Modul, JWT-Strategie, Local-Strategie (secure), User-Tenant-Verknüpfung.
* **Autorisierung (FR-AUTHZ):** RBAC-Modul (Empf.: `casl`), Datenmodell (MikroORM Entities, tenant-aware), RBAC-Guards, Ownership-Check (`ownerUserId`), Tenant-Admin-APIs/Services für RBAC.
* **Lizenzvalidierung (FR-LIC):** Validierungs-Modul/-Service, Prüfung von Constraints (Details TBD).
* **Observability (FR-OBS):** Strukturiertes Logging (Hooks/Interface), Health Checks Endpunkte.
* **Security (FR-SEC):** `helmet`-Integration, `throttler`-Integration.
* **Internationalisierung (FR-I18N):** `nestjs-i18n`-Setup, Validierungsübersetzung, Fehlerübersetzung (Basis), Service/Kontext-Bereitstellung.
* **API (FR-API):** Standard-Controller-Struktur, DTO-Validierung (i18n), OpenAPI-Setup.
* **SBOM (FR-SBOM):** Integration der SBOM-Generierung (z.B. CycloneDX) in den Build-Prozess.

## 6. Nicht-Funktionale Anforderungen (NFR) - V1

| ID     | Kategorie        | Anforderung                                                                                                                                                                                             | Messung/Ziel (Beispiel)                                   |
| :----- | :--------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | :-------------------------------------------------------- |
| NFR-01 | Performance      | API-Antwortzeiten für typische Leseoperationen (Queries) sollen gering sein. Command-Verarbeitung soll effizient sein. RLS (via Filters) soll keine signifikanten Engpässe verursachen.                    | P95 Latenz < 200ms (Queries), Baseline Command Throughput |
| NFR-02 | Skalierbarkeit   | Die Architektur muss horizontale Skalierbarkeit von Lese- und Schreibpfaden ermöglichen. Zustandslosigkeit wo möglich.                                                                                    | Skalierbarkeitstests unter Last                     |
| NFR-03 | Reliability      | Graceful Shutdown muss implementiert sein. Basis-Fehlerbehandlung in Adaptern. Event-Verarbeitung für Projektionen sollte robust sein (Ziel: Idempotenz der Handler).                                     | Tests, Code Reviews                                       |
| NFR-04 | Security         | **Muss Grundlagen für ISO 27001/SOC2 legen.** Implementierung von RLS (via Filters) muss sicher sein. Schutz vor gängigen Web-Angriffen (OWASP Top 10 Berücksichtigung). Sichere AuthN/AuthZ-Basiskomponenten. Lizenzvalidierung robust. SBOM verfügbar. | Security Reviews, Pentest (später), OWASP Mapping Check |
| NFR-05 | Maintainability  | Code soll SOLID-Prinzipien folgen, gut dokumentiert sein (Code-Kommentare, ADRs). Hohe Testabdeckung. Klare Modulgrenzen (Hexagonal).                                                                  | Code Coverage > 85% (Core Libs), Statische Code-Analyse |
| NFR-06 | Testability      | Kernlogik (Domain/Application) muss isoliert testbar sein. Integrationstests müssen zuverlässig sein (Testcontainers + MikroORM). Unit-Tests einfach schreibbar (`suites`).                               | Testpyramide umgesetzt                                  |
| NFR-07 | Extensibility    | Plugin-System muss Erweiterungen ohne Kernänderungen ermöglichen (inkl. Entity Discovery). Architektur soll Austausch von Adaptern erlauben.                                                            | Beispiel-Plugin-Implementierung, Design Review        |
| NFR-08 | Dokumentation    | Umfassende Dokumentation: Setup, Architektur, Konzepte (CQRS, ES, Multi-Tenancy, RBAC, Licensing, SBOM, MikroORM UoW/Filters), How-Tos, API-Referenz (Framework-Teile), ADRs.                              | Verfügbarkeit & Qualität der Doku                       |
| NFR-09 | Developer Exp.   | Intuitive Nutzung, gute IDE-Unterstützung (TypeScript), klares Fehler-Feedback, einfaches Projekt-Setup (Template), einfache Testbarkeit (`suites`), Monorepo-Tooling (Nx), einfache Plugin-Entwicklung. | Entwickler-Feedback                                     |
| NFR-10 | i18n Support     | Framework unterstützt Übersetzung von API-Antworten (Validierung, Fehler) basierend auf Locale via `nestjs-i18n`.                                                                                       | Tests für lokalisierte Antworten                        |
| NFR-11 | Licensing        | Der Validierungsmechanismus muss zuverlässig und sicher gegen einfache Umgehung sein.                                                                                                                | Design Review, Tests                                      |
| NFR-12 | Compliance       | Framework muss Generierung von SBOMs in Standardformaten (z.B. CycloneDX) unterstützen. Design berücksichtigt Compliance-Anforderungen.                                                              | SBOM-Generierung im Build, Design Reviews               |

## 7. Design & Architektur (Übersicht)

* **Kerntechnologien:** TypeScript, Node.js, NestJS.
* **Architekturmuster:** Hexagonale Architektur (Ports & Adapters), CQRS, Event Sourcing, Multi-Tenancy (RLS via MikroORM Filters), RBAC + Basic ABAC (Ownership).
* **Projektstruktur:** Monorepo (Empfehlung: Nx). Klare Trennung in `apps/` und `libs/`.
* **Datenbank:** PostgreSQL (für Event Store & Read Models in V1).
* **ORM:** **MikroORM** (mit Entity Discovery via Glob-Patterns).
* **Cache:** Redis (für Caching in V1).
* **Mandantenkontext:** Token/Header -> `AsyncLocalStorage`.
* **Internationalisierung:** `nestjs-i18n`.
* **Testing:** Jest, `suites` (Unit), Testcontainers (Integration mit MikroORM), `supertest`, `@nestjs/testing`.
* **Autorisierung:** Empfehlung `casl`.
* **SBOM:** Tooling TBD (z.B. `@cyclonedx/bom`), Format TBD (z.B. CycloneDX).
* **Entscheidungen:** Dokumentiert via ADRs in `docs/adr/`.
* **Diagramme:** Verwendung von geeigneten Diagrammtypen (C4, UML etc.) zur Visualisierung.

## 8. Release-Kriterien (für V1)

* Alle als "In Scope (V1)" definierten funktionalen Anforderungen (FR) sind implementiert und durch Tests abgedeckt.
* Die Kern-NFRs (insbesondere Security, Reliability, Testability, Documentation, Licensing Validation) sind nachweislich erfüllt.
* Definierte Code Coverage Ziele sind erreicht.
* Die Basis-Dokumentation ist verfügbar und prüfbar.
* Ein funktionsfähiges Beispielprojekt (`apps/sample-app`) demonstriert die Kernfeatures.
* Erfolgreiche Build- und Testläufe aller Komponenten in einer Referenz-CI-Umgebung.
* Review und Abnahme der Architektur und der Kernkomponenten.

## 9. Erfolgsmetriken

* Adoptionsrate durch Axians-Entwicklungsteams für neue Projekte.
* Reduzierung der initialen Entwicklungszeit für Projekte, die das EAF nutzen (qualitativ/quantitativ).
* Entwicklerzufriedenheit (Umfragen).
* Konsistenz und Qualität der mit dem EAF gebauten Anwendungen (Code Reviews).
* Erfolgreiche Nutzung in Kundenprojekten inkl. Lizenzierung.
* Positive Rückmeldungen bezüglich Compliance-/Audit-Unterstützung.

## 10. Offene Fragen & Annahmen

* **Lizenzierung Details:**
  * Genaue Methode zur Messung von CPU-Kernen?
  * Exakte Validierungslogik (Online/Offline? Frequenz? Verhalten bei Fehlschlag?)
  * Sicheres Reporting der Metriken?
  * Sicherheitsaspekte des Lizenzierungsmechanismus? Ist er optional für nicht-lizenzierte Produkte?
* **RLS Enforcement (MikroORM Filters):** Best Practices für die Konfiguration und dynamische Parameterübergabe (`tenant_id`) an globale Filter im NestJS-Kontext?
* **Shared Data Access:** Genaue Spezifikation, wo mandantenübergreifende Daten liegen und wie der Zugriff standardisiert erfolgen soll (z.B. via Config)?
* **Control Plane Bootstrapping:** Prozess für ersten Mandanten / ersten Admin?
* **Tenant Attributes:** Welche spezifischen Attribute (über Name/ID hinaus) benötigt die Control Plane?
* **CQRS/ES:**
  * Langfristige Strategie für Event Schema Evolution (Vorschlag für V1 notwendig?).
  * Framework-Unterstützung für idempotente Event Handler?
* **ISO/SOC2:** Welche spezifischen Kontrollen benötigen *direkte* Unterstützung durch Framework-Features in späteren Versionen?
* **Plugins:** Genaue Interaktion mit Tenancy/Licensing/RBAC? Wie werden MikroORM-Entities aus Plugins sicher erkannt und Migrationen verwaltet?
* **Technology Choices:**
  * Konkrete Wahl der RBAC-Bibliothek (`casl` empfohlen)? -> ADR erforderlich.
  * Konkrete Wahl des SBOM-Tools und -Formats? -> ADR erforderlich.
* **Annahme:** Das EAF stellt Backend-APIs/Logik für Tenant-RBAC-Admin bereit, die UI ist anwendungsspezifisch.
* **Annahme:** V1 fokussiert sich auf das Fundament, erweiterte Enterprise-Features folgen iterativ gemäß Roadmap.
* **Annahme:** Die Implementierung der RLS erfolgt über eine `tenant_id`-Spalte in relevanten Tabellen und wird über MikroORM Filters durchgesetzt.
* **Annahme:** Die Entity Discovery von MikroORM wird über Glob-Patterns konfiguriert.

## 11. Glossar (Optional)

* **ACCI EAF:** Axians Competence Center Infrastructure Enterprise Application Framework.
* **RLS:** Row-Level Security. Datenzugriffsbeschränkung auf Zeilenebene, hier durch `tenant_id`.
* **RBAC:** Role-Based Access Control. Zugriff basierend auf Benutzerrollen.
* **ABAC:** Attribute-Based Access Control. Zugriff basierend auf Attributen. Hier speziell: Ownership.
* **Control Plane:** Separate API zur zentralen Verwaltung von Mandanten durch System-Administratoren.
* **Tenant Context:** Information, die den aktuellen Mandanten für eine Operation identifiziert.
* **ADR:** Architecture Decision Record. Dokumentation von wichtigen Architekturentscheidungen.
* **CQRS:** Command Query Responsibility Segregation. Trennung von Lese- und Schreiboperationen.
* **ES:** Event Sourcing. Speicherung aller Zustandsänderungen als Sequenz von Events.
* **i18n:** Internationalisierung. Anpassung der Software an verschiedene Sprachen und Regionen.
* **SBOM:** Software Bill of Materials. Inventarliste der Softwarekomponenten und Abhängigkeiten.
* **ORM:** Object-Relational Mapper. Hier: MikroORM.
* **UoW:** Unit of Work. Ein Design Pattern (von MikroORM genutzt) zur Verwaltung von Objektänderungen und Transaktionen.
