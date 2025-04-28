# ACCI EAF - Setup Guide

## 1. Einführung

    * Zweck des Dokuments (Installation & lokale Inbetriebnahme des EAF/Beispiel-App).
    * Überblick über das ACCI EAF und die Monorepo-Struktur.
    * Zielgruppe (Entwickler bei Axians).

## 2. Voraussetzungen

    * **Software:**
        * Node.js (Spezifische LTS-Version angeben, z.B. >= 18.x).
        * Paketmanager: npm / yarn / pnpm (Bevorzugten Manager und Version angeben).
        * Docker & Docker Compose (Aktuelle Versionen).
        * Git Client.
        * (Optional) PostgreSQL Client (z.B. psql, DBeaver, pgAdmin) zur Datenbankinspektion.
        * (Optional) Redis Client (z.B. redis-cli) zur Cache-Inspektion.
        * (Optional) Nx CLI global installiert (`npm install -g nx`) oder Nutzung via `npx nx ...`.
    * **Hardware:** (Mindestanforderungen an RAM, CPU falls relevant).
    * **Zugriffsrechte:** Ggf. Zugriff auf das Axians Git-Repository, privates NPM-Registry (falls verwendet).

## 3. Erste Schritte

    * Repository klonen (`git clone <repository-url>`).
    * Ins Projektverzeichnis wechseln (`cd acci-eaf-monorepo`).
    * Installation der Abhängigkeiten (Root `package.json`, Befehl: `npm install` oder `yarn install` etc.).
    * Kurze Erklärung der wichtigsten Verzeichnisse (`apps/`, `libs/`, `docs/`, `tools/`).

## 4. Konfiguration

    * `.env`-Dateien:
        * Lokale `.env`-Datei(en) aus `.env.example` erstellen (z.B. im Root, in `apps/control-plane-api`, `apps/sample-app`).
        * Erklärung der wichtigsten Umgebungsvariablen:
            * `DATABASE_URL` (für MikroORM).
            * `REDIS_URL`.
            * `JWT_SECRET`, `JWT_EXPIRATION`.
            * Logging-Level (`LOG_LEVEL`).
            * Ports für die Anwendungen (z.B. `CONTROL_PLANE_PORT`, `SAMPLE_APP_PORT`).
            * (Ggf. Variablen für Lizenzdatei-Pfad, AuthN-Provider etc.).
    * Bezug zum `@nestjs/config` Modul.

## 5. Lokale Entwicklungsumgebung starten

    * **Abhängigkeiten starten (Docker Compose):**
        * Befehl: `docker-compose up -d` (startet PostgreSQL, Redis etc. aus der `docker-compose.yml` im Root).
        * Überprüfen, ob Container laufen (`docker ps`).
    * **Datenbank-Migrationen ausführen:**
        * Befehl: `npx nx run infrastructure-persistence:migration:up` (Beispiel, tatsächlicher Befehl hängt von Nx-Konfig und MikroORM CLI ab) oder `npm run migration:up`. Erklären, dass dies das Schema initialisiert/aktualisiert.
    * **Anwendungen starten (Nx):**
        * Control Plane API: `npx nx serve control-plane-api`.
        * Beispiel-Anwendung: `npx nx serve sample-app`.
        * Erwartete Ports und Log-Ausgaben.
    * **Erster Admin / Bootstrap (Control Plane):**
        * Ausführen des CLI-Befehls (ADR-007): `npx nx run control-plane-api:cli -- setup-admin --email admin@example.com --password VERYsecurePASSWORD` (Beispiel).
        * Weitere Informationen zu CLI-Befehlen finden Sie in der [CLI-Befehle Dokumentation](./cli-befehle.md).
    * **Zugriff testen:**
        * Beispiel-`curl`-Aufrufe oder Postman-Anfragen an Health Checks oder einfache Endpunkte.

## 6. Tests ausführen

    * Alle Tests ausführen: `npx nx run-many --target=test --all`.
    * Tests für ein spezifisches Projekt: `npx nx test <projektname>` (z.B. `nx test core-domain`).
    * Nur Unit-Tests / Integrationstests ausführen (falls separate Targets konfiguriert sind oder über Dateinamen-Pattern).
    * E2E-Tests ausführen: `npx nx e2e <app-name>-e2e`.
    * Code Coverage anzeigen (Befehl und Speicherort des Reports).

## 7. Anwendungen bauen

    * Build für eine spezifische Anwendung: `npx nx build <app-name>`.
    * Build aller betroffenen Projekte: `npx nx affected:build`.
    * Erklärung des `dist/` Verzeichnisses.

## 8. Deployment (Offline / Tarball - Für Produkt-Deployments)

    * **8.1 Paket erstellen (CI/CD-Schritt):**
        * Kurze Erklärung, wie der Tarball in der CI/CD gebaut wird (`docker save`, Skripte etc.). (Dies ist eher informativ für Entwickler).
    * **8.2 Installation auf Ziel-VM (Kunden-Szenario):**
        * Voraussetzungen auf der VM (Docker, Docker Compose).
        * Tarball übertragen & entpacken.
        * Docker Images laden (`docker load -i ...`).
        * `.env`-Datei konfigurieren.
        * Setup-Skript (`setup.sh`) ausführen (was macht es? inkl. Migrationen).
        * Starten mit `docker-compose up -d`.
    * **8.3 Update auf Ziel-VM (Kunden-Szenario):**
        * Neuen Tarball übertragen & entpacken.
        * Update-Skript (`update.sh`) ausführen (was macht es? `docker load`, `docker-compose down`, Backup-Hinweis, **Migrationen ausführen**, `docker-compose up -d`).
    * **8.4 Datenbank-Migrationen ausführen (Manuell/Skript):**
        * Detaillierter Befehl zum Ausführen der Migrationen im Container (z.B. `docker-compose run --rm <app_service_name> npm run migration:up`).
    * **8.5 Backup & Restore (Empfehlungen):**
        * Hinweis auf Wichtigkeit von Backups der Docker Volumes (insb. Postgres).
        * Empfohlene Strategien oder Verweis auf externe Tools/Dokumentation.

## 9. Troubleshooting

    * Häufige Probleme beim Setup oder Start und deren Lösungen (z.B. Port-Konflikte, DB-Verbindungsfehler, Migrationsprobleme).
    * Wie man Logs aus Docker Compose abruft (`docker-compose logs`).
    * Wo man weitere Hilfe bekommt (interne Kontakte, Channels).
