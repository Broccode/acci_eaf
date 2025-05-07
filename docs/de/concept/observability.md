# Beobachtbarkeit (Observability) im ACCI EAF

Version: 1.0
Datum: 2025-05-07
Status: Veröffentlicht

## Einleitung

Beobachtbarkeit ist ein kritischer Aspekt moderner Softwaresysteme. Sie ermöglicht Entwicklern und Betreibern, den internen Zustand einer Anwendung zu verstehen, Probleme zu diagnostizieren und die Leistung zu überwachen. Das ACCI EAF integriert grundlegende Beobachtbarkeitsfunktionen und bietet Anknüpfungspunkte für erweiterte Fähigkeiten.

Dieses Dokument beschreibt die Kernkonzepte und Implementierungen der Beobachtbarkeit innerhalb des Frameworks für V1, mit Ausblick auf zukünftige Erweiterungen.

## Zentrale Beobachtbarkeitspfeiler (V1)

### 1. Strukturiertes Logging

Umfassendes und konsistentes Logging ist der Eckpfeiler der Beobachtbarkeit.

- **Mechanismus:** ACCI EAF fördert die Verwendung von strukturiertem Logging (z.B. JSON-Format).
  - **Empfohlener Logger:** Obwohl nicht strikt auf eine einzelne Bibliothek in `libs/core` festgelegt, ist für NestJS-Anwendungen, die mit dem Framework generiert wurden oder es verwenden (wie `apps/control-plane-api` oder `apps/sample-app`), `pino` (z.B. über `nestjs-pino`) eine sehr empfohlene und performante Wahl.
- **Wichtige Log-Attribute:** Logs sollten wesentliche kontextbezogene Informationen enthalten:
  - `timestamp`: Zeitpunkt des Log-Ereignisses.
  - `level`: Schweregrad des Logs (z.B. `INFO`, `WARN`, `ERROR`, `DEBUG`).
  - `message`: Die Log-Nachricht.
  - `context` / `loggerName`: Name des Moduls oder der Klasse, die das Log ausgibt.
  - `tenant_id`: (Entscheidend für mandantenfähige Anwendungen) Die ID des Mandanten, der mit der Anfrage/Operation verbunden ist.
  - `correlationId`: Eine eindeutige ID, um eine einzelne Anfrage oder Operation über mehrere Dienste oder Log-Einträge hinweg zu verfolgen.
  - `errorDetails`: Stack-Traces, Fehlercodes usw. für Fehler-Logs.
- **Integration:** Hooks und Schnittstellen werden in `libs/core` oder `libs/infrastructure` bereitgestellt, um eine konsistente Logger-Instanziierung und -Verwendung in verschiedenen Teilen des Frameworks und den konsumierenden Anwendungen zu ermöglichen.

### 2. Health Checks (Gesundheitsprüfungen)

Health Checks bieten eine einfache Möglichkeit für Orchestrierungsplattformen (wie Kubernetes) oder Überwachungssysteme, um festzustellen, ob eine Anwendungsinstanz aktiv ist und bereit, Datenverkehr zu bedienen.

- **Mechanismus:** ACCI EAF verwendet das `@nestjs/terminus`-Modul zur Implementierung von Health-Check-Endpunkten.
- **Standard-Endpunkte:**
  - **Liveness Probe (z.B. `/health/live` oder `/live`):** Zeigt an, ob der Anwendungsprozess läuft. Ein Fehler hier könnte einen Orchestrator veranlassen, die Anwendungsinstanz neu zu starten.
  - **Readiness Probe (z.B. `/health/ready` oder `/ready`):** Zeigt an, ob die Anwendung bereit ist, neuen Datenverkehr anzunehmen. Diese Prüfung verifiziert typischerweise Abhängigkeiten wie Datenbankkonnektivität oder Cache-Verfügbarkeit. Ein Fehler hier könnte einen Orchestrator veranlassen, vorübergehend keinen Datenverkehr mehr an die Instanz zu senden.
- **Gängige Gesundheitsindikatoren:**
  - Datenbankkonnektivität (z.B. Verbindung zu PostgreSQL möglich).
  - Cache-Konnektivität (z.B. Verbindung zu Redis möglich).
  - Festplattenspeicher (seltener für typische EAF-Apps, aber möglich).
  - Benutzerdefinierte anwendungsspezifische Prüfungen.
- **Konfiguration:** Gesundheitsindikatoren werden innerhalb der Anwendung konfiguriert (z.B. im `AppModule` oder einem dedizierten Health-Modul).

## Vorteile der V1-Beobachtbarkeit

- **Verbessertes Debugging:** Strukturierte Logs mit Kontext erleichtern das Auffinden von Problemen.
- **Grundlegende Überwachung:** Health Checks ermöglichen eine grundlegende Betriebsüberwachung und automatisierte Wiederherstellung durch Orchestratoren.
- **Grundlage für erweiterte Funktionen:** Bereitet die Bühne für anspruchsvollere Beobachtbarkeitswerkzeuge.

## Zukünftige Überlegungen (Nach V1)

Während sich V1 auf Logging und Health Checks konzentriert, sind die folgenden üblichen nächsten Schritte zur Verbesserung der Beobachtbarkeit:

- **Metrik-Export (z.B. Prometheus):**
  - Bereitstellung wichtiger Anwendungs- und Geschäftsmetriken (z.B. Anfrageraten, Fehlerraten, Latenzen, Warteschlangenlängen, Anzahl von Geschäftstransaktionen) in einem Format, das von Überwachungssystemen wie Prometheus konsumiert werden kann.
  - Bibliotheken wie `prom-client` können integriert werden.
- **Distributed Tracing (z.B. OpenTelemetry):**
  - Die Implementierung von Distributed Tracing ermöglicht die Verfolgung einer einzelnen Anfrage, während sie durch mehrere Microservices oder Komponenten innerhalb eines verteilten Systems fließt.
  - Dies beinhaltet die Instrumentierung des Codes zur Weitergabe des Trace-Kontexts (Trace-IDs, Span-IDs) und den Export von Trace-Daten an ein Tracing-Backend (z.B. Jaeger, Zipkin, Grafana Tempo).
  - OpenTelemetry bietet dafür einen herstellerneutralen Satz von APIs und SDKs.
- **Alarmierung (Alerting):** Einrichtung von Alarmen basierend auf Metriken oder Log-Mustern, um Teams proaktiv über Probleme zu informieren.
- **Zentralisierte Logging-Plattform:** Übermittlung von Logs an eine zentralisierte Plattform (z.B. ELK Stack - Elasticsearch, Logstash, Kibana; Grafana Loki; Splunk) zur einfacheren Suche, Analyse und Visualisierung.

## Fazit

ACCI EAF V1 bietet wesentliche Beobachtbarkeitsfunktionen durch strukturiertes Logging und Health Checks. Diese Grundlage ist entscheidend für den Betrieb und die Wartung von Anwendungen, die mit dem Framework erstellt wurden. Mit zunehmender Reife und Skalierung von Projekten wird eine weitere Investition in Metriken, Distributed Tracing und zentralisierte Logging-Plattformen empfohlen, um tiefere Einblicke und ein proaktives Problemmanagement zu erreichen. Entwickler, die EAF verwenden, sollten von Anfang an konsistente Logging-Praktiken anwenden und die Health-Check-Mechanismen nutzen.
