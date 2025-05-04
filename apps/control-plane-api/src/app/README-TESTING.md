# Testing Guide für control-plane-api

## Einführung zu Suites Testing Framework

Dieses Projekt verwendet das [Suites Testing Framework](https://suites.dev) für Unit-Tests. Suites ist ein Meta-Framework, das jest/vitest und andere Test-Tools mit Dependency Injection Frameworks wie NestJS integriert. Es bietet vor allem automatisches Mocking und eine klare Trennung zwischen "solitary" und "sociable" Tests.

## Grundkonzepte

### Solitary Tests

- Testen einer einzelnen Klasse in vollständiger Isolation
- Alle Abhängigkeiten werden automatisch gemockt
- Geeignet für Handler, Controller und einfache Services
- Fokus auf die Logik der getesteten Klasse

```typescript
// Beispiel für einen solitary test
const { unit, unitRef } = await TestBed.solitary<MyService>(MyService).compile();
```

### Sociable Tests

- Testen einer Klasse mit einigen echten und einigen gemockten Abhängigkeiten
- Ermöglicht das Testen von Interaktionen zwischen Komponenten
- Geeignet für komplexere Services oder Domain-Logik

```typescript
// Beispiel für einen sociable test
const { unit, unitRef } = await TestBed
  .sociable<MyService>(MyService)
  .expose(DependencyToUseReal)
  .compile();
```

## Migrations-Checkliste

- [x] Command Handler Tests migriert
  - [x] CreateTenantHandler
  - [x] UpdateTenantHandler
  - [x] DeleteTenantHandler
- [x] Controller Tests migriert
  - [x] AppController
- [x] Service Tests migriert
  - [x] AppService
  - [x] TenantsService
- [x] Query Handler Tests migriert
  - [x] ListTenantsHandler
  - [x] GetTenantByIdHandler
- [ ] Weitere Controller/Services migrieren
- [ ] Integrationstests evaluieren (Entscheidung: Beibehalten der manuellen Setup-Methode vorläufig)

## Best Practices

### 1. Namenskonventionen

- `underTest` für die zu testende Klasse
- `{dependencyName}` für gemockte Abhängigkeiten
- AAA Pattern: Arrange-Act-Assert mit Kommentaren

### 2. Wann solitary vs. sociable Tests verwenden

| Komponente | Empfohlener Ansatz | Begründung |
|------------|---------------------|------------|
| Controller | Solitary | Controller sollten dünn sein und nur Anfragen routing/transformieren |
| Command/Query Handler | Solitary | Handler haben typischerweise eine einzelne Verantwortlichkeit |
| Services mit wenig Logik | Solitary | Wenn die Service-Logik einfach und selbstständig ist |
| Services mit komplexer Logik | Sociable | Wenn die Service-Logik stark mit anderen Komponenten interagiert |
| Repositories | Individuell | Je nach Komplexität, oft mit TestContainers testen |

### 3. Typische Test-Struktur

```typescript
describe('MeineKlasse (Suites)', () => {
  let underTest: MeineKlasse;
  let dependency1: Mocked<Dependency1>;
  let dependency2: Mocked<Dependency2>;

  beforeAll(async () => {
    const { unit, unitRef } = await TestBed.solitary<MeineKlasse>(MeineKlasse).compile();
    
    underTest = unit;
    dependency1 = unitRef.get(Dependency1);
    dependency2 = unitRef.get(Dependency2);
  });

  it('sollte etwas spezifisches tun', () => {
    // Arrange
    dependency1.method.mockReturnValue(123);
    
    // Act
    const result = underTest.methodToTest();
    
    // Assert
    expect(dependency1.method).toHaveBeenCalled();
    expect(result).toBe(123);
  });
});
```

## Häufige Muster

### 1. Mocking von Methoden mit Suites

```typescript
// Rückgabewert definieren
mockService.getData.mockReturnValue({ id: 1 });
mockService.getData.mockResolvedValue({ id: 1 }); // Für Promises

// Implementierung überschreiben
mockService.getData.mockImplementation((id) => {
  if (id === 1) return { found: true };
  return { found: false };
});

// Fehler werfen
mockService.getData.mockRejectedValue(new Error('Something went wrong'));
```

### 2. Präzises Überprüfen von Aufrufen

```typescript
// Überprüfen, ob aufgerufen
expect(mockService.method).toHaveBeenCalled();

// Überprüfen mit bestimmten Parametern
expect(mockService.method).toHaveBeenCalledWith('param1', 'param2');

// Überprüfen der Anzahl von Aufrufen
expect(mockService.method).toHaveBeenCalledTimes(2);
```

## Fehlerbehandlung

Wenn Sie auf Fehler oder unerwartetes Verhalten stoßen, prüfen Sie:

1. Ist die Typdefinition in `suites-typings.d.ts` korrekt?
2. Werden die richtigen Abhängigkeiten injiziert?
3. Ist der richtige Test-Typ (solitary/sociable) für den Anwendungsfall gewählt?

## Ressourcen und Dokumentation

- [Suites Offizielle Dokumentation](https://suites.dev/docs)
- [NestJS Testing Dokumentation](https://docs.nestjs.com/fundamentals/testing)
- [Jest Dokumentation](https://jestjs.io/docs/getting-started)
