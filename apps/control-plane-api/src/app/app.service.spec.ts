import { TestBed } from '@suites/unit';
import { AppService } from './app.service';

describe('AppService (Suites)', () => {
  let underTest: AppService;

  beforeAll(async () => {
    // Da der AppService keine Abhängigkeiten hat, ist solitary gleichwertig zu sociable
    const { unit } = await TestBed.solitary<AppService>(AppService).compile();
    
    underTest = unit;
  });

  describe('getData', () => {
    it('should return "Hello API"', () => {
      // Arrange & Act
      const result = underTest.getData();
      
      // Assert
      expect(result).toEqual({ message: 'Hello API' });
    });
  });
});
