import { TestBed } from '@suites/unit';
import { Mocked } from '@suites/doubles.jest';
import { AppController } from './app.controller';
import { AppService } from './app.service';

describe('AppController (Suites)', () => {
  let underTest: AppController;
  let appService: Mocked<AppService>;

  beforeAll(async () => {
    // Solitary test for the controller with a mocked service
    const { unit, unitRef } = await TestBed.solitary<AppController>(AppController).compile();
    
    underTest = unit;
    appService = unitRef.get(AppService);
  });

  describe('getData', () => {
    it('should return data from the AppService', () => {
      // Arrange
      const mockData = { message: 'Hello API' };
      appService.getData.mockReturnValue(mockData);
      
      // Act
      const result = underTest.getData();
      
      // Assert
      expect(appService.getData).toHaveBeenCalled();
      expect(result).toEqual(mockData);
    });
  });
});
