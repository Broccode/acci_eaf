# Funktionale Anforderungsspezifikation: ACCI EAF

* **Version:** 1.0 Entwurf
* **Datum:** 2025-04-25
* **Status:** Entwurf

## Einführung

Dieses Dokument bietet eine detaillierte Aufschlüsselung der funktionalen Anforderungen, die im Product Requirements Document (PRD) für das ACCI EAF dargelegt sind. Jede funktionale Anforderung wird detailliert beschrieben, einschließlich Akzeptanzkriterien, Abhängigkeiten und Implementierungsüberlegungen.

## FR-CORE: Framework-Kern

### FR-CORE-01: Monorepo-Struktur

**Beschreibung:** Implementieren Sie eine Monorepo-Struktur für das EAF unter Verwendung von Nx als empfohlenes Tooling.

**Akzeptanzkriterien:**

* Klare Trennung der Verzeichnisse `apps/` und `libs/`
* Korrekte Workspace-Konfiguration für Nx
* Geeignete Projektreferenzen und Grenzen
* Gemeinsame Linting- und Formatierungsregeln
* Konsistente Build- und Testkonfigurationen

**Implementierungsüberlegungen:**

* Erwägen Sie die Verwendung von Nx-Bibliothekstypen (feature, util, data-access) zur besseren Organisation
* Richten Sie geeignete Tags zur Durchsetzung von Modulgrenzen ein
* Konfigurieren Sie CI-freundliche Befehle zum Testen und Bauen

### FR-CORE-02: CQRS-Basisbibliotheken

**Beschreibung:** Implementieren Sie Kernbibliotheken für das CQRS-Muster, einschließlich Command-, Query- und Event-Bussen.

**Akzeptanzkriterien:**

* Command Bus-Implementierung mit Middleware-Unterstützung
* Query Bus-Implementierung mit Middleware-Unterstützung
* Event Bus-Implementierung mit Unterstützung für synchrone und asynchrone Ereignisbehandlung
* Typsichere Schnittstellen für Commands, Queries und Events
* Standardmäßige Fehlerbehandlungs- und Logging-Hooks

**Implementierungsüberlegungen:**

* Berücksichtigen Sie vorhandene Bibliotheken (z. B. `@nestjs/cqrs`), passen Sie diese jedoch bei Bedarf für die Mandantenfähigkeit an
* Stellen Sie eine korrekte Typisierung für starke Typsicherheit sicher
* Entwerfen Sie mit Blick auf Testbarkeit (z. B. einfaches Mocking von Bussen)

### FR-CORE-03: Event-Sourcing-Basis

**Beschreibung:** Implementieren Sie Basiskomponenten für Event Sourcing.

**Akzeptanzkriterien:**

* Event Store-Schnittstelle und Standard-PostgreSQL-Implementierung
* Unterstützung für das Laden und Speichern von Aggregaten über Events
* Mechanismus zur Event-Versionierung
* Snapshot-Unterstützung (Basis)
* Unterstützung für die Mandantenisolation von Events

**Implementierungsüberlegungen:**

* Design für zukünftige Schemaevolution
* Berücksichtigen Sie Leistungsauswirkungen, insbesondere bei großen Event-Streams
* Fügen Sie eine ordnungsgemäße Fehlerbehandlung für Nebenläufigkeitsprobleme hinzu

### FR-CORE-04: Plugin-Lader

**Beschreibung:** Erstellen Sie ein Plugin-System, das die Erweiterung des EAF ermöglicht, ohne Kernkomponenten zu ändern.

**Akzeptanzkriterien:**

* Plugin-Schnittstellendefinition
* Plugin-Entdeckungs- und Lademechanismus
* Unterstützung für Plugin-Lebenszyklus-Hooks (init, shutdown)
* Methode für Plugins zur Registrierung ihrer MikroORM-Entitäten
* Ordnungsgemäße Fehlerbehandlung bei Plugin-Ladefehlern

**Implementierungsüberlegungen:**

* Stellen Sie sicher, dass Plugins sich nicht gegenseitig stören können
* Berücksichtigen Sie dynamische vs. statische Ladeansätze
* Entwerfen Sie mit Blick auf die Testbarkeit von Plugin-Interaktionen

## FR-MT: Mandantenfähigkeit (Multi-Tenancy)

### FR-MT-01: Mandanten-ID-Ermittlung

**Beschreibung:** Implementieren Sie Mechanismen zur Ermittlung der aktuellen Mandanten-ID aus Anfragen.

**Akzeptanzkriterien:**

* Unterstützung für das Extrahieren der Mandanten-ID aus JWT-Tokens
* Unterstützung für das Extrahieren der Mandanten-ID aus Headern
* Fallback-Mechanismen und Fehlerbehandlung
* Konfigurationsoptionen für verschiedene Mandanten-ID-Quellen

**Implementierungsüberlegungen:**

* Sicherheitsüberlegungen (Verhinderung von Mandanten-ID-Spoofing)
* Leistung (Caching, wo angemessen)
* Klare Fehlermeldungen zur Fehlersuche

### FR-MT-02: Mandantenkontext

**Beschreibung:** Implementieren Sie einen systemweiten Mandantenkontext unter Verwendung von AsyncLocalStorage.

**Akzeptanzkriterien:**

* AsyncLocalStorage-Implementierung für den Mandantenkontext
* Kontextweitergabe über asynchrone Grenzen hinweg
* Hilfsfunktionen für den einfachen Zugriff auf den Mandantenkontext
* Ordnungsgemäße Bereinigung nach Abschluss der Anfrage

**Implementierungsüberlegungen:**

* Berücksichtigen Sie die Anforderungen an die Node.js-Version (AsyncLocalStorage ist relativ neu)
* Entwerfen Sie mit Blick auf Testbarkeit
* Dokumentieren Sie potenzielle Fallstricke bei asynchronen Operationen

### FR-MT-03: Row-Level Security über MikroORM-Filter

**Beschreibung:** Implementieren Sie die Mandantenisolation mithilfe von MikroORM-Filtern.

**Akzeptanzkriterien:**

* Globale Filterimplementierung, die die `tenant_id`-Bedingung hinzufügt
* Integration mit dem Mandantenkontext, um die aktuelle Mandanten-ID zu erhalten
* Unterstützung für den Ausschluss bestimmter Entitäten vom Filtern
* Geeignete Testsuite zur Überprüfung der Isolation

**Implementierungsüberlegungen:**

* Bewertung der Leistungsauswirkungen
* Dokumentation zur Erstellung mandantenfähiger Entitäten
* Handhabung mandantenübergreifender Operationen

## FR-CP: Control Plane API

### FR-CP-01: Mandantenverwaltungs-API

**Beschreibung:** Implementieren Sie eine separate API für Systemadministratoren zur Verwaltung von Mandanten.

**Akzeptanzkriterien:**

* Erstellen, Lesen, Aktualisieren, Löschen (CRUD)-Operationen für Mandanten
* Unterstützung für grundlegende Mandantenattribute (Name, Beschreibung, Status)
* Unterstützung für mandantenspezifische Konfiguration
* API-Dokumentation (OpenAPI)

**Implementierungsüberlegungen:**

* Authentifizierung und Autorisierung für Systemadministratoren
* Audit-Logging für alle Mandantenverwaltungsvorgänge
* Berücksichtigen Sie die zukünftige Erweiterbarkeit für zusätzliche Mandantenattribute

### FR-CP-02: Admin-Authentifizierung und -Autorisierung

**Beschreibung:** Implementieren Sie Authentifizierung und Autorisierung für die Control Plane.

**Akzeptanzkriterien:**

* Sicherer Authentifizierungsmechanismus für Systemadministratoren
* Rollenbasierte Autorisierung für Control Plane-Operationen
* Unterstützung für mehrere Administratorkonten mit unterschiedlichen Berechtigungen
* Ordnungsgemäßes Sitzungsmanagement und Sicherheitsheader

**Implementierungsüberlegungen:**

* Erwägen Sie einen separaten Authentifizierungsmechanismus von Mandantenanwendungen
* Implementieren Sie ordnungsgemäße Passwortrichtlinien und MFA (falls erforderlich)
* Entwerfen Sie unter Berücksichtigung von Sicherheits-Best-Practices

## FR-PA: Persistenzadapter

### FR-PA-01: MikroORM-Setup für PostgreSQL

**Beschreibung:** Konfigurieren Sie MikroORM für die Verwendung mit PostgreSQL, mit Unterstützung für Mandantenisolation.

**Akzeptanzkriterien:**

* Basiskonfiguration für MikroORM mit PostgreSQL
* Entitätsermittlung über Glob-Muster
* Migrationsverwaltungssystem
* Transaktionsmanagement mit Unit of Work-Muster
* Integration mit dem Mandantenkontext für RLS

**Implementierungsüberlegungen:**

* Optionen zur Leistungsoptimierung
* Konfiguration für verschiedene Umgebungen
* Einstellungen für das Connection Pooling

### FR-PA-02: Redis-Cache-Adapter

**Beschreibung:** Implementieren Sie einen Redis-basierten Caching-Adapter.

**Akzeptanzkriterien:**

* Redis-Verbindungskonfiguration
* Cache-Service mit Standardoperationen (get, set, delete, flush)
* Unterstützung für mandantenspezifische Cache-Schlüssel
* TTL (Time-to-Live)-Unterstützung
* Ordnungsgemäße Fehlerbehandlung bei Redis-Fehlern

**Implementierungsüberlegungen:**

* Fallback-Mechanismen, falls Redis nicht verfügbar ist
* Serialisierungs-/Deserialisierungsstrategien
* Konfigurationsoptionen für unterschiedliche Caching-Anforderungen

## FR-AUTHN: Authentifizierung

### FR-AUTHN-01: Authentifizierungsmodul

**Beschreibung:** Erstellen Sie ein Basis-Authentifizierungsmodul mit Unterstützung für verschiedene Strategien.

**Akzeptanzkriterien:**

* Steckbares Authentifizierungsstrategie-System
* Benutzerentität und Repository
* Passwort-Hashing und -Validierung (für lokale Strategie)
* Sitzungsmanagement (falls zutreffend)

**Implementierungsüberlegungen:**

* Sicherheits-Best-Practices (OWASP)
* Zukünftige Erweiterbarkeit für zusätzliche Authentifizierungsmethoden
* Compliance-Anforderungen

### FR-AUTHN-02: JWT-Strategie

**Beschreibung:** Implementieren Sie JWT-basierte Authentifizierung.

**Akzeptanzkriterien:**

* JWT-Generierung und -Validierung
* Mechanismus zur Token-Aktualisierung
* Integration mit dem Mandantenkontext
* Sicheres Schlüsselmanagement
* Konfigurierbare Token-Ablaufzeit

**Implementierungsüberlegungen:**

* Strategie zum Widerrufen von Tokens
* Mechanismus zur Schlüsselrotation
* Überlegungen zur Payload-Größe

### FR-AUTHN-03: Lokale Strategie

**Beschreibung:** Implementieren Sie die Authentifizierung mit Benutzername/Passwort.

**Akzeptanzkriterien:**

* Sichere Passwortbehandlung
* Schutz vor Brute-Force-Angriffen
* Mechanismus zur Kontosperrung
* Funktionalität zum Zurücksetzen des Passworts

**Implementierungsüberlegungen:**

* Konfiguration der Passwortkomplexitätsrichtlinie
* Erwägen Sie die Hinzufügung von Multi-Faktor-Authentifizierungsunterstützung
* Einhaltung von Sicherheits-Best-Practices

### FR-AUTHN-04: Benutzer-Mandanten-Verknüpfung

**Beschreibung:** Implementieren Sie die Beziehung zwischen Benutzern und Mandanten.

**Akzeptanzkriterien:**

* Datenmodell für Benutzer-Mandanten-Zuordnungen
* Unterstützung für Benutzer, die mehreren Mandanten angehören
* Mechanismus zum Mandantenwechsel
* Benutzerverwaltung im Mandantenkontext

**Implementierungsüberlegungen:**

* Berücksichtigen Sie die Leistung für Benutzer mit vielen Mandanten
* Handhaben Sie den Widerruf des Mandantenzugriffs
* Berücksichtigen Sie Sonderfälle wie Mandantenadministratorbenutzer

## FR-AUTHZ: Autorisierung

### FR-AUTHZ-01: RBAC-Modul

**Beschreibung:** Implementieren Sie rollenbasierte Zugriffskontrolle unter Verwendung von CASL.

**Akzeptanzkriterien:**

* Integration mit der CASL-Bibliothek
* Definition von Standardrollen und Berechtigungen
* Berechtigungsprüfungs-Guards für Controller/Services
* Unterstützung für mandantenspezifische Rollenkonfigurationen

**Implementierungsüberlegungen:**

* Leistungsoptimierung für Berechtigungsprüfungen
* Caching-Strategien für häufig geprüfte Berechtigungen
* Design für einfache Erweiterung mit benutzerdefinierten Berechtigungen

### FR-AUTHZ-02: Datenmodell für RBAC

**Beschreibung:** Erstellen Sie Datenbankentitäten zur Speicherung von RBAC-Informationen.

**Akzeptanzkriterien:**

* Entitäten für Rollen, Berechtigungen, Benutzer-Rollen-Zuweisungen
* Mandantenfähiges Design (`tenant_id` in relevanten Tabellen)
* Datenbankindizes zur Leistungssteigerung
* Unterstützung für hierarchische Rollen (falls zutreffend)

**Implementierungsüberlegungen:**

* Balance zwischen Flexibilität und Leistung
* Berücksichtigen Sie zukünftige Migrationsanforderungen
* Ordnungsgemäße Constraints und referenzielle Integrität

### FR-AUTHZ-03: RBAC-Guards

**Beschreibung:** Implementieren Sie NestJS-Guards zur Durchsetzung von RBAC.

**Akzeptanzkriterien:**

* Guards auf Controller- und Methodenebene
* Unterstützung für die Prüfung mehrerer Berechtigungen
* Klare Fehlermeldungen bei unbefugtem Zugriff
* Integration mit dem Mandantenkontext

**Implementierungsüberlegungen:**

* Leistung (Minimierung von DB-Abfragen für Prüfungen)
* Ordnungsgemäße Fehlerbehandlung
* Testbarkeit

### FR-AUTHZ-04: Eigentumsprüfungen

**Beschreibung:** Implementieren Sie grundlegende attributbasierte Zugriffskontrolle für Eigentum.

**Akzeptanzkriterien:**

* Unterstützung für die Prüfung, ob ein Benutzer eine Ressource besitzt
* Integration mit dem RBAC-System
* Generische Implementierung, die für verschiedene Entitätstypen verwendbar ist
* Leistungsoptimierung

**Implementierungsüberlegungen:**

* Design für die Erweiterbarkeit auf andere Attribute als Eigentum
* Balance zwischen Sicherheit und Leistung
* Berücksichtigen Sie Caching-Strategien

### FR-AUTHZ-05: Mandanten-Admin-APIs für RBAC

**Beschreibung:** Erstellen Sie APIs für Mandantenadministratoren zur Verwaltung von RBAC.

**Akzeptanzkriterien:**

* APIs zur Verwaltung von Rollen und Berechtigungen
* APIs zur Zuweisung von Rollen zu Benutzern
* Ordnungsgemäße Autorisierungsprüfungen für diese Operationen
* API-Dokumentation

**Implementierungsüberlegungen:**

* Berücksichtigen Sie UI-Anforderungen für zukünftige Admin-Oberfläche
* Audit-Logging für Rollen-/Berechtigungsänderungen
* Balance zwischen Flexibilität und Komplexität

## FR-LIC: Lizenzvalidierung

### FR-LIC-01: Lizenzvalidierungsmodul

**Beschreibung:** Erstellen Sie ein Modul zur Validierung von Lizenzen.

**Akzeptanzkriterien:**

* Logik zur Lizenzvalidierung
* Lizenzdatenmodell
* Periodischer Validierungsmechanismus
* Integration mit dem Anwendungsstartprozess

**Implementierungsüberlegungen:**

* Sicherheit gegen Manipulation
* Unterstützung für Offline-Validierung
* Handhabung von Toleranzperioden

### FR-LIC-02: Prüfung von Lizenzbeschränkungen

**Beschreibung:** Implementieren Sie die Prüfung von Lizenzbeschränkungen.

**Akzeptanzkriterien:**

* Unterstützung für verschiedene Beschränkungstypen (Benutzer, Funktionen, Zeit)
* Mechanismus zur Durchsetzung von Beschränkungen
* Klare Fehlermeldungen bei Lizenzverstößen
* Konfigurierbare Aktionen bei Verstößen (Warnung, eingeschränkter Modus, Abschaltung)

**Implementierungsüberlegungen:**

* Leistungsauswirkungen
* Handhabung von Grenzfällen
* Zukünftige Erweiterbarkeit für neue Beschränkungstypen

## FR-OBS: Beobachtbarkeit (Observability)

### FR-OBS-01: Strukturiertes Logging

**Beschreibung:** Implementieren Sie ein strukturiertes Logging-Framework.

**Akzeptanzkriterien:**

* Logger-Schnittstelle und Implementierung
* Unterstützung für verschiedene Log-Level
* Mandantenkontext in Log-Einträgen
* Request-ID-Tracking
* Standard-Logformat (JSON empfohlen)

**Implementierungsüberlegungen:**

* Leistungsauswirkungen
* Konfiguration für verschiedene Umgebungen
* Umgang mit sensiblen Daten

### FR-OBS-02: Health-Check-Endpunkte

**Beschreibung:** Implementieren Sie Health-Check-Endpunkte unter Verwendung von @nestjs/terminus.

**Akzeptanzkriterien:**

* `/health/live`-Endpunkt für Liveness-Prüfungen
* `/health/ready`-Endpunkt für Readiness-Prüfungen
* Benutzerdefinierte Health-Indikatoren für Datenbank, Cache usw.
* Ordnungsgemäße Dokumentation

**Implementierungsüberlegungen:**

* Sicherheitsüberlegungen (öffentliche vs. geschützte Endpunkte)
* Leistung (vermeiden Sie teure Prüfungen an häufig aufgerufenen Endpunkten)
* Integration mit Überwachungssystemen

## FR-SEC: Sicherheit

### FR-SEC-01: Helmet-Integration

**Beschreibung:** Integrieren Sie Helmet für HTTP-Sicherheitsheader.

**Akzeptanzkriterien:**

* Korrekte Konfiguration der Helmet-Middleware
* Einrichtung der Content Security Policy
* HTTPS-Erzwingung
* Konfigurationsoptionen für verschiedene Umgebungen

**Implementierungsüberlegungen:**

* Auswirkungen auf Frontend-Anwendungen
* Tests über verschiedene Browser hinweg
* Balance zwischen Sicherheit und Funktionalität

### FR-SEC-02: Ratenbegrenzung (Rate Limiting)

**Beschreibung:** Implementieren Sie Ratenbegrenzung unter Verwendung von @nestjs/throttler.

**Akzeptanzkriterien:**

* Globale Ratenbegrenzungskonfiguration
* Routenspezifische Ratenbegrenzungsoptionen
* Korrekte Antwortheader (verbleibende Limits, Rücksetzzeit)
* Konfigurationsoptionen für verschiedene Umgebungen

**Implementierungsüberlegungen:**

* Berücksichtigen Sie unterschiedliche Ratenlimits für authentifizierte vs. anonyme Benutzer
* IP-basierte vs. benutzerbasierte Begrenzung
* Redis-Speicher für verteilte Umgebungen

## FR-I18N: Internationalisierung

### FR-I18N-01: NestJS-i18n-Setup

**Beschreibung:** Konfigurieren Sie nestjs-i18n für Internationalisierungsunterstützung.

**Akzeptanzkriterien:**

* Basiskonfiguration von nestjs-i18n
* Struktur der Übersetzungsdateien
* Mechanismus zur Spracherkennung
* Fallback auf Standardsprache

**Implementierungsüberlegungen:**

* Leistungsauswirkungen
* Dateiorganisation zur Wartbarkeit
* Berücksichtigen Sie den Übersetzungsworkflow

### FR-I18N-02: Validierungsübersetzung

**Beschreibung:** Implementieren Sie die Übersetzung für Validierungsfehlermeldungen.

**Akzeptanzkriterien:**

* Integration mit class-validator
* Übersetzungsschlüssel für häufige Validierungsfehler
* Unterstützung für benutzerdefinierte Validierungsmeldungen
* Beispiele und Dokumentation

**Implementierungsüberlegungen:**

* Überlegungen zur Benutzererfahrung
* Konsistenz über verschiedene Validatoren hinweg
* Unterstützung von Template-Variablen in Übersetzungen

### FR-I18N-03: Fehlerübersetzung

**Beschreibung:** Implementieren Sie die Übersetzung für Fehlermeldungen.

**Akzeptanzkriterien:**

* Übersetzungssystem für API-Fehlerantworten
* Standardfehlercodes mit Übersetzungen
* Unterstützung für Fehlerparameter in Übersetzungen
* Konsistentes Fehlerantwortformat

**Implementierungsüberlegungen:**

* Fehlerkategorisierung
* Balance zwischen Sicherheit und Nützlichkeit
* Berücksichtigen Sie spezialisierte Fehlerbehandlung für verschiedene Kontexte

### FR-I18N-04: Bereitstellung des i18n-Service

**Beschreibung:** Stellen Sie i18n-Services in der gesamten Anwendung zur Verfügung.

**Akzeptanzkriterien:**

* Injizierbarer i18n-Service
* Hilfsmethoden für gängige Übersetzungsanforderungen
* Kontextabhängige Übersetzung (unter Berücksichtigung der aktuellen Sprache)
* Dokumentation und Beispiele

**Implementierungsüberlegungen:**

* Leistungsoptimierung
* Integration mit mandantenspezifischen Sprachpräferenzen
* Teststrategien

## FR-API: API-Standards

### FR-API-01: Standard-Controller-Struktur

**Beschreibung:** Definieren und implementieren Sie Standards für API-Controller.

**Akzeptanzkriterien:**

* Standard-Controller-Basisklassen oder Decorators
* Konsistentes Antwortformat
* Standardisierung der Fehlerbehandlung
* Logging-Integration

**Implementierungsüberlegungen:**

* Balance zwischen Standardisierung und Flexibilität
* Berücksichtigen Sie verschiedene API-Stile (REST, GraphQL)
* Dokumentationsgenerierung

### FR-API-02: DTO-Validierung mit i18n

**Beschreibung:** Implementieren Sie die DTO-Validierung mit Internationalisierungsunterstützung.

**Akzeptanzkriterien:**

* Integration von class-validator mit i18n
* Standard-Validierungsdecorators
* Konsistentes Validierungsfehlerformat
* Unterstützung von Validierungsgruppen

**Implementierungsüberlegungen:**

* Leistungsauswirkungen
* Benutzererfahrung bei Validierungsfehlern
* Teststrategien

### FR-API-03: OpenAPI-Setup

**Beschreibung:** Konfigurieren Sie die Generierung von OpenAPI-Dokumentation.

**Akzeptanzkriterien:**

* OpenAPI-Setup mit korrekten Metadaten
* Dokumentation für alle API-Endpunkte
* Schemagenerierung für DTOs
* Authentifizierungsdokumentation
* Beispiele für Schlüsseloperationen

**Implementierungsüberlegungen:**

* Balance zwischen Dokumentationsdetail und Wartungsaufwand
* Versionsverwaltung
* Integration mit der Generierung von API-Clients (falls zutreffend)

## FR-SBOM: Software Bill of Materials

### FR-SBOM-01: SBOM-Generierung

**Beschreibung:** Implementieren Sie die SBOM-Generierung im Build-Prozess.

**Akzeptanzkriterien:**

* Integration des SBOM-Generierungstools
* Standardformat (CycloneDX empfohlen)
* Automatisierung in CI/CD-Pipelines
* Dokumentation zur SBOM-Nutzung

**Implementierungsüberlegungen:**

* Genauigkeit der Abhängigkeitsinformationen
* Leistungsauswirkungen auf Builds
* Sicherheitsimplikationen von exponierten Informationen

## FR-DEPLOY: Deployment

### FR-DEPLOY-01: Prozess zur Erstellung von Offline-Paketen

**Beschreibung:** Definieren Sie einen Prozess innerhalb der CI/CD-Pipeline zur Generierung eines eigenständigen Offline-Deployment-Pakets (Tarball).

**Akzeptanzkriterien:**

* CI/CD-Job existiert zur Erstellung des Tarballs (z. B. `.tar.gz`).
* Tarball enthält allen notwendigen Anwendungscode, Abhängigkeiten, Konfigurationsvorlagen und Skripte.
* Prozess ist automatisiert und wiederholbar.
* Paket enthält Versionsinformationen.

**Implementierungsüberlegungen:**

* Wahl des Komprimierungsformats und der Werkzeuge.
* Scripting für eine zuverlässige Paketzusammenstellung.
* Korrekte Handhabung von Build-Artefakten.

### FR-DEPLOY-02: Paketinhalt

**Beschreibung:** Spezifizieren Sie den erforderlichen Inhalt des Offline-Deployment-Tarballs.

**Akzeptanzkriterien:**

* Enthält Anwendungs-Docker-Images, gespeichert via `docker save`.
* Enthält eine Referenz-`docker-compose.yml`-Datei, die für die Ziel-VM-Umgebung geeignet ist.
* Enthält eine Referenz-`.env.example`-Datei, die erforderliche Umgebungsvariablen detailliert.
* Enthält Installations- und Update-Skripte (`install.sh`, `update.sh`).
* Enthält notwendige Dokumentationsausschnitte (z. B. Verweis auf grundlegende Einrichtungsanleitung).
* Enthält Datenbankmigrationsdateien.

**Implementierungsüberlegungen:**

* Sicherstellung, dass alle notwendigen Docker-Image-Layer enthalten sind.
* Versionierung von Compose-Dateien und Skripten zusammen mit der Anwendung.
* Klarheit und Vollständigkeit der `.env.example`-Datei.
* Sicherstellung, dass Migrationsdateien korrekt für die Ausführung platziert sind.

### FR-DEPLOY-03: Installations- und Update-Skripte

**Beschreibung:** Stellen Sie robuste Skripte innerhalb des Tarballs für die Erstinstallation und nachfolgende Updates auf der Ziel-VM bereit.

**Akzeptanzkriterien:**

* `install.sh`-Skript handhabt die Ersteinrichtung: Laden von Docker-Images, Erstellen notwendiger Volumes/Netzwerke, Initialisieren der Umgebung aus `.env` (falls vorhanden), Starten von Containern via `docker-compose up`.
* `update.sh`-Skript handhabt die Aktualisierung auf eine neue Version: Stoppen von Containern, Laden neuer Docker-Images, Ausführen von Migrationen, Neustarten von Containern.
* Skripte geben dem Benutzer klares Feedback über Fortschritt und Erfolg/Misserfolg.
* Skripte führen grundlegende Fehlerprüfungen durch (z. B. Verfügbarkeit erforderlicher Tools).
* Skripte sind hinsichtlich Nutzung und Voraussetzungen dokumentiert.

**Implementierungsüberlegungen:**

* Annahmen über die Zielumgebung (z. B. Verfügbarkeit von Docker, Docker Compose, `bash`).
* Benutzerberechtigungen, die zum Ausführen der Skripte erforderlich sind.
* Handhabung von Konfigurationsunterschieden zwischen Umgebungen (hauptsächlich über `.env`-Datei).
* Idempotenz, wo machbar (z. B. Netzwerk-/Volume-Erstellung).
* Strategie für Backup/Rollback (grundlegende Überlegungen für V1, potenziell manuelle Schritte).

### FR-DEPLOY-04: Datenbankmigrationsmechanismus

**Beschreibung:** Stellen Sie sicher, dass Datenbankmigrationen (unter Verwendung von MikroORM-Migrationen) zuverlässig als Teil des Installations-/Update-Prozesses in der Offline-Umgebung ausgeführt werden können.

**Akzeptanzkriterien:**

* Installations-/Update-Skripte lösen den Datenbankmigrationsbefehl im entsprechenden Schritt aus (z. B. nach dem Laden der Images, vor dem Starten der Anwendungscontainer).
* Mechanismus handhabt die korrekte Erstellung des initialen Schemas bei der Erstinstallation.
* Mechanismus wendet nachfolgende Migrationen während Updates korrekt an.
* Der Ausführungsstatus der Migration (Erfolg/Misserfolg) wird von den Skripten klar protokolliert oder ausgegeben.

**Implementierungsüberlegungen:**

* Das Ausführen von Migrationen erfordert normalerweise einen dedizierten Befehl oder einen kurzlebigen Container, der auf dem Anwendungsimage basiert.
* Sicherstellung, dass die Datenbank zugänglich ist, wenn Migrationen ausgeführt werden.
* Berechtigungen, die für den Datenbankbenutzer erforderlich sind, der Migrationen durchführt.
* Fehlerhafte Migrationen innerhalb der Skriptausführung ordnungsgemäß behandeln.
