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
