# Correctness review — 040 (BlockPos and Region.blockAt)

## Round 1

### Verdict
The implementation matches the ticket exactly: `BlockPos` is pure, orders by
`x` then `y` then `z` with overflow-safe `Int.compareTo`, and
`Region.blockAt(BlockPos)` resolves through the region's own `world` to
`World.getBlockAt(Int, Int, Int)`. No correctness, math, API or integration
findings.

### Findings
No findings within scope.

### Non-findings
- `BlockPos.compareTo` (`core/src/main/kotlin/dev/rooster/region/BlockPos.kt:8-16`)
  implements exactly the specified lexicographic order and uses
  `Int.compareTo` rather than subtraction, so it is safe at `Int.MIN_VALUE`/
  `Int.MAX_VALUE` and is consistent with the data-class `equals`/`hashCode`
  (returns 0 iff all three components are equal). `Comparable` is declared on
  the class as the ticket requires (`docs/tasks/040-blockpos-and-blockat.md:14-15`).
- `BlockPos` has no imports and references only `kotlin.Int`, so the purity
  criterion (`docs/tasks/040-blockpos-and-blockat.md:21`) holds and `core`'s
  classpath is unchanged (only Paper API + joml); `ClasspathSmokeTest` still
  covers the forbidden types.
- `Region.blockAt` (`core/src/main/kotlin/dev/rooster/region/Region.kt:103`)
  uses the lazy `world` property (`Region.kt:26`, derived from `edge1.world`),
  which is the region's world, and passes the coordinates through unchanged, so
  negative and out-of-region coordinates behave as Bukkit's `getBlockAt` does —
  which is what the ticket pins ("returning
  `world.getBlockAt(position.x, position.y, position.z)`",
  `docs/tasks/040-blockpos-and-blockat.md:16-17`). No containment guard is
  expected.
- The extraction is faithful: the source class
  (`/home/cyp/repos/mc-ui-designer/src/main/kotlin/dev/cypdashuhn/uidesigner/model/BlockPos.kt`)
  and the source extension
  (`.../capture/RegionExt.kt:7-8`) are byte-for-byte equivalent in behaviour to
  the two new definitions, so consumers adopting this library lose nothing.
- `blockAt` does not clash with the existing `contains(Location)`,
  `contains(Region, Boolean)` and `contains(Entity)` overloads, and adding a
  member to `Region` does not affect the WorldEdit adapter
  (`worldedit/.../Adapter.kt` only imports `Region`).
- Concur with tester finding 1: the `blockAt addresses the region world` test
  (`core/src/test/kotlin/dev/rooster/region/RegionTest.kt:310-317`) cannot
  distinguish worlds because `WorldTestSupport` creates exactly one world
  (`WorldTestSupport.kt:17`). That is a test-coverage gap, not an implementation
  bug; the accessor itself is correctly world-scoped. No correctness finding to
  add there.
- No prior correctness findings to concur with or dissent from (round 1).

## Round 2

### Verdict
The round 1 world-scoping gap is closed without changing the production code,
and the added docs match the shipped API. The implementation still satisfies
every acceptance criterion; no new correctness, math, API or integration
findings.

### Findings
No findings within scope.

### Non-findings
- `Region.blockAt` and `BlockPos` are byte-identical to the round 1 versions
  (`core/src/main/kotlin/dev/rooster/region/Region.kt:103`,
  `core/src/main/kotlin/dev/rooster/region/BlockPos.kt`), so all round 1
  correctness non-findings still hold.
- The reworked `blockAt addresses the region world`
  (`core/src/test/kotlin/dev/rooster/region/RegionTest.kt:311-327`) now builds a
  second `WorldCreator("other")` world, constructs the region from
  `Location(otherWorld, ...)` edges, asserts `block.world == otherWorld`, writes
  `Material.STONE` through the returned `Block`, and checks the write is visible
  via `otherWorld.getBlockAt(3, 4, 5)` but not via the default `world`. This
  catches the wrong-world failure mode and is a sound correctness fix; the
  `assertNotEquals` guard is safe because `WorldTestSupport` boots a fresh
  `MockBukkit.mock()` per test and the default world's `(3, 4, 5)` is untouched
  `AIR`.
- The README `Block positions` snippet (`README.md:134-142`) uses only the
  shipped API — `BlockPos(3, 4, 5)`, `pos.x`, `region.blockAt(pos)` and the
  settable `Block.type` — and its prose claims (lexicographic order, resolution
  in the region's own world) match the implementation. No API/contract mismatch.
- `docs/architecture.md` now lists `blockAt(BlockPos)` under `Region.kt` and
  `BlockPos.kt` under `dev.rooster.region`, which is accurate; the `core`
  classpath is unchanged and still pure for `BlockPos`.
- Concur with tester round 2 finding 1 (the new README example has no
  counterpart in `ReadmeExamplesTest`). That is a test-coverage/drift concern
  within the tester's scope; the example itself is correct, so there is no
  correctness finding to add.
- No prior correctness findings to concur with or dissent from (round 2).
