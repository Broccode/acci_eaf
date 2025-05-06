import { IsOptional, IsString, ValidateIf, IsNotEmpty } from 'class-validator';

export class ValidateLicenseDto {
  @ValidateIf(o => !o.licenseFile)
  @IsString()
  @IsNotEmpty()
  licenseKey?: string;

  @ValidateIf(o => !o.licenseKey)
  @IsString()
  @IsNotEmpty()
  licenseFile?: string; // base64 encoded or raw string
} 