package dev.rooster.region.worldedit

import kotlin.test.Test
import kotlin.test.assertFailsWith

class WorldEditCompileClasspathTest {
    @Test
    fun `WorldEdit API is compile-only`() {
        assertFailsWith<ClassNotFoundException> {
            Class.forName("com.sk89q.worldedit.regions.CuboidRegion")
        }
    }
}
