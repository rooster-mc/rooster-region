# Tester review — 010 (Core region, face, and geometry helpers)

## Round 1
### Verdict
The suite covers the ticket's acceptance-criteria list well and the MockBukkit
harness is used consistently, but two assertions do not verify what their names
claim and two public-API branches (partial `intersects`, `compareToAxis`
`customBox`, filtered `entities`) have no coverage. The manual-test entry for
010 also misstates how much the harness actually exercises.

### Findings
#### 1. `intersects` never tests partial overlap where neither region contains a corner of the other
- Location: `core/src/test/kotlin/dev/rooster/region/RegionTest.kt:113-125`
- Problem: the ported `intersects` was changed from the source's
  `contains(edge1) || contains(edge2)` to an AABB overlap test
  (`core/src/main/kotlin/dev/rooster/region/Region.kt:94-100`). The existing
  cases (one-sided overlap, nested, disjoint) do not distinguish the two:
  a crossing pair like `A = (0,0,0)-(10,10,10)` and
  `B = (-5,5,5)-(5,15,5)` overlaps under the new code but was reported as
  disjoint by the source. No test pins which semantics the port is meant to
  have, so a regression to the source algorithm (or the reverse) would not be
  caught.
- Suggested fix: add the crossing pair and assert `region.intersects(other)` and
  `other.intersects(region)` are both true, so the intended semantics are
  documented and locked.

#### 2. The `entities` "without duplicates" assertion is tautological
- Location: `core/src/test/kotlin/dev/rooster/region/RegionTest.kt:321`
- Problem: `Region.entities` already ends in `.flatten().distinct()`
  (`core/src/main/kotlin/dev/rooster/region/Region.kt:136-137`), so
  `region.entities.size == region.entities.distinct().size` can never fail. The
  test name claims duplicate-freedom coverage it does not provide, and the line
  adds no signal. (The `count { it == zombie } == 1` assertion on line 320 is
  the meaningful one.)
- Suggested fix: delete line 321, or replace it with an assertion that can
  actually observe de-duplication — e.g. spawn an entity within range of several
  of the per-block `getNearbyEntities` queries and assert it appears exactly once
  in `region.entities`.

#### 3. Filtered `entities(vararg types)` has no negative-case coverage
- Location: `core/src/main/kotlin/dev/rooster/region/Region.kt:125-137`
- Problem: `players` exercises the positive filter path (`types` contains the
  entity type) and the no-arg `entities` exercises `types.isEmpty()`, but the
  branch where a non-empty `types` set rejects an entity is never taken. A
  regression that ignored the filter would leave every test green.
- Suggested fix: in the existing entities test, spawn a Zombie and assert
  `region.entities(EntityType.CREEPER)` is empty while
  `region.entities(EntityType.ZOMBIE)` contains it.

#### 4. `compareToAxis`'s `customBox` parameter is untested
- Location: `core/src/main/kotlin/dev/rooster/region/Region.kt:287-299`
- Problem: `customBox: Box? = null` is public API in the ticket scope
  (`compareToAxis`), and no test passes a box that differs from the region's own
  `box`. The `currentBox ?: this.box` branch is therefore unexercised.
- Suggested fix: add one case that passes a custom `Box` (e.g. shifted relative
  to the region) and assert the classification differs from the default-box
  result.

#### 5. Manual-test entry MT-002 understates what MockBukkit already exercises
- Location: `docs/manual-test.md:12`
- Problem: the entry says "MockBukkit covers the pure math only", but
  `RegionTest` drives `blocks`, `blocksArray`, `contains`, `entities`, `players`,
  `chunks`, and `chunksFull` through MockBukkit as well. Those are the
  fidelity-sensitive paths (chunk indexing, nearby-entity queries, live block
  reads), and the live-world gate should name them; as written a reader may
  assume they are already verified on a real server.
- Suggested fix: broaden MT-002 to name the MockBukkit-exercised Bukkit members
  (`blocks`/`blocksArray`/`entities`/`players`/`chunks`/`chunksFull`) so the
  live-world check covers them, and drop the "pure math only" clause.

### Non-findings
- Acceptance-criteria coverage is otherwise complete: min/max normalisation
  (`RegionTest.kt:18-30`), inclusive sizes/volume (`:41-63`), `contains`
  boundary ±0.5 on all six sides (`:65-78`), `allowEdges` edge-vs-corner
  (`:80-100`), `enlarge`/`shrink` per face, per axis, and empty-face (`:127-226`),
  `isCorner`/`isEdge`/`isFace` and `intersectingAxis` (`:228-241`), negative
  chunk indices via `floorDiv` (`:254-267`), and `Vector3d.distance`
  (`Vector3dMathTest.kt`).
- The chunk tests are not brittle: `chunks`/`chunksFull` are compared as
  `(x, z)` sets, and `blocksArray` asserts the public `[x][y][z]` indexing
  contract rather than iteration order.
- `WorldTestSupport` guarantees `MockBukkit.unmock()` via `@AfterEach`, so the
  cleanup leak flagged against ticket 000 does not apply here.
- `GeometryTest` covers all three axes for `value`, both `toLocation` rotation
  paths, and X/Z bounds for `Box.region`; the coverage is proportionate, not
  excessive.
- No excessive or no-value tests beyond the single tautological assertion in
  finding 2; `MockBukkitHarnessTest`/`ClasspathSmokeTest` are pre-existing
  ticket-000 files and were not re-assessed here.
- No harness path in ticket 010's scope is unreachable: every Bukkit member in
  scope is constructed under MockBukkit, so no new manual-test entry is required
  beyond correcting MT-002.

## Round 2
### Verdict
All five round-1 findings are resolved and the tests added for the correctness
fixes are effective, with one exception: the new `entities excludes entities
outside the region` test is vacuous under the pinned MockBukkit, because the
outside entity sits exactly on the query box's max face, which MockBukkit
excludes before the production filter ever runs. That test needs one change; no
other new test-quality issues found.

### Findings
#### 1. `entities excludes entities outside the region` cannot observe the filter it targets
- Location: `core/src/test/kotlin/dev/rooster/region/RegionTest.kt:340-349`;
  `core/src/main/kotlin/dev/rooster/region/Region.kt:126-138`
- Problem: the region is the single block `(0,0,0)-(0,0,0)`, so `iterateRegion`
  issues exactly one query, `getNearbyEntities(Location(0,0,0), 1.0, 1.0, 1.0)`.
  In the pinned MockBukkit 4.45.0, `WorldMock.getNearbyEntities` keeps entities
  whose location is `BoundingBox.contains(...)`; `BoundingBox.of(location, 1,1,1)`
  builds `[loc-1, loc+1)` per axis and `contains` is `v >= min && v < max` (max
  exclusive — verified in the bundled `WorldMock.java` and the paper-api
  `BoundingBox` bytecode). The outside Zombie at exactly `(1.0, 0.0, 0.0)` lies
  on the box's max X face, so it is never returned and the new
  `contains(entity.location)` filter (`Region.kt:135`) is never asked to reject
  anything. `region.entities` is `{inside}` whether or not the filter exists, so
  the test would stay green if the filter were deleted and does not lock
  correctness round-1 finding 3.
- Suggested fix: place the outside entity strictly inside a query box but outside
  the region. With the current single-block region, spawn it at
  `(0.5, 0.0, 0.0)` (query box `[-1,1)`, `contains` false because `maxX = 0`);
  or use a multi-block region such as `(0,0,0)-(10,10,10)` and spawn at
  `(10.5, 5.0, 5.0)`. Assert both `region.entities` excludes it and
  `region.contains(outside)` is false, so only the filter can make the test pass.

### Non-findings
- **Round-1 finding 1 resolved.** The crossing region `(-5,5,5)-(5,15,5)` is
  added and asserted in both directions (`RegionTest.kt:118,124-125`), pinning
  the AABB semantics against the source algorithm.
- **Round-1 finding 2 resolved.** The tautological `distinct()` assertion is gone
  and the test now asserts only `count { it == zombie } == 1`
  (`RegionTest.kt:322-328`); the zombie at `(5,5,5)` is returned by several of the
  1331 per-block queries, so de-duplication is genuinely exercised.
- **Round-1 finding 3 resolved.** `entities filters by entity type`
  (`RegionTest.kt:330-338`) covers the positive (`ZOMBIE` present) and negative
  (`CREEPER` empty) filter directions.
- **Round-1 finding 4 resolved.** `compareToAxis honours a custom box`
  (`RegionTest.kt:369-376`) exercises the `currentBox ?: this.box` branch and
  shows the custom box changes the classification.
- **Round-1 finding 5 resolved.** MT-002 now names the fidelity-sensitive Bukkit
  members and drops the "pure math only" clause (`docs/manual-test.md:12`).
- **Tests added for the correctness fixes are effective.** `isFace(location(-0.5,
  5.0, 5.0))` plus `intersectingAxis(...) == 0` (`RegionTest.kt:242,247`)
  distinguish the `blockX`/floor behaviour from the old truncation, and the
  interior `closestDistanceToAxis(Axis.X, 3.0) == 3.0` case (`:384`)
  distinguishes the `absoluteValue` fix from the old signed `min`.
- **No excessive or redundant tests.** The suite is 30 tests for a 310-line
  class; `blocks`/`blocksArray` assertions remain adequate and not brittle, and
  no test asserts on iteration order or other refactor-sensitive detail.
- **No new harness limit beyond the corrected MT-002.** The `getNearbyEntities`
  max-exclusive behaviour in finding 1 is a harness quirk, not a reason for a
  further manual-test entry: MT-002 already sends `entities` to a live server.
- **Concur with the other round-1 reports.** Correctness findings 1-3 are fixed
  and now have tests (except the exclusion test in finding 1 above); architecture
  finding 1 (`api(joml)`) and readability findings 1-3 are implemented and do not
  raise test-quality work. I do not re-report any of them.
