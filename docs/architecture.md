# Architecture

Two Gradle modules. `core` has no dependency on the Rooster framework, an ORM,
or WorldEdit; `worldedit` adds the WorldEdit bridge on top.

```
rooster-region/
  core/       -> dev.rooster.region:rooster-region
  worldedit/  -> dev.rooster.region:rooster-region-worldedit
```

## core

```
dev.rooster.region
  Region.kt            two Location edges -> min/max, size, iteration, contains,
                       intersects, enlarge/shrink, chunk math, blocks/entities
  Face.kt              TOP/BOTTOM/WEST/EAST/NORTH/SOUTH over Bukkit Axis
dev.rooster.region.util
  Geometry.kt          Location.toVector3d(), Vector3d.toLocation(world),
                       typealias Box = Pair<Vector3d, Vector3d>, Box.region(world),
                       Location.value(axis), Vector3d.value(axis)
  Vector3dMath.kt      Vector3d.distance(other) (infix)
```

Dependencies: `compileOnly(paper-api)`, `implementation(joml)`. No Rooster, no
Exposed, no WorldEdit, no Adventure.

## worldedit

```
dev.rooster.region.worldedit
  Adapter.kt           Region.toWorldEditRegion(), WERegion.toRegion(world|player),
                       BlockVector3.toLocation(world), Player.worldEditSelection()
```

Depends on `project(":core")` and the WorldEdit API (`compileOnly`, via the
FAWE artifacts that implement it). No `plugin.yml`, no hard FAWE runtime
dependency — the consumer picks WorldEdit or FAWE.

## Seams

- `Region` is the single public spatial type; the WorldEdit adapter converts
  to/from it so nothing else needs to know about WorldEdit.
- `Player.worldEditSelection()` returns `null` when no selection exists, so
  callers never catch WorldEdit exceptions themselves.

## Conventions

- Kotlin, ktlint via `.editorconfig` (max line 100).
- No code comments unless they explain a non-obvious *why*.
- Tests use backticked sentence names and JUnit 5; Bukkit types via MockBukkit.
