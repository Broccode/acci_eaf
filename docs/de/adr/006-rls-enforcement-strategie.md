# ADR-006: RLS Enforcement Strategie

* **Status:** Vorgeschlagen
* **Datum:** 2025-04-21
* **Beteiligte:** [Namen oder Rollen]

## Kontext und Problemstellung

Für die Multi-Tenancy des ACCI EAF wurde Row-Level Security (RLS) mittels `tenant_id`-Spalte beschlossen. Es wird eine zuverlässige und zentrale Methode benötigt, um sicherzustellen, dass alle Datenbankabfragen über MikroORM automatisch und korrekt nach der `tenant_id` des aktuellen Request-Kontextes gefiltert werden.

## Betrachtete Optionen

1. **Manuelle Filterung in Repositories:** Jede Repository-Methode fügt manuell `.andWhere({ tenantId: ... })` hinzu.
    * *Vorteile:* Explizit.
    * *Nachteile:* Extrem fehleranfällig (Vergessen des Filters), viel Boilerplate.
2. **Basis-Repository-Klasse:** Eine Basisklasse fügt den Filter in überschriebenen Methoden (`find`, `findOne` etc.) hinzu.
    * *Vorteile:* Reduziert Boilerplate etwas.
    * *Nachteile:* Greift nicht für Query Builder oder `EntityManager`; Disziplin erforderlich, die Basisklasse zu nutzen.
3. **MikroORM Filters (Global):** Nutzung der eingebauten Filter-Funktionalität von MikroORM.
    * *Vorteile:* Zentral definiert, wird automatisch vom ORM angewendet, weniger Boilerplate in Repositories, unterstützt alle Abfragemethoden (EM, Repo, QB).
    * *Nachteile:* Erfordert korrekte Parametrisierung pro Request.

## Entscheidung

Wir wählen **Option 3: MikroORM Filters**. Es wird ein globaler Filter `@Filter({ name: 'tenant', ... })` auf mandantenfähigen Entitäten definiert. Die Parametrisierung (`tenantId`) erfolgt pro Request über `EntityManager.setFilterParams()` mittels einer NestJS Middleware, die den Wert aus dem `AsyncLocalStorage` (bereitgestellt durch `libs/tenancy`) liest.

## Konsequenzen

* Positive: Hohe Sicherheit gegen versehentlich vergessene Filter; zentrale RLS-Logik; sauberere Repository-Implementierungen.
* Negative / Risiken: Korrekte Implementierung der Middleware und der `RequestContext`-Handhabung von MikroORM ist entscheidend. Performance-Auswirkungen der globalen Filter müssen beobachtet werden.
* Implikationen: `libs/tenancy` stellt den Kontext bereit, Middleware in `libs/infrastructure` (oder App-Modul) setzt den Filter-Parameter.
