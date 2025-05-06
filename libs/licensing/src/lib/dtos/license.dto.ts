import { IsString, IsNotEmpty, IsISO8601 } from 'class-validator';

export class LicenseDto {
  @IsString()
  @IsNotEmpty()
  key!: string;

  @IsString()
  @IsNotEmpty()
  tenantId!: string;

  @IsISO8601()
  expirationDate!: string;

  @IsString()
  @IsNotEmpty()
  signature!: string;
} 