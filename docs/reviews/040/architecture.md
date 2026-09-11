# Architecture review — 040 (BlockPos and Region.blockAt)

## Round 1

### Verdict
The code lands where the ticket and `docs/architecture.md` prescribe: `BlockPos`
is a pure `dev.rooster.region` type with no imports, and `Region.blockAt` reuses
the existing lazy `world` property, so `core`'s boundary and the WorldEdit module
are untouched. The public API is minimal and correctly placed. Three
documentation items are stale: the README never mentions the new public symbols,
`docs/architecture.md`'s `Region.kt` summary omits the new member, and
`AGENTS.md`'s "Current status" still claims 030 was the last ticket.

### Findings

#### 1. README Usage does not document `BlockPos` or `Region.blockAt`
- Location: `README.md:85-156` (Usage), `README.md:10-13` (module table)
- Problem: 040 adds two public symbols — `dev.rooster.region.BlockPos` and
  `Region.blockAt(BlockPos)`. The README is the consumer-facing doc established
  by 030 and enumerates the library's public surface, but neither symbol appears
  anywhere in it (a repo-wide search finds `BlockPos` only in
  `docs/architecture.md:18`, the sources, the tests and the ticket). The ticket's
  stated purpose is "so consumers stop rolling their own"; a consumer reading the
  README will not learn the type or accessor exists.
- Suggested fix: add a short `### Block positions` subsection under Usage with a
  `BlockPos(3, 4, 5)` / `region.blockAt(pos)` example, and mention `BlockPos` in
  the `core` module-table row (`README.md:12`) alongside `Region`/`Face`.

#### 2. `docs/architecture.md`'s `Region.kt` summary omits `blockAt`
- Location: `docs/architecture.md:16-17`
- Problem: the diff correctly added the `BlockPos.kt` line (`:18`) but left the
  `Region.kt` line listing "contains, intersects, enlarge/shrink, chunk math,
  blocks/entities" without the new `blockAt(BlockPos)` member. The module map is
  the canonical surface list for `core`, so it is now one member behind the code.
- Suggested fix: extend the `Region.kt` description with the accessor, e.g.
  append `blockAt(BlockPos)` to the existing list on `:17`.

#### 3. `AGENTS.md` "Current status" is stale after the queue gained 040/050
- Location: `AGENTS.md:85-91`
- Problem: it states tickets 000/010/020/030 are done and "Ticket 030 was the
  last ticket in the MVP queue; there is no next ticket." The branch base commit
  (`cc3b9fd`, "docs(tasks): add 040 blockpos and 050 world-scoped selection") added
  `docs/tasks/040-blockpos-and-blockat.md` and `docs/tasks/050-world-scoped-selection.md`
  (indexed at `docs/tasks/README.md:14-15`), so both claims are now false. This
  section was explicitly brought in line in 030 (`docs/reviews/030/architecture.md:50-53`),
  and `AGENTS.md:82` requires docs to be updated in the commit that invalidates
  them.
- Suggested fix: rewrite the status paragraph to record 000-030 as done and
  040/050 as the open MVP tickets. Because the invalidating change is the queue
  commit rather than 040's own diff, the orchestrator may choose to fix it
  separately; either way the text must be corrected.

### Non-findings
- **`core` boundary stays clean.** `BlockPos.kt` has no imports and references
  only `kotlin.Int` (`core/src/main/kotlin/dev/rooster/region/BlockPos.kt:1-17`),
  and `Region.blockAt` uses the pre-existing Bukkit `World`/`Block` imports
  (`Region.kt:103`). `core/build.gradle.kts` is unchanged, so the module still
  declares only `compileOnly(paper-api)`, `api(joml)`,
  `compileOnly(kotlin("stdlib"))`; `verifyCoreDependencies` and
  `ClasspathSmokeTest` continue to enforce it. I concur with correctness's
  non-finding on purity.
- **`worldedit` is untouched and still targets the generic API.** No file under
  `worldedit/` is in the diff; `Adapter.kt` still imports only
  `com.sk89q.worldedit.*`, and WorldEdit stays `compileOnly`. `blockAt` returns a
  Bukkit `Block` and does not touch the adapter.
- **Placement and public API are as prescribed.** `BlockPos` sits in
  `dev.rooster.region` (ticket `docs/tasks/040-blockpos-and-blockat.md:14` and
  `docs/architecture.md:18`), matching `Region`/`Face` rather than the `util`
  package. The type is a 3-component data class implementing
  `Comparable<BlockPos>` with no builders, factories or extra surface — minimal
  and not over-generalised. `Region.blockAt` is a thin member, not a new seam.
- **`docs/design.md` needs no change.** Its "Coordinates" decision
  (`docs/design.md:33-36`) describes `Region` as a wrapper between two `Location`
  edges; `BlockPos` is an added coordinate type and does not contradict or
  invalidate that, so no decision is stale.
- **The `Seams` claim still holds.** `docs/architecture.md:50` calls `Region` the
  single public spatial type; `BlockPos` is a coordinate, not a region, so the
  statement remains accurate.
- **No manual-test entry is warranted.** `blockAt` is a direct
  `World.getBlockAt(Int, Int, Int)` call that MockBukkit drives faithfully, and no
  new acceptance criterion reaches past the harness. I concur with the tester's
  assessment here.
- **Prior reports — concur, not re-reported.** I concur with tester finding 1
  (the `blockAt addresses the region world` test cannot distinguish worlds) and
  with correctness's verdict of no findings. Neither is a module-boundary or
  documentation matter, so I add nothing to them. I do not re-report the tester's
  finding.

## Round 2

### Verdict
All three round-1 architecture findings are resolved, and the fix commit
(`951206c`) changes documentation only on top of code I already accepted: no
module, package, dependency or public symbol moved, so `core`'s boundary and the
generic WorldEdit adapter remain intact. No new module-boundary, extendability or
documentation-staleness work.

### Findings
No findings within scope.

### Non-findings
- **Round-1 finding 1 resolved.** `README.md:127-142` adds a `### Block positions`
  section documenting `BlockPos` (pure, lexicographic `Comparable`, `sorted()`
  order) and `Region.blockAt` ("in the region's own world"), with a runnable
  `BlockPos(3, 4, 5)` / `region.blockAt(pos)` / `type = Material.STONE` snippet.
  `README.md:12` now lists `BlockPos` in the `core` module-table row. The prose
  matches the implementation (`BlockPos.kt:3-17`, `Region.kt:103`).
- **Round-1 finding 2 resolved.** `docs/architecture.md:16-18` now lists
  `blockAt(BlockPos)` on the `Region.kt` line and keeps the `BlockPos.kt` line;
  the `core` map is back in step with the public surface.
- **Round-1 finding 3 resolved.** `AGENTS.md:87-94` records 000-030 as done and
  names 040 and 050 as the open MVP tickets, matching
  `docs/tasks/README.md:14-15`; the false "no next ticket" claim is gone.
- **Boundary unchanged by the doc fix.** `git show 951206c --name-only` touches
  `AGENTS.md`, `README.md`, `docs/architecture.md`, the two `core` sources and
  their tests — no `build.gradle.kts`, no `worldedit/` file. `BlockPos.kt` still
  has no imports, and `core`'s classpath remains `compileOnly(paper-api)` +
  `api(joml)` + `compileOnly(kotlin("stdlib"))`.
- **Placement and extendability unchanged.** `BlockPos` stays in
  `dev.rooster.region` as a 3-component data class with `Comparable` and no extra
  surface; `Region.blockAt` stays a thin member. The new docs describe exactly
  that API, neither overstating nor inventing an extension point.
- **`docs/design.md` remains accurate.** Its "Coordinates" decision
  (`docs/design.md:33-36`) still describes the `Location`-edge design; the added
  public coordinate type does not contradict a recorded decision, so no edit is
  required (consistent with round 1).
- **Tester round-2 finding 1 — out of my scope, no architecture addition.** The
  missing `ReadmeExamplesTest` counterpart for the new snippet
  (`docs/reviews/040/tester.md:60-78`) is test-coverage drift; the README snippet
  itself is accurate and raises no documentation-staleness or boundary matter, so
  I neither re-report it nor add to it. Correctness concurs with it as well
  (`docs/reviews/040/correctness.md:84-87`).
- **Correctness round 2 — concur.** I concur with its verdict of no findings: the
  production code is unchanged from round 1, and the docs added in `951206c` use
  only shipped API.
