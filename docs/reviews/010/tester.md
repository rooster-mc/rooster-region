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
