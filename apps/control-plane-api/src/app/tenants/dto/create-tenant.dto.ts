import { IsEmail, IsNotEmpty, IsOptional, IsString, Length, ValidateNested } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Type } from 'class-transformer';

/**
 * DTO for creating a new tenant
 */
export class CreateTenantDto {
  @ApiProperty({
    description: 'The name of the tenant',
    example: 'Acme Corporation',
  })
  @IsString()
  @IsNotEmpty()
  @Length(3, 100)
  name!: string;

  @ApiPropertyOptional({
    description: 'The description of the tenant',
    example: 'A global company specializing in technology solutions',
  })
  @IsString()
  @IsOptional()
  @Length(0, 500)
  description?: string;

  @ApiPropertyOptional({
    description: 'Contact email for the tenant',
    example: 'admin@acme.com',
  })
  @IsEmail()
  @IsOptional()
  contactEmail?: string;

  @ApiPropertyOptional({
    description: 'Optional configuration for the tenant',
    type: 'object',
    additionalProperties: true,
    example: { maxUsers: 100, features: { advancedAnalytics: true } },
  })
  @IsOptional()
  @ValidateNested()
  @Type(() => Object)
  configuration?: Record<string, unknown>;
} 