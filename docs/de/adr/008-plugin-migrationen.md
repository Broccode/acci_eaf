# ADR-008: Handhabung von Plugin-Datenbankmigrationen (Offline Deployment)

* **Status:** Akzeptiert
* **Datum:** 2025-04-21
* **Beteiligte:** [Namen oder Rollen]

## Kontext und Problemstellung

Plugins können eigene MikroORM-Entitäten definieren. Im Offline-Deployment-Szenario (Tarball) müssen Datenbankmigrationen, die sowohl Core- als auch Plugin-Entitäten betreffen, zuverlässig ausgeführt werden können, ohne dass zur Laufzeit Code generiert oder externe Tools benötigt werden.

## Betrachtete Optionen

1. **Plugins liefern eigene Migrationsskripte:** Jedes Plugin bringt SQL-Skripte mit, die manuell ausgeführt werden müssen.
    * *Vorteile:* Isoliert.
    * *Nachteile:* Reihenfolge unklar, keine zentrale Historie, extrem fehleranfällig, nicht mit ORM-Migrationstools kompatibel.
2. **Zentrale Migrationen via MikroORM CLI:** Alle Entitäten (Core + Plugins) werden von der zentralen MikroORM-Konfiguration erkannt. Migrationen werden *während der Entwicklung* zentral generiert (`mikro-orm migration:create`). Diese Migrationsdateien werden Teil des Builds und im Tarball ausgeliefert. Ein Skript im Tarball führt dann `mikro-orm migration:up` im Container aus.
    * *Vorteile:* Nutzt Standard-MikroORM-Tooling, einzelne, konsistente Migrationshistorie, automatische Anwendung aller notwendigen Änderungen.
    * *Nachteile:* Erfordert, dass die zentrale MikroORM-Konfiguration zur Build-Zeit *alle* potenziellen Plugin-Entitäten via Glob-Patterns findet. Prozess für Entwickler muss klar sein.

## Entscheidung

Wir wählen **Option 2: Zentrale Migrationen via MikroORM CLI**. Die MikroORM-Konfiguration wird so gestaltet, dass sie alle Entitäten im Projekt findet. Migrationen werden zentral generiert und versioniert. Das `update.sh`/`setup.sh`-Skript im Tarball ist dafür verantwortlich, `npm run migration:up` (oder den äquivalenten MikroORM-CLI-Befehl) im Anwendungscontainer auszuführen, um die Datenbank auf den neuesten Stand zu bringen.

## Konsequenzen

* Die Konfiguration der Entity Discovery (`entitiesDirs` in MikroORM config) muss robust sein und alle relevanten Pfade (auch in `libs/plugins` oder `apps`) abdecken, nachdem der Build erfolgt ist (`dist/...`).
* Entwickler müssen Migrationen zentral generieren und committen.
* Das Update-Skript im Tarball erhält eine zusätzliche, kritische Aufgabe. Fehlerbehandlung bei Migrationen im Skript ist wichtig.
