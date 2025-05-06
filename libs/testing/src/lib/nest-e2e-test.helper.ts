import { INestApplication, Type, ModuleMetadata } from '@nestjs/common';
import { Test, TestingModule } from '@nestjs/testing';
import request from 'supertest';

/**
 * Helper for NestJS E2E testing.
 * Simplifies setting up a test NestJS application with supertest.
 */
export class NestE2ETestHelper {
  app!: INestApplication;
  testingModule!: TestingModule;

  /**
   * Creates a testing application from the provided module metadata.
   * 
   * @param metadata The module metadata for creating the test module
   * @returns An instance of NestE2ETestHelper
   * 
   * @example
   * ```typescript
   * const helper = await NestE2ETestHelper.createTestingApp({
   *   imports: [AppModule]
   * });
   * 
   * const response = await helper.get('/health').expect(200);
   * ```
   */
  static async createTestingApp(metadata: ModuleMetadata): Promise<NestE2ETestHelper> {
    const helper = new NestE2ETestHelper();
    await helper.init(metadata);
    return helper;
  }

  /**
   * Initializes the testing application.
   * 
   * @param metadata The module metadata for creating the test module
   */
  async init(metadata: ModuleMetadata): Promise<void> {
    this.testingModule = await Test.createTestingModule(metadata).compile();
    this.app = this.testingModule.createNestApplication();
    
    // Apply global pipes, filters, etc. here if needed
    
    await this.app.init();
  }

  /**
   * Closes the testing application.
   */
  async close(): Promise<void> {
    if (this.app) {
      await this.app.close();
    }
  }

  /**
   * Gets an instance of a service from the testing module.
   * 
   * @param service The service type to get
   * @returns An instance of the service
   */
  get<T>(service: Type<T>): T {
    return this.testingModule.get<T>(service);
  }

  /**
   * Makes a GET request to the testing application.
   * 
   * @param url The URL to request
   * @param authToken Optional authentication token to include in the Authorization header
   * @returns A supertest request
   */
  getRequest(url: string, authToken?: string): request.Test {
    const req = request(this.app.getHttpServer()).get(url);
    return this.addAuthHeader(req, authToken);
  }

  /**
   * Makes a POST request to the testing application.
   * 
   * @param url The URL to request
   * @param data The data to send in the request body
   * @param authToken Optional authentication token to include in the Authorization header
   * @returns A supertest request
   */
  postRequest(url: string, data?: any, authToken?: string): request.Test {
    const req = request(this.app.getHttpServer()).post(url);
    if (data) {
      req.send(data);
    }
    return this.addAuthHeader(req, authToken);
  }

  /**
   * Makes a PUT request to the testing application.
   * 
   * @param url The URL to request
   * @param data The data to send in the request body
   * @param authToken Optional authentication token to include in the Authorization header
   * @returns A supertest request
   */
  putRequest(url: string, data?: any, authToken?: string): request.Test {
    const req = request(this.app.getHttpServer()).put(url);
    if (data) {
      req.send(data);
    }
    return this.addAuthHeader(req, authToken);
  }

  /**
   * Makes a DELETE request to the testing application.
   * 
   * @param url The URL to request
   * @param authToken Optional authentication token to include in the Authorization header
   * @returns A supertest request
   */
  deleteRequest(url: string, authToken?: string): request.Test {
    const req = request(this.app.getHttpServer()).delete(url);
    return this.addAuthHeader(req, authToken);
  }

  /**
   * Adds an Authorization header to a request if an auth token is provided.
   * 
   * @param req The supertest request
   * @param authToken The authentication token to add
   * @returns The request with the Authorization header if an auth token was provided
   */
  private addAuthHeader(req: request.Test, authToken?: string): request.Test {
    if (authToken) {
      return req.set('Authorization', `Bearer ${authToken}`);
    }
    return req;
  }
} 