package com.axians.eaf.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EafCoreTest {
    @Test
    fun `should return correct version`() {
        assertEquals("0.1.0-SNAPSHOT", EafCore.getVersion())
    }
}
