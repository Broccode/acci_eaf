# Setup-Anleitung

Willkommen zur ACCI EAF Setup-Anleitung. Dieser Abschnitt erklärt, wie Sie das Framework auf Ihrer lokalen Maschine und in verschiedenen Deployment-Umgebungen zum Laufen bringen.

> **Status:** Entwurf – Inhaltliche Platzhalter sind mit `TODO` markiert.

## Inhaltsverzeichnis

1. [Voraussetzungen](#voraussetzungen)
2. [Lokale Entwicklungsumgebung](#lokale-entwicklungsumgebung)
3. [Docker Compose Umgebung](#docker-compose-umgebung)
4. [Offline Tarball Installation](#offline-tarball-installation)
5. [Bootstrap-Skript](#bootstrap-skript)
6. [Fehlerbehebung](#fehlerbehebung)
7. [SBOM-Erzeugung](#sbom-erzeugung)

---

## Voraussetzungen

Um effektiv mit dem ACCI EAF arbeiten zu können, stellen Sie sicher, dass Ihre Entwicklungsumgebung die folgenden Voraussetzungen erfüllt:

- **Node.js:** Version 20 oder höher. (Überprüfen mit `node -v`)
- **npm:** Version 9 oder höher (normalerweise mit Node.js installiert). (Überprüfen mit `npm -v`)
- **Docker:** Version 24 oder höher. (Überprüfen mit `docker --version`)
- **Git:** Für die Versionskontrolle. (Überprüfen mit `git --version`)
- **Nx CLI (Optional aber empfohlen):** Wenn Sie Nx-Befehle direkt verwenden möchten, installieren Sie es global oder verwenden Sie `npx`. (Überprüfen mit `nx --version`)

## Lokale Entwicklungsumgebung

Folgen Sie diesen Schritten, um das ACCI EAF für die lokale Entwicklung einzurichten:

1. **Repository klonen:**

    ```bash
    git clone <repository-url>
    cd acci-eaf
    ```

2. **Abhängigkeiten installieren:**

    ```bash
    npm ci --legacy-peer-deps
    ```

    *Hinweis: `--legacy-peer-deps` könnte abhängig von der aktuellen Struktur des Abhängigkeitsbaums notwendig sein.*
3. **Umgebungskonfiguration:**
    - Kopieren Sie die Beispiel-Umgebungsdatei:

      ```bash
      cp .env.example .env
      ```

    - Passen Sie die Variablen in `.env` nach Bedarf an (z.B. Datenbank-Zugangsdaten, Ports).
4. **Docker-Dienste starten (falls nicht bereits aktiv):**
    Das Framework ist typischerweise auf PostgreSQL und Redis angewiesen. Stellen Sie sicher, dass diese laufen, zum Beispiel über das bereitgestellte Docker Compose Setup (siehe nächster Abschnitt).
5. **Datenbankmigrationen ausführen:**

    ```bash
    npx nx run-many --target=migration:run --all
    # Oder für eine spezifische Anwendung, z.B. control-plane-api:
    # npx nx run control-plane-api:migration:run
    ```

6. **Eine Anwendung starten (z.B. `control-plane-api`):**

    ```bash
    npx nx serve control-plane-api
    ```

    Die Anwendung ist dann typischerweise unter `http://localhost:3000` (oder wie konfiguriert) verfügbar.

## Docker Compose Umgebung

Das ACCI EAF beinhaltet eine `docker-compose.yml`-Datei, um das Setup von externen Diensten wie PostgreSQL und Redis zu vereinfachen.

1. **Stellen Sie sicher, dass Docker läuft.**
2. **Navigieren Sie zum Projekt-Root-Verzeichnis.**
3. **Dienste starten:**

    ```bash
    docker-compose up -d
    ```

    Dies startet PostgreSQL und Redis im Hintergrund (detached mode).
4. **Dienste stoppen:**

    ```bash
    docker-compose down
    ```

5. **Logs anzeigen:**

    ```bash
    docker-compose logs -f <service_name> # z.B. postgresql
    ```

    Informationen zu Dienstnamen und Konfigurationen finden Sie in der `docker-compose.yml`-Datei. Die Standard-Datenbank-Zugangsdaten und Ports sind üblicherweise in dieser Datei oder in der referenzierten `.env`-Datei definiert.

## Offline Tarball Installation

Das ACCI EAF unterstützt eine Offline-Installationsmethode mittels eines Tarball-Pakets, was besonders nützlich für Umgebungen ohne Internetzugang (air-gapped) ist. Dieser Prozess ist mit FR-DEPLOY abgestimmt.

1. **Tarball erstellen (Entwickleraufgabe):**
    Ein CI/CD-Prozess oder ein manuelles Skript generiert ein Tarball (z.B. `acci-eaf-offline-vX.Y.Z.tar.gz`). Dieses Paket beinhaltet:
    - Docker-Images (gespeichert mittels `docker save`) für alle Anwendungen und Dienste.
    - Anwendungs-Bundles und Abhängigkeiten.
    - Setup- und Update-Skripte.
    - Die `docker-compose.yml`, konfiguriert für den Offline-Gebrauch.
2. **Tarball übertragen:**
    Übertragen Sie das Tarball sicher in die Ziel-Offline-Umgebung.
3. **Installationsschritte auf der Zielmaschine:**
    a.  **Docker-Images laden:**
        ```bash
        # Beispielbefehl, tatsächliches Skript kann abweichen
        tar -xzf acci-eaf-offline-vX.Y.Z.tar.gz
        cd acci-eaf-offline
        ./load-images.sh # Dieses Skript würde 'docker load < image.tar' verwenden
        ```
    b.  **Umgebung konfigurieren:**
        Richten Sie die `.env`-Datei mit den passenden Konfigurationen für die Zielumgebung ein.
    c.  **Setup-Skript ausführen:**
        Ein Installationsskript (z.B. `install.sh` oder `setup.sh`) wird:
        - Notwendige Dienste mittels `docker-compose -f docker-compose.offline.yml up -d` starten.
        - Datenbankmigrationen ausführen.
        - Jegliche initiale Datensynchronisation oder Bootstrap-Prozeduren durchführen.
    Beachten Sie die spezifischen Anweisungen, die mit dem Tarball geliefert werden, für die genauen Befehle.

## Bootstrap-Skript

Die Control Plane API benötigt einen initialen administrativen Benutzer und potenziell weitere Kerndaten, um eingerichtet zu werden. Dies wird durch ein Bootstrap-Skript oder einen Prozess gemäß ADR-007 gehandhabt.

- **Ausführung:** Dieses Skript wird typischerweise einmal nach der initialen Bereitstellung oder einem größeren Update ausgeführt.

    ```bash
    # Beispielbefehl (tatsächlicher Befehl kann ein Nx-Target oder eine direkte Skriptausführung sein)
    npx nx run control-plane-api:bootstrap
    ```

- **Funktionalität:**
  - Erstellt den ersten administrativen Benutzer für die Control Plane.
  - Kann Standardrollen oder Berechtigungen einrichten.
  - Initialisiert alle anderen wesentlichen Anfangsdaten.
- **Sicherheit:** Die Zugangsdaten für den initialen Admin-Benutzer werden üblicherweise über sichere Umgebungsvariablen bereitgestellt oder nach Abschluss des Skripts angezeigt und sollten sofort geändert werden.

    Weitere Details zum Bootstrap-Prozess finden Sie in ADR-007 und der Dokumentation der Control Plane API.

## Fehlerbehebung

TODO: Häufige Fallstricke und Lösungen sammeln.

## SBOM-Erzeugung

Sie können lokal eine Software Bill of Materials (SBOM) im CycloneDX JSON-Format erzeugen:

```bash
npm run generate:sbom
# Ausgabe: dist/sbom.json
```

Der Befehl führt das Nx-Target `sbom:generate-sbom` aus, welches `cyclonedx-npm` verwendet. Stellen Sie sicher, dass die Abhängigkeiten zuvor installiert wurden (`npm ci --legacy-peer-deps`).
