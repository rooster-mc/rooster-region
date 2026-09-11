# Tester review — 000 (Gradle setup for core and worldedit modules)

## Round 1
### Verdict
The single smoke test is a reasonable minimal classpath guard, but it only covers
half of what the ticket claims for it: it never touches the `worldedit` module,
it checks class availability rather than the published POM, and it cannot see
Adventure (which `paper-api` drags onto the test classpath) or `compileOnly`
leaks. MockBukkit is declared but never exercised, so the pinned test harness is
unverified.

### Findings
#### 1. The "both modules assemble" half of the smoke test is not tested
- Location: `core/src/test/kotlin/dev/rooster/region/ClasspathSmokeTest.kt:1-23`; `worldedit/` has no `src/test`
- Problem: ticket scope line 27 asks for "One smoke test that both modules
  assemble and the core module's classpath has no Rooster/Exposed/WorldEdit
  dependency". The only test asserts core classpath purity; nothing in the
  JUnit suite references `:worldedit` at all. `worldedit` has no test source
  set, so `:worldedit:test` is a no-op. Module assembly is only implied by
  `./gradlew test` compiling `:worldedit`; the smoke test itself does not
  assert it, and the `api(project(":core"))` wiring is never exercised.
- Suggested fix: either add a minimal `worldedit` test that references a `core`
  symbol to exercise the `api` wiring (deferred to 010/020 once a core symbol
  exists, named as such), or explicitly scope the smoke test to classpath purity
  and record that module assembly is covered by `just build`/`just test`.

#### 2. Classpath-purity criterion is only partially covered; the published POM and Adventure are unverified
- Location: `core/src/test/kotlin/dev/rooster/region/ClasspathSmokeTest.kt:9-21`; `core/build.gradle.kts:16-26`; `docs/manual-test.md`
- Problem: the test proves absence of four classes on the test runtime
  classpath. That is a proxy, not the acceptance criterion:
  - Acceptance criterion 3 ("the published `rooster-region` POM declares only
    `joml`") is never checked by any test.
  - `compileOnly` leaks are invisible: a `compileOnly` WorldEdit/Exposed entry
    in `core` is absent from `testRuntimeClasspath`, so `Class.forName` still
    throws and the test still passes.
  - Adventure is not covered and cannot be covered this way: `paper-api` is
    `testImplementation` (`core/build.gradle.kts:23`) and its POM pulls
    `adventure-api` at compile scope, so `net.kyori.adventure.text.Component`
    is on core's test classpath even though core's published runtime classpath
    is clean. Adding it to `forbidden` would make the test fail spuriously.
  - No `docs/manual-test.md` entry names ticket 000 for the publish/POM
    criterion (the table only has 010/020 entries).
- Suggested fix: add a Gradle-level assertion bound to `check` (or a small
  TestKit/`publishToMavenLocal` verification) that reads the generated POM and
  `configurations.runtimeClasspath` and asserts only `joml`; or, if the
  one-smoke-test scope is kept, add a 000 manual-test entry for the POM/publish
  criterion and note the `compileOnly`/Adventure blind spots.

#### 3. MockBukkit is declared but never exercised, so the pinned harness is unverified
- Location: `core/build.gradle.kts:24`, `worldedit/build.gradle.kts:29`; `ClasspathSmokeTest.kt`
- Problem: no `.kt` file references MockBukkit (`rg MockBukkit` over `*.kt`
  returns nothing). The build pins `mockbukkit-v1.21:4.45.0` against Paper
  `1.21.4-R0.1-SNAPSHOT` and Kotlin `2.4.20`, with the version choice explained
  in the ticket notes. Because nothing boots MockBukkit, an incompatibility
  (class/method mismatch, snapshot drift) leaves `just test` green and only
  surfaces in ticket 010/020. This is the exact test-environment fidelity the
  ticket setup is supposed to establish.
- Suggested fix: add a minimal `MockBukkit.mock()`/`unmock()` round-trip test
  (one `@Test`) to prove the pinned harness boots, or explicitly defer harness
  verification to 010/020 with the reason recorded in the ticket.

### Non-findings
- The four FQCNs are real, not typo-vacuous: `dev.rooster.core.region.Region`
  exists in `rooster-core`, `com.fastasyncworldedit.core.Fawe` exists in the
  FAWE core artifact, and `com.sk89q.worldedit.WorldEdit` /
  `org.jetbrains.exposed.sql.Table` are canonical.
- `testRuntimeClasspath` is a superset of `runtimeClasspath`, so absence there
  does imply absence on the main runtime classpath — sound for the
  no-Rooster/Exposed/WorldEdit intent of acceptance criterion 4.
- No excessive or no-value tests; the single negative test is appropriately
  minimal and is not brittle to legitimate refactors (it asserts on stable FQCN
  strings only).
- Test wiring is correct: JUnit BOM, `kotlin("test")`, and
  `junit-platform-launcher` are declared and `useJUnitPlatform()` is set
  (`core/build.gradle.kts:21-34`).

## Round 2
### Verdict
T1–T3 are substantively resolved: the new `verifyCoreDependencies` and
`verifyWorldEditClasspath` tasks assert the actual POM/runtime/compile seams, and
the MockBukkit harness now boots. Two new test-quality issues remain — the
`worldedit` JUnit test asserts the wrong seam and will obstruct ticket 020, and
the MockBukkit test does not guarantee cleanup — plus a narrow-prefix gap in the
core compile-leak check.

### Findings
#### 1. `worldedit` JUnit test asserts test-runtime absence, not the published compile-only contract, and will break ticket 020
- Location: `worldedit/src/test/kotlin/dev/rooster/region/worldedit/WorldEditCompileClasspathTest.kt:8-12`; `worldedit/build.gradle.kts:84-91`
- Problem: the test asserts `com.sk89q.worldedit.regions.CuboidRegion` is absent
  from the *test runtime* classpath. The contract the ticket cares about is that
  WorldEdit is `compileOnly` in the *published* module, which
  `verifyWorldEditClasspath` already asserts against the non-test
  `runtimeClasspath` (`worldedit/build.gradle.kts:84-91`). So the JUnit test is
  redundant for the criterion, and it actively conflicts with the next step:
  ticket 020 needs the adapter under test and will legitimately add WorldEdit as
  `testImplementation`, at which point this test fails even though the published
  artifact is still correct. The test also cannot distinguish "compile-only by
  design" from "the dependency failed to resolve at runtime".
- Suggested fix: delete `WorldEditCompileClasspathTest.kt` and rely on the
  `verifyWorldEditClasspath` runtime-classpath assertion (which is the correct
  seam); if a JUnit-level guard is still wanted, retarget it at what JUnit can
  actually observe without blocking 020 (e.g. a positive assertion that core is
  on the test classpath) rather than WE absence.

#### 2. `MockBukkitHarnessTest` can leak the global mock server when it fails
- Location: `core/src/test/kotlin/dev/rooster/region/MockBukkitHarnessTest.kt:9-13`
- Problem: `MockBukkit.unmock()` runs only on the straight-line success path. If
  `assertNotNull(server)` (or anything added to this test in 010/020) throws,
  the static MockBukkit server is left installed, and any later test that calls
  `MockBukkit.mock()` fails with an "already mocked" error. The cleanup is not
  guaranteed by the test's structure, only by the current assertion being
  unfailable.
- Suggested fix: wrap the body in `try { ... } finally { MockBukkit.unmock() }`,
  or move the unmock into an `@AfterEach` so cleanup runs on every outcome.

#### 3. Core compile-leak scan only forbids `dev.rooster.core`, not the Rooster family
- Location: `core/build.gradle.kts:74-86` (specifically `:79`)
- Problem: the `forbidden` list uses the prefix `"dev.rooster.core"`. A
  `compileOnly("dev.rooster.sql:…")` or any other non-`core` Rooster module
  (with `isTransitive = false`, or one that does not drag Exposed) would be a
  Rooster dependency on core's compile classpath yet pass this scan. The task's
  stated intent is to reject Rooster/Exposed/WorldEdit leaks; the runtime and
  POM checks do not see `compileOnly` entries, so this prefix is the only guard
  for that case and it is narrower than the criterion.
- Suggested fix: match the whole family (`"dev.rooster"`), which is safe here
  because the project's own component is dropped by `mapNotNull { it.moduleVersion }`,
  or enumerate the known Rooster groups (`dev.rooster.core`, `dev.rooster.sql`,
  `dev.rooster.commands`, …).

### Non-findings
- **Concur: T1 resolved.** `verifyWorldEditClasspath`
  (`worldedit/build.gradle.kts:49-93`) resolves the compile classpath, asserts
  `dev.rooster.region:core` and `com.fastasyncworldedit:FastAsyncWorldEdit-Core`
  are present, opens the resolved FAWE-Core jar and confirms
  `com/sk89q/worldedit/regions/CuboidRegion.class` exists, then asserts the
  runtime classpath has no FAWE/WE modules. That is a real compile-only check,
  not a proxy. Symbol-level `api(project(":core"))` exercise is correctly
  deferred to 020 and recorded in the ticket Notes.
- **Concur: T2 resolved.** `verifyCoreDependencies`
  (`core/build.gradle.kts:38-88`) depends on
  `generatePomFileForMavenPublication`, parses the generated `pom-default.xml`
  and asserts the dependency list is exactly `org.joml:joml`; it also asserts
  `runtimeClasspath` resolves to exactly `joml` and scans `compileClasspath`
  (which includes `compileOnly`) for forbidden modules. Adventure absence is now
  covered by the POM/runtime assertions; paper-api's transitive Adventure is
  correctly not flagged because it is expected on the compile classpath only.
- **Concur: T3 resolved.** `MockBukkitHarnessTest` boots the pinned
  `mockbukkit-v1.21:4.45.0` server, so the version pin is exercised; finding 2
  is only about cleanup on the failure path.
- **Both verification tasks are correctly bound to `check`**
  (`core/build.gradle.kts:90-92`, `worldedit/build.gradle.kts:95-97`), so
  `just build` runs them; `just test` alone does not, which is acceptable since
  the ticket's AC1 runs all three commands and the assertions are build-level.
- **No new 000 manual-test entry is needed.** The POM/publish criterion,
  runtime/compile purity, and MockBukkit boot are now automated by the two
  `check`-bound tasks plus the harness test; the remaining WorldEdit integration
  is already owned by 020's `MT-001` in `docs/manual-test.md:11`.
- **No excessive tests beyond finding 1.** The ticket-required
  `ClasspathSmokeTest` stays as the single smoke test; the added Gradle tasks
  assert distinct seams (POM, runtime, compile) rather than duplicating it.
- **Gradle task robustness checked.** The POM regex cannot silently pass on an
  unparseable POM (an empty match makes `dependencies != listOf("org.joml:joml")`
  and fails), and the tasks declare no outputs, so they re-run each build rather
  than going stale.
