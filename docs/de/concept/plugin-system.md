# Plugin-System im ACCI EAF

Version: 1.0
Datum: 2025-05-07
Status: Veröffentlicht

## Einleitung

Das ACCI EAF verfügt über ein Plugin-System, das seine Erweiterbarkeit und Modularität verbessern soll. Dieses System ermöglicht es Entwicklern, neue Funktionalitäten hinzuzufügen, Dienste von Drittanbietern zu integrieren oder bestehende Verhaltensweisen anzupassen, ohne den Kern-Framework-Code ändern zu müssen. Dieses Dokument beschreibt die konzeptionelle Grundlage des Plugin-Systems.

## Kernziele

- **Erweiterbarkeit:** Ermöglicht das Hinzufügen neuer Features und Fähigkeiten zu Anwendungen, die auf EAF basieren.
- **Modularität:** Verpackt unterschiedliche Funktionalitäten als in sich geschlossene Plugins.
- **Wartbarkeit:** Hält benutzerdefinierten Code vom Kern-Framework getrennt, was Aktualisierungen und Wartung für beide vereinfacht.
- **Wiederverwendbarkeit:** Ermöglicht die gemeinsame Nutzung von Plugins über verschiedene EAF-basierte Anwendungen hinweg, wenn sie generisch konzipiert sind.

## Schlüsselkonzepte

### 1. Plugin-Definition

Ein Plugin ist typischerweise ein NestJS-Modul (oder eine Reihe von Modulen), das eine spezifische Funktionalität kapselt. Es kann beinhalten:

- NestJS Controller (für neue API-Endpunkte)
- NestJS Services (für Geschäftslogik)
- MikroORM Entities (für benutzerdefinierte Datenmodelle)
- MikroORM Migrations (für Schemaänderungen im Zusammenhang mit seinen Entitäten)
- Konfigurationsmodule
- Benutzerdefinierte Provider, Guards, Interceptors usw.

### 2. Plugin-Schnittstelle (Konzeptionell)

Obwohl nicht unbedingt eine strikte TypeScript-Schnittstelle, die alle Plugins implementieren *müssen* (da NestJS-Module selbst als struktureller Vertrag dienen), halten sich Plugins an bestimmte Konventionen für die Integration:

- Sie sollten von der Hauptanwendung auffindbar sein.
- Sie sollten ein primäres NestJS-Modul exportieren, das von der Hauptanwendung importiert werden kann.
- Sie sollten ihre eigenen Abhängigkeiten verwalten.

### 3. Erkennung und Laden

- **Mechanismus:** Die Hauptanwendung ist so konfiguriert, dass sie Plugins erkennt und lädt. Dies kann auf verschiedene Weisen erreicht werden:
  - **Statische Imports:** Für eng gekoppelte oder essentielle Plugins können sie direkt in das Hauptanwendungsmodul importiert werden.
  - **Dynamische Imports/Konventionsbasiertes Laden:** Für lose gekoppelte Plugins kann ein konventionsbasierter Lademechanismus implementiert werden (z.B. Scannen eines bestimmten Verzeichnisses nach Plugin-Modulen während des Bootstraps). Die Nx-Monorepo-Struktur kann bei der Organisation dieser Plugins innerhalb von `libs/` helfen.
- **Bootstrap-Phase:** Plugins werden typischerweise während der Bootstrap-Sequenz der Anwendung geladen und integriert.

### 4. Entitäten-Erkennung (MikroORM)

Ein entscheidender Aspekt für Plugins, die ihre eigenen Datenmodelle einführen, ist die Integration mit MikroORM:

- Plugins definieren ihre MikroORM-Entitäten innerhalb ihrer eigenen Modulstruktur.
- Die MikroORM-Konfiguration der Hauptanwendung (`mikro-orm.config.ts`) ist mit Glob-Mustern eingerichtet, die breit genug sind, um Entitäten aus diesen registrierten/geladenen Plugin-Verzeichnissen zu erkennen (z.B. `dist/libs/plugins/**/entities/*.entity.js` oder ähnlich, abhängig vom Build-Output und der Plugin-Struktur).
- Dies ermöglicht die Verwaltung von Plugin-Entitäten durch dieselbe MikroORM-Instanz wie Kernentitäten.

### 5. Datenbankmigrationen (ADR-008)

Plugins, die ihre eigenen Entitäten definieren, erfordern oft Datenbankmigrationen:

- **Von Plugins bereitgestellte Migrationen:** Jedes Plugin ist für die Erstellung und Wartung seiner eigenen MikroORM-Migrationsdateien innerhalb seiner eigenen Verzeichnisstruktur verantwortlich.
- **Zentralisierte Ausführung:** Der Migrationsbefehl der Hauptanwendung (z.B. `npx mikro-orm migration:up`) ist so konfiguriert, dass er Migrationen aus allen registrierten Plugins zusätzlich zu seinen eigenen Kernmigrationen erkennt und ausführt.
  - Dies beinhaltet typischerweise die Konfiguration der Optionen `path` und `glob` in der MikroORM-Migrationskonfiguration, um Plugin-Migrationsverzeichnisse einzuschließen.
- **Reihenfolge:** Vorsicht ist geboten, wenn Abhängigkeiten zwischen Plugins bestehen, die die Migrationsreihenfolge beeinflussen, obwohl unabhängige Plugins bevorzugt werden.

Siehe ADR-008 für die detaillierte Strategie zu Plugin-Migrationen.

### 6. Konfiguration

- Plugins können ihre eigene Konfiguration erfordern.
- Dies kann mit dem `ConfigModule` von NestJS verwaltet werden, wobei Plugins ihr eigenes Konfigurationsschema definieren und Werte aus Umgebungsvariablen oder dedizierten Konfigurationsdateien laden, die entsprechend mit Namensräumen versehen sind, um Konflikte zu vermeiden.

### 7. Interaktion mit Kerndiensten

Plugins können mit Kern-EAF-Diensten und -Konzepten interagieren:

- **Dependency Injection:** Plugins können Dienste aus `libs/core`, `libs/infrastructure`, `libs/tenancy`, `libs/rbac` usw. injizieren und verwenden.
- **Mandantenfähigkeit:** Plugin-Entitäten und -Dienste sollten mandantenfähig sein, wenn sie mandantenspezifische Daten verarbeiten, und sich in den bestehenden `tenant_id`-Kontext und die RLS-Mechanismen integrieren.
- **RBAC:** Plugins können ihre eigenen Berechtigungen definieren und bei Bedarf mit `casl` für die Autorisierung integrieren.

## Vorteile

- **Saubere Trennung der Belange:** Isoliert benutzerdefinierte oder erweiterte Funktionalität.
- **Vereinfachte Upgrades:** Kern-Framework-Updates brechen die Plugin-Funktionalität weniger wahrscheinlich, wenn die Schnittstellen stabil sind.
- **Reduzierte Kernaufblähung:** Hält das Kern-Framework schlank, wobei optionale Funktionen als Plugins implementiert werden.
- **Ökosystem-Potenzial:** Ermöglicht eine Community oder einen internen Marktplatz für wiederverwendbare Plugins.

## Beispiel-Anwendungsfälle für Plugins

- Integration mit einem spezifischen Drittanbieter-Zahlungsgateway.
- Hinzufügen eines benutzerdefinierten Reporting-Moduls mit eigenen Entitäten und APIs.
- Implementierung einer spezialisierten Authentifizierungsstrategie.
- Bereitstellung von Unterstützung für einen anderen Datenbanktyp für bestimmte Daten.

## Überlegungen

- **Abhängigkeiten zwischen Plugins:** Die Verwaltung von Abhängigkeiten zwischen Plugins kann komplex werden. Ein klarer Architekturüberblick ist erforderlich, wenn solche Abhängigkeiten häufig vorkommen.
- **Versionierung:** Die Versionierung von Plugins und die Sicherstellung der Kompatibilität mit den Kern-EAF-Versionen ist wichtig.
- **Sicherheit:** Plugins werden im selben Prozess wie die Hauptanwendung ausgeführt. Sie sollten aus vertrauenswürdigen Quellen stammen oder Sicherheitsüberprüfungen unterzogen werden, da ein bösartiges Plugin die gesamte Anwendung kompromittieren könnte.
- **Performance:** Eine große Anzahl schlecht gestalteter Plugins könnte potenziell die Startzeit der Anwendung oder die Laufzeitleistung beeinträchtigen.

Dieses Dokument bietet den konzeptionellen Rahmen für das Plugin-System im ACCI EAF. Spezifische Implementierungsdetails können je nach gewählter Ladestrategie und Plugin-Komplexität variieren.
