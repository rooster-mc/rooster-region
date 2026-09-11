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

## Round 2
### Verdict
All three round-1 correctness findings are resolved and verified against the
current tree, and the round-1 refactors/renames introduce no new logic, math or
integration defects. I have no new findings; I concur with the tester's round-2
finding that the new `entities` exclusion test cannot observe the filter it
targets (a test-quality issue, not a production defect).

### Findings
No new findings. The three round-1 correctness findings are resolved in
`f3ce7ac`, and the tests added for them are effective apart from the vacuous
`entities` exclusion test the tester owns.

### Non-findings
- **Round-1 finding 1 resolved.** `intersectingAxis` now compares
  `location.blockX/blockY/blockZ` against the region bounds
  (`Region.kt:259-261`) instead of `Double.toInt()`. The floor semantics fix the
  negative-coordinate truncation; `isFace(location(-0.5, 5.0, 5.0))` is now
  `false` and `intersectingAxis(...)` is `0` (`RegionTest.kt:242,247`). The
  `contains(region, allowEdges)` cases still pass because integer coordinates
  classify identically.
- **Round-1 finding 2 resolved.** `closestDistanceToAxis` returns
  `minOf(distanceEdge1.absoluteValue, distanceEdge2.absoluteValue)`
  (`Region.kt:288`), which is the true nearest-edge distance for interior,
  before-min and after-max values, and works for reversed edges too. The interior
  case `closestDistanceToAxis(Axis.X, 3.0) == 3.0` and the outside case `15.0 →
  5.0` are both asserted (`RegionTest.kt:384-385`).
- **Round-1 finding 3 resolved in production.** `entities` now filters with
  `contains(entity.location)` (`Region.kt:135`), so entities outside the
  continuous region box are dropped. I concur with tester round-2 finding 1 that
  the test at `RegionTest.kt:340-349` is vacuous: I confirmed independently that
  paper-api's `BoundingBox.contains(Vector)` is max-exclusive
  (`org/bukkit/util/BoundingBox.java:757-761`) and MockBukkit 4.45.0's
  `WorldMock.getNearbyEntities(Location,double,double,double,Predicate)` builds
  `BoundingBox.of(location, x, y, z)` and filters with `contains`, so the outside
  Zombie at exactly `(1.0, 0.0, 0.0)` is discarded by the harness before the
  production filter runs. The production filter itself is correct; the fix is to
  the test's entity placement, as the tester says.
- **`enlarge`/`shrink` overloads and `expandBorders`/`contractBorders` refactor
  are correct.** The new one-argument overloads (`Region.kt:189,198`) resolve the
  former `enlarge(1)` ambiguity additively; `contractBorders` negating the amount
  before `changeBorders` preserves the round-1 semantics (positive faces move
  `maxEdge`, negative faces move `minEdge`), and the all-faces default still
  applies when no faces are passed (`RegionTest.kt:204-222`).
- **Rename `closesDistanceTo` → `closestDistanceTo`.** This is a deliberate,
  documented deviation from the ticket's "keep the public shape close" note
  (readability round-1 finding 1). It is a naming decision owned by readability
  with no correctness consequence; I concur and do not re-raise it.
- **`api(joml)` + `java-library`.** The integration contract is now correct for
  consumers (JOML types are on `core`'s public API), and `core`'s runtime
  classpath stays `joml`-only. No correctness impact.
- **`entities`/`contains` model note.** Filtering by `contains` makes `entities`
  consistent with `contains(entity)`, so an entity in the outer half of a
  max-side boundary block is excluded. That follows from the continuous-box
  `contains` semantics the ticket's own test pins (`RegionTest.kt:75` asserts
  `contains(10.5, 5, 5)` is false), so it is not a new defect; the alternative
  (block membership) would make `entities` disagree with `contains(entity)`.
- **No new issues in the untouched paths.** `intersects`, `blocksArray`,
  `isChunkFullyContained`, `chunks`/`chunksFull`, `compareToAxis`,
  `closestDistanceTo`, `edges`, `Face`, `Geometry.kt` and `Vector3dMath.kt` are
  unchanged from the round-1 tree I reviewed and remain correct.
- **Prior report — concur.** I concur with the tester's round-2 findings and
  non-findings (the only new item, test vacuity, is outside my scope) and do not
  re-report any of them.
