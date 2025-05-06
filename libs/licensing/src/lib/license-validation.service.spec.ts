import { LicenseValidationService } from './license-validation.service';
import { HttpService } from '@nestjs/axios';
import { ConfigService } from '@nestjs/config';
import { HttpException } from '@nestjs/common';
import { of } from 'rxjs';
import * as crypto from 'crypto';

describe('LicenseValidationService', () => {
  let service: LicenseValidationService;
  let httpService: Partial<HttpService>;
  let configService: Partial<ConfigService>;

  beforeEach(() => {
    httpService = {
      post: jest.fn(),
    };
    configService = {
      get: jest.fn(),
    };
    service = new LicenseValidationService(
      httpService as HttpService,
      configService as ConfigService,
    );
  });

  describe('validateOffline', () => {
    it('should throw if public key is not configured', async () => {
      (configService.get as jest.Mock).mockReturnValue(undefined);
      await expect(service.validateOffline('{}')).rejects.toThrow('Public key for offline validation not configured');
    });

    it('should throw if license file is invalid JSON', async () => {
      (configService.get as jest.Mock).mockReturnValue('dummy-key');
      await expect(service.validateOffline('not-json')).rejects.toThrow('Invalid license file format');
    });

    it('should throw if signature is invalid', async () => {
      (configService.get as jest.Mock).mockReturnValue('dummy-key');
      jest.spyOn(crypto, 'createVerify').mockReturnValue({
        update: jest.fn(),
        verify: jest.fn().mockReturnValue(false),
      } as any);
      const payload = JSON.stringify({ key: 'k', tenantId: 't', expirationDate: new Date(Date.now() + 10000).toISOString(), signature: 'sig' });
      await expect(service.validateOffline(payload)).rejects.toThrow('Offline license validation failed');
    });

    it('should throw if license is expired', async () => {
      (configService.get as jest.Mock).mockReturnValue('dummy-key');
      jest.spyOn(crypto, 'createVerify').mockReturnValue({
        update: jest.fn(),
        verify: jest.fn().mockReturnValue(true),
      } as any);
      const payload = JSON.stringify({ key: 'k', tenantId: 't', expirationDate: new Date(Date.now() - 10000).toISOString(), signature: 'sig' });
      await expect(service.validateOffline(payload)).rejects.toThrow('License has expired');
    });

    it('should return true for valid license', async () => {
      (configService.get as jest.Mock).mockReturnValue('dummy-key');
      jest.spyOn(crypto, 'createVerify').mockReturnValue({
        update: jest.fn(),
        verify: jest.fn().mockReturnValue(true),
      } as any);
      const payload = JSON.stringify({ key: 'k', tenantId: 't', expirationDate: new Date(Date.now() + 10000).toISOString(), signature: 'sig' });
      await expect(service.validateOffline(payload)).resolves.toBe(true);
    });
  });

  describe('validateOnline', () => {
    it('should throw if license server URL is not configured', async () => {
      (configService.get as jest.Mock).mockReturnValueOnce(undefined);
      await expect(service.validateOnline('key')).rejects.toThrow('License server URL not configured');
    });

    it('should return true if server returns valid: true', async () => {
      (configService.get as jest.Mock).mockReturnValueOnce('http://server');
      (httpService.post as jest.Mock).mockReturnValueOnce(of({ data: { valid: true } }));
      await expect(service.validateOnline('key')).resolves.toBe(true);
    });

    it('should return false if server returns valid: false', async () => {
      (configService.get as jest.Mock).mockReturnValueOnce('http://server');
      (httpService.post as jest.Mock).mockReturnValueOnce(of({ data: { valid: false } }));
      await expect(service.validateOnline('key')).resolves.toBe(false);
    });

    it('should throw if httpService throws', async () => {
      (configService.get as jest.Mock).mockReturnValueOnce('http://server');
      (httpService.post as jest.Mock).mockImplementationOnce(() => { throw new Error('fail'); });
      await expect(service.validateOnline('key')).rejects.toThrow('Online license validation failed');
    });
  });
}); 