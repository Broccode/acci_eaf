# ACCI EAF API-Referenz

Version: 1.0
Datum: 2025-05-07
Status: Platzhalter

## Einleitung

Dieser Abschnitt enthält die API-Referenzdokumentation für Anwendungen, die mit dem ACCI Enterprise Application Framework (EAF) erstellt wurden, wie z.B. die `Control-Plane API` und jede `Sample App` oder benutzerdefinierte Anwendungen.

Die API-Dokumentation beschreibt verfügbare Endpunkte, Anfrage-/Antwort-Schemata, Authentifizierungsmethoden und Statuscodes.

## Generierung der API-Dokumentation

ACCI EAF-Anwendungen nutzen typischerweise das Modul `@nestjs/swagger` (oder `@nestjs/OpenAPI`), um automatisch eine OpenAPI (früher Swagger)-Spezifikation aus den vorhandenen Controllern, DTOs und Entitätsdefinitionen zu generieren, die mit entsprechenden Decorators (`@ApiProperty()`, `@ApiOperation()`, etc.) versehen sind.

**Typischer Generierungsprozess:**

1. **Code dekorieren:** Stellen Sie sicher, dass Ihre DTOs, Entitäten und Controller-Methoden mit `@nestjs/swagger`-Decorators versehen sind, um Metadaten für die OpenAPI-Spezifikation bereitzustellen.
2. **Swagger-Modul einrichten:** Konfigurieren Sie in Ihrer `main.ts` (oder einem dedizierten Swagger-Modul) das `SwaggerModule`, um die OpenAPI-Spezifikation und die Swagger-UI zu erstellen und bereitzustellen.

    ```typescript
    // Beispiel in main.ts
    async function bootstrap() {
      const app = await NestFactory.create(AppModule);

      // ... andere App-Konfigurationen (Pipes, Middleware, etc.)

      const config = new DocumentBuilder()
        .setTitle('ACCI EAF Control Plane API')
        .setDescription('API zur Verwaltung von Mandanten und Systemeinstellungen')
        .setVersion('1.0')
        .addBearerAuth() // Bei Verwendung von JWT Bearer Tokens
        .build();
      const document = SwaggerModule.createDocument(app, config);
      SwaggerModule.setup('api-docs', app, document); // Stellt UI unter /api-docs bereit

      await app.listen(3000);
    }
    bootstrap();
    ```

3. **Zugriff auf die Dokumentation:** Sobald die Anwendung läuft, ist die interaktive Swagger UI typischerweise unter `http://localhost:3000/api-docs` (oder dem in `SwaggerModule.setup` konfigurierten Pfad) verfügbar. Die rohe OpenAPI JSON-Spezifikation ist oft unter `http://localhost:3000/api-docs-json` verfügbar.

## Wichtige APIs

Obwohl die detaillierte, automatisch generierte Dokumentation konsultiert werden sollte, hier ein allgemeiner Überblick über wichtige API-Bereiche:

### 1. Control Plane API (`apps/control-plane-api`)

- **Authentifizierung (`/auth`):**
  - `POST /auth/login`: Authentifizieren und JWT empfangen.
- **Mandantenverwaltung (`/tenants`):**
  - `POST /tenants`: Einen neuen Mandanten erstellen.
  - `GET /tenants`: Alle Mandanten auflisten.
  - `GET /tenants/{id}`: Einen spezifischen Mandanten abrufen.
  - `PUT /tenants/{id}`: Einen Mandanten aktualisieren.
  - `DELETE /tenants/{id}`: Einen Mandanten löschen.
- **Health Checks (`/health`):**
  - `GET /health/live`: Liveness-Probe.
  - `GET /health/ready`: Readiness-Probe.
- **(Zukünftig) RBAC-Verwaltung für Control Plane Admins.**

### 2. Beispielanwendungs-API (`apps/sample-app`)

- Illustrative Endpunkte, die EAF-Funktionen im Mandantenkontext demonstrieren.
- Endpunkte sind mandantenfähig und durch AuthN/AuthZ wie konfiguriert geschützt.

## Authentifizierung & Autorisierung

- APIs sind typischerweise durch JWT Bearer-Token-Authentifizierung geschützt (siehe `/auth/login`).
- Die Autorisierung wird mittels RBAC/ABAC (`casl`) durchgesetzt, wie in `docs/de/concept/security.md` beschrieben.
- Spezifische Berechtigungen, die für jeden Endpunkt erforderlich sind, sollten in der OpenAPI-Spezifikation detailliert sein (z.B. über benutzerdefinierte Decorators oder Beschreibungen).

## Rate Limiting & Security Headers

- API-Endpunkte unterliegen einem Rate Limiting (`@nestjs/throttler`).
- Standard-Sicherheitsheader werden über `helmet` angewendet.

---

*Dieses Dokument dient als Platzhalter. Für die genaueste und detaillierteste API-Referenz konsultieren Sie bitte die live generierte OpenAPI/Swagger-Dokumentation, die von jeder Anwendungsinstanz bereitgestellt wird.*
