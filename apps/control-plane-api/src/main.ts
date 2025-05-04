/**
 * This is not a production server yet!
 * This is only a minimal backend to get started.
 */

import { Logger, ValidationPipe } from '@nestjs/common';
import { NestFactory } from '@nestjs/core';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { AppModule } from './app/app.module';
import { AuthService } from './app/auth/auth.service';
import * as process from 'process';
import { MikroORM } from '@mikro-orm/core';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  
  // Sync schema automatically in test environment
  if (process.env.NODE_ENV === 'test') {
    const orm = app.get(MikroORM);
    // Ensure the test database matches entity definitions
    await orm.getSchemaGenerator().createSchema();
  }
  
  // Set global prefix
  const globalPrefix = 'api';
  app.setGlobalPrefix(globalPrefix);
  
  // Enable validation pipe
  app.useGlobalPipes(
    new ValidationPipe({
      transform: true, // Transform incoming data to DTO instances
      whitelist: true, // Strip properties not in DTO
      forbidNonWhitelisted: true, // Throw an error if non-whitelisted properties are present
    }),
  );

  // Set up Swagger documentation
  const config = new DocumentBuilder()
    .setTitle('Control Plane API')
    .setDescription('API for managing tenants and system administration')
    .setVersion('1.0')
    .addTag('tenants', 'Tenant management endpoints')
    .addBearerAuth()
    .build();
  
  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup(`${globalPrefix}/docs`, app, document);

  // Debug environment variables
  console.log('Environment variables:');
  console.log('- BOOTSTRAP_ADMIN_TOKEN:', process.env.BOOTSTRAP_ADMIN_TOKEN ? '[SET]' : '[NOT SET]');
  console.log('- NODE_ENV:', process.env.NODE_ENV);
  console.log('- DB_TYPE:', process.env.DB_TYPE);
  console.log('- DB_NAME:', process.env.DB_NAME);
  
  // Create admin user directly for testing
  try {
    const authService = app.get(AuthService);
    console.log('Creating admin user directly via AuthService...');
    const admin = await authService.createAdmin('admin', 'whiskey');
    console.log('Admin user created successfully:', admin.username);
  } catch (error) {
    console.error('Failed to create admin user:', error.message);
    // Continue even if it fails - might already exist
  }

  // Start the application
  const port = process.env.PORT || 3000;
  await app.listen(port);
  
  Logger.log(
    `🚀 Application is running on: http://localhost:${port}/${globalPrefix}`
  );
  Logger.log(
    `📚 API documentation available at: http://localhost:${port}/${globalPrefix}/docs`
  );
}

bootstrap();
