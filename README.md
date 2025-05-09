# ACCI EAF - Axians Competence Center Infrastructure Enterprise Application Framework 🚀

<div align="center">
  
  ![Version](https://img.shields.io/badge/version-1.0-blue.svg)
  ![License](https://img.shields.io/badge/license-Internal-red.svg)
  
</div>

---

# 🇬🇧 English Version

## 📋 Description

The ACCI EAF is an internal software framework designed to accelerate, standardize, and secure the development of robust, scalable, and maintainable enterprise applications for Axians and its customers. It addresses common pain points in enterprise app development by providing a solid foundation, reusable components, and best practices for multi-tenancy, security, observability, and compliance.

## 🎯 Goals & Features

- ⚡ **Accelerate Development:** Providing a solid foundation, reusable components, and clear patterns
- 🏆 **Promote Best Practices:** Implementing Hexagonal Architecture, CQRS/ES, Multi-Tenancy, RBAC/ABAC
- ✅ **Improve Quality:** High test coverage through defined strategies (Unit/Integration/E2E)
- 📈 **Increase Scalability:** Designing for horizontal scalability through CQRS and stateless components
- 🔍 **Ensure Traceability:** Through Event Sourcing with defined evolution strategy
- 🧩 **Enhance Maintainability:** Clear separation of concerns, modularity, standardized processes
- 🔌 **Enable Flexibility:** Through a plugin system and configurability
- 🛡️ **Support Compliance:** Providing features and documentation that aid in certification processes

## 🏗️ Architecture & Technology Stack

The framework leverages modern technologies and proven architectural patterns:

### Core Architecture Patterns

- 🔷 **Hexagonal Architecture (Ports & Adapters):** Isolating core logic from external dependencies
- 🔀 **CQRS/ES Pattern:** Controllers delegate write operations to command handlers and read operations to query handlers, using events as the single source of truth
- 🏢 **Multi-Tenancy (via RLS):** Securely isolating data for different tenants within the same database
- 🧩 **Plugin System:** Allowing extension without modifying core libraries

### Technology Stack

- 📜 TypeScript & Node.js
- 🏗️ NestJS (Backend framework)
- 🗄️ PostgreSQL (Database)
- 🔗 MikroORM (ORM with Entity Discovery and RLS Filters)
- 💾 Redis (Caching)
- 📦 Nx (Monorepo management)
- 🧪 Testing: Jest (Unit & Integration), @nestjs/testing, Testcontainers & E2E with supertest
- 🔒 Security: casl (RBAC/ABAC), helmet, throttler
- 📊 Observability: terminus (Health checks)
- 📝 Documentation: OpenAPI
- 📑 Compliance: cyclonedx (SBOM generation)

## 📂 Project Structure

```
apps/
  control-plane-api/    # API for tenant management (NestJS)
  sample-app/           # Sample application using EAF (NestJS)
libs/
  core/                 # Domain and application logic (tech-agnostic)
  infrastructure/       # Adapters implementing ports
  tenancy/              # Tenant context handling
  rbac/                 # RBAC/ABAC core logic, Guards
  licensing/            # Core logic for license validation
  plugins/              # Plugin interface and loading mechanism
  testing/              # Shared test utilities
  shared/               # Shared DTOs, constants, utilities
docs/
  adr/                  # Architecture Decision Records
  concept/              # Concept documents
  diagrams/             # Architecture diagrams
  setup/                # Setup guides
  fr-spec/              # Functional requirements specifications
```

## 🚀 Setup & Development

### Prerequisites

- 🟢 Node.js (v16+)
- 🐘 PostgreSQL (v14+)
- 🔴 Redis (v6+)
- 🐳 Docker (recommended for local development)

### Setup

```bash
# Clone the repository
git clone [repository-url]
cd acci_eaf

# Install dependencies
npm install

# Setup database (using docker-compose)
npm run db:setup

# Build all packages
npx nx run-many --target=build --all

# Run the sample application
npx nx serve sample-app
```

### Development Commands

```bash
# Generate a new lib
npx nx g @nx/js:lib my-new-lib

# Run tests
npx nx test core  # Test the core lib
npx nx test       # Test all
```

## 📚 Documentation

For more detailed documentation, refer to:

- 📄 `docs/adr/` - Architecture Decision Records
- 📘 `docs/concept/` - Concept documents
- 🛠️ `docs/setup/` - Setup guides
- 🏗️ `docs/ARCH.md` - Architecture overview
- 📋 `docs/PRD.md` - Product Requirements Document

## 📜 License

Internal use only. © Axians, All rights reserved.

## 🌡️ Performance & Benchmarks

### Benchmark Setup

- **Environment:** Dockerized PostgreSQL v14, Node.js v16, 2 CPU cores, 4GB RAM
- **Tooling:** `wrk` for HTTP benchmarks, `ab` for API endpoint tests
- **Scenarios:**
  - Tenant creation workflow
  - Tenant listing
  - Concurrent read/write load (1000 simultaneous connections)

### Benchmark Results

| Scenario                       | Latency (p50) | Latency (p95) | Throughput (req/s) |
|--------------------------------|---------------|---------------|--------------------|
| Tenant creation                | 45ms          | 120ms         | 500 req/s          |
| Tenant listing (1000 tenants)  | 12ms          | 30ms          | 2000 req/s         |
| Concurrent load (1000 clients) | 55ms          | 200ms         | 480 req/s         |

## SBOM Generation & Validation

You can generate a Software Bill of Materials (SBOM) for the entire monorepo using CycloneDX:

```bash
npm run generate:sbom
# Output: dist/sbom.json
```

The command runs the Nx target `sbom:generate-sbom` which executes `cyclonedx-npm`.

### CI Pipeline

The GitLab CI pipeline automatically generates the SBOM and stores it as a pipeline artefact for every merge request and push to `main`.

### Validation (Optional)

To validate the generated SBOM against the CycloneDX schema, you can use the [cyclonedx-cli](https://github.com/CycloneDX/cyclonedx-cli) tool:

```bash
cyclonedx validate --input-file dist/sbom.json --fail-on-errors
```

For more details, see `docs/sbom/README.md`.

---

# 🇩🇪 Deutsche Version

## 📋 Beschreibung

Das ACCI EAF ist ein internes Software-Framework, das entwickelt wurde, um die Entwicklung robuster, skalierbarer und wartbarer Unternehmensanwendungen für Axians und seine Kunden zu beschleunigen, zu standardisieren und abzusichern. Es adressiert typische Herausforderungen in der Entwicklung von Unternehmensanwendungen, indem es ein solides Fundament, wiederverwendbare Komponenten und Best Practices für Multi-Tenancy, Sicherheit, Beobachtbarkeit und Compliance bereitstellt.

## 🎯 Ziele & Funktionen

- ⚡ **Entwicklung beschleunigen:** Bereitstellung eines soliden Fundaments, wiederverwendbarer Komponenten und klarer Muster
- 🏆 **Best Practices fördern:** Implementierung von Hexagonaler Architektur, CQRS/ES, Multi-Tenancy, RBAC/ABAC
- ✅ **Qualität verbessern:** Hohe Testabdeckung durch definierte Strategien (Unit/Integration/E2E)
- 📈 **Skalierbarkeit erhöhen:** Design für horizontale Skalierbarkeit durch CQRS und zustandslose Komponenten
- 🔍 **Nachverfolgbarkeit sicherstellen:** Durch Event Sourcing mit definierter Evolutionsstrategie
- 🧩 **Wartbarkeit verbessern:** Klare Trennung von Zuständigkeiten, Modularität, standardisierte Prozesse
- 🔌 **Flexibilität ermöglichen:** Durch ein Plugin-System und Konfigurierbarkeit
- 🛡️ **Compliance unterstützen:** Bereitstellung von Funktionen und Dokumentation, die bei Zertifizierungsprozessen helfen

## 🏗️ Architektur & Technologie-Stack

Das Framework nutzt moderne Technologien und bewährte Architekturmuster:

### Kern-Architekturmuster

- 🔷 **Hexagonale Architektur (Ports & Adapter):** Isolierung der Kernlogik von externen Abhängigkeiten
- 🔀 **CQRS/ES Pattern:** Controllers delegate write operations to command handlers and read operations to query handlers, using events as the single source of truth
- 🏢 **Multi-Tenancy (über RLS):** Sichere Isolierung von Daten verschiedener Mandanten in derselben Datenbank
- 🧩 **Plugin-System:** Ermöglicht Erweiterungen ohne Änderung der Kernbibliotheken

### Technologie-Stack

- 📜 TypeScript & Node.js
- 🏗️ NestJS (Backend-Framework)
- 🗄️ PostgreSQL (Datenbank)
- 🔗 MikroORM (ORM mit Entity Discovery und RLS-Filtern)
- 💾 Redis (Caching)
- 📦 Nx (Monorepo-Management)

- 🔒 Sicherheit: casl (RBAC/ABAC), helmet, throttler
- 📊 Beobachtbarkeit: terminus (Health-Checks)
- 📝 Dokumentation: OpenAPI
- 📑 Compliance: cyclonedx (SBOM-Generierung)

## 📂 Projektstruktur

```
apps/
  control-plane-api/    # API für Mandantenverwaltung (NestJS)
  sample-app/           # Beispielanwendung mit EAF (NestJS)
libs/
  core/                 # Domänen- und Anwendungslogik (technologieunabhängig)
  infrastructure/       # Adapter zur Implementierung von Ports
  tenancy/              # Mandantenkontext-Verwaltung
  rbac/                 # RBAC/ABAC-Kernlogik, Guards
  licensing/            # Kernlogik zur Lizenzvalidierung
  plugins/              # Plugin-Schnittstelle und Lademechanismus
  testing/              # Gemeinsame Test-Utilities
  shared/               # Gemeinsame DTOs, Konstanten, Hilfsfunktionen
docs/
  adr/                  # Architekturentscheidungsaufzeichnungen
  concept/              # Konzeptdokumente
  diagrams/             # Architekturdiagramme
  setup/                # Einrichtungsanleitungen
  fr-spec/              # Funktionale Anforderungsspezifikationen
```

## 🚀 Einrichtung & Entwicklung

### Voraussetzungen

- 🟢 Node.js (v16+)
- 🐘 PostgreSQL (v14+)
- 🔴 Redis (v6+)
- 🐳 Docker (empfohlen für lokale Entwicklung)

### Einrichtung

```bash
# Repository klonen
git clone [repository-url]
cd acci_eaf

# Abhängigkeiten installieren
npm install

# Datenbank einrichten (mit docker-compose)
npm run db:setup

# Alle Pakete bauen
npx nx run-many --target=build --all

# Beispielanwendung starten
npx nx serve sample-app
```

### Entwicklungsbefehle

```bash
# Neue Bibliothek erstellen
npx nx g @nx/js:lib meine-neue-lib

# Tests ausführen
npx nx test core  # Core-Lib testen
npx nx test       # Alle testen
```

## 📚 Dokumentation

Für detailliertere Dokumentation siehe:

- 📄 `docs/adr/` - Architekturentscheidungsaufzeichnungen
- 📘 `docs/concept/` - Konzeptdokumente
- 🛠️ `docs/setup/` - Einrichtungsanleitungen
- 📚 `docs/de/` - Deutsche Dokumentation
- 🏗️ `docs/ARCH.md` - Architekturübersicht
- 📋 `docs/PRD.md` - Produktanforderungsdokument

## 📜 Lizenz

Nur für internen Gebrauch. © Axians, Alle Rechte vorbehalten.

## 🌡️ Leistung & Benchmarks

### Benchmark Setup

- **Umgebung:** Dockerized PostgreSQL v14, Node.js v16, 2 CPU-Kerne, 4GB RAM
- **Werkzeuge:** `wrk` für HTTP-Benchmarks, `ab` für API-Endpunkttests
- **Szenarien:**
  - Mandantenerstellung
  - Mandantenliste (1000 Mandanten)
  - Gleichzeitige Lese-/Schreiblast (1000 gleichzeitige Verbindungen)

### Benchmark-Ergebnisse

| Szenario                         | Latenz (p50) | Latenz (p95) | Durchsatz (req/s) |
|----------------------------------|--------------|--------------|-------------------|
| Mandantenerstellung              | 45ms         | 120ms        | 500 req/s         |
| Mandantenliste (1000 Mandanten)  | 12ms         | 30ms         | 2000 req/s        |
| Gleichzeitige Last (1000 Clients)| 55ms         | 200ms        | 480 req/s        |
