# Correctness review — 000 (Gradle setup for core and worldedit modules)

## Round 1
### Verdict
All four acceptance criteria hold against the actual build outputs and the
artifacts already in `~/.m2/repository/dev/rooster/region/`: both modules build,
test, format and publish, the `rooster-region` POM is `joml`-only, and core's
runtime variant contains nothing else. I found no correctness, API or
integration defect in the build setup, and I concur with the tester's three
test-quality findings.

### Findings
None. The build files match the ticket's scope, and the generated POM/module
metadata verify the classpath and publishing criteria directly rather than by
proxy.

### Non-findings
- **AC1 — `just build` / `just test` / `just format` succeed.** The justfile
  maps to real tasks (`build` → `./gradlew build`, `test` → `./gradlew test`,
  `format` → `./gradlew ktlintFormat`). Evidence of a green run:
  `core/build/libs/rooster-region-1.0-SNAPSHOT.jar` and
  `worldedit/build/libs/rooster-region-worldedit-1.0-SNAPSHOT.jar` exist;
  `core/build/test-results/test/TEST-dev.rooster.region.ClasspathSmokeTest.xml`
  shows `tests="1" failures="0" errors="0"`; every
  `core/build/reports/ktlint/**` and `worldedit/build/reports/ktlint/**` report
  is empty (the `ktlintKotlinScriptCheck`, `ktlintKotlinScriptFormat` and
  `ktlintTestSourceSet*` tasks all ran), so `ktlintCheck` inside `build` and
  `ktlintFormat` inside `format` both pass.
- **AC2 — `just publish` publishes both artifacts.** Both
  `rooster-region-1.0-SNAPSHOT.{pom,jar,module}` and
  `rooster-region-worldedit-1.0-SNAPSHOT.{pom,jar,module}` plus both
  `maven-metadata-local.xml` are present under
  `~/.m2/repository/dev/rooster/region/`, with the expected group/artifactId.
- **AC3 — `rooster-region` POM declares only `joml`.** The published POM
  (`rooster-region-1.0-SNAPSHOT.pom:12-19`) has exactly one dependency,
  `org.joml:joml:1.10.9` at `runtime` scope. No paper-api, Adventure, Rooster,
  Exposed or WorldEdit entry. `kotlin.stdlib.default.dependency=false`
  (`gradle.properties:1`) plus `compileOnly(kotlin("stdlib"))`
  (`core/build.gradle.kts:19`) keeps stdlib out of both the POM and the module
  metadata, as the ticket notes intend.
- **AC4 — `:core:dependencies --configuration runtimeClasspath` is clean.** The
  published `rooster-region-1.0-SNAPSHOT.module` `runtimeElements` variant lists
  only `org.joml:joml`; `apiElements` lists nothing. Since `kotlin("jvm")`
  applies the `java` plugin, the `dependencies` task exists on `:core`, so the
  criterion's exact command works.
- **`worldedit` project-dependency coordinates are correct.** The generated
  `rooster-region-worldedit-1.0-SNAPSHOT.pom:12-18` and the module metadata's
  `apiElements`/`runtimeElements` both reference
  `dev.rooster.region:rooster-region:1.0-SNAPSHOT` (compile scope), i.e. Gradle
  resolved the target publication's `artifactId`, not the project name `core`.
  The `worldedit` POM contains nothing else — FAWE and Paper are `compileOnly`,
  so no hard runtime dependency leaks.
- **FAWE/BOM wiring is sound.** `bom-newest:1.52` pins FAWE `2.12.3` (verified
  from the Maven Central POM, matching the ticket notes).
  `compileOnly(platform(...))` contributes its constraints to `compileClasspath`
  because `compileClasspath` extends `compileOnly`, so the unversioned
  `FastAsyncWorldEdit-Core`/`-Bukkit` declarations resolve. Inspecting the FAWE
  artifacts confirms `FastAsyncWorldEdit-Core` ships `com.sk89q.worldedit.*`
  (`WorldEdit`, `BlockVector3`, `CuboidRegion`, `Region`) and
  `FastAsyncWorldEdit-Bukkit` ships `com.sk89q.worldedit.bukkit.BukkitAdapter`,
  so 020's imports will resolve; `isTransitive = false` on the Bukkit artifact
  still keeps the artifact itself on the classpath.
- **joml scope — noted, not a defect.** `implementation(joml)`
  (`core/build.gradle.kts:18`) puts joml at `runtime` scope in the POM and keeps
  it off the `apiElements` variant, while core's future public API exposes
  `org.joml.Vector3d` (`Region.dimensions`/`vector1`/`vector2`/`box` and the
  `dev.rooster.region.util` helpers). Strictly, a public-API type should be an
  `api` dependency, which would need `java-library` on `core`. In practice it is
  masked today: `paper-api:1.21.4-R0.1-SNAPSHOT` declares `org.joml:joml:1.10.8`
  at compile scope, and every consumer that can use core's Bukkit-typed API —
  including the `worldedit` module via its own `compileOnly(paper-api)` — has
  paper-api on its compile classpath. The ticket scope explicitly mandates
  `implementation(joml)`, so this is the specified modelling, not a violation.
- **Wrapper / tooling.** `gradlew` is executable; `gradle-wrapper.properties`
  points at `gradle-9.7.0-bin.zip`, which is present locally; the published
  module metadata records `createdBy.gradle.version = 9.7.0`, so the wrapper
  drives the intended distribution.
- **No `plugin.yml` in `worldedit`** (ticket note), and no resources at all, as
  expected for a library.
- **Concur with the tester's three findings** (smoke test does not exercise
  `:worldedit` assembly; the POM/Adventure criterion is unverified by the test;
  MockBukkit is declared but never booted). They are test-quality matters, so I
  add no duplicate correctness finding. One related nuance: because `worldedit`
  has no main source, `:worldedit:compileKotlin` is `NO-SOURCE` and its
  `compileClasspath` (the FAWE/BOM resolution) is never exercised by
  `just build`/`just test`; the dependency graph is only proven by the reasoning
  above, not by the green tree.

## Round 2
### Verdict
Commit `b232485` adds verification without changing the published contract: the
new Gradle tasks assert the correct seams, the `artifactName` hoist keeps both
artifactIds and archive names consistent, and the published POM/module metadata
is still `joml`-only (core) and core-only (worldedit). I found no new
correctness, API or integration defect and I concur with the tester's three
Round 2 findings, all of which are test-quality.

### Findings
None.

### Non-findings
- **`verifyCoreDependencies` asserts the right things**
  (`core/build.gradle.kts:38-92`). It depends on
  `generatePomFileForMavenPublication`, so the POM it reads is freshly generated
  rather than stale; the regex
  (`<dependency>\s*<groupId>…</groupId>\s*<artifactId>…</artifactId>`) matches the
  actual `pom-default.xml` and yields exactly `["org.joml:joml"]`; the
  `runtimeClasspath == [joml]` assertion covers the runtime/POM seam; and the
  `compileClasspath` scan covers `compileOnly` leaks that the POM/runtime checks
  cannot see. An empty or unparseable POM makes the list mismatch and fails, so
  there is no silent pass. The task is `check`-bound
  (`core/build.gradle.kts:90-92`) and evidence shows it ran: the POM mtime moved
  to 23:38 alongside the `check` tasks.
- **`verifyWorldEditClasspath` asserts the compile-only contract**
  (`worldedit/build.gradle.kts:49-97`). It resolves the real (non-test)
  `compileClasspath`, confirms `dev.rooster.region:core` and
  `com.fastasyncworldedit:FastAsyncWorldEdit-Core` are present, opens the
  resolved FAWE-Core jar and confirms `com/sk89q/worldedit/regions/CuboidRegion.class`
  exists, then asserts the non-test `runtimeClasspath` has no FAWE/WE modules.
  Because the green tree means this task passed, it also proves the
  `compileOnly(platform("…bom-newest:1.52"))` constraint propagates to
  `compileClasspath` and resolves the unversioned FAWE declarations — closing the
  one thing Round 1 could only reason about. It is `check`-bound
  (`worldedit/build.gradle.kts:95-97`).
- **`artifactName` hoist is correct** (`core/build.gradle.kts:7,10,98`;
  `worldedit/build.gradle.kts:10,13,103`). The same value feeds `base.archivesName`
  and the publication `artifactId`, so the jar name and Maven coordinate stay in
  sync; the published POMs confirm `rooster-region` and
  `rooster-region-worldedit`, and the worldedit POM still references
  `dev.rooster.region:rooster-region:1.0-SNAPSHOT`.
- **Published contract unchanged.** The re-published
  `rooster-region-1.0-SNAPSHOT.pom` still lists only `org.joml:joml:1.10.9`
  (runtime), and `rooster-region-worldedit-1.0-SNAPSHOT.pom` still lists only the
  core project (compile); both `module.json` variants are likewise clean. The new
  tasks do not alter the publications.
- **New tests run and pass.** `TEST-dev.rooster.region.MockBukkitHarnessTest.xml`
  and `TEST-dev.rooster.region.worldedit.WorldEditCompileClasspathTest.xml` both
  report `failures="0" errors="0"`, and the former's 0.451s runtime plus SLF4J
  output confirms the pinned `mockbukkit-v1.21:4.45.0` actually boots.
- **Doc/Notes edits match the build.** `docs/design.md` and
  `docs/architecture.md` now state that Kotlin stdlib is `compileOnly` and the
  published POM is `joml`-only (consumers supply stdlib at runtime) — accurate
  against `gradle.properties:3` and the build files. The ticket Notes accurately
  describe the BOM-as-`compileOnly`, `isTransitive = false`, the two `check`-bound
  tasks, and the deliberate deferral of symbol-level `api(project(":core"))`
  exercise to 020.
- **Concur with the tester's Round 2 findings 1–3.** Finding 1
  (`WorldEditCompileClasspathTest` asserts test-runtime absence, not the published
  contract, and will obstruct 020 once WE is added as `testImplementation`),
  finding 2 (`MockBukkitHarnessTest` unmocks only on the success path), and
  finding 3 (the `dev.rooster.core` prefix is narrower than the Rooster-family
  intent) are valid and within the tester's scope; I add no duplicate correctness
  finding. On finding 3 I agree `dev.rooster` is the safe broadening because the
  helper already drops the project's own `dev.rooster.region:core` component.
- **Round 1 non-findings still hold.** No Round 1 correctness finding was
  outstanding, and the joml-scope note is unchanged by this commit.
- **Minor coverage nuance (not a defect):** `verifyWorldEditClasspath` checks
  `CuboidRegion` in the FAWE-Core jar but not `BukkitAdapter` in the FAWE-Bukkit
  jar; since 020's adapter imports `com.sk89q.worldedit.bukkit.BukkitAdapter`, a
  missing/renamed Bukkit-jar class would still surface only at 020 compile time.
  The artifact is declared and the reference build uses it, so this is a coverage
  gap, not a wrong assertion.
