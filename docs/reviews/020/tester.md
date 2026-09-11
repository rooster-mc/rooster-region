# Tester review — 020 (WorldEdit adapter module)

## Round 1
### Verdict
The three tests are lean, correctly scoped to MockBukkit, and green, but the one
test that claims to cover "min and max points" cannot actually observe that
behaviour, because `Region` normalises its own edges; the generic `WERegion`
contract is therefore unguarded. Deferring `Region.toWorldEditRegion()` and
`Player.worldEditSelection()` to MT-001 is justified: I confirmed from the
pinned FAWE-Bukkit 2.12.3 bytecode that `BukkitAdapter.adapt(World)` needs the
plugin-populated `INSTANCE.adapter` and cannot run under MockBukkit.

### Findings
#### 1. The "min and max points" test does not distinguish `minimumPoint`/`maximumPoint` from the raw corners
- Location: `worldedit/src/test/kotlin/dev/rooster/region/worldedit/AdapterTest.kt:40-52`; `worldedit/src/main/kotlin/dev/rooster/region/worldedit/Adapter.kt:20-27`
- Problem: the test builds a reversed `CuboidRegion` and then asserts on the
  converted `Region`'s `minX`/`maxX`. But `Region` derives its own min/max with
  `coerceAtMost`/`coerceAtLeast` (`core/src/main/kotlin/dev/rooster/region/Region.kt:28-33`),
  so an adapter that passed the raw `pos1`/`pos2` corners (or either order) would
  yield exactly the asserted values. The test name claims the adapter uses the
  region's `minimumPoint`/`maximumPoint`, yet no implementation that produces
  both corners can fail it; only `CuboidRegion` is exercised, and its raw-corner
  accessors normalise to the same result. The remaining value is only "both
  corners are read", not "min/max are read".
- Suggested fix: add a case over a non-cuboid `WERegion`, whose only corners are
  the interface `minimumPoint`/`maximumPoint`, e.g.
  `EllipsoidRegion(BlockVector3.at(5, 5, 5), Vector3.at(2.0, 2.0, 2.0))`, and
  assert the converted `Region` is the bounding box `(3,3,3)-(7,7,7)`. I checked
  the pinned FAWE-Core 2.12.3 (the version `bom-newest:1.52` resolves to):
  `EllipsoidRegion(BlockVector3, Vector3)` and its `getMinimumPoint`/
  `getMaximumPoint` touch no world or plugin, so this runs under the existing
  MockBukkit harness. The reversed `CuboidRegion` case can stay, but it is the
  non-cuboid case that pins the generic contract and would reject a regression
  that narrows the receiver to `CuboidRegion`.

### Non-findings
- **`Region.toWorldEditRegion()` is correctly deferred to manual testing, not
  missing.** It calls `BukkitAdapter.adapt(edge1.world)`
  (`Adapter.kt:15`), and in FAWE-Bukkit 2.12.3 that method delegates to
  `getAdapter()` → `INSTANCE.adapter`, which is only populated by a loaded
  WorldEdit/FAWE plugin. It throws under MockBukkit, so no unit test can cover
  it. `MT-001` (`docs/manual-test.md:11`) records the corner check; that is the
  right home for it.
- **`Player.worldEditSelection()` is correctly deferred.** It calls
  `BukkitAdapter.adapt(this)` → `WorldEditPlugin.getInstance().wrapPlayer(...)`
  plus `WorldEdit.getInstance().sessionManager` (`Adapter.kt:33-38`); both need a
  live plugin/session. `MT-001` covers the selection and the no-selection `null`
  path.
- **`verifyWorldEditClasspath` is still meaningful.** It inspects the main
  `compileClasspath`/`runtimeClasspath`, not `testRuntimeClasspath`; Gradle's
  `testImplementation` extends `implementation` but not the reverse, so the new
  test dependency does not leak into the configuration the task asserts. The
  compile-only vs published-runtime seam remains guarded.
- **The added test dependency is scoped and consistent.** `testImplementation`
  of the BOM + `FastAsyncWorldEdit-Core` resolves to FAWE 2.12.3, the same
  provider the `compileOnly` configuration uses, and the tests reference only
  `com.sk89q.worldedit.*` (no `com.fastasyncworldedit.*`), so the acceptance
  criterion on imports is not violated. A source/bytecode scan to enforce that
  criterion would be disproportionate given the adapter's only WorldEdit imports
  are `com.sk89q.worldedit`; the consumer-facing contract is already enforced by
  the runtime-classpath check.
- **No degenerate/zero-volume case is needed.** `CuboidRegion(at(p), at(p))`
  has no distinct branch in the adapter — it forwards two equal points and
  `Region` handles min == max; the coverage would not exercise new code.
- **"Selection world vs supplied world" is untestable and adequately gated.**
  A `WERegion` carrying its own world requires `BukkitAdapter.adapt(World)`, the
  same blocked path; the adapter intentionally uses the supplied world
  (`Adapter.kt:20-27`), and `MT-001`'s conversion check covers the observable
  result on a real server.
- **The player-world test is adequate, if not maximally discriminating.** It
  asserts against `player.world` rather than the test's `world`, which is the
  correct source, and `toRegion(player)` has no other world in scope to confuse
  it with. Using a second world and teleporting the player there would be
  stronger, but is not required.
- **No excessive or tautological tests beyond finding 1.** The three cases cover
  distinct functions (`BlockVector3.toLocation`, `WERegion.toRegion(world)`,
  `WERegion.toRegion(player)`); none asserts on iteration order, private detail,
  or a value the production code cannot vary. The test count is proportionate to
  the 39-line adapter.
- **Harness lifecycle is sound.** `AdapterTest` boots and unmocks MockBukkit in
  `@BeforeEach`/`@AfterEach` (`AdapterTest.kt:18-27`), so cleanup is guaranteed
  on failure. The duplication of `WorldTestSupport` is necessary because that
  class lives in `core`'s test source set, which is not shared with `worldedit`.
- **No earlier report in this round to concur with or dissent from.**

## Round 2
### Verdict
Round-1 finding 1 is resolved: the new `EllipsoidRegion` case genuinely pins the
generic `minimumPoint`/`maximumPoint` contract that the reversed `CuboidRegion`
case could not, and it runs green under the existing harness. The correctness
gate added to `worldEditSelection` is only reachable behind the live plugin, and
`MT-001` now names the incomplete-selection scenarios, so there is no new
automatable test to add. No new test-quality or harness-fidelity findings.

### Findings
#### No new findings.

### Non-findings
- **Round-1 finding 1 resolved.** `world edit region conversion uses the generic
  min and max points` (`AdapterTest.kt:56-68`) converts an `EllipsoidRegion`
  whose only corners are the interface `minimumPoint`/`maximumPoint` and asserts
  the bounding box `(3,3,3)-(7,7,7)`. Because the receiver is the generic
  `WERegion`, a regression that narrowed `toRegion` to `CuboidRegion` (or cast to
  reach `getPos1`/`getPos2`) would now fail to compile or throw, which the
  reversed `CuboidRegion` case alone could not detect. The new test passed in the
  committed run (`worldedit/build/test-results/test/…AdapterTest.xml`, 4 tests,
  0 failures).
- **The old reversed-`CuboidRegion` case is now redundant but harmless.** It
  still documents that a `CuboidRegion` input works and that both corners are
  read; it is three cheap assertions and not worth deleting. It is not
  tautological in the sense of never being able to fail — an adapter that used
  one corner twice would fail it.
- **The `isSelectionDefined` gate is correctly left to the manual gate.** The
  branch (`Adapter.kt:37-38`) sits behind `BukkitAdapter.adapt(player)` and
  `WorldEdit.getInstance().sessionManager`, both of which need a live
  plugin/session, so no unit test can reach it. `MT-001`
  (`docs/manual-test.md:11`) now explicitly covers "no selection" and an
  "incomplete selection (only `//pos1`, or after switching worlds)" returning
  `null`, which is exactly the new branch's observable behaviour.
- **The new imports stay generic.** `EllipsoidRegion` and `Vector3` are
  `com.sk89q.worldedit.*`, so the no-`com.fastasyncworldedit.*`-imports criterion
  still holds.
- **No test is missing or excessive for this round's source change.** The only
  production change since round 1 is the `isSelectionDefined` gate, whose path is
  unautomatable; adding a test around it would require stubbing WorldEdit's
  session manager, which the harness cannot do. The suite remains four tests for
  a 40-line adapter.
- **Harness lifecycle and fidelity unchanged.** `@BeforeEach`/`@AfterEach`
  still guarantee `MockBukkit.unmock()`, and `Region.toWorldEditRegion()` /
  `Player.worldEditSelection()` remain the only paths the MockBukkit harness
  cannot exercise — both already owned by `MT-001`.
- **No new manual-test entry is required.** `MT-001` was extended in the same
  commit as the production change it gates, which is the correct handling; no
  further harness limit surfaced in this round.
- **No round-2 findings from other reviewers to concur with or dissent from
  within tester scope.** The architecture doc/status updates and the correctness
  gate do not raise test-quality work.
