---
name: Core region, face, and geometry helpers
status: todo
parent: MVP
depends-on: ["000"]
reviewers: [tester, correctness, architecture, readability]
---

## Goal
Port the region code into the standalone `core` module: `Region`, `Face`, and
the geometry helpers they need, with no Rooster/ORM/WorldEdit/Adventure
dependency.

## Source of truth
- `/home/cyp/repos/rooster-core/src/main/kotlin/dev/rooster/core/region/Region.kt`
- `/home/cyp/repos/rooster-core/src/main/kotlin/dev/rooster/core/region/Face.kt`
- `/home/cyp/repos/rooster-core/src/main/kotlin/dev/rooster/core/util/LocationUtils.kt`
  (the geometry parts only)
- The older copy in
  `/home/cyp/repos/rooster-monolith/src/main/kotlin/dev/cypdashuhn/rooster/gameengine/region/`
  is a useful cross-check.

## Scope
- `dev.rooster.region.Region`: two Bukkit `Location` edges, min/max
  normalisation, sizes/volume, `contains` (location/region/entity),
  `intersects`, `blocks`, `blocksArray`, `entities`, `players`, `chunks`,
  `chunksFull`, `enlarge`/`shrink` (by `Axis` and by `Face`),
  `isCorner`/`isEdge`/`isFace`, `edges`, distance helpers, `compareToAxis`.
- `dev.rooster.region.Face` (`TOP`/`BOTTOM`/`WEST`/`EAST`/`NORTH`/`SOUTH`).
- `dev.rooster.region.util`: `Location.toVector3d()`,
  `Vector3d.toLocation(world, yaw, pitch)`, `typealias Box`,
  `Box.region(world)`, `Location.value(axis)`, `Vector3d.value(axis)`,
  `Vector3d.distance(other)`.
- Rename the package from `dev.rooster.core.region` to `dev.rooster.region`;
  fix imports accordingly.
- Tests with MockBukkit covering the pure math and a few Bukkit behaviours.

## Acceptance criteria
- `core` compiles with only the Bukkit API and `joml` on its classpath.
- Tests cover: min/max normalisation, inclusive sizes, `contains` boundary
  conditions, `intersects`, `enlarge`/`shrink` per face and axis,
  `isCorner`/`isEdge`/`isFace`, chunk indices (including negative coordinates),
  and `Vector3d.distance`.
- No `compareVectors` (player messaging) and no Adventure import.

## Out of scope
- WorldEdit integration (ticket 020).
- Non-cuboid regions.

## Notes
- Keep `Region`'s public shape as close to the source as possible so consumers
  can swap imports with minimal churn.
- `Vector3dHelper.compareVectors` is deliberately dropped; it is localization,
  not geometry.
- If MockBukkit cannot construct a `World`/`Block` for a member, test the
  arithmetic instead and record a manual-test entry.
