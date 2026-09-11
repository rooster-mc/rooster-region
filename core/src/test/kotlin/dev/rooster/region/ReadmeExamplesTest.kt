package dev.rooster.region

import dev.rooster.region.util.Box
import dev.rooster.region.util.distance
import dev.rooster.region.util.region
import dev.rooster.region.util.toLocation
import dev.rooster.region.util.toVector3d
import dev.rooster.region.util.value
import org.bukkit.Axis
import org.bukkit.Location
import org.bukkit.Material
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadmeExamplesTest : WorldTestSupport() {
    private fun location(x: Double, y: Double, z: Double) = Location(world, x, y, z)

    @Test
    fun `region example compiles and behaves as documented`() {
        val edge1 = location(0.0, 64.0, 0.0)
        val edge2 = location(15.0, 79.0, 15.0)
        val region = Region(edge1, edge2)

        assertEquals(location(0.0, 64.0, 0.0), region.min)
        assertEquals(location(15.0, 79.0, 15.0), region.max)
        assertEquals(16, region.sizeX)
        assertEquals(16, region.sizeY)
        assertEquals(16, region.sizeZ)
        assertEquals(4096, region.volume)

        assertTrue(region.contains(location(8.0, 70.0, 8.0)))
        assertFalse(region.contains(location(16.0, 70.0, 8.0)))

        val other = Region(location(10.0, 70.0, 10.0), location(20.0, 80.0, 20.0))
        assertTrue(region.intersects(other))

        val east = region.enlarge(2, Face.EAST)
        assertEquals(0, east.minX)
        assertEquals(17, east.maxX)

        val y = region.shrink(1, Axis.Y)
        assertEquals(65, y.minY)
        assertEquals(78, y.maxY)

        val all = region.enlarge(1)
        assertEquals(-1, all.minX)
        assertEquals(16, all.maxX)
        assertEquals(63, all.minY)
        assertEquals(80, all.maxY)
        assertEquals(-1, all.minZ)
        assertEquals(16, all.maxZ)
    }

    @Test
    fun `block positions example compiles and behaves as documented`() {
        val region = Region(location(0.0, 64.0, 0.0), location(15.0, 79.0, 15.0))

        val pos = BlockPos(3, 4, 5)
        assertEquals(3, pos.x)

        val block = region.blockAt(pos)
        assertEquals(3, block.x)
        assertEquals(4, block.y)
        assertEquals(5, block.z)
        assertEquals(world, block.world)

        block.type = Material.STONE
        assertEquals(Material.STONE, world.getBlockAt(3, 4, 5).type)
    }

    @Test
    fun `geometry example compiles and behaves as documented`() {
        val vector = location(1.5, 64.0, -3.25).toVector3d()
        assertEquals(Vector3d(1.5, 64.0, -3.25), vector)

        val back = vector.toLocation(world, yaw = 90f, pitch = 45f)
        assertEquals(world, back.world)
        assertEquals(1.5, back.x)
        assertEquals(64.0, back.y)
        assertEquals(-3.25, back.z)
        assertEquals(90f, back.yaw)
        assertEquals(45f, back.pitch)

        val box: Box = Vector3d(0.0, 64.0, 0.0) to Vector3d(15.0, 79.0, 15.0)
        val fromBox: Region = box.region(world)
        assertEquals(0, fromBox.minX)
        assertEquals(64, fromBox.minY)
        assertEquals(0, fromBox.minZ)
        assertEquals(15, fromBox.maxX)
        assertEquals(79, fromBox.maxY)
        assertEquals(15, fromBox.maxZ)

        assertEquals(1.5, vector.value(Axis.X))
        assertEquals(3.0, location(1.0, 2.0, 3.0).value(Axis.Z))

        val difference = Vector3d(5.0, 5.0, 5.0) distance Vector3d(2.0, 1.0, 4.0)
        assertEquals(Vector3d(3.0, 4.0, 1.0), difference)
    }
}
