# ADR-001: Auswahl von CASL als RBAC-Implementierungsbibliothek

* **Status:** Akzeptiert
* **Datum:** 2025-04-25
* **Autoren:** [Systemarchitekt]

## Kontext

Das ACCI EAF benötigt ein robustes System für rollenbasierte Zugriffskontrolle (RBAC) mit folgenden Kernanforderungen:

1. Unterstützung für die Definition und Durchsetzung rollenbasierter Berechtigungen
2. Fähigkeit, mit attributbasierten Regeln zu arbeiten (insbesondere Eigentumsprüfung)
3. Tenant-Isolation bei Berechtigungsprüfungen
4. Integration mit NestJS
5. Typsichere API mit guter TypeScript-Unterstützung
6. Wartbare und erweiterbare Berechtigungsdefinitionen
7. Gute Leistungsmerkmale für Berechtigungsprüfungen
8. Aktive Wartung und Community-Unterstützung

Mehrere Optionen wurden in Betracht gezogen:

* **CASL**: Eine vielseitige isomorphe Autorisierungsbibliothek
* **AccessControl**: Eine rollen- und attributbasierte Zugriffskontrollbibliothek
* **NestJS RBAC**: NestJS-spezifische RBAC-Implementierung
* **Eigene Implementierung**: Entwicklung eines eigenen RBAC-Systems von Grund auf

## Entscheidung

Wir werden **CASL** als RBAC-Implementierungsbibliothek für das ACCI EAF verwenden.

## Begründung

CASL wurde aus folgenden Gründen ausgewählt:

1. **Ausdrucksstärke**: CASL bietet eine flexible und intuitive API für die Definition von Berechtigungen, die über einfache rollenbasierte Prüfungen hinausgeht und attributbasierte Bedingungen wie Eigentum unterstützt.

2. **TypeScript-Unterstützung**: CASL bietet hervorragende TypeScript-Integration mit starker Typisierung, was mit unseren Entwicklungspraktiken übereinstimmt.

3. **Framework-Agnostik**: Während es leicht mit NestJS integrierbar ist, ist CASL framework-agnostisch, was mit unserem Prinzip der hexagonalen Architektur übereinstimmt, Kernlogik von Infrastruktur zu trennen.

4. **Aktive Wartung**: CASL wird aktiv gepflegt, mit regelmäßigen Updates und hat eine reaktionsschnelle Community.

5. **Leistung**: CASL hat gute Leistungsmerkmale, insbesondere mit seiner Fähigkeit, Berechtigungsregeln für schnellere Laufzeitprüfungen zu kompilieren.

6. **Multi-Tenancy-Unterstützung**: CASL kann so konfiguriert werden, dass es den Tenant-Kontext in Berechtigungsregeln einbezieht, was für unsere Multi-Tenant-Architektur essentiell ist.

7. **Dokumentationsqualität**: CASL hat eine umfassende Dokumentation mit Beispielen, die unsere Anwendungsfälle abdecken.

8. **Community-Akzeptanz**: CASL wird in vielen Produktionsanwendungen eingesetzt, was seine Zuverlässigkeit und Wirksamkeit belegt.

9. **Integrationsbeispiele**: Es gibt mehrere Beispiele und Artikel, die die CASL-Integration mit NestJS demonstrieren, was unsere Implementierung beschleunigen wird.

Die anderen Optionen wurden aus folgenden Gründen abgelehnt:

* **AccessControl**: Obwohl fähig, hat es weniger Funktionen für attributbasierte Bedingungen und weniger aktive Entwicklung.
* **NestJS RBAC**: Zu eng an NestJS gekoppelt, was im Widerspruch zu unserem hexagonalen Architekturansatz steht.
* **Eigene Implementierung**: Würde erheblichen Entwicklungsaufwand erfordern und unnötiges Risiko einführen, ohne wesentliche Vorteile gegenüber bestehenden Lösungen zu bieten. Es würde auch kontinuierlichen Wartungsaufwand erfordern.

## Konsequenzen

### Positiv

* Schnellere Entwicklung des RBAC-Systems mit einer bewährten Bibliothek
* Bessere Wartbarkeit durch die Verwendung einer gut dokumentierten Standardbibliothek
* Starke Typisierungsunterstützung für Berechtigungsregeln
* Fähigkeit, sowohl RBAC als auch grundlegende ABAC (Eigentum) Anforderungen zu implementieren

### Negativ

* Team muss CASLs spezifische API und Konzepte erlernen
* Potenzieller Leistungs-Overhead im Vergleich zu einer hochoptimierten benutzerdefinierten Lösung
* Abhängigkeit von einer Drittanbieter-Bibliothek für eine kritische Sicherheitskomponente

### Risikominderung

* Bereitstellung von Schulungen und Beispielen für das Team zur CASL-Nutzung
* Erstellung von Abstraktionsschichten, wo nötig, um Kerngeschäftslogik von CASL-spezifischem Code zu isolieren
* Implementierung umfassender Tests für Berechtigungsregeln
* Überwachung der CASL-Leistung in unseren spezifischen Anwendungsfällen und Optimierung nach Bedarf

## Implementierungshinweise

* CASL wird hauptsächlich im `libs/rbac`-Paket integriert
* Wir werden NestJS-Guards erstellen, die CASL für Berechtigungsprüfungen verwenden
* Tenant-Kontext wird in CASLs Fähigkeitskontext eingebunden
* Berechtigungsdefinitionen werden zentralisiert und nach Domänenbereichen organisiert
* Wir werden CASLs Fähigkeits-Caching-Funktionen zur Leistungsoptimierung nutzen

## Betrachtete Alternativen

### AccessControl

AccessControl bietet eine einfachere, stärker festgelegte RBAC-Implementierung. Während es eine gute rollenbasierte Berechtigungskontrolle bietet, ist es weniger flexibel für attributbasierte Bedingungen und hat weniger aktive Mitwirkende.

### NestJS RBAC

NestJS hat seine eigene RBAC-Implementierung, aber sie ist eng an das Framework gekoppelt und fehlt einige der Flexibilität, die wir benötigen, insbesondere für attributbasierte Bedingungen und Tenant-Isolation.

### Eigene Implementierung

Der Aufbau unserer eigenen Lösung würde maximale Flexibilität bieten, würde aber erhebliche Entwicklungszeit erfordern und potenzielle Sicherheitsrisiken einführen. Es würde auch kontinuierlichen Wartungsaufwand erfordern.

## Referenzen

* [CASL-Dokumentation](https://casl.js.org/)
* [CASL GitHub-Repository](https://github.com/stalniy/casl)
* [NestJS mit CASL-Integrationsbeispiel](https://github.com/nestjsx/nest-casl)
* [AccessControl-Dokumentation](https://onury.io/accesscontrol/)
* [NestJS Access Control-Dokumentation](https://docs.nestjs.com/security/authorization)
