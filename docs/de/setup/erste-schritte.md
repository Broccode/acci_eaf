# Enterprise Application Framework - Erste Schritte

Diese Anleitung führt Sie durch die Installation und Konfiguration des ACCI Enterprise Application Frameworks (EAF).

## Voraussetzungen

Bevor Sie beginnen, stellen Sie sicher, dass folgende Voraussetzungen erfüllt sind:

* Node.js (v18.x oder höher)
* npm (v9.x oder höher)
* Docker (v20.x oder höher)
* Docker Compose (v2.x oder höher)
* Git

## Installation

### 1. Repository klonen

```bash
git clone https://github.com/acci-org/enterprise-application-framework.git
cd enterprise-application-framework
```

### 2. Abhängigkeiten installieren

```bash
npm install
```

### 3. Umgebungsvariablen konfigurieren

Kopieren Sie die Beispiel-Umgebungsdatei und passen Sie sie an Ihre Bedürfnisse an:

```bash
cp .env.example .env
```

Öffnen Sie die `.env`-Datei und konfigurieren Sie die folgenden wichtigen Variablen:

```
# Datenbank-Konfiguration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=eaf_db
DB_USER=eaf_user
DB_PASSWORD=your_secure_password

# JWT-Konfiguration
JWT_SECRET=your_jwt_secret_key
JWT_EXPIRATION=86400

# API-Konfiguration
API_PORT=3000
API_BASE_URL=/api/v1

# Umgebung
NODE_ENV=development
```

### 4. Docker-Container starten

Um die erforderlichen Dienste (Datenbank, Redis, usw.) zu starten:

```bash
docker-compose up -d
```

### 5. Datenbankmigrationen ausführen

```bash
npm run migrate
```

### 6. Anfangsdaten laden (optional)

```bash
npm run seed
```

## Entwicklungsserver starten

Starten Sie den Entwicklungsserver mit:

```bash
npm run dev
```

Der Server ist nun unter <http://localhost:3000> erreichbar.

## Produktionsaufbau

Für eine Produktionsumgebung:

1. Kompilieren Sie die Anwendung:

```bash
npm run build
```

2. Starten Sie den Produktionsserver:

```bash
npm run start
```

## Authentifizierung

Nach der Installation erstellen Sie einen Admin-Benutzer:

```bash
npm run create:admin
```

Folgen Sie den Anweisungen auf dem Bildschirm, um die Admin-Benutzerdetails zu konfigurieren.

## Konfiguration des RBAC-Systems

Das RBAC-System (Role-Based Access Control) wird in der Datei `config/rbac.js` konfiguriert. Sie können Rollen und Berechtigungen nach Ihren organisatorischen Anforderungen definieren:

```javascript
// Beispiel für eine RBAC-Konfiguration
module.exports = {
  roles: {
    admin: {
      description: 'Administrator mit Vollzugriff',
      permissions: ['*']
    },
    manager: {
      description: 'Manager mit eingeschränktem Zugriff',
      permissions: [
        'users:read',
        'users:create',
        'reports:*'
      ]
    },
    user: {
      description: 'Standardbenutzer',
      permissions: [
        'profile:read',
        'profile:update',
        'documents:read'
      ]
    }
  }
};
```

## SBOM-Generierung

Um eine Software Bill of Materials (SBOM) zu generieren:

```bash
npm run generate:sbom
```

Dies erzeugt eine CycloneDX-kompatible SBOM-Datei im `reports`-Verzeichnis.

## Lizenzvalidierung

Führen Sie die Lizenzvalidierung mit folgendem Befehl aus:

```bash
npm run validate:licenses
```

## Fehlerbehebung

### Häufige Probleme

1. **Verbindungsprobleme mit der Datenbank**

   Stellen Sie sicher, dass Ihre Docker-Container laufen:

   ```bash
   docker-compose ps
   ```

   Überprüfen Sie die Datenbankverbindungsdetails in Ihrer `.env`-Datei.

2. **Port-Konflikte**

   Wenn der Port 3000 bereits verwendet wird, ändern Sie `API_PORT` in Ihrer `.env`-Datei.

3. **Berechtigungsprobleme**

   Auf Linux-/Unix-Systemen:

   ```bash
   chmod +x ./scripts/*.sh
   ```

### Logs überprüfen

Bei Problemen überprüfen Sie die Anwendungslogs:

```bash
npm run logs
```

## Nächste Schritte

* Machen Sie sich mit der [API-Dokumentation](../api/README.md) vertraut
* Konfigurieren Sie [CI/CD-Pipelines](../ci-cd/README.md)
* Richten Sie [Überwachung und Logging](../monitoring/README.md) ein

## Support

Für technischen Support wenden Sie sich bitte an:

* E-Mail: <support@acci-org.com>
* Helpdesk: <https://support.acci-org.com>
* Dokumentation: <https://docs.acci-org.com>
