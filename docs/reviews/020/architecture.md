# Architecture review — 020 (WorldEdit adapter module)

## Round 1
### Verdict
The adapter lands in the prescribed package and single-file seam, the module
boundary is intact (`worldedit` depends on `core` via `api`, WorldEdit/FAWE stay
`compileOnly` in main and only enter the test source set), and the main source
imports only `com.sk89q.worldedit.*`. Two documentation items remain:
`AGENTS.md`'s "Current status" is invalidated by 020's implementation, and
`docs/architecture.md`'s `worldedit` dependency paragraph omits the
`compileOnly` Paper/stdlib seam that the `core` paragraph documents.

### Findings
#### 1. `AGENTS.md` "Current status" still describes 020 as the next ticket
- Location: `AGENTS.md:85-90`
- Problem: it reads "Tickets 000 ... and 010 ... are implemented and in review.
  Next is ticket 020 ... the WorldEdit adapter". 020's adapter is now implemented
  in this working tree, and 010 has since been marked `done`
  (`docs/tasks/README.md:11`, commit `9d9ca01`), so the status is stale on both
  counts. The 010 architecture review fixed this same section for the same reason
  (`docs/reviews/010/architecture.md:48-56,132-133`), and `AGENTS.md`'s own
  convention is to update docs in the commit that invalidates them
  (`AGENTS.md:82`).
- Suggested fix: update the status paragraph in the 020 commit (mark 020
  implemented/in review and point at 030), or record that the orchestrator owns
  the rewrite.

#### 2. `docs/architecture.md`'s `worldedit` dependency paragraph omits the Paper/stdlib `compileOnly` seam
- Location: `docs/architecture.md:40-42` vs `worldedit/build.gradle.kts:23-32`
  and `gradle.properties:3`
- Problem: the `core` section (`docs/architecture.md:26-30`) documents that Paper
  API and Kotlin stdlib are `compileOnly` and that
  `kotlin.stdlib.default.dependency=false` keeps stdlib out of the published POM,
  so consumers supply it at runtime. The `worldedit` paragraph lists only
  `project(":core")` and the WorldEdit API, even though the module declares
  `compileOnly(paper-api)` and `compileOnly(kotlin("stdlib"))` and relies on the
  same global property. A Java-only consumer of `rooster-region-worldedit`
  therefore gets core+joml from the POM but not stdlib or Paper, which is not
  visible from the module's canonical description. This is a pre-existing gap,
  not caused by 020, but 020 is where the section gains real code.
- Suggested fix: extend the worldedit dependency sentence to mirror `core`:
  `compileOnly(paper-api)`, `compileOnly(kotlin("stdlib"))`, and the
  stdlib/POM consequence, alongside the WorldEdit API `compileOnly`. One sentence
  is enough; no need to repeat the full rationale.

### Non-findings
- **Module boundary holds.** `worldedit/build.gradle.kts:23` uses
  `api(project(":core"))`; Paper, the FAWE BOM, FAWE-Core and FAWE-Bukkit are
  `compileOnly` (lines 24-32), so the published POM exposes core only.
  `verifyWorldEditClasspath` (`worldedit/build.gradle.kts:51-103`) still asserts
  main compile has WorldEdit and main runtime does not.
- **The added test dependency respects the boundary.** The two new lines
  (`worldedit/build.gradle.kts:38-39`) are `testImplementation` of the BOM and
  FAWE-Core; `testImplementation` does not extend main `runtimeClasspath`, so the
  runtime-classpath assertion is unaffected (concurring with the tester's
  non-finding). Test-scope WorldEdit is required precisely because main's
  WorldEdit is `compileOnly` and absent from the test runtime.
- **Package layout matches design.** Main is `dev.rooster.region.worldedit`
  (`Adapter.kt:1`), matching `docs/design.md:43-45` and
  `docs/architecture.md:35`; the test uses the same package. One `Adapter.kt` is
  exactly the seam `docs/architecture.md:36` prescribes.
- **Public API is minimal and not over-generalised.** The five functions match
  the ticket scope (`docs/tasks/020-worldedit-adapter.md:20-26`) and the
  architecture map. `WERegion.toRegion` accepts the generic interface while
  `Region.toWorldEditRegion` returns the concrete `CuboidRegion`; that asymmetry
  is right for a cuboid-only core (`docs/design.md:54`).
- **Extension-function seam is the intended design.** Public extensions on
  foreign WorldEdit/Bukkit types are the ticket's explicit shape and are
  discoverable by importing `dev.rooster.region.worldedit.*`; no additional
  facade/object is prescribed or needed. I see no missing seam.
- **Generic API target is preserved.** `Adapter.kt:3-6,11` imports only
  `com.sk89q.worldedit.*`; no `com.fastasyncworldedit.*` appears in main. Using
  the FAWE artifacts as the compile provider is the documented choice
  (`docs/design.md:23-27`, `docs/architecture.md:40-42`), not a FAWE API leak.
- **`docs/manual-test.md` MT-001 is the right home for the non-automatable
  paths.** The diff extends it to cover `Region.toWorldEditRegion()`'s corner
  round-trip; `Player.worldEditSelection()` and the `BukkitAdapter.adapt(World)`
  path cannot run under MockBukkit. I concur with correctness finding 1's request
  to add the incomplete-selection case to MT-001; that is the same finding and I
  do not re-report it.
- **`docs/tasks/README.md` is not stale.** 020 still reads `todo`, which is
  correct until the ticket is marked done (`docs/workflow.md:51`); it is
  orchestrator-owned and I do not edit it.
- **Ticket frontmatter `status: todo`** (`docs/tasks/020-worldedit-adapter.md:3`)
  is likewise orchestrator-owned in-flight queue state, not doc staleness.
- **`docs/design.md` needs no change.** Its WorldEdit decisions
  (optional/separate, generic API, `compileOnly`, FAWE as provider) still match
  the code.
- **Prior reports — concur.** I concur with tester finding 1 (the "min and max
  points" test cannot distinguish raw corners from min/max) and with correctness
  finding 1 (`IncompleteRegionException` on an incomplete selection). Neither is
  a module-boundary or doc matter, so I do not re-report them. `architecture.md:48-49`
  states the null contract that correctness finding 1 shows the code does not yet
  fully meet; the fix belongs with that finding, and no separate doc edit is
  needed once it lands.

## Round 2
### Verdict
Both round-1 architecture findings are resolved: `AGENTS.md`'s status now matches
the tree (000/010 done, 020 implemented and in review, 030 next) and
`docs/architecture.md`'s `worldedit` paragraph now documents the
`api(project(":core"))`, `compileOnly(paper-api)`, WorldEdit API `compileOnly`
via FAWE, and `compileOnly(kotlin("stdlib"))`/POM seam. The round-1 code and test
changes stay within the module boundary and packages, and I found no new
module/package or documentation issues.

### Findings
#### No new findings.

### Non-findings
- **Round-1 finding 1 resolved.** `AGENTS.md:87-92` now reads "Tickets 000 ...
  and 010 ... are done", "020 ... is implemented and in review", and points at
  030 — accurate against `docs/tasks/README.md:10-13` and the working tree.
- **Round-1 finding 2 resolved.** `docs/architecture.md:40-45` now lists
  `api(project(":core"))`, `compileOnly(paper-api)`, the WorldEdit API
  `compileOnly` via FAWE, and `compileOnly(kotlin("stdlib"))`, plus the
  `kotlin.stdlib.default.dependency=false` POM consequence — matching
  `worldedit/build.gradle.kts:23-32` and `gradle.properties:3`, and mirroring the
  core paragraph (`docs/architecture.md:26-30`).
- **Boundary unchanged by the round-1 fixes.** `Adapter.kt` still imports only
  `com.sk89q.worldedit.*` (lines 3-6, 11); the `isSelectionDefined` gate (line 38)
  is an internal control-flow change with no new dependency. The `worldedit`
  module still publishes core only, with Paper/FAWE/stdlib `compileOnly`.
- **Package and seam unchanged.** Main and test remain in
  `dev.rooster.region.worldedit`, and `Adapter.kt` remains the single prescribed
  seam (`docs/architecture.md:35-37`).
- **The new test stays inside the generic API.** `AdapterTest.kt:4-6` imports
  `com.sk89q.worldedit.math.Vector3` and `...regions.EllipsoidRegion`, both
  `com.sk89q.worldedit.*`; no `com.fastasyncworldedit.*`.
- **Docs remain in step after the correctness fix.** `docs/architecture.md:51-52`'s
  "returns `null` when no selection exists" now matches `Adapter.kt:37-39`, and
  `docs/manual-test.md:11` names the incomplete-selection case, matching the gate.
- **`docs/tasks/README.md:12` and `docs/tasks/020-worldedit-adapter.md:3` still
  read `todo`** — correct in-flight queue state owned by the orchestrator until
  the ticket is marked done (`docs/workflow.md:51`), not doc staleness.
- **Prior round-2 reports — concur.** I concur with the tester's and
  correctness's round-2 verdicts (their round-1 findings resolved, no new
  findings); nothing in them raises a module-boundary or documentation matter.
