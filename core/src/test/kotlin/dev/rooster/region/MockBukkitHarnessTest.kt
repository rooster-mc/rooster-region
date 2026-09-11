package dev.rooster.region

import org.mockbukkit.mockbukkit.MockBukkit
import kotlin.test.Test
import kotlin.test.assertNotNull

class MockBukkitHarnessTest {
    @Test
    fun `MockBukkit boots and shuts down`() {
        val server = MockBukkit.mock()
        try {
            assertNotNull(server)
        } finally {
            MockBukkit.unmock()
        }
    }
}
