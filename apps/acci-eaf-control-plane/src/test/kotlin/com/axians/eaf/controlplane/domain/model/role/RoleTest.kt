package com.axians.eaf.controlplane.domain.model.role

import com.axians.eaf.controlplane.domain.model.tenant.TenantId
import com.axians.eaf.controlplane.domain.model.user.Permission
import com.axians.eaf.controlplane.domain.model.user.PermissionId
import com.axians.eaf.controlplane.domain.model.user.Role
import com.axians.eaf.controlplane.domain.model.user.RoleId
import com.axians.eaf.controlplane.domain.model.user.RoleScope
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for Role domain model.
 *
 * Tests cover business logic, validation rules, permission management, and edge cases for role
 * operations.
 */
class RoleTest {
    @Test
    fun `should create platform role successfully`() {
        // Given
        val roleId = RoleId.generate()
        val name = "Platform Administrator"
        val description = "Platform-wide administrative role"

        // When
        val role =
            Role.createPlatformRole(id = roleId, name = name, description = description)

        // Then
        assertEquals(roleId, role.id)
        assertEquals(name, role.name)
        assertEquals(description, role.description)
        assertEquals(RoleScope.PLATFORM, role.scope)
        assertEquals(null, role.tenantId)
        assertTrue(role.permissions.isEmpty())
        assertNotNull(role.createdAt)
        assertEquals(role.createdAt, role.updatedAt)
    }

    @Test
    fun `should create tenant role successfully`() {
        // Given
        val roleId = RoleId.generate()
        val tenantId = TenantId.generate()
        val name = "Tenant Admin"
        val description = "Tenant-specific administrative role"

        // When
        val role =
            Role.createTenantRole(
                id = roleId,
                name = name,
                description = description,
                tenantId = tenantId,
            )

        // Then
        assertEquals(roleId, role.id)
        assertEquals(name, role.name)
        assertEquals(description, role.description)
        assertEquals(RoleScope.TENANT, role.scope)
        assertEquals(tenantId, role.tenantId)
        assertTrue(role.permissions.isEmpty())
        assertNotNull(role.createdAt)
    }

    @Test
    fun `should reject role creation with blank name`() {
        // Given & When & Then
        assertThrows<IllegalArgumentException> {
            Role.createPlatformRole(
                id = RoleId.generate(),
                name = "",
                description = "Valid description",
            )
        }
    }

    @Test
    fun `should reject role creation with blank description`() {
        // Given & When & Then
        assertThrows<IllegalArgumentException> {
            Role.createPlatformRole(
                id = RoleId.generate(),
                name = "Valid Name",
                description = "",
            )
        }
    }

    @Test
    fun `should add permission to role successfully`() {
        // Given
        val role =
            Role.createPlatformRole(
                id = RoleId.generate(),
                name = "Test Role",
                description = "Test role for permission management",
            )
        val permission =
            Permission.create(
                id = PermissionId.generate(),
                name = "READ_USERS",
                description = "Permission to read user data",
                resource = "users",
                action = "read",
            )

        // When
        val updatedRole = role.addPermission(permission)

        // Then
        assertTrue(updatedRole.permissions.contains(permission))
        assertEquals(1, updatedRole.permissions.size)
        assertTrue(updatedRole.updatedAt.isAfter(role.updatedAt))
    }

    @Test
    fun `should not add duplicate permission to role`() {
        // Given
        val role =
            Role.createPlatformRole(
                id = RoleId.generate(),
                name = "Test Role",
                description = "Test role",
            )
        val permission =
            Permission.create(
                id = PermissionId.generate(),
                name = "READ_USERS",
                description = "Permission to read user data",
                resource = "users",
                action = "read",
            )
        val roleWithPermission = role.addPermission(permission)

        // When
        val roleWithDuplicateAttempt = roleWithPermission.addPermission(permission)

        // Then
        assertEquals(1, roleWithDuplicateAttempt.permissions.size)
        assertEquals(roleWithPermission.permissions, roleWithDuplicateAttempt.permissions)
    }

    @Test
    fun `should remove permission from role successfully`() {
        // Given
        val role =
            Role.createPlatformRole(
                id = RoleId.generate(),
                name = "Test Role",
                description = "Test role",
            )
        val permission1 =
            Permission.create(
                id = PermissionId.generate(),
                name = "READ_USERS",
                description = "Read users",
                resource = "users",
                action = "read",
            )
        val permission2 =
            Permission.create(
                id = PermissionId.generate(),
                name = "WRITE_USERS",
                description = "Write users",
                resource = "users",
                action = "write",
            )
        val roleWithPermissions = role.addPermission(permission1).addPermission(permission2)

        // When
        val updatedRole = roleWithPermissions.removePermission(permission1)

        // Then
        assertFalse(updatedRole.permissions.contains(permission1))
        assertTrue(updatedRole.permissions.contains(permission2))
        assertEquals(1, updatedRole.permissions.size)
    }

    @Test
    fun `should not fail when removing non-existent permission`() {
        // Given
        val role =
            Role.createPlatformRole(
                id = RoleId.generate(),
                name = "Test Role",
                description = "Test role",
            )
        val permission =
            Permission.create(
                id = PermissionId.generate(),
                name = "READ_USERS",
                description = "Read users",
                resource = "users",
                action = "read",
            )

        // When
        val updatedRole = role.removePermission(permission)

        // Then
        assertEquals(role.permissions, updatedRole.permissions)
        assertTrue(updatedRole.permissions.isEmpty())
    }

    @Test
    fun `should check if role has specific permission`() {
        // Given
        val role =
            Role.createPlatformRole(
                id = RoleId.generate(),
                name = "Test Role",
                description = "Test role",
            )
        val permission =
            Permission.create(
                id = PermissionId.generate(),
                name = "READ_USERS",
                description = "Read users",
                resource = "users",
                action = "read",
            )
        val roleWithPermission = role.addPermission(permission)

        // When & Then
        assertTrue(roleWithPermission.hasPermission(permission))
        assertFalse(role.hasPermission(permission))
    }

    @Test
    fun `should update role details successfully`() {
        // Given
        val role =
            Role.createPlatformRole(
                id = RoleId.generate(),
                name = "Original Name",
                description = "Original description",
            )
        val newName = "Updated Name"
        val newDescription = "Updated description"

        // When
        val updatedRole = role.updateDetails(name = newName, description = newDescription)

        // Then
        assertEquals(newName, updatedRole.name)
        assertEquals(newDescription, updatedRole.description)
        assertEquals(role.id, updatedRole.id)
        assertEquals(role.scope, updatedRole.scope)
        assertEquals(role.tenantId, updatedRole.tenantId)
        assertTrue(updatedRole.updatedAt.isAfter(role.updatedAt))
    }

    @Test
    fun `should reject updating role with blank name`() {
        // Given
        val role =
            Role.createPlatformRole(
                id = RoleId.generate(),
                name = "Original Name",
                description = "Original description",
            )

        // When & Then
        assertThrows<IllegalArgumentException> {
            role.updateDetails(name = "", description = "Valid description")
        }
    }

    @Test
    fun `should check if role is platform-scoped`() {
        // Given
        val platformRole =
            Role.createPlatformRole(
                id = RoleId.generate(),
                name = "Platform Role",
                description = "Platform role",
            )
        val tenantRole =
            Role.createTenantRole(
                id = RoleId.generate(),
                name = "Tenant Role",
                description = "Tenant role",
                tenantId = TenantId.generate(),
            )

        // When & Then
        assertTrue(platformRole.isPlatformScoped())
        assertFalse(tenantRole.isPlatformScoped())
    }

    @Test
    fun `should check if role is tenant-scoped`() {
        // Given
        val platformRole =
            Role.createPlatformRole(
                id = RoleId.generate(),
                name = "Platform Role",
                description = "Platform role",
            )
        val tenantRole =
            Role.createTenantRole(
                id = RoleId.generate(),
                name = "Tenant Role",
                description = "Tenant role",
                tenantId = TenantId.generate(),
            )

        // When & Then
        assertFalse(platformRole.isTenantScoped())
        assertTrue(tenantRole.isTenantScoped())
    }

    @Test
    fun `should validate role belongs to tenant`() {
        // Given
        val tenantId = TenantId.generate()
        val otherTenantId = TenantId.generate()
        val tenantRole =
            Role.createTenantRole(
                id = RoleId.generate(),
                name = "Tenant Role",
                description = "Tenant role",
                tenantId = tenantId,
            )
        val platformRole =
            Role.createPlatformRole(
                id = RoleId.generate(),
                name = "Platform Role",
                description = "Platform role",
            )

        // When & Then
        assertTrue(tenantRole.belongsToTenant(tenantId))
        assertFalse(tenantRole.belongsToTenant(otherTenantId))
        assertFalse(platformRole.belongsToTenant(tenantId))
    }

    @Test
    fun `should handle multiple permission operations correctly`() {
        // Given
        val role =
            Role.createPlatformRole(
                id = RoleId.generate(),
                name = "Multi-Permission Role",
                description = "Role for testing multiple permissions",
            )
        val permissions =
            (1..5).map { i ->
                Permission.create(
                    id = PermissionId.generate(),
                    name = "PERMISSION_$i",
                    description = "Permission $i",
                    resource = "resource$i",
                    action = "action$i",
                )
            }

        // When
        val roleWithAllPermissions =
            permissions.fold(role) { acc, permission -> acc.addPermission(permission) }

        // Then
        assertEquals(5, roleWithAllPermissions.permissions.size)
        permissions.forEach { permission ->
            assertTrue(roleWithAllPermissions.hasPermission(permission))
        }

        // When removing some permissions
        val roleWithFewerPermissions =
            roleWithAllPermissions
                .removePermission(permissions[0])
                .removePermission(permissions[2])

        // Then
        assertEquals(3, roleWithFewerPermissions.permissions.size)
        assertFalse(roleWithFewerPermissions.hasPermission(permissions[0]))
        assertTrue(roleWithFewerPermissions.hasPermission(permissions[1]))
        assertFalse(roleWithFewerPermissions.hasPermission(permissions[2]))
        assertTrue(roleWithFewerPermissions.hasPermission(permissions[3]))
        assertTrue(roleWithFewerPermissions.hasPermission(permissions[4]))
    }

    @Test
    fun `should maintain immutability of role objects`() {
        // Given
        val originalRole =
            Role.createPlatformRole(
                id = RoleId.generate(),
                name = "Original Role",
                description = "Original description",
            )
        val permission =
            Permission.create(
                id = PermissionId.generate(),
                name = "TEST_PERMISSION",
                description = "Test permission",
                resource = "test",
                action = "test",
            )

        // When
        val modifiedRole =
            originalRole
                .addPermission(permission)
                .updateDetails("New Name", "New Description")

        // Then
        // Original role should remain unchanged
        assertEquals("Original Role", originalRole.name)
        assertEquals("Original description", originalRole.description)
        assertTrue(originalRole.permissions.isEmpty())

        // Modified role should have changes
        assertEquals("New Name", modifiedRole.name)
        assertEquals("New Description", modifiedRole.description)
        assertEquals(1, modifiedRole.permissions.size)
        assertTrue(modifiedRole.hasPermission(permission))
    }
}
