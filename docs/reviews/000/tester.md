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
