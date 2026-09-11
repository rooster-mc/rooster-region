# Rooster Region

A standalone Kotlin library for Bukkit 3D-space regions, with an optional
WorldEdit adapter. It gives Paper plugins a `Region` built from two `Location`
edges plus a small set of geometry helpers, without pulling in the Rooster
framework, an ORM, or WorldEdit.

## Modules

| Module | Coordinates | Package | Purpose |
|---|---|---|---|
| `core` | `dev.rooster.region:rooster-region` | `dev.rooster.region` | `Region`, `BlockPos`, `Face`, geometry helpers |
| `worldedit` | `dev.rooster.region:rooster-region-worldedit` | `dev.rooster.region.worldedit` | WorldEdit selection conversions |

Version: `1.0-SNAPSHOT`.

`core` depends on `joml` and compiles against the Paper API and the Kotlin
stdlib (`compileOnly`), so its published POM lists `joml` only. Paper and the
stdlib are supplied by the consumer's server/plugin. `worldedit` adds the core
artifact plus the generic WorldEdit API as `compileOnly`; the consumer chooses a
WorldEdit or FastAsyncWorldEdit implementation at runtime.

## Install

### From `mavenLocal`

Publish the library to your local Maven repository:

```sh
just publish        # or ./gradlew publishToMavenLocal
```

Then add `mavenLocal()` to the consumer's repositories and depend on the
coordinates:

```kotlin
// build.gradle.kts
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("dev.rooster.region:rooster-region:1.0-SNAPSHOT")

    // Optional: only if you use the WorldEdit adapter.
    implementation("dev.rooster.region:rooster-region-worldedit:1.0-SNAPSHOT")
}
```

`rooster-region-worldedit` brings `rooster-region` and `joml` transitively. Add
the WorldEdit implementation you run on the server (`WorldEdit` or
`FastAsyncWorldEdit`) as a `compileOnly`/`plugin` dependency yourself.

### As a Gradle composite build

Point the consumer's `settings.gradle.kts` at this repository and map the
published coordinates onto the included projects. The artifact IDs
(`rooster-region`, `rooster-region-worldedit`) differ from the Gradle project
names (`core`, `worldedit`), so the mapping is explicit:

```kotlin
// settings.gradle.kts
includeBuild("/path/to/rooster-region") {
    dependencySubstitution {
        substitute(module("dev.rooster.region:rooster-region"))
            .using(project(":core"))
        substitute(module("dev.rooster.region:rooster-region-worldedit"))
            .using(project(":worldedit"))
    }
}
```

The consumer then declares the same coordinates as the `mavenLocal` path:

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.rooster.region:rooster-region:1.0-SNAPSHOT")
}
```

The included build is compiled from source, so `just publish` is not needed.

## Usage

### `Region`

`Region` wraps two Bukkit `Location` edges. The edges must share a world; the
region normalises them, so min/max do not depend on argument order.

```kotlin
import dev.rooster.region.Face
import dev.rooster.region.Region
import org.bukkit.Axis
import org.bukkit.Location

val edge1 = Location(world, 0.0, 64.0, 0.0)
val edge2 = Location(world, 15.0, 79.0, 15.0)
val region = Region(edge1, edge2)

region.min                 // Location(world, 0.0, 64.0, 0.0)
region.max                 // Location(world, 15.0, 79.0, 15.0)
region.sizeX               // 16 (inclusive block count)
region.sizeY               // 16
region.sizeZ               // 16
region.volume              // 4096

region.contains(Location(world, 8.0, 70.0, 8.0))   // true
region.contains(Location(world, 16.0, 70.0, 8.0))  // false

val other = Region(
    Location(world, 10.0, 70.0, 10.0),
    Location(world, 20.0, 80.0, 20.0),
)
region.intersects(other)   // true

val east = region.enlarge(2, Face.EAST)  // only the east face moves: maxX 17
val y = region.shrink(1, Axis.Y)         // both Y faces move in: minY 65, maxY 78
val all = region.enlarge(1)              // every edge moves out by 1
```

`enlarge`/`shrink` accept a `Face` (`TOP`, `BOTTOM`, `WEST`, `EAST`, `NORTH`,
`SOUTH`), an `Axis` (both faces on that axis), or nothing (every face). They
return a new `Region` and never mutate the receiver.

### Block positions

`BlockPos` is a plain integer coordinate with no Bukkit or joml dependency. It
implements `Comparable<BlockPos>`, ordered lexicographically by `x`, then `y`,
then `z`, so `sorted()` gives a stable spatial order. `Region.blockAt` resolves
the block at a position in the region's own world.

```kotlin
import dev.rooster.region.BlockPos

val pos = BlockPos(3, 4, 5)
pos.x                       // 3

region.blockAt(pos)         // Block at (3, 4, 5) in region.world
region.blockAt(pos).type = Material.STONE
```

### Geometry helpers

```kotlin
import dev.rooster.region.util.Box
import dev.rooster.region.util.distance
import dev.rooster.region.util.region
import dev.rooster.region.util.toLocation
import dev.rooster.region.util.toVector3d
import dev.rooster.region.util.value
import org.bukkit.Axis
import org.bukkit.Location
import org.joml.Vector3d

val vector = Location(world, 1.5, 64.0, -3.25).toVector3d()  // Vector3d(1.5, 64.0, -3.25)
val back = vector.toLocation(world, yaw = 90f, pitch = 45f)  // Location(world, 1.5, 64.0, -3.25, 90f, 45f)

val box: Box = Vector3d(0.0, 64.0, 0.0) to Vector3d(15.0, 79.0, 15.0)
val fromBox: Region = box.region(world)

vector.value(Axis.X)                            // 1.5
Location(world, 1.0, 2.0, 3.0).value(Axis.Z)    // 3.0

Vector3d(5.0, 5.0, 5.0) distance Vector3d(2.0, 1.0, 4.0)  // Vector3d(3.0, 4.0, 1.0)
```

`Box` is the type alias `Pair<Vector3d, Vector3d>`, and `Box.region(world)`
builds a `Region` from its two corners. `Vector3d.distance` is an `infix`
function that returns the **component-wise difference** as a new `Vector3d` (not
a scalar length). `Location.value(axis)` and `Vector3d.value(axis)` read a single
coordinate.

### WorldEdit adapter

```kotlin
import dev.rooster.region.Region
import dev.rooster.region.worldedit.toRegion
import dev.rooster.region.worldedit.toWorldEditRegion
import dev.rooster.region.worldedit.worldEditSelection
import org.bukkit.entity.Player

fun selectionOf(player: Player): Region? =
    player.worldEditSelection()?.toRegion(player.world)

fun cuboidOf(region: Region) = region.toWorldEditRegion()
```

`Player.worldEditSelection()` returns `null` when there is no selection, when
only one position is set, or when the selection belongs to a world other than
the player's current one (for example a stale selection made before a world
change). It never throws for a missing selection, so callers can use `?.` instead
of catching WorldEdit exceptions. The adapter targets the generic
`com.sk89q.worldedit` API and converts a selection through its min/max points,
so any WorldEdit region type works.

## Publishing

Both modules publish to `mavenLocal` with `maven-publish`:

```sh
just publish   # ./gradlew publishToMavenLocal
```

To verify the installed artifacts and their POMs after publishing:

```sh
./gradlew verifyPublishedArtifacts
```

That task publishes both modules and asserts `rooster-region` declares only
`org.joml:joml` and `rooster-region-worldedit` declares only
`dev.rooster.region:rooster-region`. The `check` task additionally runs
`verifyCoreDependencies` and `verifyWorldEditClasspath`, which keep the
framework/ORM/WorldEdit classes off the `core` runtime classpath and the
WorldEdit API off the `worldedit` runtime classpath.

## Development

```sh
just build      # assemble both modules
just test       # JUnit suite
just format     # ktlint
just publish    # publishToMavenLocal
```

See `docs/design.md` and `docs/architecture.md` for the module boundaries and
`docs/manual-test.md` for the checks that need a real server.
