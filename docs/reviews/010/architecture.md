# Architecture review — 010 (Core region, face, and geometry helpers)

## Round 1
### Verdict
The port lands in the prescribed packages and file layout, `core` stays free of
Rooster/ORM/WorldEdit/Adventure, and the package rename to `dev.rooster.region`
is complete in source. Three items remain: `core`'s published metadata does not
expose `joml` even though JOML types are now part of its public API,
`docs/architecture.md` understates `Vector3d.toLocation`'s signature, and
`AGENTS.md`'s status still calls 010 the next ticket.

### Findings
#### 1. `joml` is `implementation`, but JOML types are now part of `core`'s public API
- Location: `core/build.gradle.kts:20`; `core/src/main/kotlin/dev/rooster/region/Region.kt:54,67-70,287`;
  `core/src/main/kotlin/dev/rooster/region/util/Geometry.kt:9-30`
- Problem: ticket 010 makes `org.joml.Vector3d` part of the published surface —
  `Region.dimensions`, `Region.vector1`/`vector2`, `Region.box`, the
  `compareToAxis(..., customBox: Box?)` parameter, and every `util` extension
  (`toVector3d`, `toLocation`, `value`, `distance`) expose JOML types. But
  `core` applies only the `java` plugin and declares `joml` as `implementation`.
  The generated metadata confirms the consequence: `core/build/publications/maven/module.json`
  lists `org.joml:joml` only under `runtimeElements`; the `apiElements` variant
  has no dependencies, so a Gradle consumer of `rooster-region` does not get
  `joml` on its compile classpath and cannot compile a call to
  `region.dimensions` without adding `joml` itself. The published POM likewise
  marks `joml` `<scope>runtime</scope>`. Ticket 000's architecture review
  recorded this exact seam as one to revisit in 010 once the public API was
  known; it is now known and the seam is live.
- Suggested fix: apply the `java-library` plugin and change
  `implementation("org.joml:joml:1.10.9")` to `api("org.joml:joml:1.10.9")`,
  then update `docs/architecture.md:26`. `verifyCoreDependencies` still passes:
  it compares dependency coordinates, not scope, and the runtime classpath stays
  `joml`-only. If the team prefers to keep `implementation`, record in
  `docs/design.md`/`docs/architecture.md` that consumers must supply `joml`
  themselves (currently masked only because `paper-api` transitively provides
  it), so the boundary is deliberate rather than accidental.

#### 2. `docs/architecture.md` understates `Vector3d.toLocation`'s signature
- Location: `docs/architecture.md:20`
- Problem: the `core` map lists `Vector3d.toLocation(world)`, but the landed
  function and the ticket's own scope say `toLocation(world, yaw, pitch)`
  (`Geometry.kt:11-12`, `docs/tasks/010-core-region.md:31`). The doc is the
  canonical map of the module, so the rotation parameters — which the tests
  exercise (`GeometryTest.kt:23-40`) — should be visible there.
- Suggested fix: write `Vector3d.toLocation(world, yaw, pitch)` in the
  `Geometry.kt` line, in the same commit as the port.

#### 3. `AGENTS.md` "Current status" still points at ticket 010 as next
- Location: `AGENTS.md:87-90`
- Problem: it reads "Next is ticket 010 ... which ports `Region`, `Face` and the
  geometry helpers into `core`", but that port is now implemented in this
  working tree. `AGENTS.md` is invalidated by the same change, and the ticket-000
  architecture review treated this section as a finding for the same reason.
- Suggested fix: update the status paragraph in the commit that lands ticket 010
  (point at the next ticket / mark 010 in review), or record that the
  orchestrator owns the rewrite.

### Non-findings
- **`core` boundary is clean.** `core/build.gradle.kts:18-28` declares only
  Paper API (`compileOnly`), `joml`, `compileOnly(kotlin("stdlib"))` and test
  dependencies — no Rooster, Exposed, WorldEdit or Adventure. WorldEdit/FAWE
  remains confined to `worldedit/build.gradle.kts:20-23`. No main source under
  `core/src/main` references `net.kyori`, `org.jetbrains`, `com.sk89q` or
  `com.fastasyncworldedit`; `compareVectors` is absent, matching
  `docs/design.md:28-30`.
- **Package rename is complete.** Every main and test file declares
  `package dev.rooster.region` or `dev.rooster.region.util`, imports point at
  `dev.rooster.region(.util)`, and no source refers to `dev.rooster.core.region`.
  The one remaining `dev.rooster.core.region.Region` string
  (`ClasspathSmokeTest.kt:11`) is an intentional negative assertion that the
  upstream rooster-core class is absent, not a rename leftover; I do not flag
  it.
- **File layout matches the prescription.** `Region.kt`, `Face.kt`,
  `util/Geometry.kt`, `util/Vector3dMath.kt` are exactly the tree in
  `docs/architecture.md:14-24`; the `util`→`Region` import cycle
  (`Geometry.kt:3,16`) is itself prescribed by that doc (`Box.region(world)`),
  so it is not a deviation.
- **Public API is source-faithful, not over-generalised.** `Region` mirrors the
  ticket's scope list (min/max, sizes, contains/intersects, blocks/entities,
  chunks, enlarge/shrink, corner/edge/face, distance, `compareToAxis`); no
  speculative abstractions or interfaces were introduced.
- **`worldedit` targeting is unchanged and still generic.** The module has no
  `src` yet (ticket 020) and `docs/architecture.md:32-42` still describes the
  `com.sk89q.worldedit` API target, so no WorldEdit-API staleness is introduced
  here.
- **Prior findings — concur.** I concur with tester findings 1-4 (crossing-overlap
  test, tautological `distinct` assertion, missing negative filter case, untested
  `customBox`) and correctness findings 1-3; none are module-boundary or doc
  matters and I do not re-report them. I concur with tester finding 5 on
  `docs/manual-test.md:12`: MT-002's "pure math only" clause is stale given what
  `RegionTest` exercises, and I leave the wording fix with that finding rather
  than re-raising it.

## Round 2
### Verdict
All three round-1 architecture findings are resolved in `f3ce7ac`: `core` now
applies `java-library` and exposes `joml` via `api(...)`, `docs/architecture.md`
records the full `toLocation` signature and the `api(joml)` scope, and
`AGENTS.md` points at ticket 020. One documentation gap remains: the deliberate
public-API rename `closesDistanceTo` → `closestDistanceTo` is not recorded in
any non-review doc, so the ticket's "keep the public shape close" note now reads
as if the surface is unchanged. No module-boundary, package-placement or
extendability issues.

### Findings
#### 1. The deliberate `closesDistanceTo` → `closestDistanceTo` rename is unrecorded
- Location: `core/src/main/kotlin/dev/rooster/region/Region.kt:277`;
  `docs/tasks/010-core-region.md:50-56`; `docs/design.md:13-44`
- Problem: readability round-1 finding 1 asked for the typo rename to be recorded
  as a deliberate deviation from the ticket's "Keep `Region`'s public shape as
  close to the source as possible" note. The rename landed (`Region.kt:277`), but
  no non-review doc records it: the ticket Notes still read as if the public
  shape is unchanged, and `docs/design.md`'s decisions list — which records other
  deliberate deviations such as dropping `compareVectors` — does not mention it.
  A reader of the ticket/design can no longer tell whether the name difference
  from `rooster-core` is intentional. I am not re-raising the naming decision
  itself (readability owns it and correctness concurred); only the missing record.
- Suggested fix: add a Note bullet to `docs/tasks/010-core-region.md` (or a line
  in `docs/design.md`'s decisions) recording the rename and the reason
  (misspelling fixed while the library is unpublished, so no consumer churn).

### Non-findings
- **Round-1 finding 1 resolved.** `core/build.gradle.kts:3` applies
  `java-library` and `:21` declares `api("org.joml:joml:1.10.9")`. The generated
  metadata now lists `org.joml:joml` under both `apiElements` and
  `runtimeElements`, and the POM still declares only `joml`, so consumers get the
  JOML types on their compile classpath while the runtime classpath stays
  `joml`-only.
- **Round-1 finding 2 resolved.** `docs/architecture.md:20` now reads
  `Vector3d.toLocation(world, yaw, pitch)`, matching `Geometry.kt:11-12` and the
  ticket scope (`docs/tasks/010-core-region.md:31`).
- **Round-1 finding 3 resolved.** `AGENTS.md:87-90` now says tickets 000 and 010
  are implemented and in review and points at ticket 020.
- **`core` boundary remains clean.** `core/build.gradle.kts:19-29` declares only
  Paper API (`compileOnly`), `api(joml)`, `compileOnly(kotlin("stdlib"))` and
  test dependencies; no Rooster, Exposed, WorldEdit or Adventure. WorldEdit/FAWE
  stays in `worldedit/build.gradle.kts:20-23`, and no `core/src/main` source
  references `net.kyori`, `org.jetbrains`, `com.sk89q` or `com.fastasyncworldedit`.
- **Package placement and file layout unchanged and correct.** Sources remain
  under `dev.rooster.region` / `dev.rooster.region.util`, matching
  `docs/architecture.md:14-24`; the round-2 edits introduce no
  `dev.rooster.core.region` reference and no new package or module.
- **Round-2 code changes stay inside the boundary.** The
  `expandBorders`/`contractBorders` split, the `enlarge(Int)`/`shrink(Int)`
  overloads, the `intersectingAxis` block-coordinate change, the `entities`
  `contains` filter and the `closestDistanceToAxis` absolute-value fix are all
  internal to `Region.kt`; `api(joml)` already exposes the JOML types those
  members use.
- **`docs/manual-test.md` MT-002 is now accurate** (`:12`), and the `worldedit`
  module's documented target (`docs/architecture.md:32-42`) is unchanged.
- **Prior reports — concur.** I concur with tester round-2 finding 1 (the new
  `entities` exclusion test is vacuous under MockBukkit's max-exclusive query
  box) and its non-findings; it is test quality, outside my scope, so I do not
  re-report it. I concur with correctness round 2 (no new findings; its three
  round-1 findings are resolved and the `closestDistanceTo` rename has no
  correctness consequence). I also concur with readability round 1's other two
  findings; my finding 1 covers only the recording gap its finding 1 called for,
  not the rename itself.
