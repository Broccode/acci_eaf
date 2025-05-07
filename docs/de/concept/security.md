# Sicherheit im ACCI EAF

Version: 1.0
Datum: 2025-05-07
Status: Veröffentlicht

## Einleitung

Sicherheit ist ein vorrangiges Anliegen und ein erstklassiger Aspekt im ACCI EAF. Das Framework wurde unter Berücksichtigung der OWASP Top 10 entwickelt und zielt darauf ab, eine robuste Grundlage für die Erstellung sicherer Unternehmensanwendungen zu schaffen, die den Grundstein für Zertifizierungen wie ISO 27001 und SOC2 legt. Dieses Dokument beschreibt die Kernkonzepte, Mechanismen und Best Practices für Sicherheit, die in das EAF integriert sind.

## Zentrale Sicherheitspfeiler

### 1. Authentifizierung (AuthN)

Authentifizierung ist der Prozess der Überprüfung der Identität eines Benutzers, Systems oder Dienstes.

- **Mechanismus:** ACCI EAF verwendet ein flexibles Authentifizierungsmodul, das auf der Passport-Integration von NestJS basiert.
- **Unterstützte Strategien (V1):**
  - **JWT (JSON Web Token):** Bearer-Token-basierte Authentifizierung, geeignet für zustandslose API-Kommunikation. Tokens werden typischerweise nach einer erfolgreichen Anmeldung ausgestellt und bei nachfolgenden Anfragen validiert.
  - **Lokale Strategie:** Benutzername- und passwortbasierte Authentifizierung, oft für die initiale Anmeldung verwendet, um ein JWT zu erhalten.
- **Erweiterbarkeit:** Die Architektur ermöglicht bei Bedarf das Hinzufügen weiterer Authentifizierungsstrategien (z.B. OAuth2, OIDC, LDAP).
- **Benutzer-Mandanten-Verknüpfung:** Die Authentifizierung ist mandantenfähig. Benutzeridentitäten sind typischerweise auf einen bestimmten Mandanten beschränkt.

### 2. Autorisierung (AuthZ)

Autorisierung ist der Prozess, der bestimmt, ob ein authentifizierter Benutzer, ein System oder ein Dienst die erforderlichen Berechtigungen hat, um eine bestimmte Aktion auszuführen oder auf eine bestimmte Ressource zuzugreifen.

- **Mechanismus (ADR-001):** ACCI EAF verwendet eine Kombination aus rollenbasierter Zugriffskontrolle (RBAC) und grundlegender attributbasierter Zugriffskontrolle (ABAC), hauptsächlich für Eigentumsprüfungen, unter Verwendung der `casl`-Bibliothek.
  - **RBAC:** Berechtigungen werden Rollen zugewiesen, und Benutzer werden Rollen zugewiesen. Dies vereinfacht die Rechteverwaltung.
  - **ABAC (Eigentum):** Ermöglicht eine feingranulare Kontrolle, z.B. indem einem Benutzer erlaubt wird, nur die Ressourcen zu bearbeiten, deren Eigentümer er ist (z.B. über ein `ownerUserId`-Feld einer Entität).
- **`casl`-Bibliothek:** Eine leistungsstarke und flexible JavaScript/TypeScript-Bibliothek zur Verwaltung von Berechtigungen. Sie ermöglicht die Definition von Fähigkeiten (Berechtigungen) für Subjekte (Entitäten oder Konzepte) für bestimmte Aktionen (z.B. `create`, `read`, `update`, `delete`, `manage`).
- **Datenmodell:** Beinhaltet MikroORM-Entitäten für Benutzer, Rollen und Berechtigungen, die gegebenenfalls mandantenfähig sind.
- **Durchsetzung:**
  - **NestJS Guards:** `CaslGuard` (benutzerdefiniert oder aus einer Bibliothek) wird als deklarative Methode zum Schutz von NestJS-Controllern und -Resolvern verwendet. Beispiel: `@UseGuards(JwtAuthGuard, CaslGuard('update', 'Article'))`.
  - **Programmatische Prüfungen:** Dienste können auch imperative Berechtigungsprüfungen mittels `casl`-Fähigkeiten für komplexere Szenarien durchführen.
- **Mandanten-Admin-APIs:** Das Framework bietet eine Grundlage für Mandantenadministratoren zur Verwaltung von Rollen und Berechtigungen innerhalb ihres eigenen Mandantenbereichs (FR-AUTHZ).

### 3. Sichere Programmierpraktiken & Eingabevalidierung

- **DTO-Validierung:** Alle eingehenden Daten über API-Anfragen (DTOs - Data Transfer Objects) werden mittels `class-validator` und `class-transformer`, integriert mit NestJS Pipes, rigoros validiert. Dies hilft, Injection-Angriffe zu verhindern und die Datenintegrität sicherzustellen.
- **Parametrisierte Abfragen:** MikroORM verwendet von Natur aus parametrisierte Abfragen, was eine primäre Verteidigung gegen SQL-Injection-Angriffe darstellt.
- **Ausgabe-Kodierung:** Es wird darauf geachtet, dass Daten, die an Clients gesendet werden, angemessen kodiert sind, insbesondere wenn sie in HTML-Kontexten gerendert werden könnten (obwohl EAF primär Backend ist).

### 4. Schutz vor gängigen Web-Schwachstellen

- **Security Headers (`helmet`):** Die `helmet`-Middleware ist integriert, um verschiedene HTTP-Sicherheitsheader zu setzen (z.B. `X-Content-Type-Options`, `Strict-Transport-Security`, `X-Frame-Options`, `X-XSS-Protection`, `Content-Security-Policy` (Basiseinrichtung)). Diese Header weisen Browser an, Schutzmechanismen zu aktivieren, wodurch Risiken wie XSS und Clickjacking gemindert werden.
- **Rate Limiting (`@nestjs/throttler`):** Das `@nestjs/throttler`-Modul wird verwendet, um die Anzahl der Anfragen zu begrenzen, die eine IP-Adresse innerhalb eines bestimmten Zeitfensters an API-Endpunkte stellen kann. Dies bietet grundlegenden Schutz vor Brute-Force-Angriffen und einfachen Denial-of-Service (DoS)-Angriffen.
- **CSRF-Schutz:** Obwohl für zustandslose APIs, die hauptsächlich von Nicht-Browser-Clients konsumiert werden, weniger kritisch, würde bei Verwendung einer sitzungsbasierten Authentifizierung oder direkt ausgelieferten Formularen ein CSRF-Schutz (z.B. `csurf`) in Betracht gezogen.

### 5. Mandantenfähige Sicherheit (ADR-006)

Die Datenisolation zwischen Mandanten ist eine kritische Sicherheitsanforderung, die durch Row-Level Security (RLS) erreicht wird, wie im Konzeptdokument zur Mandantenfähigkeit und in ADR-006 beschrieben.

### 6. Sicherheit der Lizenzvalidierung (ADR-003)

Der Mechanismus zur Lizenzvalidierung selbst ist so konzipiert, dass er robust gegen einfache Umgehungs- und Manipulationsversuche ist, wie in ADR-003 dargelegt.

### 7. Verwaltung von Geheimnissen (Secrets)

- Konfigurationen, einschließlich sensibler Daten wie Datenbank-Zugangsdaten und API-Schlüssel, MÜSSEN über Umgebungsvariablen oder einen dedizierten Dienst zur Verwaltung von Geheimnissen (wie HashiCorp Vault oder AWS Secrets Manager in der Produktion) verwaltet werden. Geheimnisse dürfen NICHT fest im Anwendungscode verankert oder in die Versionskontrolle eingecheckt werden.
- Die `.env`-Datei wird für die lokale Entwicklung verwendet und sollte in `.gitignore` enthalten sein.

## Beobachtbarkeit für Sicherheit (Observability for Security)

- **Strukturiertes Logging:** Das Erfassen relevanter Sicherheitsereignisse (z.B. fehlgeschlagene Anmeldeversuche, Autorisierungsfehler, kritische Fehler) mit Kontext (wie `tenant_id`, `correlationId`, Quell-IP) ist entscheidend für die Sicherheitsüberwachung und Reaktion auf Vorfälle.
- **Audit Trail (Zukünftig V2+):** Obwohl ein dedizierter, unveränderlicher Audit-Trail-Dienst für V1 außerhalb des Geltungsbereichs liegt, bietet das Event-Sourcing-Muster eine gute Grundlage, da alle Zustandsänderungen als Ereignisse aufgezeichnet werden. Dies kann für zukünftige Audit-Funktionen genutzt werden.

## Testen der Sicherheit

- **Unit- und Integrationstests:** Beinhaltet Testfälle für Authentifizierungs- und Autorisierungslogik (z.B. Verhalten von Guards, Berechtigungsprüfungen) und Eingabevalidierung.
- **E2E-Tests:** Decken sicherheitsrelevante Benutzerabläufe ab.
- **Sicherheitsüberprüfungen & Penetrationstests:** Regelmäßige Sicherheitsüberprüfungen des Codes und periodische Penetrationstests (insbesondere vor größeren Releases oder für sensible Anwendungen) werden empfohlen, um Schwachstellen proaktiv zu identifizieren und zu beheben.

## Wichtige sicherheitsrelevante ADRs

- **ADR-001: Auswahl der RBAC-Bibliothek (`casl`)**
- **ADR-003: Lizenzvalidierung**
- **ADR-006: RLS-Durchsetzungsstrategie**

## Fazit

ACCI EAF integriert Sicherheit auf mehreren Ebenen, vom Architekturdesign über spezifische Bibliotheksentscheidungen bis hin zu empfohlenen Praktiken. Durch die Bereitstellung einer starken Sicherheitsgrundlage hilft das Framework Entwicklungsteams, widerstandsfähigere und vertrauenswürdigere Unternehmensanwendungen zu erstellen. Eine kontinuierliche Beachtung der Sicherheit während des gesamten Entwicklungszyklus bleibt unerlässlich.
