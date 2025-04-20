# ACCI EAF - Architekturübersicht

* **Version:** 1.0 Draft
* **Datum:** 2025-04-20
* **Status:** Draft

## 1. Einleitung

Dieses Dokument beschreibt die High-Level-Architektur des **ACCI EAF** (Axians Competence Center Infrastructure Enterprise Application Framework). Es erläutert die gewählten Architekturmuster, die Kernkomponenten, den Technologie-Stack und die grundlegenden Designentscheidungen. Dieses Dokument dient als technischer Leitfaden für Entwickler und Architekten, die das Framework nutzen oder weiterentwickeln.

Detaillierte funktionale und nicht-funktionale Anforderungen sind im separaten Product Requirements Document (PRD) zu finden. Wichtige Designentscheidungen werden zudem in Architecture Decision Records (ADRs) im Verzeichnis `docs/adr/` festgehalten.

## 2. Architektur-Treiber

Die Architektur des ACCI EAF wird maßgeblich durch folgende Kernanforderungen aus dem PRD bestimmt:

* **Multi-Tenancy:** Notwendigkeit, mehrere Mandanten sicher und isoliert innerhalb einer Anwendungsinstanz zu verwalten (RLS-Ansatz).
* **CQRS/ES:** Trennung von Lese- und Schreibpfaden sowie Event Sourcing zur Nachvollziehbarkeit und als Grundlage für Projektionen.
* **Hexagonale Architektur:** Strikte Trennung von Kernlogik (Domain, Application) und Infrastruktur (Frameworks, DB, externe Services) für Testbarkeit und Wartbarkeit.
* **Security & Compliance:** Hohe Sicherheitsanforderungen, Unterstützung für Zertifizierungen (ISO 27001, SOC2), robustes AuthN/AuthZ (RBAC/ABAC), Audit Trail (zukünftig), SBOM.
* **Lizenzierung:** Unterstützung des Axians-Geschäftsmodells durch integrierte Lizenzvalidierung.
* **Extensibility:** Ermöglichung der Erweiterung des Frameworks und darauf basierender Anwendungen durch ein Plugin-System.
* **Enterprise Readiness:** Bereitstellung von Features für Observability, Reliability, i18n, Konfigurationsmanagement etc.

## 3. Gesamtstruktur (Monorepo)

Das ACCI EAF wird als **Monorepo** entwickelt und verwaltet, um Code-Sharing, zentrale Abhängigkeitsverwaltung und konsistente Entwicklungsprozesse zu fördern. Die Verwendung von **Nx** als Monorepo-Management-Tool wird empfohlen. Die typische Struktur umfasst:

* **`apps/`**: Enthält eigenständig deploybare Anwendungen.
  * `control-plane-api`: Die API für das zentrale Mandanten-Management.
  * `sample-app`: Eine Beispielanwendung, die das EAF nutzt.
* **`libs/`**: Enthält wiederverwendbare Bibliotheken, die den Kern des EAF und gemeinsam genutzte Funktionalität bilden.
  * Framework-Kernbibliotheken (z.B. `core`, `infrastructure`, `tenancy`, `rbac`, `licensing`, `plugins`, `testing`).
  * Anwendungsspezifische Bibliotheken für die `sample-app`.

## 4. Kernarchitekturmuster

Das ACCI EAF basiert auf einer Kombination etablierter Architekturmuster:

### 4.1. Hexagonale Architektur (Ports & Adapters)

* **Ziel:** Isolation der Kernlogik von externen Einflüssen.
* **Schichten:**
  * **Core (`libs/core`):** Enthält die technologie-agnostische Geschäfts- und Anwendungslogik.
    * `domain`: Aggregate, Entities, Value Objects, Domain Events, Repository-*Interfaces* (Ports). Enthält keine Abhängigkeiten zu externen Frameworks.
    * `application`: Use Cases, Command/Query Handlers, Application Services, DTOs, Port-*Interfaces* für Infrastruktur. Hängt nur von `domain` ab.
  * **Infrastructure (`libs/infrastructure`, `apps/*`):** Implementiert die Ports aus dem Core mithilfe spezifischer Technologien (NestJS, MikroORM, Redis etc.). Enthält die Adapter (z.B. REST-Controller, DB-Repositories, Cache-Clients). Hängt vom `core` ab.
* **Abhängigkeitsregel:** Abhängigkeiten zeigen immer von außen nach innen (Infrastructure -> Application -> Domain).

### 4.2. CQRS/ES (Command Query Responsibility Segregation / Event Sourcing)

* **Ziel:** Trennung von Schreib- (Commands) und Leseoperationen (Queries); Nutzung von Events als primäre Datenquelle.
* **Command Flow:** HTTP Request -> NestJS Controller -> `CommandBus` -> Command Handler (lädt Aggregat via Repository) -> Aggregat (validiert, erzeugt Domain Event) -> Event Store Adapter (speichert Event) -> `EventBus`.
* **Event Flow:** `EventBus` (oder Polling des Stores) -> Event Handler / Projector (aktualisiert Read Model).
* **Query Flow:** HTTP Request -> NestJS Controller -> `QueryBus` -> Query Handler -> Read Model Repository Adapter (liest aus optimierter Read Model DB) -> Response.
* **Komponenten:** Commands, Queries, Events (Domain/Integration), Command/Query/Event Handlers, Aggregate Roots, Event Store, Read Models (optimierte Datenbankstrukturen), Projektoren.

### 4.3. Multi-Tenancy (Row-Level Security)

* **Ziel:** Sichere Datentrennung verschiedener Mandanten innerhalb derselben Datenbank.
* **Ansatz:** Verwendung einer `tenant_id`-Spalte in allen mandantenfähigen Tabellen.
* **Kontext:** Der aktuelle `tenant_id` wird aus dem Request (Token/Header) extrahiert und mittels `AsyncLocalStorage` im Request-Kontext verfügbar gemacht (`libs/tenancy`).
* **Durchsetzung:** **MikroORM Filters** werden verwendet, um den `WHERE tenant_id = ?`-Filter automatisch und global auf alle Abfragen für mandantenfähige Entitäten anzuwenden. Die Filter werden mit dem `tenant_id` aus dem `AsyncLocalStorage` parametrisiert.

### 4.4. Plugin-System

* **Ziel:** Erweiterbarkeit des EAF und darauf basierender Anwendungen.
* **Ansatz:** Definition eines Plugin-Interfaces und eines Mechanismus zum Laden und Registrieren von Plugins.
* **Entity Discovery:** Nutzt die Fähigkeit von **MikroORM**, Entitäten über Glob-Patterns zu finden, um Plugins die Definition eigener Datenbank-Entitäten zu ermöglichen, die zentral verwaltet werden.
* **Interaktion:** Interaktion von Plugins mit Tenancy, RBAC, Licensing muss im Design berücksichtigt werden (teilweise TBD).

## 5. Technologie-Stack (V1 Auswahl)

* **Sprache/Runtime:** TypeScript, Node.js
* **Backend Framework:** NestJS
* **Datenbank:** PostgreSQL
* **ORM:** MikroORM (mit Glob-Pattern Entity Discovery)
* **Cache:** Redis
* **Testing:** Jest, `suites` (Unit), Testcontainers (Integration), `supertest`, `@nestjs/testing`
* **Autorisierung:** Empfehlung: `casl`
* **Internationalisierung:** `nestjs-i18n`
* **Security Middleware:** `helmet`, `@nestjs/throttler`
* **Observability:** `@nestjs/terminus` (Health Checks), Strukturiertes Logging (Implementierung TBD)
* **API Dokumentation:** OpenAPI (Integration via NestJS)
* **Monorepo Management:** Empfehlung: Nx
* **SBOM:** Tooling TBD (z.B. `@cyclonedx/bom`), Format TBD (z.B. CycloneDX)

*(Begründungen für spezifische Entscheidungen sind oder werden in ADRs dokumentiert)*

## 6. Komponentenübersicht (Geplant)

* **`libs/`:**
  * `core`: Domain-Logik, Application Use Cases & Ports.
  * `infrastructure`: Adapter (Persistence [MikroORM/PG], Cache [Redis], Web [NestJS], i18n Setup etc.).
  * `tenancy`: Mandantenkontext-Handling (AsyncLocalStorage, Middleware).
  * `rbac`: RBAC/ABAC Kernlogik, Guards (Empf.: `casl`-Integration).
  * `licensing`: Kernlogik zur Lizenzvalidierung.
  * `plugins`: Plugin-Interface und Lade-Mechanismus.
  * `testing`: Gemeinsame Test-Utilities (Testcontainers Setup etc.).
  * `shared`: Gemeinsam genutzte DTOs, Konstanten, einfache Utilities.
* **`apps/`:**
  * `control-plane-api`: NestJS-Anwendung für Mandantenverwaltung.
  * `sample-app`: NestJS-Anwendung zur Demonstration des EAF.

## 7. Wichtige Konzepte im Detail

### 7.1. Datenpersistenz

* MikroORM wird konfiguriert, um mit PostgreSQL zu kommunizieren.
* Der Event Store wird als Tabelle (z.B. `events`) mit Spalten für `stream_id`, `version`, `event_type`, `payload`, `timestamp`, `tenant_id` implementiert.
* Read Models sind separate Tabellen, die für Lesezugriffe optimiert sind und ebenfalls `tenant_id` enthalten.
* RLS wird durch global aktivierte MikroORM Filters sichergestellt, die dynamisch mit der `tenant_id` aus dem Kontext parametrisiert werden.
* Entity Discovery via Glob-Patterns ermöglicht Plugins das einfache Beisteuern eigener Entitäten.

### 7.2. Authentifizierung & Autorisierung

* Flexibles AuthN-Modul (V1: JWT, Local). Konfigurierbar pro Mandant (später).
* RBAC + Basic Ownership (ABAC). Empfehlung: `casl` zur Definition und Prüfung von Berechtigungen.
* Standardisierte NestJS Guards zur einfachen Absicherung von Endpunkten/Methoden.
* Bereitstellung von Backend-Services/APIs zur Verwaltung von Rollen/Berechtigungen auf Mandantenebene.

### 7.3. Observability

* V1: Sicherstellung von strukturiertem Logging (Format TBD), Health Checks (`/health/live`, `/health/ready`).
* Roadmap: Integration von Metriken (Prometheus) und Distributed Tracing (OpenTelemetry).

### 7.4. Lizenzvalidierung

* Dediziertes Modul (`libs/licensing`).
* Validierungslogik (Online/Offline TBD) wird beim Start und/oder periodisch aufgerufen.
* Muss robust gegen Umgehung sein.

### 7.5. Testing Strategy Overview

* Unit Tests (`suites`, Jest) für Core-Logik (gemockte Abhängigkeiten).
* Integration Tests (Jest, Testcontainers, MikroORM) für Adapter gegen echte DB/Cache.
* E2E Tests (Jest, `supertest`, `@nestjs/testing`) für komplette API-Flows.

### 7.6. SBOM

* Generierung von SBOMs (z.B. CycloneDX) wird Teil des CI/CD-Build-Prozesses für alle Anwendungen und Kernbibliotheken.

## 8. Deployment-Überlegungen

* Anwendungen werden typischerweise als Docker-Container paketiert.
* Monorepo-Builds (z.B. mit Nx) erzeugen optimierte Artefakte pro Anwendung.
* Health Check Endpunkte unterstützen Orchestrierungs-Plattformen (Kubernetes).
* Graceful Shutdown ist implementiert.

## 9. Referenzen

* Product Requirements Document (PRD) ACCI EAF V1.0
* Architecture Decision Records (ADRs) im Verzeichnis `docs/adr/`

## 10. Diagramme (Platzhalter)

*Detaillierte Diagramme werden nach Bedarf erstellt und gepflegt. Mögliche Diagrammtypen:*

* C4 Model: System Context, Containers, Components
* UML: Component Diagram, Sequence Diagrams (für CQRS Flows), Class Diagrams (für Kern-Entitäten)
* Freie Flussdiagramme

*(Beispielhaft könnten hier eingebettet oder verlinkt werden:)*

* *High-Level Komponentenübersicht (Monorepo)*
* *Request Flow (Command)*
* *Request Flow (Query)*
* *Tenant Context Propagation*
