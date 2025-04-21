# ADR-010: Strategie für Event Schema Evolution

* **Status:** Akzeptiert
* **Datum:** 2025-04-21
* **Beteiligte:** [Namen oder Rollen]

## Kontext und Problemstellung

In langlebigen Event-Sourced-Systemen ändern sich die Anforderungen und damit die Struktur von Domain Events über die Zeit. Es wird eine Strategie benötigt, wie das System mit alten Event-Versionen umgehen kann, wenn Aggregate geladen oder Projektionen aufgebaut werden.

## Betrachtete Optionen

1. **Keine Strategie / Big Bang Migration:** Alte Events werden ignoriert oder erfordern komplexe, einmalige Datenmigrationen des gesamten Event Stores. Sehr riskant und aufwändig.
2. **Schwache Schemas / Tolerant Reader:** Events als JSON speichern und Handler/Aggregate so schreiben, dass sie fehlende/zusätzliche Felder tolerieren. Einfach zu starten, aber implizit und fehleranfällig bei komplexen Änderungen.
3. **Mehrere Event-Typen:** Für jede Änderung wird ein neuer Event-Typ eingeführt (`UserRegisteredV1`, `UserRegisteredV2`). Handler/Aggregate müssen alle relevanten Versionen kennen. Kann zu sehr vielen Event-Typen führen.
4. **Event-Versionierung und Upcasting:** Events haben eine Versionsnummer. Beim Laden alter Events werden "Upcaster"-Funktionen angewendet, um sie auf die aktuelle Version zu transformieren, bevor sie verarbeitet werden.

## Entscheidung

Wir wählen **Option 4: Event-Versionierung und Upcasting** als bevorzugte Strategie für das ACCI EAF.

1. Jedes Event erhält Metadaten `eventId` und `eventVersion`.
2. Payloads werden als `JSONB` gespeichert.
3. Der Event Store Adapter wird so konzipiert, dass er eine Upcasting-Pipeline unterstützt (Hooks für Upcaster-Funktionen beim Laden von Events).
4. Für V1 werden keine komplexen Upcaster implementiert, aber die Architektur und die Event-Metadaten sehen dies vor. Die Strategie wird dokumentiert.

## Konsequenzen

* Design des Event Store Adapters muss Upcasting ermöglichen.
* Event-Metadaten (`eventVersion`) sind von Anfang an erforderlich.
* Entwickler müssen bei Event-Änderungen die Version erhöhen und Upcaster implementieren (Prozess muss definiert werden).
* Bietet eine robuste und explizite Methode zur Handhabung von Schemaänderungen über die Zeit.
