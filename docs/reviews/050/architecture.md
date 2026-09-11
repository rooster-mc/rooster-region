# Architecture review — 050 (World-scoped worldEditSelection)

## Round 1

### Verdict
The code change stays entirely inside the `worldedit` module and adds no
dependency: `Adapter.kt:38` only calls the already-imported generic
`com.sk89q.worldedit.bukkit.BukkitAdapter`, so the `core` boundary and the
generic-API rule hold. `AGENTS.md`'s "Current status" and
`docs/architecture.md`'s `worldEditSelection()` seam now describe the world
scoping correctly. One consumer-facing document is stale: the README's contract
sentence still enumerates only the old `null` cases and never states the new
world-mismatch behaviour this ticket exists to provide.

### Findings

#### 1. README's `worldEditSelection()` contract omits the world-mismatch case
- Location: `README.md:190-192` (against `worldedit/src/main/kotlin/dev/rooster/region/worldedit/Adapter.kt:38`)
- Problem: the README says the function returns `null` "when there is no
  selection, when only one position is set, or when a stale selection was
  cleared after a world change." The third clause describes only the case where
  WorldEdit itself cleared the selection; it does not describe the new,
  deliberate behaviour where a still-defined selection made in world A is
  rejected because the player is now in world B. That mismatch case is the whole
  point of ticket 050, and the README is the consumer-facing contract that 030
  established, so a consumer reading it cannot learn that cross-world selections
  are now filtered. This is the staleness `correctness` flagged and explicitly
  handed to `architecture` (`docs/reviews/050/correctness.md:63-68`), so it is a
  first architecture finding rather than a re-report.
- Suggested fix: rewrite the clause to name the world scope, e.g. "returns
  `null` when there is no selection, when only one position is set, or when the
  selection belongs to a world other than the player's current one (for example
  a stale selection made before a world change)." Keep the existing
  `?.toRegion(player.world)` example (`README.md:184-185`); with the guard in
  place that argument is now always the selection's world.

### Non-findings
- **`AGENTS.md` "Current status" is correct and well-worded — concur.**
  `AGENTS.md:87-94` now lists 000, 010, 020, 030, 040 and 050 as done and ends
  "No MVP tickets remain." 040 is in the done list (matching the merged 040 work
  at `55e30cf`/`9907185`), the 050 entry names both the path
  (`docs/tasks/050-world-scoped-selection.md`) and the behaviour (world-scoped
  `worldEditSelection`), and the old false "open MVP tickets are 040 and 050"
  text is gone. No further edit needed.
- **`docs/architecture.md` seam matches the implementation — verified.**
  `docs/architecture.md:53-55` now reads "returns `null` when no selection exists
  or when the selection belongs to a world other than the player's current one",
  which matches the guard at `Adapter.kt:37-38`. The `Adapter.kt` summary
  (`docs/architecture.md:38-39`) still lists `Player.worldEditSelection()` and
  remains accurate.
- **Module boundary intact.** `git show 207d012 --name-only` touches only
  `docs/architecture.md` and `worldedit/.../Adapter.kt`; `ec0445e` touches only
  `AGENTS.md`. No file under `core/`, no `build.gradle.kts`, and no
  `gradle.properties` changed, so `core` keeps its
  `compileOnly(paper-api)`/`api(joml)`/`compileOnly(kotlin("stdlib"))` classpath
  and the `verifyCoreDependencies`/`verifyWorldEditClasspath` guards are
  untouched.
- **The adapter still targets the generic API.** `Adapter.kt:4,38` uses
  `com.sk89q.worldedit.bukkit.BukkitAdapter`, an already-present
  `com.sk89q.worldedit.*` import; no `com.fastasyncworldedit.*` class is
  introduced, matching `docs/design.md:23-27` and
  `docs/tasks/020-worldedit-adapter.md:36`.
- **Package and placement unchanged.** No new type or package; `Adapter.kt`
  stays the single seam in `dev.rooster.region.worldedit` prescribed by
  `docs/architecture.md:36-40`, so the public surface is not widened.
- **`docs/design.md` needs no change.** It records the module split and the
  generic-API decision but never states a `worldEditSelection()` contract, so
  the new behaviour invalidates nothing there.
- **`docs/tasks/README.md:15` and the 050 frontmatter still read `todo` — not
  doc staleness.** That is the in-flight queue state owned by the orchestrator
  until the ticket is marked done (`docs/workflow.md:51`), consistent with the
  020 review precedent (`docs/reviews/020/architecture.md:133-135`). The
  `AGENTS.md` done-list is the ticket-orchestrator's record for this branch; I
  do not treat the index as stale mid-review.
- **Tester finding 1 (missing `docs/manual-test.md` entry naming 050) — out of
  my scope, concur, not re-reported.** The manual gate is owned by `tester`; it
  asserts no module-boundary or architecture-doc defect, so I add nothing to it.
- **Correctness verdict — concur.** Its no-findings conclusion is about logic
  and API fit and raises no module/package or documentation matter beyond the
  README item I report above.

## Round 2

### Verdict
My Round 1 README finding is resolved and `AGENTS.md`'s "Current status" is
still correct. The round-2 commits are documentation-only; the branch as a whole
changes exactly one production line inside `worldedit`, so `core`'s boundary,
the package layout, and the generic-API rule all remain intact. No new
module-boundary or documentation-staleness work.

### Findings
No new findings within scope.

### Non-findings
- **Round 1 finding 1 resolved.** `README.md:190-194` now reads "returns `null`
  when there is no selection, when only one position is set, or when the
  selection belongs to a world other than the player's current one (for example a
  stale selection made before a world change)", which matches the guard at
  `Adapter.kt:38`. The runnable example (`README.md:184-185`) is unchanged and
  its `?.` still absorbs the new `null`, so the documented contract and the
  sample agree with the code.
- **`AGENTS.md` "Current status" is still correct.** `AGENTS.md:87-94` lists 000,
  010, 020, 030, 040 and 050 as done, keeps 040 in the done list (path
  `docs/tasks/040-blockpos-and-blockat.md`, `BlockPos`/`Region.blockAt`) and 050
  with its path (`docs/tasks/050-world-scoped-selection.md`, world-scoped
  `worldEditSelection`), and ends "No MVP tickets remain." No edit needed.
- **Module boundaries intact across the branch.** `git diff main...HEAD
  --name-only` is `AGENTS.md`, `README.md`, `docs/architecture.md`,
  `docs/manual-test.md`, the three `docs/reviews/050/*` files and
  `worldedit/src/main/kotlin/dev/rooster/region/worldedit/Adapter.kt` — no file
  under `core/`, no `build.gradle.kts`, no `gradle.properties`. `core` keeps its
  `compileOnly(paper-api)`/`api(joml)`/`compileOnly(kotlin("stdlib"))` classpath
  and the `verifyCoreDependencies`/`verifyWorldEditClasspath` guards are
  untouched.
- **The one source line stays generic and inside `worldedit`.** The branch's only
  code delta is `Adapter.kt:38` from `207d012`; the round-2 commits
  (`ec0445e..HEAD`) touch only README, manual-test and review files. `Adapter.kt`
  still imports only `com.sk89q.worldedit.*` and introduces no
  `com.fastasyncworldedit.*` class, matching `docs/design.md:23-27`.
- **`docs/manual-test.md` addition is consistent with the docs I own.** `MT-004`
  (`docs/manual-test.md:14`) names ticket 050 and both world-scoping acceptance
  criteria, and `docs/architecture.md:53-55` continues to describe the same
  contract; no architecture doc contradicts it.
- **`docs/tasks/README.md:15` and the 050 frontmatter still read `todo` — not
  doc staleness.** That remains the orchestrator-owned in-flight queue state
  until step 8 marks the ticket done (`docs/workflow.md:51`), as in Round 1.
- **Tester Round 2 — out of my scope, concur, not re-reported.** Its resolution
  of the manual-test finding asserts no module/package or architecture-doc
  defect, so I add nothing to it.
- **Correctness Round 2 — concur.** Its no-findings verdict rests on the
  implementation being unchanged since Round 1 and the README now matching the
  code; neither raises a new boundary or documentation matter.
