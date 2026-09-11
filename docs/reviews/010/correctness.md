# Correctness review — 010 (Core region, face, and geometry helpers)

## Round 1
### Verdict
The six deliberate behaviour changes are correct and the ported public surface
matches the source, but three logic defects remain: boundary classification
truncates coordinates instead of flooring them, `closestDistanceToAxis` returns
the far-edge offset for interior points, and `entities` can return entities that
are outside the region. The two retained source behaviours were judged
separately; `contains(region, allowEdges)` is acceptable, `closestDistanceToAxis`
is not.

### Findings
#### 1. `intersectingAxis` truncates coordinates, misclassifying fractional and negative locations
- Location: `core/src/main/kotlin/dev/rooster/region/Region.kt:249-251`
- Problem: `Double.toInt()` truncates toward zero, whereas the region's block
  coordinates floor (`Location.blockX`). So a location that is *outside* the
  region can be reported as lying on a boundary. Reproduction: for
  `Region(loc(0,0,0), loc(10,10,10))`, `isFace(loc(-0.5, 5.0, 5.0))` returns
  `true` because `(-0.5).toInt() == 0 == minX`, while `contains` returns `false`
  (x = -0.5 < 0). The same misclassification flows into
  `contains(region, allowEdges = false)`, which calls `isEdge(region.edge1/edge2)`:
  an inner region whose edge is at a fractional/negative coordinate can be
  rejected (or accepted) incorrectly. The existing `isCorner`/`isEdge`/`isFace`
  test (`RegionTest.kt:228-241`) only uses exact integers, so it does not catch
  this. The source has the same defect; it was not among the six fixes.
- Suggested fix: classify on block coordinates —
  `location.blockX == minX || location.blockX == maxX`, and likewise for Y/Z —
  which floors and is consistent with the block-based region; if exact-plane
  semantics is intended instead, compare `location.x` with `dMinX`/`dMaxX`.
  Add a case with a negative fractional location.

#### 2. `closestDistanceToAxis` returns the far edge's offset for points inside the region
- Location: `core/src/main/kotlin/dev/rooster/region/Region.kt:274-279`
- Problem: `distanceEdge1 = value - edge1` and `distanceEdge2 = value - edge2`
  are signed, and `min(d1, d2)` picks the *negative* one whenever `value` lies
  between the two edges — i.e. the offset to the farther edge, not the closest
  one. Reproduction: `Region(loc(0,0,0), loc(10,10,10)).closestDistanceToAxis(Axis.X, 3.0)`
  yields `min(3, -7) = -7.0`, while the nearest edge is 3 away. The only test
  (`RegionTest.kt:348`) uses `value = 15.0`, which is outside past the max edge,
  where the two signed offsets happen to agree with the closest distance, so the
  bug is untested. The helper is public API and unused in-repo today.
- Suggested fix: return `minOf(distanceEdge1.absoluteValue, distanceEdge2.absoluteValue)`
  (or clamp to `0.0` if the intent is "distance to the region" rather than "to
  the nearest edge"). The existing `15.0` assertion still passes; add an interior
  case.

#### 3. `entities` returns entities outside the region
- Location: `core/src/main/kotlin/dev/rooster/region/Region.kt:125-137`
- Problem: each per-block query is
  `getNearbyEntities(Location(x, y, z), 1.0, 1.0, 1.0)` — Bukkit documents the
  `1.0`s as half-extents, so the search box spans `[x-1, x+1]` per axis — and the
  result is filtered only by entity type, never by membership. Entities up to one
  block outside the region are therefore returned. Reproduction: with
  `Region(loc(0,0,0), loc(0,0,0))` (a single block) a Zombie at
  `(1.0, 0.0, 0.0)` is outside (`contains` is `false`) yet is included in
  `region.entities`. `players` inherits the same over-inclusion. The entities test
  (`RegionTest.kt:316-322`) spawns at the region centre, so it does not observe
  this.
- Suggested fix: filter the flattened results with `contains(entity.location)`
  (and/or centre the query on the block centre `x + 0.5`). Add a test with an
  entity just outside a boundary.

### Non-findings
- The six deliberate changes are correct:
  - `Math.floorDiv` chunk indices (`Region.kt:62-65`): block `x` maps to chunk
    `floor(x/16)`, correct for negatives (e.g. `minX = -1` → chunk `-1`, not `0`).
  - `isChunkFullyContained` using `chunk.x * 16` (`Region.kt:179-186`): the source
    divided the chunk coordinate by 16 a second time, so its `chunkMin/MaxX` were
    wrong; the fix matches the real block span `[chunk.x*16, chunk.x*16+15]`.
  - Rewritten `changeBorders` (`Region.kt:202-239`): positive faces move
    `maxEdge`, negative faces move `minEdge`; `enlarge` grows and `shrink`
    contracts on both normalised and reversed edges. Verified per face, per axis,
    all-faces, and reversed-edge cases against the tests.
  - Symmetric AABB `intersects` (`Region.kt:94-100`): standard inclusive overlap,
    symmetric, and consistent with the inclusive `contains`. It correctly reports
    true for crossing regions where neither contains the other's corner.
  - `blocksArray` (`Region.kt:109-120`): the nested
    `Array(sizeX) { Array(sizeY) { Array(sizeZ) { blocks[index++] } } }` fills in
    the same x-outer / y-middle / z-inner order as `iterateRegion`, so `[x][y][z]`
    is the block at the relative coordinate; the source's modulo/division
    decomposition scrambled the mapping.
  - `entities().distinct()` (`Region.kt:136-137`): correctly removes the
    duplicates produced by overlapping per-block queries.
- `contains(region, allowEdges)` (`Region.kt:80-90`): judged acceptable. The rule
  is self-consistent — `allowEdges = true` accepts anything contained, `false`
  rejects only when an inner corner lies exactly on an outer *edge*
  (`intersectingAxis == 2`), allowing face and corner contact. The tests at
  `RegionTest.kt:80-100` lock this, no acceptance criterion defines otherwise,
  and it is source-faithful. (Its `isEdge` calls do inherit finding 1.)
- `closesDistanceTo` (`Region.kt:267-272`) is source-faithful: it returns the
  smaller Euclidean distance to the two defining corners. It is a "nearest
  defining corner" helper, not distance-to-region, but that matches the source and
  its test.
- Over-shrinking past the region's own size still normalises and can grow the
  region (e.g. a 1-block region `shrink(1, Face.EAST)`). This is inherited from
  the source and undefined input; not flagged.
- `changeBorders` now rebuilds from normalised `min`/`max` and so drops fractional
  edge coordinates that the source's (buggy) `edge1`/`edge2` handling would have
  kept. Acceptable for a block-based region and needed to fix reversed edges.
- `Vector3d.distance` (`Vector3dMath.kt:5-6`) returns the component-wise
  difference, matching the source. The infix call resolves to the extension, not
  JOML's non-infix `distance(Vector3d): Double`.
- World identity: the constructor enforces `edge1.world == edge2.world`;
  cross-region `contains`/`intersects` do not check worlds, which is
  source-faithful and outside the ticket's criteria.
- Package/imports: `Region.kt`/`Face.kt`/`Geometry.kt`/`Vector3dMath.kt` import
  only Bukkit, JOML, and `dev.rooster.region(.util)`. No Adventure, no
  `compareVectors`.
- Prior findings: I concur with all five tester findings (crossing-overlap test,
  tautological `distinct` assertion, missing negative filter case, untested
  `customBox`, MT-002 wording). I have not re-reported them.
