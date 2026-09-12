package dev.rooster.region.util

import org.joml.Vector3d

// TODO: As mentioned
infix fun Vector3d.distance(other: Vector3d): Vector3d =
    Vector3d(this.x - other.x, this.y - other.y, this.z - other.z)
