import { Tenant, TenantStatus } from './tenant.entity';
import { validate as uuidValidate } from 'uuid';

describe('Tenant Entity', () => {
  describe('constructor', () => {
    it('should create a tenant with required properties', () => {
      const tenant = new Tenant('Test Tenant');
      
      expect(tenant.name).toBe('Test Tenant');
      expect(tenant.status).toBe(TenantStatus.ACTIVE);
      expect(uuidValidate(tenant.id)).toBe(true);
      expect(tenant.createdAt).toBeInstanceOf(Date);
      expect(tenant.updatedAt).toBeInstanceOf(Date);
    });

    it('should create a tenant with all properties', () => {
      const config = { key: 'value' };
      const tenant = new Tenant(
        'Full Tenant', 
        'Test Description', 
        'contact@example.com', 
        config
      );
      
      expect(tenant.name).toBe('Full Tenant');
      expect(tenant.description).toBe('Test Description');
      expect(tenant.contactEmail).toBe('contact@example.com');
      expect(tenant.configuration).toEqual(config);
    });
  });

  describe('status management', () => {
    let tenant: Tenant;

    beforeEach(() => {
      tenant = new Tenant('Test Tenant');
    });

    it('should be created with ACTIVE status by default', () => {
      expect(tenant.status).toBe(TenantStatus.ACTIVE);
    });

    it('should activate a tenant', () => {
      tenant.status = TenantStatus.INACTIVE;
      tenant.activate();
      expect(tenant.status).toBe(TenantStatus.ACTIVE);
    });

    it('should deactivate a tenant', () => {
      tenant.deactivate();
      expect(tenant.status).toBe(TenantStatus.INACTIVE);
    });

    it('should suspend a tenant', () => {
      tenant.suspend();
      expect(tenant.status).toBe(TenantStatus.SUSPENDED);
    });
  });

  describe('update method', () => {
    let tenant: Tenant;
    const initialConfig = { setting: 'initial' };

    beforeEach(() => {
      tenant = new Tenant(
        'Initial Name', 
        'Initial Description', 
        'initial@example.com', 
        initialConfig
      );
    });

    it('should update name when provided with truthy value', () => {
      tenant.update('Updated Name');
      expect(tenant.name).toBe('Updated Name');
      expect(tenant.description).toBe('Initial Description');
      expect(tenant.contactEmail).toBe('initial@example.com');
      expect(tenant.configuration).toEqual(initialConfig);
    });

    it('should not update name when provided with empty string', () => {
      tenant.update('');
      expect(tenant.name).toBe('Initial Name');
    });
    
    it('should not update name when provided with undefined', () => {
      tenant.update(undefined);
      expect(tenant.name).toBe('Initial Name');
    });

    it('should update description when provided', () => {
      tenant.update(undefined, 'Updated Description');
      expect(tenant.name).toBe('Initial Name');
      expect(tenant.description).toBe('Updated Description');
    });

    it('should update description to empty string when provided', () => {
      tenant.update(undefined, '');
      expect(tenant.description).toBe('');
    });

    it('should update contactEmail when provided', () => {
      tenant.update(undefined, undefined, 'updated@example.com');
      expect(tenant.contactEmail).toBe('updated@example.com');
    });

    it('should update contactEmail to empty string when provided', () => {
      tenant.update(undefined, undefined, '');
      expect(tenant.contactEmail).toBe('');
    });

    it('should update configuration when provided', () => {
      const newConfig = { setting: 'updated' };
      tenant.update(undefined, undefined, undefined, newConfig);
      expect(tenant.configuration).toEqual(newConfig);
    });

    it('should update configuration to empty object when provided', () => {
      tenant.update(undefined, undefined, undefined, {});
      expect(tenant.configuration).toEqual({});
    });

    it('should update multiple properties at once', () => {
      const newConfig = { setting: 'new' };
      tenant.update('New Name', 'New Description', 'new@example.com', newConfig);
      
      expect(tenant.name).toBe('New Name');
      expect(tenant.description).toBe('New Description');
      expect(tenant.contactEmail).toBe('new@example.com');
      expect(tenant.configuration).toEqual(newConfig);
    });
  });
}); 