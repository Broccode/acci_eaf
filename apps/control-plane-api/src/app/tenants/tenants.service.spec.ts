import { TestBed } from '@suites/unit';
import { Mocked } from '@suites/doubles.jest';
import { TenantsService } from './tenants.service';
import { TenantRepository } from './entities/tenant.repository';
import { EntityManager } from '@mikro-orm/core';
import { Tenant, TenantStatus } from './entities/tenant.entity';
import { NotFoundException } from '@nestjs/common';

describe('TenantsService (Suites)', () => {
  let underTest: TenantsService;
  let tenantRepository: Mocked<TenantRepository>;
  let entityManager: Mocked<EntityManager>;

  beforeAll(async () => {
    // Solitary test for the service with mocked dependencies
    const { unit, unitRef } = await TestBed.solitary<TenantsService>(TenantsService).compile();
    
    underTest = unit;
    tenantRepository = unitRef.get(TenantRepository);
    entityManager = unitRef.get(EntityManager);
  });

  describe('create', () => {
    it('should call tenantRepository.createTenant with correct params', async () => {
      // Arrange
      const createTenantDto = {
        name: 'New Tenant',
        description: 'New tenant description',
        contactEmail: 'new@example.com',
        configuration: { feature: true },
      };
      
      const mockTenant: Tenant = {
        id: 'new-uuid',
        name: createTenantDto.name,
        description: createTenantDto.description,
        contactEmail: createTenantDto.contactEmail,
        configuration: createTenantDto.configuration,
        status: TenantStatus.ACTIVE,
        createdAt: new Date(),
        updatedAt: new Date(),
      } as Tenant;
      
      tenantRepository.createTenant.mockResolvedValue(mockTenant);
      
      // Act
      const result = await underTest.create(createTenantDto);
      
      // Assert
      expect(tenantRepository.createTenant).toHaveBeenCalledWith(
        createTenantDto.name, 
        createTenantDto.description,
        createTenantDto.contactEmail,
        createTenantDto.configuration
      );
      expect(result).toEqual(mockTenant);
    });
  });

  describe('findAll', () => {
    it('should call tenantRepository.findAllTenants', async () => {
      // Arrange
      const mockTenants: Tenant[] = [
        {
          id: 'tenant-1',
          name: 'Tenant 1',
        } as Tenant,
        {
          id: 'tenant-2',
          name: 'Tenant 2',
        } as Tenant,
      ];
      
      tenantRepository.findAllTenants.mockResolvedValue(mockTenants);
      
      // Act
      const result = await underTest.findAll();
      
      // Assert
      expect(tenantRepository.findAllTenants).toHaveBeenCalled();
      expect(result).toEqual(mockTenants);
    });
  });

  describe('findActive', () => {
    it('should call tenantRepository.findActiveTenants', async () => {
      // Arrange
      const mockTenants: Tenant[] = [
        {
          id: 'active-tenant',
          name: 'Active Tenant',
          status: TenantStatus.ACTIVE,
        } as Tenant,
      ];
      
      tenantRepository.findActiveTenants.mockResolvedValue(mockTenants);
      
      // Act
      const result = await underTest.findActive();
      
      // Assert
      expect(tenantRepository.findActiveTenants).toHaveBeenCalled();
      expect(result).toEqual(mockTenants);
    });
  });

  describe('findOne', () => {
    it('should return tenant when found', async () => {
      // Arrange
      const mockTenant: Tenant = {
        id: 'existing-tenant',
        name: 'Existing Tenant',
      } as Tenant;
      
      tenantRepository.findTenantById.mockResolvedValue(mockTenant);
      
      // Act
      const result = await underTest.findOne('existing-tenant');
      
      // Assert
      expect(tenantRepository.findTenantById).toHaveBeenCalledWith('existing-tenant');
      expect(result).toEqual(mockTenant);
    });

    it('should throw NotFoundException when tenant not found', async () => {
      // Arrange
      tenantRepository.findTenantById.mockResolvedValue(null);
      
      // Act & Assert
      await expect(underTest.findOne('non-existent')).rejects.toThrow(NotFoundException);
      expect(tenantRepository.findTenantById).toHaveBeenCalledWith('non-existent');
    });
  });

  describe('update', () => {
    it('should update tenant properties and flush changes', async () => {
      // Arrange
      const existingTenant: Tenant = {
        id: 'update-tenant',
        name: 'Original Name',
        description: 'Original description',
        contactEmail: 'original@example.com',
        configuration: {},
        status: TenantStatus.ACTIVE,
      } as Tenant;

      const updateDto = {
        name: 'Updated Name',
        description: 'Updated description',
        contactEmail: 'updated@example.com',
        configuration: { updated: true },
      };
      
      // Mock findOne to return our test tenant
      jest.spyOn(underTest, 'findOne').mockResolvedValue(existingTenant);
      
      // Act
      const result = await underTest.update('update-tenant', updateDto);
      
      // Assert
      expect(underTest.findOne).toHaveBeenCalledWith('update-tenant');
      expect(entityManager.flush).toHaveBeenCalled();
      
      // Verify tenant was updated with new values
      expect(result.name).toBe(updateDto.name);
      expect(result.description).toBe(updateDto.description);
      expect(result.contactEmail).toBe(updateDto.contactEmail);
      expect(result.configuration).toBe(updateDto.configuration);
    });
  });

  describe('remove', () => {
    it('should find tenant, remove it and flush changes', async () => {
      // Arrange
      const existingTenant: Tenant = {
        id: 'delete-tenant',
        name: 'Tenant to Delete',
      } as Tenant;
      
      // Mock findOne to return our test tenant
      jest.spyOn(underTest, 'findOne').mockResolvedValue(existingTenant);
      
      // Act
      await underTest.remove('delete-tenant');
      
      // Assert
      expect(underTest.findOne).toHaveBeenCalledWith('delete-tenant');
      expect(entityManager.removeAndFlush).toHaveBeenCalledWith(existingTenant);
    });
  });
}); 