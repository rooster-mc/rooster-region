package dev.rooster.region

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlockPosTest {
    @Test
    fun `compareTo orders by x first`() {
        assertTrue(BlockPos(1, 9, 9) < BlockPos(2, 0, 0))
        assertTrue(BlockPos(-1, 9, 9) < BlockPos(0, 0, 0))
    }

    @Test
    fun `compareTo breaks x ties with y`() {
        assertTrue(BlockPos(1, 1, 9) < BlockPos(1, 2, 0))
    }

    @Test
    fun `compareTo breaks x and y ties with z`() {
        assertTrue(BlockPos(1, 2, 3) < BlockPos(1, 2, 4))
    }

    @Test
    fun `compareTo treats identical positions as equal`() {
        assertEquals(0, BlockPos(1, 2, 3).compareTo(BlockPos(1, 2, 3)))
        assertEquals(BlockPos(1, 2, 3), BlockPos(1, 2, 3))
    }

    @Test
    fun `sorted orders positions lexicographically`() {
        val positions =
            listOf(
                BlockPos(1, 2, 4),
                BlockPos(2, 0, 0),
                BlockPos(1, 2, 3),
                BlockPos(1, 1, 9),
                BlockPos(1, 2, 3),
            )

        assertEquals(
            listOf(
                BlockPos(1, 1, 9),
                BlockPos(1, 2, 3),
                BlockPos(1, 2, 3),
                BlockPos(1, 2, 4),
                BlockPos(2, 0, 0),
            ),
            positions.sorted(),
        )
    }
}
