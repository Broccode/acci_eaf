# Mandantenfähigkeit im ACCI EAF

Version: 1.0
Datum: 2025-05-07
Status: Veröffentlicht

## Einleitung

Mandantenfähigkeit (Multi-Tenancy) ist ein zentrales Architekturprinzip des ACCI EAF. Es ermöglicht einer einzelnen Anwendungsinstanz, mehrere Mandanten (z.B. verschiedene Kunden oder Organisationseinheiten) zu bedienen, während deren Daten isoliert und sicher gehalten werden. Dieses Dokument beschreibt den Ansatz zur Mandantenfähigkeit innerhalb des Frameworks, wobei der Schwerpunkt auf Row-Level Security (RLS) liegt.

Dieses Konzept ist entscheidend für SaaS-Anwendungen und jedes System, das eine logische Datentrennung zwischen verschiedenen Benutzergruppen erfordert.

## Kernkonzepte

### 1. Mandantendefinition

Ein **Mandant** repräsentiert eine isolierte Gruppe von Benutzern und deren zugehörigen Daten. Jeder Mandant agiert so, als hätte er seine eigene dedizierte Anwendungsinstanz, obwohl sie sich die zugrundeliegenden Ressourcen teilen.

### 2. Datenisolationsstrategie: Row-Level Security (RLS)

ACCI EAF verwendet primär RLS, um die Datenisolation auf Datenbankebene durchzusetzen.

- **`tenant_id`-Spalte:** Eine obligatorische `tenant_id`-Spalte wird allen Datenbanktabellen hinzugefügt, die mandantenspezifische Daten speichern. Diese Spalte verknüpft jede Zeile mit einem spezifischen Mandanten.
- **Automatisierte Filterung:** Datenbankabfragen werden automatisch basierend auf der ID des aktuellen Mandanten gefiltert, wodurch sichergestellt wird, dass ein Mandant nur auf seine eigenen Daten zugreifen kann.

### 3. Weitergabe des Mandantenkontexts

Um RLS effektiv anzuwenden, muss die Anwendung bei jeder eingehenden Anfrage den aktuellen Mandantenkontext kennen.

- **Mandantenidentifikation:** Die `tenant_id` wird typischerweise aus einer eingehenden HTTP-Anfrage extrahiert. Gängige Quellen sind:
  - Ein Claim innerhalb eines JWT (JSON Web Token).
  - Ein benutzerdefinierter HTTP-Header (z.B. `X-Tenant-ID`).
  - Teil des Hostnamens oder URL-Pfads (weniger verbreitet in der primären Strategie des EAF, aber möglich).
- **`AsyncLocalStorage`:** Nach der Identifizierung wird die `tenant_id` in einer `AsyncLocalStorage`-Instanz gespeichert. Dies macht die `tenant_id` während des gesamten asynchronen Ausführungsflusses einer einzelnen Anfrage verfügbar, ohne sie explizit durch alle Funktionsaufrufe weitergeben zu müssen.
  - Eine NestJS-Middleware ist dafür verantwortlich, die `tenant_id` zu extrahieren und den `AsyncLocalStorage` für den Anfragebereich zu initialisieren.

### 4. Durchsetzung mit MikroORM Global Filters (ADR-006)

MikroORM, der im ACCI EAF verwendete Object-Relational Mapper, spielt eine entscheidende Rolle bei der Durchsetzung von RLS:

- **Globale Filter:** Die globalen Filter von MikroORM werden verwendet, um einen Mandantenfilter zu definieren, der auf alle relevanten Entitäten angewendet wird.
- **Dynamische Parametrisierung:** Der Filter ist so konfiguriert, dass er die `tenant_id` dynamisch aus dem `AsyncLocalStorage` empfängt.
- **Automatische Abfrageanpassung:** Wenn eine Abfrage für eine Entität durchgeführt wird, für die der Mandantenfilter aktiviert ist, hängt MikroORM automatisch die notwendige SQL `WHERE`-Klausel (z.B. `WHERE tenant_id = :currentTenantId`) an, bevor die Abfrage an die Datenbank gesendet wird.

Dieser Ansatz zentralisiert die RLS-Logik und macht sie für die Anwendungsdienste und Repositories transparent, wodurch das Risiko einer versehentlichen Datenpreisgabe verringert wird.

## Implementierungsdetails (`libs/tenancy`)

Die `libs/tenancy`-Bibliothek kapselt die Kernlogik der Mandantenfähigkeit:

- **Tenant Context Middleware:** Eine NestJS-Middleware zum Extrahieren der `tenant_id` und Befüllen des `AsyncLocalStorage`.
- **Tenant Context Service:** Ermöglicht den Zugriff auf die aktuelle `tenant_id` aus dem `AsyncLocalStorage`.
- **MikroORM Filterkonfiguration:** Einrichtung und Registrierung des globalen Mandantenfilters für MikroORM.

## Vorteile

- **Starke Datenisolation:** Verhindert, dass Mandanten auf die Daten anderer zugreifen können.
- **Vereinfachte Entwicklung:** Entwickler müssen nicht manuell `tenant_id`-Bedingungen zu jeder Abfrage hinzufügen.
- **Skalierbarkeit:** Eine einzelne Anwendungsinstanz kann viele Mandanten bedienen, was die Ressourcennutzung optimiert.
- **Wartbarkeit:** Zentralisierte Mandantenlogik ist einfacher zu verwalten und zu aktualisieren.

## Überlegungen

- **Geteilte Daten:** Für Daten, die wirklich von allen Mandanten gemeinsam genutzt werden (z.B. systemweite Konfigurationen), wäre die `tenant_id`-Spalte nicht anwendbar, oder es könnte ein spezieller NULL-/Platzhalterwert verwendet werden, und die Entitäten würden so konfiguriert, dass sie den RLS-Filter umgehen.
- **Performance:** Obwohl RLS im Allgemeinen effizient ist, erfordern komplexe Abfragen auf sehr großen mandantenfähigen Tabellen möglicherweise eine sorgfältige Indizierung der `tenant_id`-Spalte und anderer häufig abgefragter Spalten.
- **Mandantenübergreifende Operationen:** Operationen, die legitimerweise auf Daten von mehreren Mandanten zugreifen müssen (z.B. durch einen Super-Administrator), erfordern eine spezielle Behandlung, potenziell durch Umgehung der RLS-Filter unter strengen, auditierten Bedingungen.

## Zugehörige ADRs

- **ADR-006: RLS Enforcement Strategy:** Detailliert die Entscheidung, MikroORM Global Filters für RLS zu verwenden.

## Zukünftige Überlegungen

- Mandantenspezifische Konfigurationen über die Datenisolation hinaus (z.B. Feature-Flags pro Mandant).
- Strategien zum Sharding von Mandanten auf mehrere Datenbanken, falls eine einzelne Datenbank zum Engpass wird (obwohl RLS innerhalb einer einzelnen DB die primäre V1-Strategie ist).

Dieses Dokument bietet ein grundlegendes Verständnis der Mandantenfähigkeit im ACCI EAF. Für spezifische Implementierungsmuster und deren Verwendung siehe die Codebasis von `libs/tenancy` und die zugehörigen ADRs.
