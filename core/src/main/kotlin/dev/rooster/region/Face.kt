package dev.rooster.region

import org.bukkit.Axis

enum class Face(
    val axis: Axis,
    val positive: Boolean
) {
    TOP(Axis.Y, true),
    BOTTOM(Axis.Y, false),
    WEST(Axis.X, false),
    EAST(Axis.X, true),
    NORTH(Axis.Z, false),
    SOUTH(Axis.Z, true),
}
