---
name: Lazy block enumeration and loaded-chunk filtering
status: done
parent: MVP
depends-on: ["040"]
reviewers: [tester, correctness, architecture, readability]
---

## Goal
`mc-ui-designer`'s chest scanner currently hand-rolls a triple `for` loop over
`minX..maxX`/`minY..maxY`/`minZ..maxZ` and an `isChunkLoaded` guard. Move that
enumeration into `Region` so consumers iterate coordinates without duplicating
the loop.

## Scope
- Add `Region.blockPositions: Sequence<BlockPos>` — a lazy x/y/z triple loop over
  the region's inclusive bounds. Nothing is materialised until collected.
- Add `Region.loadedBlockPositions: Sequence<BlockPos>` — the same coordinates,
  but visiting only chunks that are currently loaded, chunk-major (the
  `chunks`-style wrapper), so the loaded check is paid once per chunk instead of
  once per block.
- Rebuild the existing `blocks: List<Block>` on top of `blockPositions` so the
  loop lives in one place; behaviour unchanged.
- Tests (MockBukkit `WorldTestSupport`): ordering/extent of `blockPositions`,
  that `loadedBlockPositions` yields nothing for a region in an unloaded chunk,
  and that it yields the in-region coordinates of loaded chunks.

## Out of scope
- Changing `blocks` semantics (it still visits every coordinate, loaded or not)
  or `entities`.
- Any chest/material knowledge — that stays in `mc-ui-designer`.

## Acceptance criteria
- `just build`, `just test`, `just format` pass.
- `blockPositions` covers exactly `sizeX * sizeY * sizeZ` coordinates with no
  duplicates.
- `loadedBlockPositions` never yields a coordinate in an unloaded chunk.

## Notes
- Consumed by `mc-ui-designer` ticket 120 (`capture/ChestScanner.kt`).
