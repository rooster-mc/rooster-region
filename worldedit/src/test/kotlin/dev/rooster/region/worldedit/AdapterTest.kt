package dev.rooster.region.worldedit

import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.Vector3
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.EllipsoidRegion
import org.bukkit.World
import org.bukkit.WorldCreator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import kotlin.test.assertEquals

class AdapterTest {
    private lateinit var server: ServerMock
    private lateinit var world: World

    @BeforeEach
    fun setUpWorld() {
        server = MockBukkit.mock()
        world = server.createWorld(WorldCreator("world"))!!
    }

    @AfterEach
    fun tearDownWorld() {
        MockBukkit.unmock()
    }

    @Test
    fun `block vector converts to a location in the supplied world`() {
        val location = BlockVector3.at(1, -2, 3).toLocation(world)

        assertEquals(world, location.world)
        assertEquals(1.0, location.x)
        assertEquals(-2.0, location.y)
        assertEquals(3.0, location.z)
    }

    @Test
    fun `world edit region converts using its min and max points`() {
        val weRegion = CuboidRegion(BlockVector3.at(10, 20, 30), BlockVector3.at(1, 2, 3))

        val region = weRegion.toRegion(world)

        assertEquals(world, region.world)
        assertEquals(1, region.minX)
        assertEquals(2, region.minY)
        assertEquals(3, region.minZ)
        assertEquals(10, region.maxX)
        assertEquals(20, region.maxY)
        assertEquals(30, region.maxZ)
    }

    @Test
    fun `world edit region conversion uses the generic min and max points`() {
        val weRegion = EllipsoidRegion(BlockVector3.at(5, 5, 5), Vector3.at(2.0, 2.0, 2.0))

        val region = weRegion.toRegion(world)

        assertEquals(3, region.minX)
        assertEquals(3, region.minY)
        assertEquals(3, region.minZ)
        assertEquals(7, region.maxX)
        assertEquals(7, region.maxY)
        assertEquals(7, region.maxZ)
    }

    @Test
    fun `world edit region converts for a player using the player world`() {
        val player = server.addPlayer()
        val weRegion = CuboidRegion(BlockVector3.at(1, 2, 3), BlockVector3.at(4, 5, 6))

        val region = weRegion.toRegion(player)

        assertEquals(player.world, region.world)
        assertEquals(1, region.minX)
        assertEquals(2, region.minY)
        assertEquals(3, region.minZ)
        assertEquals(4, region.maxX)
        assertEquals(5, region.maxY)
        assertEquals(6, region.maxZ)
    }
}
