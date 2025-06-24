package com.axians.eaf.controlplane.domain.model.permission

import com.axians.eaf.controlplane.domain.model.user.Permission
import com.axians.eaf.controlplane.domain.model.user.PermissionId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for Permission domain model.
 *
 * Tests cover business logic, validation rules, and edge cases for permission operations.
 */
class PermissionTest {
    @Test
    fun `should create permission successfully`() {
        // Given
        val permissionId = PermissionId.generate()
        val name = "READ_USERS"
        val description = "Permission to read user data"
        val resource = "users"
        val action = "read"

        // When
        val permission =
            Permission.create(
                id = permissionId,
                name = name,
                description = description,
                resource = resource,
                action = action,
            )

        // Then
        assertEquals(permissionId, permission.id)
        assertEquals(name, permission.name)
        assertEquals(description, permission.description)
        assertEquals(resource, permission.resource)
        assertEquals(action, permission.action)
        assertNotNull(permission.createdAt)
        assertEquals(permission.createdAt, permission.updatedAt)
    }

    @Test
    fun `should reject permission creation with blank name`() {
        // Given & When & Then
        assertThrows<IllegalArgumentException> {
            Permission.create(
                id = PermissionId.generate(),
                name = "",
                description = "Valid description",
                resource = "users",
                action = "read",
            )
        }
    }

    @Test
    fun `should reject permission creation with blank description`() {
        // Given & When & Then
        assertThrows<IllegalArgumentException> {
            Permission.create(
                id = PermissionId.generate(),
                name = "READ_USERS",
                description = "",
                resource = "users",
                action = "read",
            )
        }
    }

    @Test
    fun `should reject permission creation with blank resource`() {
        // Given & When & Then
        assertThrows<IllegalArgumentException> {
            Permission.create(
                id = PermissionId.generate(),
                name = "READ_USERS",
                description = "Valid description",
                resource = "",
                action = "read",
            )
        }
    }

    @Test
    fun `should reject permission creation with blank action`() {
        // Given & When & Then
        assertThrows<IllegalArgumentException> {
            Permission.create(
                id = PermissionId.generate(),
                name = "READ_USERS",
                description = "Valid description",
                resource = "users",
                action = "",
            )
        }
    }

    @Test
    fun `should update permission details successfully`() {
        // Given
        val permission =
            Permission.create(
                id = PermissionId.generate(),
                name = "READ_USERS",
                description = "Original description",
                resource = "users",
                action = "read",
            )
        val newDescription = "Updated description for reading user data"

        // When
        val updatedPermission = permission.updateDescription(newDescription)

        // Then
        assertEquals(permission.id, updatedPermission.id)
        assertEquals(permission.name, updatedPermission.name)
        assertEquals(newDescription, updatedPermission.description)
        assertEquals(permission.resource, updatedPermission.resource)
        assertEquals(permission.action, updatedPermission.action)
        assertTrue(updatedPermission.updatedAt.isAfter(permission.updatedAt))
    }

    @Test
    fun `should reject updating permission with blank description`() {
        // Given
        val permission =
            Permission.create(
                id = PermissionId.generate(),
                name = "READ_USERS",
                description = "Original description",
                resource = "users",
                action = "read",
            )

        // When & Then
        assertThrows<IllegalArgumentException> { permission.updateDescription("") }
    }

    @Test
    fun `should maintain permission identity`() {
        // Given
        val permissionId = PermissionId.generate()
        val permission1 =
            Permission.create(
                id = permissionId,
                name = "READ_USERS",
                description = "Description 1",
                resource = "users",
                action = "read",
            )
        val permission2 =
            Permission.create(
                id = permissionId,
                name = "READ_USERS",
                description = "Description 2",
                resource = "users",
                action = "read",
            )

        // When & Then
        assertEquals(permission1.id, permission2.id)
        assertEquals(permission1, permission2) // Should be equal based on ID
    }

    @Test
    fun `should create permissions with different resource-action combinations`() {
        // Given
        val resources = listOf("users", "tenants", "roles", "permissions")
        val actions = listOf("create", "read", "update", "delete")

        // When
        val permissions = mutableListOf<Permission>()
        resources.forEach { resource ->
            actions.forEach { action ->
                val permission =
                    Permission.create(
                        id = PermissionId.generate(),
                        name = "${action.uppercase()}_${resource.uppercase()}",
                        description = "Permission to $action $resource",
                        resource = resource,
                        action = action,
                    )
                permissions.add(permission)
            }
        }

        // Then
        assertEquals(16, permissions.size) // 4 resources × 4 actions

        // Verify each permission has correct properties
        permissions.forEach { permission ->
            assertTrue(permission.name.isNotBlank())
            assertTrue(permission.description.isNotBlank())
            assertTrue(permission.resource.isNotBlank())
            assertTrue(permission.action.isNotBlank())
            assertTrue(resources.contains(permission.resource))
            assertTrue(actions.contains(permission.action))
        }
    }

    @Test
    fun `should handle special characters in permission name correctly`() {
        // Given
        val name = "SPECIAL_PERMISSION_WITH-HYPHENS_AND.DOTS"
        val description = "Permission with special characters in name"

        // When
        val permission =
            Permission.create(
                id = PermissionId.generate(),
                name = name,
                description = description,
                resource = "special",
                action = "test",
            )

        // Then
        assertEquals(name, permission.name)
        assertEquals(description, permission.description)
    }

    @Test
    fun `should handle unicode characters in description`() {
        // Given
        val description = "Berechtigung zum Lesen von Benutzerdaten (German text with ü, ö, ä, ß)"

        // When
        val permission =
            Permission.create(
                id = PermissionId.generate(),
                name = "READ_USERS_DE",
                description = description,
                resource = "users",
                action = "read",
            )

        // Then
        assertEquals(description, permission.description)
    }

    @Test
    fun `should create permission with maximum length values`() {
        // Given
        val longName = "A".repeat(255) // Typical max length for names
        val longDescription = "A".repeat(1000) // Typical max length for descriptions
        val longResource = "A".repeat(100)
        val longAction = "A".repeat(50)

        // When
        val permission =
            Permission.create(
                id = PermissionId.generate(),
                name = longName,
                description = longDescription,
                resource = longResource,
                action = longAction,
            )

        // Then
        assertEquals(longName, permission.name)
        assertEquals(longDescription, permission.description)
        assertEquals(longResource, permission.resource)
        assertEquals(longAction, permission.action)
    }

    @Test
    fun `should validate permission names are typically uppercase with underscores`() {
        // Given
        val validNames =
            listOf(
                "READ_USERS",
                "CREATE_TENANT",
                "DELETE_ROLE",
                "MANAGE_PERMISSIONS",
                "ADMIN_ALL_RESOURCES",
            )

        // When & Then
        validNames.forEach { name ->
            val permission =
                Permission.create(
                    id = PermissionId.generate(),
                    name = name,
                    description = "Test description",
                    resource = "test",
                    action = "test",
                )

            assertEquals(name, permission.name)
            assertTrue(name.matches(Regex("^[A-Z_]+$"))) // Only uppercase letters and underscores
        }
    }

    @Test
    fun `should maintain immutability of permission objects`() {
        // Given
        val originalPermission =
            Permission.create(
                id = PermissionId.generate(),
                name = "READ_USERS",
                description = "Original description",
                resource = "users",
                action = "read",
            )

        // When
        val updatedPermission = originalPermission.updateDescription("New description")

        // Then
        // Original permission should remain unchanged
        assertEquals("Original description", originalPermission.description)

        // Updated permission should have changes
        assertEquals("New description", updatedPermission.description)

        // Other properties should remain the same
        assertEquals(originalPermission.id, updatedPermission.id)
        assertEquals(originalPermission.name, updatedPermission.name)
        assertEquals(originalPermission.resource, updatedPermission.resource)
        assertEquals(originalPermission.action, updatedPermission.action)
    }

    @Test
    fun `should create hierarchical permission names correctly`() {
        // Given
        val hierarchicalPermissions =
            listOf(
                "TENANT_USER_READ",
                "TENANT_USER_WRITE",
                "TENANT_ROLE_MANAGE",
                "PLATFORM_ADMIN_ALL",
                "SYSTEM_CONFIG_UPDATE",
            )

        // When & Then
        hierarchicalPermissions.forEach { name ->
            val permission =
                Permission.create(
                    id = PermissionId.generate(),
                    name = name,
                    description = "Hierarchical permission: $name",
                    resource = name.split("_")[0].lowercase(),
                    action = name.split("_").last().lowercase(),
                )

            assertEquals(name, permission.name)
            assertTrue(permission.resource.isNotBlank())
            assertTrue(permission.action.isNotBlank())
        }
    }
}
