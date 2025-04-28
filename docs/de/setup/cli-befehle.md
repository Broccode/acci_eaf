# ACCI EAF - CLI-Befehle

## Einführung

Dieses Dokument bietet Informationen über die verfügbaren Command Line Interface (CLI) Befehle im ACCI Enterprise Application Framework. Diese Befehle unterstützen bei administrativen Aufgaben, Bootstrapping und Management-Operationen.

## Überblick der Befehle

Die CLI-Befehle sind mit [nest-commander](https://docs.nestjs.com/recipes/nest-commander) implementiert, einem Paket, das das NestJS-Framework mit der Commander.js CLI-Bibliothek integriert. Dies ermöglicht die Erstellung robuster, testbarer CLI-Befehle, die die Dependency-Injection und Modulstruktur von NestJS nutzen.

### Control Plane API CLI-Befehle

#### Admin-Benutzer einrichten

Der Befehl `setup-admin` wird verwendet, um den ersten administrativen Benutzer für die Control Plane API zu erstellen. Dieser Befehl implementiert den Bootstrapping-Mechanismus, der in ADR-007 beschrieben ist.

**Befehlssyntax:**

```bash
npx nx run control-plane-api:cli -- setup-admin --email <admin-email> --password <sicheres-passwort>
```

**Optionen:**

- `--email`, `-e`: Die E-Mail-Adresse für den Admin-Benutzer (erforderlich)
- `--password`, `-p`: Das Passwort für den Admin-Benutzer (erforderlich, min. 8 Zeichen)

**Beispiele:**

```bash
# Einen neuen Admin-Benutzer erstellen
npx nx run control-plane-api:cli -- setup-admin --email admin@example.com --password sicheresPasswort123

# Mit Kurzoptionen
npx nx run control-plane-api:cli -- setup-admin -e admin@example.com -p sicheresPasswort123
```

**Verhalten:**

- Wenn bereits ein Admin mit der angegebenen E-Mail existiert, wird kein Duplikat erstellt (idempotente Operation)
- Der Befehl validiert das E-Mail-Format und die Passwortlänge
- Das Passwort wird sicher gehasht, bevor es gespeichert wird
- Bei Erfolg gibt der Befehl eine Bestätigungsnachricht mit der E-Mail des erstellten Admins aus

**Fehlerbehandlung:**

- Ungültiges E-Mail-Format: Der Befehl wird mit einem Fehler beendet, wenn das E-Mail-Format ungültig ist
- Passwort zu kurz: Der Befehl wird mit einem Fehler beendet, wenn das Passwort weniger als 8 Zeichen hat
- Fehlende Parameter: Der Befehl wird mit einem Fehler beendet, wenn entweder E-Mail oder Passwort fehlt

## Ausführung in Produktionsumgebungen

In Produktionsumgebungen (unter Verwendung der Offline-Tarball-Deployment-Methode) sollten CLI-Befehle innerhalb des Docker-Containers ausgeführt werden:

```bash
docker exec -it control-plane-api-container node /app/cli.js setup-admin --email admin@example.com --password sicheresPasswort123
```

Oder mit `docker-compose`:

```bash
docker-compose run --rm control-plane-api node /app/cli.js setup-admin --email admin@example.com --password sicheresPasswort123
```

## Sicherheitsaspekte

- Verwenden Sie immer starke Passwörter bei der Erstellung von Admin-Benutzern
- Erwägen Sie die Verwendung von Umgebungsvariablen oder einer sicheren Methode zur Übergabe sensibler Informationen wie Passwörter
- CLI-Befehle sollten nur für autorisiertes Personal zugänglich sein
- In Produktionsumgebungen sollte ein dediziertes Servicekonto für die Ausführung administrativer Befehle verwendet werden

## Erweiterung der CLI

Das CLI-Framework ermöglicht die Erstellung zusätzlicher Befehle nach Bedarf. Um einen neuen Befehl zu erstellen:

1. Erstellen Sie eine neue Befehlsklasse, die `CommandRunner` aus nest-commander erweitert
2. Implementieren Sie die erforderlichen Methoden (`run` und Option-Parser)
3. Fügen Sie den Befehl dem entsprechenden CLI-Modul hinzu

Weitere Informationen finden Sie in der [nest-commander Dokumentation](https://docs.nestjs.com/recipes/nest-commander).
