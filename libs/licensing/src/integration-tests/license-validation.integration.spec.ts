import { Test } from '@nestjs/testing';
import { HttpModule, HttpService } from '@nestjs/axios';
import { ConfigService } from '@nestjs/config';
import { LicenseValidationService } from '../lib/license-validation.service';
import { AddressInfo } from 'net';
import * as http from 'http';
import { createVerify, createSign, generateKeyPairSync } from 'crypto';
import { lastValueFrom } from 'rxjs';

// Helper to send test request outside of Nest if needed
const requestJson = async (url: string, body: any): Promise<any> => {
  const axios = (await import('axios')).default;
  const response = await axios.post(url, body);
  return response.data;
};

describe('LicenseValidationService Integration', () => {
  let service: LicenseValidationService;
  let configService: ConfigService;
  let httpService: HttpService;

  // Mock License Server
  let server: http.Server;
  let serverUrl: string;

  // RSA Key Pair for offline validation
  const { publicKey, privateKey } = generateKeyPairSync('rsa', {
    modulusLength: 2048,
    publicKeyEncoding: { type: 'pkcs1', format: 'pem' },
    privateKeyEncoding: { type: 'pkcs1', format: 'pem' },
  });

  beforeAll((done) => {
    server = http.createServer((req, res) => {
      if (req.method === 'POST') {
        let body = '';
        req.on('data', (chunk) => (body += chunk));
        req.on('end', () => {
          try {
            const parsed = JSON.parse(body);
            // Simple validation logic: key === 'VALID'
            const valid = parsed.key === 'VALID';
            res.setHeader('Content-Type', 'application/json');
            res.end(JSON.stringify({ valid }));
          } catch (e) {
            res.statusCode = 400;
            res.end();
          }
        });
      } else {
        res.statusCode = 404;
        res.end();
      }
    });
    server.listen(0, () => {
      const { port } = server.address() as AddressInfo;
      serverUrl = `http://127.0.0.1:${port}`;
      done();
    });
  });

  afterAll((done) => {
    server.close(done);
  });

  beforeEach(async () => {
    const moduleRef = await Test.createTestingModule({
      imports: [HttpModule],
      providers: [
        LicenseValidationService,
        {
          provide: ConfigService,
          useValue: {
            get: (key: string) => {
              if (key === 'LICENSE_SERVER_URL') return serverUrl;
              if (key === 'LICENSE_PUBLIC_KEY') return publicKey;
              return undefined;
            },
          },
        },
      ],
    }).compile();

    service = moduleRef.get(LicenseValidationService);
    configService = moduleRef.get(ConfigService);
    httpService = moduleRef.get(HttpService);
  });

  describe('validateOnline', () => {
    it('should return true for VALID key', async () => {
      await expect(service.validateOnline('VALID')).resolves.toBe(true);
    });

    it('should return false for INVALID key', async () => {
      await expect(service.validateOnline('INVALID')).resolves.toBe(false);
    });
  });

  describe('validateOffline', () => {
    const createLicenseFile = (expOffsetMs: number) => {
      const payload = {
        key: 'OFFLINE',
        tenantId: 'tenant-1',
        expirationDate: new Date(Date.now() + expOffsetMs).toISOString(),
      } as const;
      const sign = createSign('RSA-SHA256');
      sign.update(JSON.stringify(payload));
      const signature = sign.sign(privateKey, 'base64');
      return JSON.stringify({ ...payload, signature });
    };

    it('should return true for valid signature and not expired', async () => {
      const licenseFile = createLicenseFile(24 * 60 * 60 * 1000); // +1 day
      await expect(service.validateOffline(licenseFile)).resolves.toBe(true);
    });

    it('should throw for expired license', async () => {
      const licenseFile = createLicenseFile(-1); // expired
      await expect(service.validateOffline(licenseFile)).rejects.toThrow('License has expired');
    });

    it('should throw for tampered payload', async () => {
      const licenseFile = createLicenseFile(24 * 60 * 60 * 1000);
      // Tamper with payload
      const parsed = JSON.parse(licenseFile);
      parsed.key = 'TAMPERED';
      await expect(service.validateOffline(JSON.stringify(parsed))).rejects.toThrow('Offline license validation failed');
    });
  });
}); 