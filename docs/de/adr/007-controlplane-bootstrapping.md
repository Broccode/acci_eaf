# ADR-007: Control Plane Bootstrapping

* **Status:** Akzeptiert
* **Datum:** 2025-04-21
* **Beteiligte:** [Namen oder Rollen]

## Kontext und Problemstellung

Die Control Plane API benötigt mindestens einen initialen System-Administrator-Benutzer (und ggf. einen initialen Mandanten), um nach der Installation/dem ersten Start überhaupt nutzbar zu sein. Dieser initiale Setup-Prozess muss definiert werden, insbesondere für das Offline-Deployment-Szenario.

## Betrachtete Optionen

1. **Manuelle Datenbank-Einträge:** Admin muss nach der Installation manuell SQL-Befehle ausführen.
    * *Vorteile:* Einfachste Implementierung im EAF.
    * *Nachteile:* Fehleranfällig, umständlich für den Kunden/Admin, nicht idempotent.
2. **Automatischer Check beim Start:** Die API prüft beim Start, ob Admins existieren und legt ggf. einen Standard-Admin an.
    * *Vorteile:* Automatisiert.
    * *Nachteile:* "Magisch", Passwort-Management schwierig, wann genau soll es laufen?
3. **Dedizierter CLI-Befehl:** Ein Kommandozeilenbefehl (Teil der Control Plane API) zum initialen Setup.
    * *Vorteile:* Explizit, kontrollierbar, idempotent implementierbar, kann Parameter (Passwort) sicher entgegennehmen, gut skriptbar (für Setup/Update-Skripte).
    * *Nachteile:* Erfordert Ausführung eines separaten Befehls nach der Installation.

## Entscheidung

Wir wählen **Option 3: Dedizierter CLI-Befehl**. Die Control Plane API wird einen Befehl wie `setup-admin --email <email> --password <password>` bereitstellen (ausführbar z.B. via `npm run cli -- ...` im Container). Dieser Befehl prüft, ob bereits ein Admin existiert und legt andernfalls den ersten Admin-Benutzer sicher an.

## Konsequenzen

* Setup-/Installations-Skripte im Tarball müssen diesen Befehl nach dem ersten Start der Container aufrufen.
* Der Befehl muss idempotent sein (darf bei erneutem Aufruf nichts tun oder melden, dass Setup erfolgt ist).
* Prozess muss im Setup-Guide klar dokumentiert sein.
