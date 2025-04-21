# ADR-009: Unterstützung für idempotente Event Handler

* **Status:** Vorgeschlagen
* **Datum:** 2025-04-21
* **Beteiligte:** [Namen oder Rollen]

## Kontext und Problemstellung

Bei CQRS/ES, insbesondere wenn eine "At-Least-Once"-Zustellungsgarantie vom Event Bus oder bei Fehlern/Retries besteht, können Event Handler (Projektoren, Saga-Listener etc.) potenziell mehrfach mit demselben Event aufgerufen werden. Dies kann zu Inkonsistenzen führen, wenn die Handler nicht idempotent sind (d.h. bei Mehrfachausführung das gleiche Ergebnis liefern wie bei Einmalausführung). Das Framework sollte Entwickler beim Schreiben idempotenter Handler unterstützen.

## Betrachtete Optionen

1. **Reine Dokumentation/Konvention:** Entwickler sind selbst verantwortlich, Handler idempotent zu gestalten (z.B. durch UPSERT-Logik in Projektionen).
    * *Vorteile:* Kein Overhead im Framework.
    * *Nachteile:* Fehleranfällig, keine einheitliche Lösung, schwer sicherzustellen.
2. **Optimistic Locking / Versionierung von Read Models:** Handler prüfen Versionen von Read Models vor dem Update.
    * *Vorteile:* Löst Concurrency-Probleme gut.
    * *Nachteile:* Löst das Problem der reinen Mehrfachausführung nicht direkt, zusätzlicher Overhead in Read Models.
3. **Tracking verarbeiteter Events:** Das Framework bietet einen Mechanismus, um pro Handler zu speichern, welche Event-IDs bereits erfolgreich verarbeitet wurden.
    * *Vorteile:* Direkte Lösung für Mehrfachausführung, robust, kann zentral bereitgestellt werden.
    * *Nachteile:* Benötigt zusätzlichen Speicher (DB-Tabelle), leichte Performance-Auswirkung (Check vor Ausführung).

## Entscheidung

Wir wählen **Option 3: Tracking verarbeiteter Events** als unterstützenden Mechanismus.

1. Events erhalten standardmäßig eine eindeutige `eventId`.
2. Eine zentrale Tabelle (`processed_events`) wird genutzt (`handler_name`, `event_id`, `tenant_id`, `processed_at`).
3. Das EAF **könnte** einen Decorator (`@IdempotentEventHandler`) oder einen Basis-Service bereitstellen, der:
    * Vor der Ausführung des Handlers prüft, ob das Event für diesen Handler (+Tenant) schon verarbeitet wurde.
    * Nach erfolgreicher Ausführung des Handlers (und idealerweise der DB-Transaktion des Handlers) die Event-ID als verarbeitet markiert.
4. Mindestens aber wird dieses Muster als Best Practice dokumentiert und empfohlen.

## Konsequenzen

* Benötigt zusätzliche DB-Tabelle und Logik zum Prüfen/Schreiben.
* Entwickler müssen den Mechanismus nutzen (Decorator) oder das Muster selbst implementieren.
* Erhöht die Zuverlässigkeit der Event-Verarbeitung signifikant.
* Design des Decorators/Services muss Transaktionsgrenzen berücksichtigen (Markierung erst nach erfolgreicher Handler-Transaktion).
