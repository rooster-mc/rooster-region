package dev.rooster.region

import dev.rooster.region.util.distance
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Vector3dMathTest {
    @Test
    fun `distance returns the component-wise difference`() {
        val result = Vector3d(5.0, 5.0, 5.0) distance Vector3d(2.0, 1.0, 4.0)

        assertEquals(Vector3d(3.0, 4.0, 1.0), result)
    }

    @Test
    fun `distance is zero for equal vectors`() {
        val result = Vector3d(1.0, 2.0, 3.0) distance Vector3d(1.0, 2.0, 3.0)

        assertEquals(Vector3d(0.0, 0.0, 0.0), result)
    }
}
