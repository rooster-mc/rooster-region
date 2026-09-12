package dev.rooster.region

// TODO: Strange that there are no helper methods to deal between BlockPos, Bukkit Location and
// rooster Region
data class BlockPos(
    val x: Int,
    val y: Int,
    val z: Int
) : Comparable<BlockPos> {
    override fun compareTo(other: BlockPos): Int {
        val xComparison = x.compareTo(other.x)
        if (xComparison != 0) return xComparison

        val yComparison = y.compareTo(other.y)
        if (yComparison != 0) return yComparison

        return z.compareTo(other.z)
    }
}
