package dev.rooster.region

import org.bukkit.World
import org.bukkit.WorldCreator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock

abstract class WorldTestSupport {
    protected lateinit var server: ServerMock
    protected lateinit var world: World

    @BeforeEach
    fun setUpWorld() {
        server = MockBukkit.mock()
        world = server.createWorld(WorldCreator("world"))!!
    }

    @AfterEach
    fun tearDownWorld() {
        MockBukkit.unmock()
    }
}
