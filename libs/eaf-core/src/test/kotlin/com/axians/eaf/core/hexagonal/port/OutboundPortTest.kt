package com.axians.eaf.core.hexagonal.port

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OutboundPortTest {
    @Test
    fun `should define OutboundPort as marker interface`() {
        // Given/When
        val portClass = OutboundPort::class.java

        // Then
        assertNotNull(portClass)
        assertTrue(portClass.isInterface, "OutboundPort should be an interface")

        // Marker interface should have no methods except those inherited from Object
        val declaredMethods = portClass.declaredMethods
        assertTrue(declaredMethods.isEmpty(), "OutboundPort should be a marker interface with no declared methods")
    }

    @Test
    fun `should allow concrete implementation of OutboundPort`() {
        // Given
        class TestRepositoryImpl : OutboundPort {
            private val storage = mutableMapOf<String, String>()

            fun save(entity: String): String {
                val id = "id-${entity.hashCode()}"
                storage[id] = entity
                return id
            }

            fun findById(id: String): String? = storage[id]
        }

        // When
        val repository: OutboundPort = TestRepositoryImpl()

        // Then
        assertNotNull(repository)
        assertTrue(repository is TestRepositoryImpl, "Implementation should be assignable to OutboundPort")

        // Test concrete functionality
        val concreteRepo = repository as TestRepositoryImpl
        val id = concreteRepo.save("test-entity")
        val retrieved = concreteRepo.findById(id)
        assertTrue(retrieved == "test-entity")
    }
}
