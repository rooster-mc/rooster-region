---
name: BlockPos and Region.blockAt
status: done
parent: MVP
depends-on: ["010"]
reviewers: [tester, correctness, architecture, readability]
---

## Goal
Add a reusable integer block position type and a block accessor on `Region`, so
consumers stop rolling their own.

## Scope
- `dev.rooster.region.BlockPos(x: Int, y: Int, z: Int)`: pure data class
  implementing `Comparable<BlockPos>` ordered by `x`, then `y`, then `z`.
- `Region.blockAt(position: BlockPos): Block` returning
  `world.getBlockAt(position.x, position.y, position.z)`.
- Tests for `BlockPos` ordering and `Region.blockAt` (MockBukkit).

## Acceptance criteria
- `BlockPos` is pure (no Bukkit/joml) and orders lexicographically.
- `Region.blockAt` returns the block at the given coordinates in the region's
  world.
- `just build`/`test`/`format` green; `core` still has only the Bukkit API and
  joml on its classpath.

## Out of scope
- Changing `Region`'s `Location`-based shape.

## Notes
- Extracted from `mc-ui-designer`'s `model/BlockPos.kt`; that copy is deleted
  when it adopts this.
