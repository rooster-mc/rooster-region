# Tester review — 040 (BlockPos and Region.blockAt)

## Round 1

### Verdict
The suite covers the ticket's two behaviours at the right layers: `BlockPos`
ordering is unit tested without booting MockBukkit, and `Region.blockAt` is
tested through the Bukkit world. One acceptance criterion — that the accessor
resolves the *region's* world — is not actually exercised by the test that
claims to cover it.

### Findings

#### 1. `blockAt addresses the region world` cannot distinguish worlds
- Location: `core/src/test/kotlin/dev/rooster/region/RegionTest.kt:311-317`
- Problem: `WorldTestSupport` creates exactly one world, and the region is
  built from `location(...)` using that same `world`. So
  `region.blockAt(...).type = Material.STONE` followed by
  `world.getBlockAt(3, 4, 5)` would still pass if `blockAt` resolved the block
  from any world, e.g. `Bukkit.getWorlds().first()` or a captured default. The
  ticket's acceptance criterion is specifically "in the region's world"
  (`docs/tasks/040-blockpos-and-blockat.md:22-23`), and that scoping is the
  property ticket 050 builds on, so this is the one failure mode the test is
  named for and does not catch.
- Suggested fix: create a second world (as the different-worlds test already
  does at `RegionTest.kt:37` with `MockBukkit.getMock()!!.createWorld(...)`),
  build the region in that second world, and assert both that
  `region.blockAt(pos).world === otherWorld` and that a type set through
  `region.blockAt` is visible via `otherWorld.getBlockAt(...)` (and not via the
  default `world`).

### Non-findings
- `BlockPosTest` is a plain unit test (no `WorldTestSupport`), which is correct:
  the class is pure and needs no MockBukkit. Good layer choice and cheap to run.
- The five `BlockPosTest` cases are not excessive: each pins a distinct ordering
  rule (x, x/y tie, x/y/z tie, equality, full `sorted()` order), and the
  `sorted()` case is the only one that exercises the comparator through the
  standard library rather than the `<` operator. No stiffness or over-mocking;
  all assertions are on public behaviour, so they survive internal rewrites.
- `blockAt returns the block at the given position` (`RegionTest.kt:299-308`)
  checks coordinates and `block.world`, which is sufficient for the coordinate
  mapping; the world-scoping gap is finding 1 only.
- No manual-test entry is needed for this ticket. `blockAt` is a thin
  `world.getBlockAt(Int, Int, Int)` call that MockBukkit drives faithfully, and
  no acceptance criterion here reaches past the harness. The existing MT-002
  covers real-chunk block behaviour generally.
- No prior findings to concur with or dissent from (round 1).

## Round 2

### Verdict
The round 1 world-scoping gap is fixed correctly: the test now builds the region
in a second MockBukkit world and proves both that the returned block belongs to
it and that writes land there rather than in the default world. The suite is in
good shape; the only remaining gap is that the new README usage example is not
covered by the repo's README-example test harness.

### Findings

#### 1. New README `Block positions` example is not mirrored in `ReadmeExamplesTest`
- Location: `README.md:127-142`, `core/src/test/kotlin/dev/rooster/region/ReadmeExamplesTest.kt`
- Problem: `ReadmeExamplesTest` exists to keep the runnable README usage
  examples compiling and behaving as documented — it covers the `Region`
  example (`ReadmeExamplesTest.kt:20-54`) and the geometry example
  (`:56-83`). The commit added a third runnable example, the `Block positions`
  snippet, and no counterpart test. The snippet's claims (`pos.x`, the
  `region.blockAt(pos)` block, the `type = Material.STONE` write) are exercised
  by `RegionTest`, so this is drift risk rather than an untested behaviour, but
  it is the one README example that MockBukkit can run and the suite does not
  run. The WorldEdit example is the precedent for excluding an example, and that
  exclusion is explicit in `docs/manual-test.md` (MT-001); this one has no such
  reason.
- Suggested fix: add a `block positions example compiles and behaves as
  documented` test to `ReadmeExamplesTest` that constructs a region, asserts
  `BlockPos(3, 4, 5).x`, that `region.blockAt(pos)` reports `(3, 4, 5)` in the
  region's world, and that assigning `Material.STONE` is observable through
  `world.getBlockAt(...)`. Add the `org.bukkit.Material` import the snippet
  implies.

### Non-findings
- Round 1 finding 1 is resolved. `RegionTest.kt:311-327` now creates
  `WorldCreator("other")`, builds the region in it, asserts
  `assertEquals(otherWorld, block.world)`, writes through `block`, and checks
  both that `otherWorld.getBlockAt(3, 4, 5)` sees `STONE` and that the default
  `world` does not. That catches the hardcoded/global-world regression the
  earlier test could not.
- The `assertNotEquals(Material.STONE, world.getBlockAt(3, 4, 5).type)` guard is
  sound, not brittle: `WorldTestSupport` boots a fresh `MockBukkit.mock()` per
  test, so the default world's `(3, 4, 5)` is always the untouched default.
- `BlockPosTest` is unchanged from round 1 and remains appropriately scoped and
  non-excessive; no new tests there are needed.
- No new test-environment fidelity limits. `Region.blockAt` is fully exercised
  under MockBukkit, and creating a second world is supported (already done at
  `RegionTest.kt:37`), so no new `docs/manual-test.md` entry is warranted.
