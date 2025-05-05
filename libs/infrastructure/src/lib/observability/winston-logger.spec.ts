import { TestBed } from '@suites/unit';
import { WinstonLoggerService, WinstonLoggerFactory } from './winston-logger';
import * as winston from 'winston';

// Mock winston
jest.mock('winston', () => {
  const mockFormat = {
    combine: jest.fn().mockReturnThis(),
    timestamp: jest.fn().mockReturnThis(),
    json: jest.fn().mockReturnThis(),
    colorize: jest.fn().mockReturnThis(),
    printf: jest.fn().mockImplementation((fn) => fn),
    ms: jest.fn().mockReturnThis(),
  };

  const mockLogger = {
    info: jest.fn(),
    error: jest.fn(),
    warn: jest.fn(),
    debug: jest.fn(),
    verbose: jest.fn(),
  };

  return {
    format: mockFormat,
    createLogger: jest.fn().mockReturnValue(mockLogger),
    transports: {
      Console: jest.fn(),
    },
  };
});

describe('WinstonLoggerService', () => {
  let loggerService: WinstonLoggerService;
  let mockWinstonLogger: any;

  beforeEach(() => {
    jest.clearAllMocks();
    mockWinstonLogger = (winston as any).createLogger();
    loggerService = new WinstonLoggerService('TestContext');
  });

  it('should be defined', () => {
    expect(loggerService).toBeDefined();
  });

  it('should create logger with default options', () => {
    expect(winston.createLogger).toHaveBeenCalled();
    expect(winston.format.combine).toHaveBeenCalled();
    expect(winston.format.timestamp).toHaveBeenCalled();
    expect(winston.transports.Console).toHaveBeenCalled();
  });

  it('should call info method with context when log is called', () => {
    const message = 'Test message';
    const meta = { test: 'data' };
    
    loggerService.log(message, undefined, meta);
    
    expect(mockWinstonLogger.info).toHaveBeenCalledWith(message, {
      context: 'TestContext',
      test: 'data',
    });
  });

  it('should call error method with trace and context when error is called', () => {
    const message = 'Error message';
    const trace = 'Error trace';
    const meta = { test: 'data' };
    
    loggerService.error(message, trace, undefined, meta);
    
    expect(mockWinstonLogger.error).toHaveBeenCalledWith(message, {
      context: 'TestContext',
      trace,
      test: 'data',
    });
  });

  it('should call warn method with context when warn is called', () => {
    const message = 'Warning message';
    const meta = { test: 'data' };
    
    loggerService.warn(message, undefined, meta);
    
    expect(mockWinstonLogger.warn).toHaveBeenCalledWith(message, {
      context: 'TestContext',
      test: 'data',
    });
  });

  it('should call debug method with context when debug is called', () => {
    const message = 'Debug message';
    const meta = { test: 'data' };
    
    loggerService.debug(message, undefined, meta);
    
    expect(mockWinstonLogger.debug).toHaveBeenCalledWith(message, {
      context: 'TestContext',
      test: 'data',
    });
  });

  it('should call verbose method with context when verbose is called', () => {
    const message = 'Verbose message';
    const meta = { test: 'data' };
    
    loggerService.verbose(message, undefined, meta);
    
    expect(mockWinstonLogger.verbose).toHaveBeenCalledWith(message, {
      context: 'TestContext',
      test: 'data',
    });
  });

  it('should use provided context instead of constructor context', () => {
    const message = 'Test message';
    const context = 'OverrideContext';
    
    loggerService.log(message, context);
    
    expect(mockWinstonLogger.info).toHaveBeenCalledWith(message, {
      context: 'OverrideContext',
    });
  });
});

describe('WinstonLoggerFactory using Suites', () => {
  let underTest: WinstonLoggerFactory;

  beforeEach(async () => {
    // Since WinstonLoggerFactory doesn't have dependencies,
    // we don't need TestBed.solitary here, but we include it for demonstration
    const { unit } = await TestBed.solitary(WinstonLoggerFactory).compile();
    underTest = unit;
  });

  it('should be defined', () => {
    expect(underTest).toBeDefined();
  });

  it('should create logger with provided context', () => {
    const logger = underTest.createLogger('TestContext');
    
    expect(logger).toBeInstanceOf(WinstonLoggerService);
    expect((logger as any).context).toBe('TestContext');
  });

  it('should pass options to created logger', () => {
    const options = { level: 'debug' };
    const factory = new WinstonLoggerFactory(options);
    const logger = factory.createLogger('TestContext');
    
    expect(logger).toBeInstanceOf(WinstonLoggerService);
    expect((logger as any).context).toBe('TestContext');
    expect((factory as any).options).toBe(options);
  });
}); 