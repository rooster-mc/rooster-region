package dev.rooster.region

import dev.rooster.region.util.Box
import org.bukkit.Axis
import org.bukkit.Location
import org.bukkit.WorldCreator
import org.bukkit.entity.EntityType
import org.bukkit.entity.Zombie
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegionTest : WorldTestSupport() {
    private fun location(x: Double, y: Double, z: Double) = Location(world, x, y, z)

    @Test
    fun `min and max normalise reversed edges`() {
        val region = Region(location(10.0, 20.0, 30.0), location(1.0, 2.0, 3.0))

        assertEquals(1, region.minX)
        assertEquals(2, region.minY)
        assertEquals(3, region.minZ)
        assertEquals(10, region.maxX)
        assertEquals(20, region.maxY)
        assertEquals(30, region.maxZ)
        assertEquals(location(1.0, 2.0, 3.0), region.min)
        assertEquals(location(10.0, 20.0, 30.0), region.max)
    }

    @Test
    fun `region rejects edges from different worlds`() {
        val otherWorld = MockBukkit.getMock()!!.createWorld(WorldCreator("other"))!!

        assertFailsWith<IllegalArgumentException> {
            Region(location(0.0, 0.0, 0.0), Location(otherWorld, 0.0, 0.0, 0.0))
        }
    }

    @Test
    fun `sizes are inclusive and volume is their product`() {
        val region = Region(location(1.0, 2.0, 3.0), location(3.0, 5.0, 8.0))

        assertEquals(3, region.sizeX)
        assertEquals(4, region.sizeY)
        assertEquals(6, region.sizeZ)
        assertEquals(72, region.volume)
        assertEquals(24, region.sideSizeX)
        assertEquals(18, region.sideSizeY)
        assertEquals(12, region.sideSizeZ)
        assertEquals(Vector3d(3.0, 4.0, 6.0), region.dimensions)
    }

    @Test
    fun `a single block region has size and volume one`() {
        val region = Region(location(5.0, 5.0, 5.0), location(5.0, 5.0, 5.0))

        assertEquals(1, region.sizeX)
        assertEquals(1, region.sizeY)
        assertEquals(1, region.sizeZ)
        assertEquals(1, region.volume)
    }

    @Test
    fun `contains includes the boundary blocks and excludes everything outside`() {
        val region = Region(location(0.0, 0.0, 0.0), location(10.0, 10.0, 10.0))

        assertTrue(region.contains(location(0.0, 0.0, 0.0)))
        assertTrue(region.contains(location(10.0, 10.0, 10.0)))
        assertTrue(region.contains(location(5.0, 5.0, 5.0)))
        assertFalse(region.contains(location(-0.5, 5.0, 5.0)))
        assertFalse(region.contains(location(10.5, 5.0, 5.0)))
        assertFalse(region.contains(location(5.0, -0.5, 5.0)))
        assertFalse(region.contains(location(5.0, 10.5, 5.0)))
        assertFalse(region.contains(location(5.0, 5.0, -0.5)))
        assertFalse(region.contains(location(5.0, 5.0, 10.5)))
    }

    @Test
    fun `contains region honours allowEdges`() {
        val outer = Region(location(0.0, 0.0, 0.0), location(10.0, 10.0, 10.0))
        val inner = Region(location(2.0, 2.0, 2.0), location(8.0, 8.0, 8.0))
        assertTrue(outer.contains(inner))
        assertTrue(outer.contains(inner, allowEdges = true))

        val cornerOnOuterCorner = Region(location(0.0, 0.0, 0.0), location(8.0, 8.0, 8.0))
        assertTrue(outer.contains(cornerOnOuterCorner, allowEdges = true))
        assertTrue(outer.contains(cornerOnOuterCorner, allowEdges = false))

        val cornerOnOuterEdge = Region(location(0.0, 0.0, 5.0), location(8.0, 8.0, 8.0))
        assertTrue(outer.contains(cornerOnOuterEdge, allowEdges = true))
        assertFalse(outer.contains(cornerOnOuterEdge, allowEdges = false))

        val touchingOuterFace = Region(location(2.0, 2.0, 0.0), location(8.0, 8.0, 8.0))
        assertTrue(outer.contains(touchingOuterFace, allowEdges = false))

        val partiallyOutside = Region(location(5.0, 5.0, 5.0), location(15.0, 15.0, 15.0))
        assertFalse(outer.contains(partiallyOutside, allowEdges = true))
    }

    @Test
    fun `contains entity follows the entity location`() {
        val region = Region(location(0.0, 0.0, 0.0), location(10.0, 10.0, 10.0))
        val inside = world.spawn(location(5.0, 5.0, 5.0), Zombie::class.java)
        val outside = world.spawn(location(20.0, 5.0, 5.0), Zombie::class.java)

        assertTrue(region.contains(inside))
        assertFalse(region.contains(outside))
    }

    @Test
    fun `intersects detects overlapping regions symmetrically`() {
        val region = Region(location(0.0, 0.0, 0.0), location(10.0, 10.0, 10.0))
        val overlap = Region(location(5.0, 5.0, 5.0), location(15.0, 15.0, 15.0))
        val crossing = Region(location(-5.0, 5.0, 5.0), location(5.0, 15.0, 5.0))
        val nested = Region(location(2.0, 2.0, 2.0), location(8.0, 8.0, 8.0))
        val disjoint = Region(location(20.0, 20.0, 20.0), location(30.0, 30.0, 30.0))

        assertTrue(region.intersects(overlap))
        assertTrue(overlap.intersects(region))
        assertTrue(region.intersects(crossing))
        assertTrue(crossing.intersects(region))
        assertTrue(region.intersects(nested))
        assertTrue(nested.intersects(region))
        assertFalse(region.intersects(disjoint))
        assertFalse(disjoint.intersects(region))
    }

    @Test
    fun `enlarge by face moves only that face outward`() {
        val region = Region(location(0.0, 0.0, 0.0), location(10.0, 10.0, 10.0))

        val east = region.enlarge(2, Face.EAST)
        assertEquals(0, east.minX)
        assertEquals(12, east.maxX)

        val west = region.enlarge(2, Face.WEST)
        assertEquals(-2, west.minX)
        assertEquals(10, west.maxX)

        val top = region.enlarge(2, Face.TOP)
        assertEquals(0, top.minY)
        assertEquals(12, top.maxY)

        val bottom = region.enlarge(2, Face.BOTTOM)
        assertEquals(-2, bottom.minY)
        assertEquals(10, bottom.maxY)

        val south = region.enlarge(2, Face.SOUTH)
        assertEquals(0, south.minZ)
        assertEquals(12, south.maxZ)

        val north = region.enlarge(2, Face.NORTH)
        assertEquals(-2, north.minZ)
        assertEquals(10, north.maxZ)
    }

    @Test
    fun `shrink by face moves only that face inward`() {
        val region = Region(location(0.0, 0.0, 0.0), location(10.0, 10.0, 10.0))

        val east = region.shrink(2, Face.EAST)
        assertEquals(0, east.minX)
        assertEquals(8, east.maxX)

        val west = region.shrink(2, Face.WEST)
        assertEquals(2, west.minX)
        assertEquals(10, west.maxX)

        val top = region.shrink(2, Face.TOP)
        assertEquals(8, top.maxY)

        val bottom = region.shrink(2, Face.BOTTOM)
        assertEquals(2, bottom.minY)

        val south = region.shrink(2, Face.SOUTH)
        assertEquals(8, south.maxZ)

        val north = region.shrink(2, Face.NORTH)
        assertEquals(2, north.minZ)
    }

    @Test
    fun `enlarge and shrink by axis affect both faces on that axis`() {
        val region = Region(location(0.0, 0.0, 0.0), location(10.0, 10.0, 10.0))

        val enlarged = region.enlarge(2, Axis.X)
        assertEquals(-2, enlarged.minX)
        assertEquals(12, enlarged.maxX)
        assertEquals(0, enlarged.minY)
        assertEquals(10, enlarged.maxY)

        val shrunk = region.shrink(2, Axis.X)
        assertEquals(2, shrunk.minX)
        assertEquals(8, shrunk.maxX)
        assertEquals(0, shrunk.minY)
        assertEquals(10, shrunk.maxY)
    }

    @Test
    fun `enlarge and shrink without faces affect every axis symmetrically`() {
        val region = Region(location(0.0, 0.0, 0.0), location(10.0, 10.0, 10.0))

        val enlarged = region.enlarge(1)
        assertEquals(-1, enlarged.minX)
        assertEquals(11, enlarged.maxX)
        assertEquals(-1, enlarged.minY)
        assertEquals(11, enlarged.maxY)
        assertEquals(-1, enlarged.minZ)
        assertEquals(11, enlarged.maxZ)

        val shrunk = region.shrink(1)
        assertEquals(1, shrunk.minX)
        assertEquals(9, shrunk.maxX)
        assertEquals(1, shrunk.minY)
        assertEquals(9, shrunk.maxY)
        assertEquals(1, shrunk.minZ)
        assertEquals(9, shrunk.maxZ)
    }

    @Test
    fun `enlarge normalises a region built from reversed edges`() {
        val reversed = Region(location(10.0, 10.0, 10.0), location(0.0, 0.0, 0.0))

        val enlarged = reversed.enlarge(2, Face.EAST)
        assertEquals(0, enlarged.minX)
        assertEquals(12, enlarged.maxX)
    }

    @Test
    fun `isCorner isEdge and isFace classify boundary locations`() {
        val region = Region(location(0.0, 0.0, 0.0), location(10.0, 10.0, 10.0))

        assertTrue(region.isCorner(location(0.0, 0.0, 0.0)))
        assertTrue(region.isCorner(location(10.0, 10.0, 10.0)))
        assertTrue(region.isEdge(location(0.0, 0.0, 5.0)))
        assertTrue(region.isFace(location(0.0, 5.0, 5.0)))
        assertFalse(region.isFace(location(5.0, 5.0, 5.0)))
        assertFalse(region.isFace(location(-0.5, 5.0, 5.0)))
        assertEquals(3, region.intersectingAxis(location(0.0, 0.0, 0.0)))
        assertEquals(2, region.intersectingAxis(location(0.0, 0.0, 5.0)))
        assertEquals(1, region.intersectingAxis(location(0.0, 5.0, 5.0)))
        assertEquals(0, region.intersectingAxis(location(5.0, 5.0, 5.0)))
        assertEquals(0, region.intersectingAxis(location(-0.5, 5.0, 5.0)))
    }

    @Test
    fun `edges returns all eight corners`() {
        val region = Region(location(0.0, 0.0, 0.0), location(1.0, 1.0, 1.0))

        val edges = region.edges()
        assertEquals(8, edges.size)
        assertEquals(8, edges.toSet().size)
        assertTrue(edges.contains(location(0.0, 0.0, 0.0)))
        assertTrue(edges.contains(location(1.0, 1.0, 1.0)))
    }

    @Test
    fun `chunk indices use floor division for negative coordinates`() {
        val positive = Region(location(0.0, 0.0, 0.0), location(31.0, 0.0, 31.0))
        assertEquals(0, positive.minXChunk)
        assertEquals(1, positive.maxXChunk)
        assertEquals(0, positive.minZChunk)
        assertEquals(1, positive.maxZChunk)

        val negative = Region(location(-17.0, 0.0, -17.0), location(-1.0, 0.0, -1.0))
        assertEquals(-2, negative.minXChunk)
        assertEquals(-1, negative.maxXChunk)
        assertEquals(-2, negative.minZChunk)
        assertEquals(-1, negative.maxZChunk)
    }

    @Test
    fun `chunks covers every chunk the region touches`() {
        val region = Region(location(0.0, 0.0, 0.0), location(31.0, 0.0, 31.0))

        val expected = setOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)
        assertEquals(expected, region.chunks.map { it.x to it.z }.toSet())
    }

    @Test
    fun `chunksFull keeps only chunks entirely inside the region`() {
        val partial = Region(location(0.0, 0.0, 0.0), location(30.0, 0.0, 30.0))
        assertEquals(setOf(0 to 0), partial.chunksFull.map { it.x to it.z }.toSet())

        val exact = Region(location(0.0, 0.0, 0.0), location(31.0, 0.0, 31.0))
        val expected = setOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)
        assertEquals(expected, exact.chunksFull.map { it.x to it.z }.toSet())

        val negative = Region(location(-16.0, 0.0, -16.0), location(-1.0, 0.0, -1.0))
        assertEquals(setOf(-1 to -1), negative.chunksFull.map { it.x to it.z }.toSet())
    }

    @Test
    fun `blocks iterates every block exactly once`() {
        val region = Region(location(0.0, 0.0, 0.0), location(1.0, 2.0, 3.0))

        val blocks = region.blocks
        assertEquals(24, blocks.size)
        assertEquals(setOf(0, 1), blocks.map { it.x }.toSet())
        assertEquals(setOf(0, 1, 2), blocks.map { it.y }.toSet())
        assertEquals(setOf(0, 1, 2, 3), blocks.map { it.z }.toSet())
    }

    @Test
    fun `blocksArray indexes blocks by their relative coordinate`() {
        val region = Region(location(0.0, 0.0, 0.0), location(1.0, 2.0, 3.0))

        val array = region.blocksArray
        assertEquals(2, array.size)
        assertEquals(3, array[0].size)
        assertEquals(4, array[0][0].size)
        assertEquals(0, array[0][0][0].x)
        assertEquals(1, array[1][2][3].x)
        assertEquals(2, array[1][2][3].y)
        assertEquals(3, array[1][2][3].z)
    }

    @Test
    fun `entities finds a spawned entity inside the region exactly once`() {
        val region = Region(location(0.0, 0.0, 0.0), location(10.0, 10.0, 10.0))
        val zombie = world.spawn(location(5.0, 5.0, 5.0), Zombie::class.java)

        assertEquals(1, region.entities.count { it == zombie })
    }

    @Test
    fun `entities filters by entity type`() {
        val region = Region(location(0.0, 0.0, 0.0), location(10.0, 10.0, 10.0))
        val zombie = world.spawn(location(5.0, 5.0, 5.0), Zombie::class.java)

        assertEquals(1, region.entities(EntityType.ZOMBIE).size)
        assertTrue(region.entities(EntityType.ZOMBIE).contains(zombie))
        assertTrue(region.entities(EntityType.CREEPER).isEmpty())
    }

    @Test
    fun `entities excludes entities outside the region`() {
        val region = Region(location(0.0, 0.0, 0.0), location(0.0, 0.0, 0.0))
        val inside = world.spawn(location(0.0, 0.0, 0.0), Zombie::class.java)
        val outside = world.spawn(location(0.5, 0.0, 0.0), Zombie::class.java)

        assertFalse(region.contains(outside))
        assertEquals(1, region.entities.size)
        assertTrue(region.entities.contains(inside))
        assertFalse(region.entities.contains(outside))
    }

    @Test
    fun `players returns players inside the region`() {
        val region = Region(location(0.0, 0.0, 0.0), location(10.0, 10.0, 10.0))
        val player = server.addPlayer()
        player.teleport(location(5.0, 5.0, 5.0))

        assertTrue(region.players.contains(player))
    }

    @Test
    fun `compareToAxis classifies a value relative to the region`() {
        val region = Region(location(0.0, 0.0, 0.0), location(10.0, 10.0, 10.0))

        assertEquals(Region.AxisComparison.INTERSECTING, region.compareToAxis(Axis.X, 5.0))
        assertEquals(Region.AxisComparison.BEHIND, region.compareToAxis(Axis.X, 15.0))
        assertEquals(Region.AxisComparison.BEFORE, region.compareToAxis(Axis.X, -5.0))
    }

    @Test
    fun `compareToAxis honours a custom box`() {
        val region = Region(location(0.0, 0.0, 0.0), location(10.0, 10.0, 10.0))
        val shiftedBox: Box = Vector3d(20.0, 0.0, 0.0) to Vector3d(30.0, 0.0, 0.0)

        assertEquals(Region.AxisComparison.INTERSECTING, region.compareToAxis(Axis.X, 5.0))
        assertEquals(Region.AxisComparison.BEFORE, region.compareToAxis(Axis.X, 5.0, shiftedBox))
    }

    @Test
    fun `closestDistanceToAxis and closestDistanceTo measure the nearest edge`() {
        val region = Region(location(0.0, 0.0, 0.0), location(10.0, 10.0, 10.0))

        assertEquals(0.0, region.closestDistanceTo(location(0.0, 0.0, 0.0)))
        assertEquals(10.0, region.closestDistanceTo(location(0.0, 0.0, 10.0)))
        assertEquals(3.0, region.closestDistanceToAxis(Axis.X, 3.0))
        assertEquals(5.0, region.closestDistanceToAxis(Axis.X, 15.0))
    }
}
