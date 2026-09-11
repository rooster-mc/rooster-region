# Correctness review — 030 (README, publishing, and consumption)

## Round 1
### Verdict
The README examples match the real API names, packages, signatures and
defaults, the install/composite/publishing instructions agree with the installed
POMs and the build wiring, and the new snippet test asserts the documented
behaviour. I found no logic, math, API or integration defect; one claim is
imprecise but not false, and the `AGENTS.md`/index status split follows the
established in-flight convention.

### Findings
#### No new findings.

### Non-findings
- **README `Region` example matches the code.** `README.md:98-120` uses
  `Region(edge1, edge2)`, `min`/`max`, `sizeX/Y/Z`, `volume`, `contains`,
  `intersects`, `enlarge(Int, Face)`/`shrink(Int, Axis)`/`enlarge(Int)` — all
  present in `Region.kt:18-49,73-101,189-205` and `Face.kt:5-14`. The numeric
  comments are correct for the given edges (0..15 ⇒ size 16, volume 4096;
  `enlarge(2, EAST)` ⇒ maxX 17; `shrink(1, Axis.Y)` ⇒ minY 65/maxY 78;
  `enlarge(1)` ⇒ (-1,63,-1)..(16,80,16)); `ReadmeExamplesTest.kt:20-54` asserts
  exactly these. `enlarge(1)` resolves to the non-vararg overload as the
  existing `RegionTest.kt:207,215` already exercise, so no overload ambiguity.
- **README geometry example matches the code.** `README.md:140-149` uses
  `Location.toVector3d()`, `Vector3d.toLocation(world, yaw, pitch)`,
  `typealias Box = Pair<Vector3d, Vector3d>`, `Box.region(world)`,
  `Location.value`/`Vector3d.value` and the infix `Vector3d.distance`, all
  defined in `Geometry.kt:9-29` and `Vector3dMath.kt:5-6`. The documented
  component-wise difference (5,5,5) − (2,1,4) = (3,4,1) is what the code
  returns, and `ReadmeExamplesTest.kt:56-83` checks it.
- **README WorldEdit example matches the adapter and the 020 contract.**
  `README.md:160-178` uses `Player.worldEditSelection()?.toRegion(player.world)`
  and `Region.toWorldEditRegion()`, which are `Adapter.kt:13-27,32-40`. The
  documented `null` cases (no selection, one position, cleared after a world
  change) are exactly what the `selectionWorld ?: return null` +
  `isSelectionDefined` gate now yields (verified in
  `docs/reviews/020/correctness.md:105-146`), and `?.toRegion(player.world)`
  resolves to the `World` overload unambiguously (`Adapter.kt:20-27`).
- **`mavenLocal` instructions are correct.** `README.md:25-53` publishes with
  `just publish` (`justfile:18-20` = `./gradlew publishToMavenLocal`) and
  declares `dev.rooster.region:rooster-region:1.0-SNAPSHOT` /
  `...-worldedit`. The installed POMs at
  `~/.m2/repository/dev/rooster/region/...` carry those coordinates and version,
  and `build.gradle.kts:6-9` sets `group`/`version` accordingly.
- **The "brings core and joml transitively" claim holds.** The installed
  `rooster-region-worldedit` POM declares `dev.rooster.region:rooster-region`
  (`<scope>compile</scope>`), and the `rooster-region` POM declares
  `org.joml:joml:1.10.9`; both Gradle `.module` files carry the same edges
  (`runtimeElements` → core → joml), so Gradle and Maven consumers both get
  `joml` transitively.
- **The composite-build mapping is correct.** `README.md:62-83` maps
  `dev.rooster.region:rooster-region` → `project(":core")` and
  `...-worldedit` → `project(":worldedit")`. The project names are `core` and
  `worldedit` (`settings.gradle.kts:7`) while the published artifact ids are
  `rooster-region`/`rooster-region-worldedit` (`core/build.gradle.kts:8-12`,
  `worldedit/build.gradle.kts:10-14`), so automatic substitution would not match
  and the explicit `dependencySubstitution` is both needed and correctly
  targeted.
- **`verifyPublishedArtifacts` correctly asserts the installed POMs.**
  `build.gradle.kts:11-53` depends on both `publishToMavenLocal` tasks, resolves
  `dev/rooster/region/<artifactId>/1.0-SNAPSHOT/<artifactId>-1.0-SNAPSHOT.pom`,
  and the regex captures `groupId:artifactId` from the `<dependency>` blocks.
  The installed POMs are exactly `[org.joml:joml]` and
  `[dev.rooster.region:rooster-region]`, so both `check`s pass. The README
  description of the task (`README.md:188-196`) matches this.
- **The `check` claim is accurate.** `core/build.gradle.kts:96-98` and
  `worldedit/build.gradle.kts:122-124` each wire their verification task into
  `check`; an aggregate `./gradlew check` therefore runs both
  `verifyCoreDependencies` and `verifyWorldEditClasspath`, as `README.md:196-199`
  states. `verifyCoreDependencies` additionally checks the generated core POM
  and runtime/compile classpaths, and `verifyWorldEditClasspath` now also checks
  the generated worldedit POM (`worldedit/build.gradle.kts:62-74`).
- **POM contents match the ticket's acceptance criterion.** The installed
  `rooster-region` POM (`.../rooster-region-1.0-SNAPSHOT.pom`) lists only
  `org.joml:joml`, and the installed `rooster-region-worldedit` POM lists only
  `dev.rooster.region:rooster-region`; the generated
  `build/publications/maven/pom-default.xml` files are identical, so the
  classpath-only Paper/stdlib/WorldEdit seams hold.
- **`AGENTS.md` status is accurate for the tree.** `AGENTS.md:84-89` now says
  000/010/020 are done and 030 is implemented and in review; the untracked
  `README.md` and `ReadmeExamplesTest.kt` plus the modified build files are that
  implementation, and 030 is the last ticket (`docs/tasks/README.md:10-13`).
  `docs/tasks/README.md:13` and the ticket frontmatter still read `todo`, but
  that is the orchestrator-owned in-flight queue state accepted in the 020 round
  (`docs/reviews/020/architecture.md:81-85`), not a contradiction.
- **`MT-003` is an acceptable home for the non-automatable consumer check.**
  The snippet test compiles the core examples against the source API, and
  `docs/manual-test.md:13` records the `mavenLocal`/composite consumer build and
  the real-server `worldEditSelection()` check, satisfying the ticket's
  "snippet test or a documented manual step" criterion.
- **"any WorldEdit region type works" is imprecise but not false.**
  `README.md:176-178` first says the conversion goes "through its min/max
  points", which is exactly `Adapter.kt:20-21`; a non-cuboid selection is
  therefore reduced to its axis-aligned bounding box, and
  `Region.toWorldEditRegion()` only ever returns a `CuboidRegion`
  (`docs/design.md:54` scopes non-cuboid types out). The sentence is true for
  the conversion direction it describes; a reader is not told the shape is
  preserved, so I did not treat it as a false claim.
- **The ticket's "no Adventure" scope item is a pre-existing nuance, not a 030
  regression.** Paper API does put `net.kyori.adventure-*` on `core`'s compile
  classpath transitively, but `core/build.gradle.kts:19-29` declares no direct
  Adventure dependency and the README (`README.md:3-6,17-21`) never claims one;
  `verifyCoreDependencies` checks the direct framework/ORM/WorldEdit leaks it
  names. Nothing in the 030 change makes a false statement here.

## Round 2
### Verdict
Both round-1 readability fixes are behaviour-preserving and correct: the README
now binds the `enlarge`/`shrink` results exactly as `ReadmeExamplesTest.kt` does
with the same numeric comments, and the `localMavenRepo` rename/reconstruction
resolves the same installed-POM paths with the same assertions. No new
correctness issues.

### Findings
#### No new findings.

### Non-findings
- **The README binding is API- and numerically correct.** `README.md:118-120`
  now binds `val east = region.enlarge(2, Face.EAST)`,
  `val y = region.shrink(1, Axis.Y)` and `val all = region.enlarge(1)`. The
  overloads resolve as before (`Face`/`Axis` varargs for `east`/`y`, the
  non-vararg `enlarge(Int)` for `all`; `Region.kt:189-205`), and the comments
  match what the test asserts: `east.maxX == 17` (`ReadmeExamplesTest.kt:39-41`),
  `y.minY == 65`/`y.maxY == 78` (`:43-45`), and all six edges for `all`
  (`:47-53`). The snippet now mirrors the test's bound form, so it no longer
  reads as mutation and agrees with the "returns a new `Region` / never mutate"
  prose at `README.md:123-125`.
- **The `localMavenRepo` rename preserves path resolution.** `build.gradle.kts:18-20`
  builds `File(System.getProperty("user.home"), ".m2/repository")` when
  `maven.repo.local` is unset and `file(it)` when it is; both branches are
  `File`, and `installedPom` (`:35-38`) calls `resolve(...)` on it, yielding the
  same `dev/rooster/region/<artifactId>/1.0-SNAPSHOT/<artifactId>-1.0-SNAPSHOT.pom`
  path as round 1. The `publishToMavenLocal` dependencies (`:15`) and both
  `check` assertions (`:40-49`) are unchanged, and `File` resolves via Gradle's
  Kotlin DSL default imports.
- **No new integration surface.** The commit changes only the README snippet and
  a local variable name; the task graph, `verifyCoreDependencies`,
  `verifyWorldEditClasspath`, the installed POMs and the composite-build
  instructions are untouched. The working tree is clean and the round-1
  non-findings (POM contents, composite mapping, `mavenLocal` transitivity,
  `AGENTS.md`/`MT-003` state) still hold.
- **Prior reports — concur.** I concur with `docs/reviews/030/readability.md`'s
  two round-1 findings (bare transform statements; `mavenLocal` naming) and note
  both are fixed as described above, and with
  `docs/reviews/030/architecture.md`'s no-findings verdict. Neither change
  alters a claim I verified in round 1.
