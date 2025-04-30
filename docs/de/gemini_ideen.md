# ACCI EAF - Zusammenfassung der Entstehung und Entscheidungen (Stand: 2025-04-21)

Dieses Dokument fasst den Gesprächsverlauf und die wichtigsten Entscheidungen zusammen, die zur Definition des ACCI EAF V1 geführt haben.

## 1. Initialer Anforderungs- & Technologie-Stack

* **Ziel:** Erstellung eines Enterprise Application Frameworks (EAF).
* **Gesetzte Technologien/Muster:** TypeScript, NestJS, Testcontainers, PostgreSQL, Redis, CQRS/ES, Hexagonale Architektur, Plugin-System.
* **Ergebnis:** Erster Entwurf einer Architektur und eines PRDs, die diese Elemente integrieren, mit Fokus auf Schichtentrennung (Hexagonal), CQRS-Implementierung in NestJS und Testbarkeit.

## 2. Inspiration & Erste Anpassungen

* Diskussion über externe Dokumente als Inspiration (ADRs, detaillierte NFRs, OWASP, API-Standards).
* **Entscheidung:** Übernahme von Best Practices:
  * Nutzung von Architecture Decision Records (ADRs).
  * Detailliertere, messbare Non-Functional Requirements (NFRs).
  * Explizite Berücksichtigung von OWASP / Security Guidelines.
  * Empfehlung zur Nutzung von OpenAPI für APIs.
* **Wichtige Scope-Änderung:** **Multi-Tenancy** wird zur Kernanforderung für V1 (nicht optional). Eine **Control Plane API** zur Mandantenverwaltung wird ebenfalls benötigt.

## 3. Konkretisierung: Name, Multi-Tenancy, Monorepo

* **Projektname:** Festlegung auf **ACCI EAF**.
* **Multi-Tenancy Implementierung:**
  * Datenisolation: Entscheidung für **Row-Level Security (RLS)** mittels `tenant_id`-Spalte.
  * Mandantenkontext: Ermittlung aus Token/Header, Weitergabe über **`AsyncLocalStorage`**.
* **Control Plane API:** Als separate Anwendung für Admin-Mandanten-CRUD bestätigt.
* **Projektstruktur:** Entscheidung für einen **Monorepo**-Ansatz zur Verwaltung des EAF und der darauf basierenden Produkte (wie DPCM). Empfehlung für **Nx** als Management-Tool. Diskussion über Skalierbarkeit und CI/CD-Workflows im Monorepo (Bestätigung der Machbarkeit mit Nx `affected`-Befehlen und Caching).

## 4. Tooling-Entscheidungen: Testing & ORM

* **Unit Testing:** Nach Klärung von Missverständnissen Entscheidung für das Framework **`suites` (`suites.dev`)** zur Vereinfachung von Unit-Tests für DI-Komponenten in NestJS.
* **ORM (Diskussionsintensiv):**
  * Anfängliche Empfehlung: **Prisma** (wegen Typsicherheit, DX, RLS-Middleware).
  * **Problem:** Zentrale `schema.prisma`-Datei kollidiert mit der Anforderung an einfache **Plugin Entity Discovery** (Bevorzugung von Glob-Patterns). Prisma daher ausgeschlossen.
  * Vergleich **TypeORM vs. MikroORM:** Beide unterstützen Glob-Discovery.
    * TypeORM: Größeres Ökosystem, RLS-Implementierung aufwändiger.
    * MikroORM: Kleinere Community, aber "Filters"-Feature als vielversprechende Lösung für RLS identifiziert.
  * **Finale Entscheidung:** **MikroORM** wird gewählt, da es den Plugin-Anforderungen entgegenkommt und eine gute RLS-Lösung bietet. Drizzle ORM wurde ebenfalls diskutiert (wg. "Impedance Mismatch"), aber aufgrund der Nachteile bei Plugin Discovery und RLS verworfen.
* **CLI:** **`nest-commander`** als geeignetes Werkzeug für die Implementierung von CLI-Befehlen (wie den `setup-admin`-Befehl) identifiziert.

## 5. Feature- & Anforderungs-Verfeinerung

* **Autorisierung (RBAC/ABAC):**
  * Anforderung eines **vollständigen RBAC-Systems** für V1 bestätigt.
  * Administration soll auf **Mandantenebene** erfolgen.
  * Zusätzliche Anforderung für **ABAC (Ownership)** basierend auf `ownerUserId`.
  * **V1 Scope:** RBAC Core-Logik + Enforcement Guards + Basic Ownership Check (`ownerUserId`). User Groups werden auf Roadmap verschoben. Empfehlung für **`casl`** als Bibliothek.
* **Deployment:**
  * Klarstellung: Kubernetes ist bei Kunden selten, VMs sind üblich.
  * Anpassung: Fokus auf Deployment mittels **Docker Compose** auf VMs.
  * Neue Anforderung: **Offline-Fähigkeit** (kein Internet auf Kunden-VM).
  * Finale Strategie: Bereitstellung eines **Offline-Tarballs** (enthält via `docker save` gesicherte Images, Compose-Datei, Skripte), Installation/Update via `docker load` und Skripte (inkl. Offline-Migrationen).
* **Lizenzierung:**
  * Klärung: Muss primär offline-fähig sein (signierte Datei).
  * **Hybrider Ansatz:** Optionale Online-Checks gegen Axians-Server, wenn Konnektivität erlaubt/konfiguriert ist. "Fail Open"-Prinzip bei Netzwerkfehlern empfohlen.
* **Internationalisierung (i18n):** Integration von **`nestjs-i18n`** für API-Texte/Validierung als Anforderung hinzugefügt.
* **SBOM:** Anforderung zur automatischen Generierung von **Software Bill of Materials** im Build-Prozess hinzugefügt (Tool/Format TBD per ADR).
* **Weitere "Enterprise Features":** Diskussion und Aufnahme (als V1-Basics oder Roadmap) von Anforderungen zu erweiterter Observability (Structured Logging, Health Checks V1; Metrics, Tracing später), Reliability (Graceful Shutdown V1; Retry/Circuit Breaker später), Security (Helmet, Rate Limiter V1; Audit Trail, erweiterte AuthN später) etc.

## 6. Dokumentationsstrategie

* Diskussion über **Arc42** als Strukturvorlage.
* **Entscheidung:** Beibehaltung des Ansatzes mit mehreren, spezifischen Dokumenten (PRD, Architekturübersicht, ADRs, Setup-Guide, Konzept-Dokumentationen etc.), anstatt alles in eine strikte Arc42-Struktur zu überführen.
* Erstellung der finalen PRD- und Architekturübersichts-Dokumente.
* Erstellung von Vorlagen/Gliederungen/Entwürfen für ADRs und weitere relevante Dokumente.

## 7. Finaler Stand & Offene Fragen

* Das Ergebnis ist eine detaillierte Definition für **ACCI EAF V1** mit einem klaren Scope und einer Roadmap für zukünftige Erweiterungen.
* Die Kerntechnologien und Architekturmuster sind festgelegt.
* Wichtige Implementierungsdetails und einige Technologieauswahlen sind noch offen und müssen im weiteren Verlauf geklärt und in **ADRs** dokumentiert werden (siehe Abschnitt 10 im finalen PRD). Dies betrifft insbesondere Details der Lizenzierung, RLS-Filter-Implementierung, Bootstrapping, Plugin-Migrationen, Idempotenz-Support, RBAC-Lib-Wahl und SBOM-Tool-Wahl.

Diese Zusammenfassung zeichnet den Weg unserer gemeinsamen Überlegungen und die getroffenen Entscheidungen nach, die zur aktuellen Definition des ACCI EAF geführt haben.
