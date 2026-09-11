package dev.rooster.region.util

import dev.rooster.region.Region
import org.bukkit.Axis
import org.bukkit.Location
import org.bukkit.World
import org.joml.Vector3d

fun Location.toVector3d(): Vector3d = Vector3d(this.x, this.y, this.z)

fun Vector3d.toLocation(world: World, yaw: Float = 0f, pitch: Float = 0f): Location =
    Location(world, this.x, this.y, this.z, yaw, pitch)

typealias Box = Pair<Vector3d, Vector3d>

fun Box.region(world: World) = Region(first.toLocation(world), second.toLocation(world))

fun Location.value(axis: Axis): Double =
    when (axis) {
        Axis.X -> this.x
        Axis.Y -> this.y
        Axis.Z -> this.z
    }

fun Vector3d.value(axis: Axis): Double =
    when (axis) {
        Axis.X -> this.x
        Axis.Y -> this.y
        Axis.Z -> this.z
    }
