# Architekturübersicht

Dieses Dokument bietet einen allgemeinen Überblick über die Architektur des ACCI EAF.

> **Status:** Entwurf – Abschnitte sind mit `TODO` markiert.

## Inhaltsverzeichnis

1. [Hexagonale Architektur](#hexagonale-architektur)
2. [CQRS & Event Sourcing](#cqrs--event-sourcing)
3. [Mandantenfähigkeit (Multi-Tenancy)](#mandantenfähigkeit-multi-tenancy)
4. [Plugin-System](#plugin-system)
5. [Sicherheitsebene](#sicherheitsebene)
6. [Diagramme](#diagramme)

---

## Hexagonale Architektur

Das ACCI EAF verwendet eine Hexagonale Architektur (auch bekannt als Ports and Adapters), um die Kernanwendungslogik von externen Abhängigkeiten zu isolieren. Dies fördert Testbarkeit, Wartbarkeit und Flexibilität, indem es ermöglicht, verschiedene Technologien oder externe Dienste mit minimalen Auswirkungen auf den Kern auszutauschen.

- **Kern (`libs/core`):** Enthält die technologieunabhängige Geschäftslogik.
  - `domain`: Beinhaltet Aggregate, Entitäten, Wertobjekte, Domänenereignisse und Repository-*Schnittstellen* (Ports). Diese Schicht hat keine Abhängigkeiten von externen Frameworks.
  - `application`: Beherbergt Anwendungsfälle, Befehls-/Anfrage-Handler, Anwendungsdienste und Datenübertragungsobjekte (DTOs). Sie definiert *Schnittstellen* (Ports) für Infrastrukturkomponenten und hängt nur von der `domain`-Schicht ab.
- **Infrastruktur (`libs/infrastructure`, `apps/*`):** Implementiert die im `core` definierten Ports unter Verwendung spezifischer Technologien (z.B. NestJS für Web-Controller, MikroORM für Datenbank-Repositories, Redis für Caching). Diese Schicht enthält Adapter wie Controller, Datenbank-Repository-Implementierungen und Cache-Clients. Sie hängt von der `core`-Schicht ab.
- **Abhängigkeitsregel:** Abhängigkeiten fließen strikt nach innen: Infrastruktur → Anwendung → Domäne. Dadurch wird sichergestellt, dass die Kernlogik unabhängig von spezifischen Technologieentscheidungen bleibt.

Weitere Details finden Sie im Hauptarchitekturdokument (`.ai/arch.md`).

## CQRS & Event Sourcing

Command Query Responsibility Segregation (CQRS) und Event Sourcing (ES) sind Schlüsselmuster im ACCI EAF zur Verwaltung des Anwendungszustands und zur Verarbeitung von Anfragen.

- **CQRS:** Trennt Lese- (Queries) und Schreiboperationen (Commands). Dies ermöglicht die unabhängige Optimierung jedes Pfades.
  - **Befehlsfluss (Command Flow):** Eine eingehende HTTP-Anfrage wird typischerweise über einen NestJS-Controller an einen `CommandBus` geleitet. Der `CommandBus` leitet den Befehl an seinen jeweiligen Handler weiter. Der Handler lädt ein Aggregat (über ein Repository), das den Befehl validiert und bei Erfolg ein oder mehrere Domänenereignisse erzeugt. Diese Ereignisse werden dann von einem Event Store Adapter gespeichert und über einen `EventBus` veröffentlicht.
  - **Anfragefluss (Query Flow):** Eine HTTP-Anfrage nach Daten wird über einen Controller an einen `QueryBus` geleitet. Der `QueryBus` leitet die Anfrage an seinen Handler weiter, der dann Daten aus einem optimierten Read Model Repository (oft eine denormalisierte Projektion) liest und das Ergebnis zurückgibt.
- **Event Sourcing:** Verwendet eine Sequenz von Ereignissen als einzige Wahrheitsquelle für den Zustand eines Aggregats. Anstatt den aktuellen Zustand zu speichern, werden alle Änderungen als unveränderliche Ereignisse gespeichert.
  - **Event Store:** Eine spezialisierte Datenbank (z.B. eine PostgreSQL-Tabelle), die Ereignisse anhängt. Wichtige Spalten sind `stream_id` (Aggregat-ID), `version`, `event_type` (versioniert, z.B. `UserRegistered.v1`), `payload` (JSONB) und `timestamp`.
  - **Projektionen:** Event-Handler lauschen auf den `EventBus` (oder pollen den Event Store) und aktualisieren Read Models basierend auf den Ereignissen. Diese Read Models sind für Abfragen optimiert.
- **Wichtige Überlegungen:**
  - **Event Schema Evolution (ADR-010):** Ereignisse werden versioniert (z.B. `UserRegistered.v1`, `UserRegistered.v2`). Upcasting-Funktionen werden implementiert, um ältere Ereignisversionen beim Laden oder Verarbeiten von Ereignissen in das neueste Schema umzuwandeln. Dies gewährleistet Abwärtskompatibilität.
  - **Idempotente Handler (ADR-009):** Event-Handler sind so konzipiert, dass sie dasselbe Ereignis mehrmals sicher verarbeiten können, ohne unbeabsichtigte Nebeneffekte. Dies wird typischerweise durch eine Tracking-Tabelle (`processed_events`) erreicht, die `event_id` (und möglicherweise Handler-Identifikatoren) speichert, um bereits verarbeitete Ereignisse zu überspringen.

Siehe ADR-009 und ADR-010 für detaillierte Entscheidungen zu Idempotenz und Event-Evolution.

## Mandantenfähigkeit (Multi-Tenancy)

ACCI EAF bietet Mandantenfähigkeit mittels eines Row-Level Security (RLS)-Ansatzes, der sicherstellt, dass Daten verschiedener Mandanten innerhalb derselben Datenbankinstanz sicher isoliert sind.

- **Ansatz:** Eine `tenant_id`-Spalte wird allen mandantenspezifischen Datenbanktabellen hinzugefügt.
- **Kontextweitergabe:**
    1. Die `tenant_id` wird aus eingehenden Anfragen (z.B. aus einem JWT-Claim oder einem benutzerdefinierten HTTP-Header) durch eine dedizierte NestJS-Middleware extrahiert.
    2. Diese `tenant_id` wird dann während des gesamten Anfragelebenszyklus mittels `AsyncLocalStorage` (bereitgestellt von `libs/tenancy`) verfügbar gemacht.
- **Durchsetzung (ADR-006):**
  - MikroORM Global Filters werden zentral konfiguriert.
  - Diese Filter fügen automatisch eine `WHERE tenant_id = :currentTenantId`-Klausel zu allen relevanten SQL-Abfragen hinzu.
  - Der `:currentTenantId`-Parameter wird dynamisch aus der in `AsyncLocalStorage` gespeicherten `tenant_id` befüllt.
    Dies stellt sicher, dass der Datenzugriff automatisch und transparent auf die Daten des aktuellen Mandanten beschränkt wird.

Details finden Sie in ADR-006 und der `libs/tenancy`-Dokumentation.

## Plugin-System

Das Framework beinhaltet ein Plugin-System, um seine Funktionalität zu erweitern, ohne die Kernbibliotheken ändern zu müssen. Dies fördert die Modularität und erleichtert das Hinzufügen benutzerdefinierter Funktionen oder Integrationen.

- **Plugin-Schnittstelle:** Eine definierte Schnittstelle, die Plugins implementieren müssen.
- **Lade-Mechanismus:** Plugins werden typischerweise während des Anwendungsstarts erkannt und geladen.
- **Registrierungspunkte:** Plugins können verschiedene Komponenten wie NestJS-Module, Controller, Dienste und MikroORM-Entitäten registrieren.
- **Entitäten-Erkennung (Entity Discovery):** Der Mechanismus zur Entitätenerkennung von MikroORM (über Glob-Muster in `mikro-orm.config.ts`) wird genutzt. Plugins können ihre Entitäten innerhalb ihrer eigenen Verzeichnisstruktur definieren, und diese werden automatisch von der zentralen MikroORM-Konfiguration erkannt.
- **Migrationen (ADR-008):**
  - Plugins sind für die Bereitstellung ihrer eigenen MikroORM-Migrationsdateien verantwortlich.
  - Der Migrationsprozess der Hauptanwendung (ausgeführt über MikroORM CLI) ist so konfiguriert, dass er diese pluginspezifischen Migrationen zusammen mit den Kernanwendungsmigrationen erkennt und ausführt. Dies stellt sicher, dass Datenbankschemata für Plugins konsistent verwaltet werden.
- **Dynamisches Laden:** Das System unterstützt das dynamische Laden dieser Plugins, was eine flexible und erweiterbare Architektur ermöglicht.

Siehe ADR-008 für die Plugin-Migrationsstrategie.

## Sicherheitsebene

Sicherheit ist ein grundlegender Aspekt des ACCI EAF, mit verschiedenen Komponenten und Strategien zum Schutz von Anwendungen.

- **Authentifizierung (AuthN):**
  - Wird von einem flexiblen NestJS-Auth-Modul gehandhabt.
  - Version 1 beinhaltet Unterstützung für JWT (Bearer-Token) und lokale (Benutzername/Passwort) Strategien.
  - Die Architektur ist so konzipiert, dass sie potenziell mandantenkonfigurierbare Authentifizierungsstrategien in der Zukunft ermöglicht.
- **Autorisierung (AuthZ - ADR-001):**
  - Verwendet eine Kombination aus rollenbasierter Zugriffskontrolle (RBAC) und grundlegender attributbasierter Zugriffskontrolle (ABAC), hauptsächlich für Eigentumsprüfungen.
  - Die `casl`-Bibliothek wird zur Definition und Überprüfung von Berechtigungen (z.B. `manage`, `read`) für verschiedene Subjekte (z.B. `Tenant`, `User`) verwendet.
- **Guards:** Standardmäßige NestJS-Guards werden zum Schutz von API-Endpunkten und -Methoden verwendet (z.B. `@UseGuards(JwtAuthGuard, CaslGuard('manage', 'Tenant'))`).
- **Security Headers:** Integration der `helmet`-Middleware zur Anwendung verschiedener HTTP-Sicherheitsheader, die zum Schutz vor gängigen Web-Schwachstellen beitragen (z.B. XSS, Clickjacking).
- **Rate Limiting:** Grundlegender Schutz vor Brute-Force- und Denial-of-Service (DoS)-Angriffen wird durch die `@nestjs/throttler`-Middleware bereitgestellt.

Wichtige Entscheidungen bezüglich der Autorisierung sind in ADR-001 dokumentiert.

## Diagramme

Dieser Abschnitt enthält oder verlinkt zu verschiedenen Diagrammen, die die ACCI EAF-Architektur illustrieren.

- **Allgemeine Komponentenübersicht (Monorepo-Struktur - C4 Container Diagramm):**

    ```mermaid
    graph TD
        A[Benutzer] --> B(Browser/Client-Anwendung)
        B --> C{ACCI EAF Anwendung (NestJS)}
        C --> D[PostgreSQL Datenbank]
        C --> E[Redis Cache]
        C --> F(Externe Dienste / APIs)

        subgraph Monorepo
            direction LR
            subgraph "apps/"
                direction TB
                G["apps/control-plane-api"]
                H["apps/sample-app"]
            end
            subgraph "libs/"
                direction TB
                I["libs/core"]
                J["libs/infrastructure"]
                K["libs/tenancy"]
                L["libs/rbac"]
                M["libs/licensing"]
                N["libs/plugins"]
                O["libs/testing"]
                P["libs/shared"]
            end
        end
        G --> I; G --> J; G --> K; G --> L; G --> M; G --> P;
        H --> I; H --> J; H --> K; H --> L; H --> M; H --> P;
        J --> I;
    end
    ```

- **Anfragefluss - Befehl (Sequenzdiagramm):**

    ```mermaid
    sequenceDiagram
        participant Client
        participant Controller (NestJS)
        participant CommandBus
        participant CommandHandler
        participant Aggregate
        participant EventStoreAdapter
        participant EventBus

        Client->>Controller: HTTP POST /resource
        Controller->>CommandBus: dispatch(CreateResourceCommand)
        CommandBus->>CommandHandler: handle(CreateResourceCommand)
        CommandHandler->>Aggregate: constructor() / load()
        CommandHandler->>Aggregate: executeCommand(data)
        Aggregate->>Aggregate: validate()
        Aggregate->>EventStoreAdapter: save(ResourceCreatedEvent)
        EventStoreAdapter-->>Aggregate: event saved
        Aggregate-->>CommandHandler: success
        CommandHandler->>EventBus: publish(ResourceCreatedEvent)
        CommandHandler-->>Controller: result
        Controller-->>Client: HTTP 201 Created
    end
    ```

- **Anfragefluss - Abfrage (Sequenzdiagramm):**

    ```mermaid
    sequenceDiagram
        participant Client
        participant Controller (NestJS)
        participant QueryBus
        participant QueryHandler
        participant ReadModelRepository

        Client->>Controller: HTTP GET /resource/{id}
        Controller->>QueryBus: dispatch(GetResourceQuery)
        QueryBus->>QueryHandler: handle(GetResourceQuery)
        QueryHandler->>ReadModelRepository: findById(id)
        ReadModelRepository-->>QueryHandler: resourceData
        QueryHandler-->>Controller: resourceData
        Controller-->>Client: HTTP 200 OK with resourceData
    end
    ```

- **Mandantenkontext-Weitergabe & RLS-Filter (Vereinfachtes Sequenzdiagramm):**

    ```mermaid
    sequenceDiagram
        participant Client
        participant Middleware (NestJS)
        participant AsyncLocalStorage
        participant Service/Handler
        participant MikroORM_GlobalFilter
        participant Database

        Client->>Middleware: Request with Tenant ID (e.g., JWT/Header)
        Middleware->>AsyncLocalStorage: set('tenantId', extractedTenantId)
        Middleware->>Service/Handler: processRequest()
        Service/Handler->>MikroORM_GlobalFilter: DB Query (e.g., find Entity)
        MikroORM_GlobalFilter->>AsyncLocalStorage: get('tenantId')
        AsyncLocalStorage-->>MikroORM_GlobalFilter: currentTenantId
        MikroORM_GlobalFilter->>Database: SELECT * FROM entity WHERE tenant_id = currentTenantId AND ...
        Database-->>MikroORM_GlobalFilter: Filtered Data
        MikroORM_GlobalFilter-->>Service/Handler: Filtered Data
        Service/Handler-->>Client: Response
    end
    ```

*(Weitere Diagramme wie Event-Handling und Kernentitätsbeziehungen werden bei Bedarf basierend auf `.ai/arch.md` hinzugefügt)*
