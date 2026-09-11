package dev.rooster.region

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ClasspathSmokeTest {
    @Test
    fun `core classpath excludes framework, ORM, and WorldEdit classes`() {
        val forbidden =
            listOf(
                "dev.rooster.core.region.Region",
                "org.jetbrains.exposed.sql.Table",
                "com.sk89q.worldedit.WorldEdit",
                "com.fastasyncworldedit.core.Fawe",
            )

        forbidden.forEach { type ->
            assertFailsWith<ClassNotFoundException>("$type must not be on core's classpath") {
                Class.forName(type)
            }
        }
    }
}
