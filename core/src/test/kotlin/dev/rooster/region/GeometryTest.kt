package dev.rooster.region

import dev.rooster.region.util.Box
import dev.rooster.region.util.region
import dev.rooster.region.util.toLocation
import dev.rooster.region.util.toVector3d
import dev.rooster.region.util.value
import org.bukkit.Axis
import org.bukkit.Location
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GeometryTest : WorldTestSupport() {
    @Test
    fun `location converts to a matching vector`() {
        val location = Location(world, 1.5, -2.0, 3.25)

        assertEquals(Vector3d(1.5, -2.0, 3.25), location.toVector3d())
    }

    @Test
    fun `vector converts to a location with yaw and pitch`() {
        val location = Vector3d(1.5, -2.0, 3.25).toLocation(world, 90f, 45f)

        assertEquals(world, location.world)
        assertEquals(1.5, location.x)
        assertEquals(-2.0, location.y)
        assertEquals(3.25, location.z)
        assertEquals(90f, location.yaw)
        assertEquals(45f, location.pitch)
    }

    @Test
    fun `vector converts to a location with default rotation`() {
        val location = Vector3d(1.0, 2.0, 3.0).toLocation(world)

        assertEquals(0f, location.yaw)
        assertEquals(0f, location.pitch)
    }

    @Test
    fun `box region builds a region from its two corners`() {
        val box: Box = Vector3d(0.0, 0.0, 0.0) to Vector3d(10.0, 10.0, 10.0)

        val region = box.region(world)
        assertEquals(0, region.minX)
        assertEquals(10, region.maxX)
        assertEquals(0, region.minZ)
        assertEquals(10, region.maxZ)
    }

    @Test
    fun `value reads the matching axis component`() {
        val location = Location(world, 1.0, 2.0, 3.0)
        val vector = Vector3d(4.0, 5.0, 6.0)

        assertEquals(1.0, location.value(Axis.X))
        assertEquals(2.0, location.value(Axis.Y))
        assertEquals(3.0, location.value(Axis.Z))
        assertEquals(4.0, vector.value(Axis.X))
        assertEquals(5.0, vector.value(Axis.Y))
        assertEquals(6.0, vector.value(Axis.Z))
    }
}
