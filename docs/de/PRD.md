# Product Requirements Document: ACCI EAF

* **Version:** 1.1 Draft
* **Datum:** 2025-04-30
* **Autor:** Coding-Assistent (basierend auf User-Input)
* **Status:** Draft

## 1. Einführung & Vision

Dieses Dokument beschreibt die Anforderungen an das **ACCI EAF** (Axians Competence Center Infrastructure Enterprise Application Framework). Das ACCI EAF ist ein internes Software-Framework, das als Grundlage für die Entwicklung robuster, skalierbarer, sicherer, mandantenfähiger und wartbarer Enterprise-Anwendungen dient, die von Axians entwickelt und an Kunden lizenziert werden.

Es kombiniert moderne Technologien (TypeScript, NestJS) mit bewährten Architekturmustern (Hexagonale Architektur, CQRS/Event Sourcing, Multi-Tenancy via RLS, RBAC/ABAC) und Best Practices (Observability, Security by Design, Testbarkeit, i18n, SBOM), um die Entwicklungsgeschwindigkeit zu erhöhen, die Codequalität zu verbessern, die technische Konsistenz sicherzustellen und die Voraussetzungen für Industriestandards und Zertifizierungen (z.B. ISO 27001, SOC2) zu schaffen. Als ORM wird **MikroORM** eingesetzt (siehe ADR-001).

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
* **Gewährleistung der Nachvollziehbarkeit:** Durch Event Sourcing und Vorbereitung für einen dedizierten Audit Trail (Roadmap).
* **Steigerung der Wartbarkeit:** Klare Trennung von Belangen, Modularität, standardisierte Prozesse (ADRs).
* **Ermöglichung von Flexibilität:** Durch ein Plugin-System (mit MikroORM Entity Discovery) und Konfigurierbarkeit (z.B. AuthN-Methoden pro Mandant - Roadmap).
* **Unterstützung des Geschäftsmodells:** Integration eines Mechanismus zur Lizenzvalidierung (hybrid: offline/online).
* **Compliance-Unterstützung:** Bereitstellung von Features und Dokumentation, die bei Zertifizierungsprozessen (ISO 27001, SOC2) helfen.

## 3. Zielgruppe

* **Primär:** Softwareentwicklungsteams bei Axians, die Enterprise-Anwendungen für Kunden entwickeln.
* **Sekundär:** Technische Architekten bei Axians, die Lösungsdesigns entwerfen und technische Standards definieren. Security & Compliance Officer bei Axians.

## 4. Umfang (Scope)

Das ACCI EAF wird phasenweise entwickelt.

### 4.1. Version 1.0 (V1) - Das Fundament

* **Kernarchitektur:** Hexagonale Architektur, CQRS/ES-Basismechanismen, Plugin-System-Grundlagen.
* **Multi-Tenancy:** Kernfunktionalität via Row-Level Security (RLS) mit `tenant_id`-Spalte (umgesetzt über MikroORM Filter - siehe ADR-002, ADR-006); Mandantenkontext-Ermittlung (Token/Header) und -Weitergabe (via `AsyncLocalStorage`).
* **Control Plane API:** Separate API (im Monorepo) für System-Administratoren zum Verwalten von Mandanten (CRUD-Operationen für Basis-Mandantenattribute).
* **Standard-Adapter:** PostgreSQL-Adapter (MikroORM für Event Store Schema & Read Models - siehe ADR-001), Redis-Adapter (für Caching).
* **Internationalisierung (i18n):** Basis-Integration von `nestjs-i18n` für API-Antworten (Fehler, Validierung) und Locale-Ermittlung.
* **Lizenzvalidierung:** Kernmechanismus zur Validierung einer bereitgestellten Lizenz (hybrid: primär offline-fähig via Datei, optional online Check). (Details zur Implementierung TBD, siehe Offene Fragen).
* **Observability Basics:** Hooks/Struktur für strukturiertes Logging; Health Check Endpunkte (`@nestjs/terminus`).
* **Security Basics:** Standard-Security-Header (`helmet`), Rate Limiting (`@nestjs/throttler`), Basis-Authentifizierung (JWT-Validierung, lokale Passwort-Strategie), Basis-Autorisierung (RBAC Core-Logik und Enforcement Guards [Empfehlung: `casl` - siehe ADR-005], Basic Ownership Check für `ownerUserId`), OWASP Top 10 Berücksichtigung im Design.
* **Reliability Basics:** Hooks für Graceful Shutdown.
* **Testing Framework:** Definierte Strategie und Tools (`suites` für Unit-Tests von DI-Komponenten - siehe ADR-004, Testcontainers für Integrationstests mit MikroORM, `@nestjs/testing`+`supertest` für E2E-Tests).
* **API Standards:** Empfehlung und Basiskonfiguration zur Nutzung von OpenAPI.
* **SBOM Generation:** Standardisierter Prozess zur Generierung von Software Bill of Materials (Tool/Format TBD - siehe ADR-TBD).
* **Entwicklungspraktiken:** Monorepo-Setup (Empfehlung: Nx - siehe ADR-003), Nutzung von Architecture Decision Records (ADRs), MikroORM Entity Discovery über Glob-Patterns.
* **Basis-Dokumentation:** Setup-Guide, Architektur-Überblick, Kernkonzepte, ADRs.
* **Projekt-Template:** Basis-Template zur Erstellung neuer Anwendungen mit dem EAF.
* **Deployment-Fokus:** Unterstützung für Offline-Deployment via Tarball (inkl. Docker Images via `docker save`/`load`, `docker-compose.yml`, Skripte) auf Kunden-VMs.

### 4.2. Geplant für spätere Versionen (Roadmap)

* **Advanced Observability:** Metrik-Export (Prometheus), Distributed Tracing (OpenTelemetry).
* **Advanced AuthN:** OIDC-, LDAP/AD-Integration; Mandanten-spezifische AuthN-Konfiguration.
* **Dedizierter Audit Trail:** Separater Service/Modul für unveränderbare Audit Logs.
* **Advanced Reliability:** Retry-Mechanismen, Circuit Breaker Patterns.
* **Advanced Configuration:** Dynamic Configuration, Feature Flag System.
* **Background Job System:** Integration mit Task Queues (z.B. BullMQ).
* **User Groups:** Unterstützung für Benutzergruppen im RBAC/ABAC-Modell.
* **Vollständige ISO 27001 / SOC2 Support-Werkzeuge:** Dedizierte Compliance Reports, vollautomatisierte Kontrollnachweise.
* **CLI Enhancements:** Benutzerdefinierte Schematics für `nest g ...`.
* **Verwaltungs-UIs:** Frontend für die Control Plane (`react-admin` geplant), Frontend für mandantenspezifische RBAC-Verwaltung.
* **Online Lizenzserver:** Betrieb und API-Definition des Axians Lizenzservers für optionale Online-Checks.
* **Erweiterte Plugin-Features:** Detailliertes Interaktionsmodell (Tenancy, RBAC, Licensing), robustere Migrations-Handhabung.
* **Event Schema Evolution:** Implementierung der Upcasting-Pipeline (siehe ADR-010).
* **Idempotency Support:** Bereitstellung eines Framework-Helpers (Decorator/Service) für idempotente Event Handler (siehe ADR-009).

## 5. Funktionale Anforderungen (FR) - V1

*(Hinweis: Detaillierte Aufschlüsselung folgt in separater Spezifikation, hier eine thematische Übersicht mit Verweisen auf Roadmap/ADRs)*

* **Framework Core (FR-CORE):** Monorepo-Struktur, Basis-Bibliotheken, CQRS-Busse, ES-Basis, Plugin-Loader (Basis).
* **Multi-Tenancy (FR-MT):** `tenant_id`-Ermittlung/Kontext, RLS-Implementierung via MikroORM Filters (ADR-002, ADR-006).
* **Control Plane API (FR-CP):** Separate API, Mandanten-CRUD (Basis-Attribute TBD), Admin-AuthZ.
* **Persistence Adapters (FR-PA):** MikroORM Setup für PG (ADR-001) (tenant-aware Entities via Discovery, RLS via Filters), Redis Cache Adapter.
* **Authentifizierung (FR-AUTHN):** AuthN-Modul, JWT-Strategie, Local-Strategie (secure), User-Tenant-Verknüpfung. (Roadmap: OIDC/LDAP, Konfiguration pro Mandant).
* **Autorisierung (FR-AUTHZ):** RBAC-Modul (Empf.: `casl` - ADR-005), Datenmodell (MikroORM Entities, tenant-aware), RBAC-Guards, Ownership-Check (`ownerUserId`), Backend-Services/APIs zur Verwaltung von Rollen/Berechtigungen auf Mandantenebene. (Roadmap: User Groups).
* **Lizenzvalidierung (FR-LIC):** Hybrid-Validierungs-Modul/-Service (Offline-Datei primär, optional Online-Check), Prüfung von Basis-Constraints (Details TBD). Konfiguration für Online-Modus. (Roadmap: Online Lizenzserver).
* **Observability (FR-OBS):** Strukturiertes Logging (Hooks/Interface), Health Checks Endpunkte. (Roadmap: Metrics, Tracing).
* **Security (FR-SEC):** `helmet`-Integration, `throttler`-Integration. (Roadmap: Audit Trail).
* **Internationalisierung (FR-I18N):** `nestjs-i18n`-Setup, Validierungsübersetzung, Fehlerübersetzung (Basis), Service/Kontext-Bereitstellung.
* **API (FR-API):** Standard-Controller-Struktur, DTO-Validierung (i18n), OpenAPI-Setup.
* **SBOM (FR-SBOM):** Integration der SBOM-Generierung (Format/Tool per ADR-TBD) in den Build-Prozess.
* **Deployment (FR-DEPLOY):** CI/CD-Prozess zum Erstellen des Offline-Tarball-Pakets (inkl. `docker save`, Skripten). Bereitstellung von Referenz `docker-compose.yml` und `.env.example`. Mechanismus zur Ausführung von Migrationen im Offline-Setup (ADR-008).

## 6. Nicht-Funktionale Anforderungen (NFR) - V1

| ID     | Kategorie        | Anforderung                                                                                                                                                                                             | Messung/Ziel (Beispiel) / ADR Ref.                      |
| :----- | :--------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | :------------------------------------------------------ |
| NFR-01 | Performance      | API-Antwortzeiten für typische Leseoperationen (Queries) sollen gering sein. Command-Verarbeitung soll effizient sein. RLS (via Filters) soll keine signifikanten Engpässe verursachen.                    | P95 Latenz < 200ms (Queries), Baseline Command Throughput |
| NFR-02 | Skalierbarkeit   | Die Architektur muss horizontale Skalierbarkeit von Lese- und Schreibpfaden ermöglichen (innerhalb der Grenzen einer VM/Compose-Umgebung). Zustandslosigkeit wo möglich.                                | Skalierbarkeitstests unter Last (simuliert)             |
| NFR-03 | Reliability      | Graceful Shutdown muss implementiert sein. Basis-Fehlerbehandlung in Adaptern. Event-Verarbeitung für Projektionen sollte robust sein (Ziel: Idempotenz der Handler - siehe ADR-009). Offline-Update-Prozess soll robust sein. | Tests, Code Reviews, Test des Update-Skripts            |
| NFR-04 | Security         | **Muss Grundlagen für ISO 27001/SOC2 legen.** Implementierung von RLS (via Filters - ADR-002, ADR-006) muss sicher sein. Schutz vor gängigen Web-Angriffen (OWASP Top 10 Berücksichtigung). Sichere AuthN/AuthZ-Basiskomponenten (ADR-005 Empf.). Lizenzvalidierung (offline/online) robust. SBOM verfügbar (ADR-TBD). | Security Reviews, Pentest (später), OWASP Mapping Check   |
| NFR-05 | Maintainability  | Code soll SOLID-Prinzipien folgen, gut dokumentiert sein (Code-Kommentare, ADRs). Hohe Testabdeckung. Klare Modulgrenzen (Hexagonal). Monorepo-Verwaltung (ADR-003 Empf.).                              | Code Coverage > 85% (Core Libs), Statische Code-Analyse   |
| NFR-06 | Testability      | Kernlogik (Domain/Application) muss isoliert testbar sein. Integrationstests müssen zuverlässig sein (Testcontainers + MikroORM). Unit-Tests einfach schreibbar (`suites` - ADR-004).                     | Testpyramide umgesetzt                                    |
| NFR-07 | Extensibility    | Plugin-System muss Erweiterungen ohne Kernänderungen ermöglichen (inkl. Entity Discovery via MikroORM - ADR-001, ADR-008). Architektur soll Austausch von Adaptern erlauben.                               | Beispiel-Plugin-Implementierung, Design Review          |
| NFR-08 | Dokumentation    | Umfassende Dokumentation: Setup (inkl. Offline/Tarball), Architektur, Konzepte (CQRS, ES, Multi-Tenancy, RBAC, Licensing, SBOM, MikroORM UoW/Filters), How-Tos, API-Referenz (Framework-Teile), ADRs.       | Verfügbarkeit & Qualität der Doku                       |
| NFR-09 | Developer Exp.   | Intuitive Nutzung, gute IDE-Unterstützung (TypeScript), klares Fehler-Feedback, einfaches Projekt-Setup (Template), einfache Testbarkeit (`suites`), Monorepo-Tooling (Nx - ADR-003 Empf.), einfache Plugin-Entwicklung. | Entwickler-Feedback                                       |
| NFR-10 | i18n Support     | Framework unterstützt Übersetzung von API-Antworten (Validierung, Fehler) basierend auf Locale via `nestjs-i18n`.                                                                                       | Tests für lokalisierte Antworten                          |
| NFR-11 | Licensing        | Der hybride Validierungsmechanismus muss zuverlässig und sicher gegen einfache Umgehung sein (insb. offline).                                                                                        | Design Review, Tests                                      |
| NFR-12 | Compliance       | Framework muss Generierung von SBOMs in Standardformaten (z.B. CycloneDX) unterstützen (ADR-TBD). Design berücksichtigt Compliance-Anforderungen.                                                    | SBOM-Generierung im Build, Design Reviews                 |
| NFR-13 | Deployment       | Das erzeugte Tarball-Paket muss alle notwendigen Artefakte für eine Offline-Installation enthalten. Setup-/Update-Skripte müssen robust sein.                                                            | Test der Installation/des Updates aus Tarball         |

## 7. Design & Architektur (Übersicht)

* **Kerntechnologien:** TypeScript, Node.js, NestJS.
* **Architekturmuster:** Hexagonale Architektur (Ports & Adapters), CQRS, Event Sourcing, Multi-Tenancy (RLS via MikroORM Filters - ADR-002, ADR-006), RBAC + Basic ABAC (Ownership).
* **Projektstruktur:** Monorepo (Empfehlung: Nx - ADR-003). Klare Trennung in `apps/` und `libs/`.
* **Datenbank:** PostgreSQL (für Event Store & Read Models in V1).
* **ORM:** **MikroORM** (mit Entity Discovery via Glob-Patterns - ADR-001).
* **Cache:** Redis (für Caching in V1).
* **Mandantenkontext:** Token/Header -> `AsyncLocalStorage`.
* **Internationalisierung:** `nestjs-i18n`.
* **Testing:** Jest, `suites` (Unit - ADR-004), Testcontainers (Integration mit MikroORM), `supertest`, `@nestjs/testing`.
* **Autorisierung:** Empfehlung `casl` (ADR-005).
* **SBOM:** Tooling/Format per ADR-TBD.
* **Lizenzierung:** Hybrider Ansatz (Offline-Datei + optional Online-Check).
* **Deployment:** Docker Images in Offline-Tarball mit Docker Compose für VM-Installation.
* **Entscheidungen:** Dokumentiert via ADRs in `docs/adr/`.
* **Diagramme:** Verwendung von geeigneten Diagrammtypen (C4, UML etc.) zur Visualisierung.

## 8. Release-Kriterien (für V1)

* Alle als "In Scope (V1)" definierten funktionalen Anforderungen (FR) sind implementiert und durch Tests abgedeckt.
* Die Kern-NFRs (insbesondere Security, Reliability, Testability, Documentation, Licensing Validation, Deployment) sind nachweislich erfüllt.
* Definierte Code Coverage Ziele sind erreicht.
* Die Basis-Dokumentation (inkl. Offline-Setup) ist verfügbar und prüfbar.
* Ein funktionsfähiges Beispielprojekt (`apps/sample-app`) demonstriert die Kernfeatures und kann aus dem Tarball installiert werden.
* Erfolgreiche Build- und Testläufe aller Komponenten in einer Referenz-CI-Umgebung, inkl. Erstellung des Tarballs.
* Review und Abnahme der Architektur und der Kernkomponenten.

## 9. Erfolgsmetriken

* Adoptionsrate durch Axians-Entwicklungsteams für neue Projekte.
* Reduzierung der initialen Entwicklungszeit für Projekte, die das EAF nutzen (qualitativ/quantitativ).
* Entwicklerzufriedenheit (Umfragen).
* Konsistenz und Qualität der mit dem EAF gebauten Anwendungen (Code Reviews).
* Erfolgreiche Nutzung in Kundenprojekten inkl. Lizenzierung und Offline-Installation.
* Positive Rückmeldungen bezüglich Compliance-/Audit-Unterstützung.

## 10. Offene Fragen & Annahmen

* **Offene Fragen (Auswahl):**
  * **Lizenzierung Details:** Genaue Methode zur Messung von CPU-Kernen? Exakte Validierungslogik (Offline: Dateiformat, Signaturmechanismus? Online: API-Spezifikation des Axians-Servers? Frequenz? Verhalten bei Fehlschlag online/offline?) Sicheres Reporting der Metriken (falls online)? Sicherheitsaspekte? Optionalität? Policy bei Netzwerkfehler (Fail Open/Closed)?
  * **RLS Enforcement (MikroORM Filters):** Best Practices für Konfiguration/Parameterübergabe im NestJS-Kontext? (ADR-006 Detail)
  * **Shared Data Access:** Genaue Methode für Zugriff auf nicht-DB Shared Data (z.B. via Config)?
  * **Control Plane Bootstrapping:** Konkreter Prozess für ersten Admin? (ADR-007 Detail)
  * **Tenant Attributes:** Welche spezifischen Attribute benötigt die Control Plane?
  * **CQRS/ES:** Langfristige Strategie für Event Schema Evolution (Vorschlag ADR-010 akzeptiert?)? Framework-Unterstützung für Idempotenz (Vorschlag ADR-009 akzeptiert?)?
  * **ISO/SOC2:** Welche spezifischen Kontrollen benötigen *direkte* Unterstützung in späteren Versionen?
  * **Plugins:** Genaue Interaktion mit Tenancy/Licensing/RBAC? Wie werden MikroORM-Migrationen für Plugins im Offline-Setup gehandhabt? (ADR-008 Detail)
  * **Technology Choices:** Finale Entscheidung/ADR für RBAC-Bibliothek (`casl` empfohlen - ADR-005)? Finale Entscheidung/ADR für SBOM-Tool/Format?
  * **Deployment:** Genaue Struktur/Inhalt der Setup-/Update-Skripte? Backup/Restore-Strategie?
* **Wichtige Annahmen:**
  * Das EAF stellt Backend-APIs/Logik für Tenant-RBAC-Admin bereit, die UI ist
