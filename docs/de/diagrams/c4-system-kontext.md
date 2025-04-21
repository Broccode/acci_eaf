# C4 Systemkontextdiagramm - ACCI EAF

* **Version:** 1.0 Entwurf
* **Datum:** 2025-04-25
* **Status:** Entwurf

## Systemkontextdiagramm

```mermaid
C4Context
title Systemkontextdiagramm für ACCI EAF

Person(tenant_benutzer, "Mandanten-Benutzer", "Ein Benutzer einer mit dem EAF erstellten Anwendung, gehört zu einem bestimmten Mandanten")
Person(tenant_admin, "Mandanten-Administrator", "Verwaltet Benutzer, Rollen und Berechtigungen innerhalb eines bestimmten Mandanten")
Person(system_admin, "Systemadministrator", "Verwaltet Mandanten und globale Einstellungen")
Person(entwickler, "Anwendungsentwickler", "Entwickelt Anwendungen mit dem EAF")

System_Boundary(eaf, "ACCI EAF") {
    System(tenant_anwendung, "Mandanten-Anwendung", "Mit EAF erstellte Unternehmensanwendung, unterstützt mehrere Mandanten")
    System(steuerungs_api, "Steuerungsebene API", "Verwaltet Mandanten und globale Konfiguration")
}

System_Ext(lizenz_system, "Axians Lizenzsystem", "Validiert Anwendungslizenzen")
System_Ext(auth_provider, "Externer Auth-Provider", "Optionale externe Authentifizierung (OAuth, OIDC)")
System_Ext(monitoring, "Monitoringsysteme", "Metriken, Protokollierung und Alarmierung")

Rel(tenant_benutzer, tenant_anwendung, "Nutzt")
Rel(tenant_admin, tenant_anwendung, "Verwaltet mandantenspezifische Einstellungen, Benutzer und Rollen")
Rel(system_admin, steuerungs_api, "Verwaltet Mandanten und globale Einstellungen")
Rel(entwickler, eaf, "Erstellt Anwendungen mit")

Rel(tenant_anwendung, lizenz_system, "Validiert Lizenz", "HTTPS")
Rel(tenant_anwendung, auth_provider, "Authentifiziert sich mit (optional)", "HTTPS")
Rel(tenant_anwendung, monitoring, "Sendet Metriken und Logs", "HTTPS")
Rel(steuerungs_api, monitoring, "Sendet Metriken und Logs", "HTTPS")

UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

## Containerdiagramm

```mermaid
C4Container
title Containerdiagramm für ACCI EAF

Person(tenant_benutzer, "Mandanten-Benutzer", "Ein Benutzer einer mit dem EAF erstellten Anwendung")
Person(tenant_admin, "Mandanten-Administrator", "Verwaltet mandantenspezifische Einstellungen")
Person(system_admin, "Systemadministrator", "Verwaltet Mandanten und globale Einstellungen")

System_Boundary(eaf, "ACCI EAF") {
    Container(tenant_api, "Mandanten-API", "NestJS", "RESTful API für Mandantenanwendungen")
    Container(steuerungs_api, "Steuerungsebene API", "NestJS", "API für Mandantenverwaltung")
    
    ContainerDb(event_store, "Ereignisspeicher", "PostgreSQL", "Speichert Domänenereignisse mit Mandantenisolation")
    ContainerDb(read_models, "Lesemodelle", "PostgreSQL", "Optimierte Datenmodelle für Abfragen")
    ContainerDb(cache, "Cache", "Redis", "Zwischenspeichert häufig abgerufene Daten")
    
    Container(core_libs, "Kernbibliotheken", "TypeScript", "Domänen- und Anwendungsschichten, Kernlogik")
    Container(infra_libs, "Infrastrukturbibliotheken", "TypeScript", "Adapter für Datenbanken, Messaging etc.")
    Container(tenancy_lib, "Mandantenfähigkeits-Bibliothek", "TypeScript", "Implementierung der Mandantenfähigkeit")
    Container(rbac_lib, "RBAC-Bibliothek", "TypeScript", "Rollenbasierte Zugriffskontrolle")
    Container(licensing_lib, "Lizenzierungs-Bibliothek", "TypeScript", "Lizenzvalidierungsmechanismen")
}

System_Ext(lizenz_system, "Axians Lizenzsystem", "Validiert Anwendungslizenzen")
System_Ext(auth_provider, "Externer Auth-Provider", "Optionale externe Authentifizierung")
System_Ext(monitoring, "Monitoringsysteme", "Metriken, Protokollierung und Alarmierung")

Rel(tenant_benutzer, tenant_api, "Nutzt", "HTTPS")
Rel(tenant_admin, tenant_api, "Verwaltet Mandanteneinstellungen", "HTTPS")
Rel(system_admin, steuerungs_api, "Verwaltet Mandanten", "HTTPS")

Rel(tenant_api, core_libs, "Nutzt")
Rel(tenant_api, infra_libs, "Nutzt")
Rel(tenant_api, tenancy_lib, "Nutzt")
Rel(tenant_api, rbac_lib, "Nutzt")
Rel(tenant_api, licensing_lib, "Nutzt")

Rel(steuerungs_api, core_libs, "Nutzt")
Rel(steuerungs_api, infra_libs, "Nutzt")

Rel(core_libs, event_store, "Speichert Ereignisse", "SQL")
Rel(infra_libs, read_models, "Liest/Schreibt Daten", "SQL")
Rel(infra_libs, cache, "Cached Daten", "Redis Protokoll")

Rel(licensing_lib, lizenz_system, "Validiert Lizenz", "HTTPS")
Rel(tenant_api, auth_provider, "Authentifiziert sich mit (optional)", "HTTPS")
Rel(tenant_api, monitoring, "Sendet Metriken und Logs", "HTTPS")
Rel(steuerungs_api, monitoring, "Sendet Metriken und Logs", "HTTPS")

UpdateLayoutConfig($c4ShapeInRow="4", $c4BoundaryInRow="1")
```

## Komponentendiagramm (Kernbibliotheken)

```mermaid
C4Component
title Komponentendiagramm für ACCI EAF Kernbibliotheken

Container_Boundary(core_libs, "Kernbibliotheken") {
    Component(domain_layer, "Domänenschicht", "TypeScript", "Aggregate, Entities, Value Objects, Domänenereignisse")
    Component(application_layer, "Anwendungsschicht", "TypeScript", "Use Cases, Command/Query Handler")
    
    Component(command_bus, "Command Bus", "TypeScript", "Leitet Befehle an Handler weiter")
    Component(query_bus, "Query Bus", "TypeScript", "Leitet Abfragen an Handler weiter")
    Component(event_bus, "Event Bus", "TypeScript", "Verteilt Ereignisse an Handler")
    
    Component(aggregate_base, "Aggregat-Basis", "TypeScript", "Basisklasse für Domänenaggregate")
    Component(repository_interfaces, "Repository-Schnittstellen", "TypeScript", "Ports für Persistenz")
    Component(domain_services, "Domänenservices", "TypeScript", "Komplexe Domänenoperationen")
}

Container(tenant_api, "Mandanten-API", "NestJS", "RESTful API für Mandantenanwendungen")
ContainerDb(event_store, "Ereignisspeicher", "PostgreSQL", "Speichert Domänenereignisse mit Mandantenisolation")
ContainerDb(read_models, "Lesemodelle", "PostgreSQL", "Optimierte Datenmodelle für Abfragen")

Container_Boundary(infra_libs, "Infrastrukturbibliotheken") {
    Component(postgres_event_store, "PostgreSQL Ereignisspeicher", "TypeScript", "Implementierung der Ereignisspeicherung")
    Component(read_model_repositories, "Lesemodell-Repositories", "TypeScript", "MikroORM-basierte Repositories")
    Component(nestjs_controllers, "NestJS Controller", "TypeScript", "API-Endpunkte")
}

Rel(tenant_api, nestjs_controllers, "Nutzt")
Rel(nestjs_controllers, command_bus, "Leitet Befehle weiter")
Rel(nestjs_controllers, query_bus, "Leitet Abfragen weiter")

Rel(command_bus, application_layer, "Leitet weiter an")
Rel(query_bus, application_layer, "Leitet weiter an")
Rel(event_bus, application_layer, "Leitet weiter an")

Rel(application_layer, domain_layer, "Nutzt")
Rel(application_layer, repository_interfaces, "Nutzt")
Rel(domain_layer, aggregate_base, "Erweitert")
Rel(domain_layer, event_bus, "Veröffentlicht Ereignisse an")

Rel(postgres_event_store, repository_interfaces, "Implementiert")
Rel(read_model_repositories, read_models, "Liest/Schreibt", "SQL/MikroORM")
Rel(postgres_event_store, event_store, "Speichert Ereignisse", "SQL/MikroORM")

UpdateLayoutConfig($c4ShapeInRow="3", $c4BoundaryInRow="1")
```

## Komponentendiagramm (Mandantenfähigkeit)

```mermaid
C4Component
title Komponentendiagramm für ACCI EAF Mandantenfähigkeit

Container_Boundary(tenancy_lib, "Mandantenfähigkeits-Bibliothek") {
    Component(tenant_context, "Mandantenkontext", "TypeScript", "AsyncLocalStorage-basierter Mandantenkontext")
    Component(tenant_middleware, "Mandanten-Middleware", "TypeScript", "Extrahiert Mandanten-ID aus Anfrage")
    Component(tenant_provider, "Mandanten-Provider", "TypeScript", "Injizierbarer Service für Mandantenkontext")
    Component(tenant_entity, "Mandanten-Entität", "TypeScript", "Mandanten-Datenmodell")
    Component(tenant_service, "Mandantenservice", "TypeScript", "Mandanten-CRUD-Operationen")
    Component(tenant_filter, "MikroORM Mandantenfilter", "TypeScript", "Wendet tenant_id-Filterung an")
}

Container(tenant_api, "Mandanten-API", "NestJS", "RESTful API für Mandantenanwendungen")
Container(steuerungs_api, "Steuerungsebene API", "NestJS", "API für Mandantenverwaltung")
ContainerDb(database, "Datenbank", "PostgreSQL", "Speichert mandantenisolierte Daten")

Rel(tenant_api, tenant_middleware, "Nutzt")
Rel(tenant_middleware, tenant_context, "Setzt Mandanten-ID in")
Rel(tenant_api, tenant_provider, "Injiziert")
Rel(tenant_provider, tenant_context, "Ruft ab aus")
Rel(tenant_filter, tenant_context, "Erhält Mandanten-ID von")
Rel(tenant_filter, database, "Wendet `WHERE tenant_id = ?` auf Abfragen an")
Rel(steuerungs_api, tenant_service, "Nutzt")
Rel(tenant_service, tenant_entity, "Verwaltet")

UpdateLayoutConfig($c4ShapeInRow="4", $c4BoundaryInRow="1")
```

*Hinweis: Diese Diagramme sollten in einem Markdown-Editor oder Renderer betrachtet werden, der Mermaid-Syntax unterstützt. Sie verwenden die C4-Modellnotation, die eine hierarchische Methode zur Beschreibung der Softwarearchitektur auf verschiedenen Detailebenen bietet.*
