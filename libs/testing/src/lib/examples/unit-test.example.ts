import { Injectable } from '@nestjs/common';
import { NestUnitTestHelper } from '../nest-unit-test.helper';

/**
 * Example dependency service for demonstration
 */
@Injectable()
class ExampleDependency {
  getData(): string {
    return 'real data';
  }
}

/**
 * Example service to test
 */
@Injectable()
class ExampleService {
  constructor(private readonly dependency: ExampleDependency) {}

  processData(): string {
    const data = this.dependency.getData();
    return `Processed: ${data}`;
  }
}

/**
 * Example unit test for the ExampleService
 * This is just for demonstration purposes and won't be executed
 */
describe('ExampleService', () => {
  it('should process data correctly', async () => {
    // Create a mock dependency
    const mockDependency = {
      getData: jest.fn().mockReturnValue('mock data')
    };

    // Set up the testing module with the service and mock dependency
    const { service } = await NestUnitTestHelper.createTestingModule({
      providers: [
        ExampleService,
        {
          provide: ExampleDependency,
          useValue: mockDependency
        }
      ]
    }).compile(ExampleService);

    // Test the service
    expect(service.processData()).toBe('Processed: mock data');
    expect(mockDependency.getData).toHaveBeenCalled();
  });

  it('can also use the createMockService helper', async () => {
    // Create a mock dependency using the helper
    const mockDependency = NestUnitTestHelper.createMockService(ExampleDependency);
    mockDependency['getData'].mockReturnValue('another mock data');

    // Set up the testing module
    const { service } = await NestUnitTestHelper.createTestingModule({
      providers: [
        ExampleService,
        {
          provide: ExampleDependency,
          useValue: mockDependency
        }
      ]
    }).compile(ExampleService);

    // Test the service
    expect(service.processData()).toBe('Processed: another mock data');
  });
}); 