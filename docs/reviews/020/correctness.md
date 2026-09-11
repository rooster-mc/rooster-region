# Correctness review — 020 (WorldEdit adapter module)

## Round 1
### Verdict
The conversion functions are a faithful port and behave correctly against the
pinned FAWE 2.12.3 API, but `Player.worldEditSelection()` does not actually
satisfy the ticket's "returns `null` (not an exception) when the player has no
selection" criterion: an incomplete selection (or a cleared selection after a
world change) makes `LocalSession.getSelection(world)` throw
`IncompleteRegionException`, because the `selectionWorld == null` guard only
catches the never-touched-session case. One finding.

### Findings
#### 1. `worldEditSelection()` throws `IncompleteRegionException` for an incomplete selection
- Location: `worldedit/src/main/kotlin/dev/rooster/region/worldedit/Adapter.kt:32-39`
  (the `selectionWorld ?: return null` guard at line 37 and the
  `getSelection(selectionWorld)` call at line 38)
- Problem: the guard returns `null` only when
  `LocalSession.getSelectionWorld()` is `null`, i.e. when the selector's
  incomplete region has a null world (a session whose selector was never
  touched). Once the selector has a world, `getSelectionWorld()` returns it even
  when no region is actually defined, and `LocalSession.getSelection(world)`
  calls `selector.getRegion()`, which throws `IncompleteRegionException` when
  either position is unset. Verified in the pinned FAWE-Core 2.12.3:
  - `LocalSession.getSelectionWorld()` is
    `selector.getIncompleteRegion().getWorld()` with no `isDefined()` check.
  - `LocalSession.getSelection(World)` only checks world equality, then calls
    `selector.getRegion()`.
  - `CuboidRegionSelector.getRegion()` throws `IncompleteRegionException` when
    `position1 == null || position2 == null`.
  - `CuboidRegionSelector.setWorld` sets the region's world without clearing it,
    and `clear()` nulls the positions but leaves the world set.
  `IncompleteRegionException` extends `WorldEditException extends RuntimeException`,
  so it propagates out of the adapter unchecked.

  Reproduction A (one-point selection): a player uses `//wand` and left-clicks
  one block. `getRegionSelector(world)` has set the selector's world and
  `selectPrimary` has set `position1`; `position2` is still null. Calling
  `player.worldEditSelection()` finds `selectionWorld != null`, enters
  `getSelection(world)`, and throws `IncompleteRegionException`. Expected per the
  acceptance criterion (and per `docs/architecture.md:48-49`, "returns `null`
  when no selection exists, so callers never catch WorldEdit exceptions
  themselves"): `null`.

  Reproduction B (cleared after a world change): a player with a complete
  selection in world A moves to world B and interacts with the wand (or runs a
  selection command) in B. `getRegionSelector(B)` calls `setWorld(B)` and
  `clear()`, leaving world B set and both positions null. `worldEditSelection()`
  again throws, even though no selection exists in B.

  The source-of-truth bridge has the same code, but the ticket's stated goal is
  to "return `null` instead of throwing when there is no selection" and the
  acceptance criterion makes that the contract, so the verbatim port does not
  meet it. This is a runtime-only path, which is why the current unit tests and
  MT-001 ("repeat with no selection and confirm `null`") do not catch it.
- Suggested fix: gate on a defined selection before converting, e.g.
  ```kotlin
  val selectionWorld = localSession.selectionWorld ?: return null
  if (!localSession.isSelectionDefined(selectionWorld)) return null
  return localSession.getSelection(selectionWorld)
  ```
  `isSelectionDefined(world)` checks the world is non-null, equal to the
  supplied world, and `selector.isDefined()`. Alternatively wrap
  `getSelection` in `try/catch (IncompleteRegionException)` returning `null`.
  Extend MT-001 to set only `//pos1` (or switch worlds) and confirm `null`.

### Non-findings
- **`Region.toWorldEditRegion()` raw corners vs "min/max points" is fine.**
  `Adapter.kt:13-18` passes `edge1`/`edge2`, not `min`/`max`, but
  `CuboidRegion(World, BlockVector3, BlockVector3)` runs `recalculate()` and
  computes `minX/minY/minZ/maxX/maxY/maxZ` with `Math.min`/`Math.max` (verified
  via `javap` on FAWE-Core 2.12.3), so the produced region's
  `getMinimumPoint()`/`getMaximumPoint()` equal the core `Region`'s min/max
  regardless of corner order. Passing `min`/`max` would yield the identical
  region. Only `getPos1()`/`getPos2()` retain the raw order, which is the WE
  API's normal meaning of those accessors.
- **`CuboidRegion(World, pos1, pos2)` exists in the pinned API.** Confirmed by
  `javap` on FAWE-Core 2.12.3; no signature/version mismatch.
- **The WE `Region` interface exposes `getMinimumPoint`/`getMaximumPoint`.** The
  receiver need not be a `CuboidRegion`, so `WERegion.toRegion` compiles and
  works for any region type. Collapsing a non-cuboid region to its bounding box
  is lossy, but converting to the cuboid `core.Region` is inherently lossy and
  `docs/design.md:54` scopes non-cuboid types out.
- **`BlockVector3.toLocation` maps coordinates correctly.** `Adapter.kt:29-30`
  uses `x()`/`y()`/`z()`, which are the accessors present in FAWE-Core 2.12.3;
  `Location(world, x, y, z)` defaults yaw/pitch as expected.
- **`WERegion.toRegion(world|player)` preserves the same-world invariant.** Both
  edges are built from one supplied world, so `core.Region`'s
  `require(edge1.world == edge2.world)` cannot fail.
- **No FAWE-specific imports in main.** `rg` over `worldedit/src/main` shows only
  `com.sk89q.worldedit.*`; the acceptance criterion holds.
- **No version skew between compile and test classpaths.** BOM `1.52` pins
  `FastAsyncWorldEdit-Core`/`-Bukkit` to `2.12.3` for both `compileOnly` and the
  new `testImplementation` (verified from the BOM descriptor), so tests exercise
  the same API version the main code compiles against.
- **`WorldWrapper` does not reintroduce a throw.** `getSelectionWorld()` unwraps
  `WorldWrapper` to its parent while `getSelection` compares the wrapped world,
  but `WorldWrapper.equals(other)` delegates to `parent.equals(other)`, so the
  comparison still succeeds.
- **Tester finding 1 is out of my scope and I have no correctness dissent.** The
  underlying conversion behaviour it probes is functionally correct per the
  min/max normalisation above; the observation that the test cannot distinguish
  raw corners from min/max is a test-quality matter for the tester.

## Round 2
### Verdict
Round-1 finding 1 is correctly fixed: the `isSelectionDefined` gate closes the
`IncompleteRegionException` path before `getSelection` is called, so the
null-on-no-selection contract now holds for the untouched, one-point, and
world-changed cases. No new findings.

### Findings
#### No new findings.

### Non-findings
- **The `isSelectionDefined` gate is sufficient to prevent the throw.**
  `Adapter.kt:37-39` reads `selectionWorld`, returns `null` if it is null, then
  returns `null` unless `localSession.isSelectionDefined(selectionWorld)`, and
  only then calls `getSelection(selectionWorld)`. Verified against the pinned
  FAWE-Core 2.12.3: `isSelectionDefined(World)` returns
  `selector.getIncompleteRegion().getWorld() != null && …equals(world) &&
  selector.isDefined()`, and `CuboidRegionSelector.isDefined()` is exactly
  `position1 != null && position2 != null` — the same condition
  `CuboidRegionSelector.getRegion()` throws on. So whenever the gate passes,
  `getSelection` cannot throw `IncompleteRegionException`. `isSelectionDefined`
  itself only reads the selector and `checkNotNull(world)` (already non-null), so
  it cannot throw on this path.
- **The `WorldWrapper` unwrap is still consistent.** `selectionWorld` comes from
  `getSelectionWorld()`, which returns `WorldWrapper.getParent()` when the
  incomplete region's world is a wrapper. `isSelectionDefined(parent)` then
  compares `wrapper.equals(parent)`, and `WorldWrapper.equals(other)` delegates
  to `parent.equals(other)`, which is true; the same holds for the subsequent
  `getSelection(parent)` world check.
- **A defined selection still round-trips.** With both positions set,
  `isSelectionDefined` is true and `getSelection(selectionWorld)` returns the
  region as before, so the fix does not change the successful path.
- **The new `EllipsoidRegion` case is mathematically correct.** `AdapterTest.kt:56-68`
  builds a radius-2 ellipsoid centred at `(5,5,5)` and asserts the bounding box
  `(3,3,3)-(7,7,7)`. `EllipsoidRegion` exposes only the interface
  `getMinimumPoint`/`getMaximumPoint` (confirmed via `javap`), so the test pins
  the generic conversion, and `getMinimumPoint`/`getMaximumPoint` are
  `center ∓ radius` for these exact values. This is tester-scope; I note only
  that it introduces no correctness issue.
- **The adapter still converts a non-cuboid region to its bounding box.** That
  remains intentionally lossy for a cuboid `core.Region` and within the design's
  cuboid-only scope (`docs/design.md:54`); no change since round 1.
- **MT-001 now names the incomplete-selection cases.** The manual gate's new
  wording matches the behaviour the gate implements, so the acceptance criterion
  is covered by a real-server check rather than left unguarded.
- **No new correctness findings from the tester's round-2 report.** Its findings
  are test-quality and harness-fidelity observations; nothing in them conflicts
  with the adapter's logic, math, or API contract.
