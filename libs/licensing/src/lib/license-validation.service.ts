import { Injectable, HttpException } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { ConfigService } from '@nestjs/config';
import { AxiosResponse } from 'axios';
import { createVerify } from 'crypto';
import { lastValueFrom } from 'rxjs';

@Injectable()
export class LicenseValidationService {
  constructor(
    private readonly httpService: HttpService,
    private readonly configService: ConfigService,
  ) {}

  async validateOnline(licenseKey: string): Promise<boolean> {
    const url = this.configService.get<string>('LICENSE_SERVER_URL');
    if (!url) {
      throw new HttpException('License server URL not configured', 500);
    }
    try {
      const response: AxiosResponse<{ valid: boolean }> = await lastValueFrom(
        this.httpService.post<{ valid: boolean }>(url, { key: licenseKey }),
      );
      return response.data.valid;
    } catch (err) {
      throw new HttpException('Online license validation failed', 502);
    }
  }

  async validateOffline(licenseFile: Buffer | string): Promise<boolean> {
    const publicKey = this.configService.get<string>('LICENSE_PUBLIC_KEY');
    if (!publicKey) {
      throw new HttpException('Public key for offline validation not configured', 500);
    }
    const payload = typeof licenseFile === 'string' ? Buffer.from(licenseFile, 'utf-8') : licenseFile;
    // Assuming licenseFile contains JSON payload
    let data: { key: string; tenantId: string; expirationDate: string; signature: string };
    try {
      data = JSON.parse(payload.toString());
    } catch (err) {
      throw new HttpException('Invalid license file format', 400);
    }

    const { signature, signature: _, ...payloadData } = data;
    const verifier = createVerify('RSA-SHA256');
    verifier.update(JSON.stringify(payloadData));
    const isValid = verifier.verify(publicKey, signature, 'base64');
    if (!isValid) {
      throw new HttpException('Offline license validation failed', 400);
    }

    const now = new Date();
    const expiry = new Date(data.expirationDate);
    if (expiry < now) {
      throw new HttpException('License has expired', 403);
    }

    return true;
  }
} 