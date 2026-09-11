---
name: Gradle setup for core and worldedit modules
status: done
parent: MVP
depends-on: []
reviewers: [tester, correctness, architecture, readability]
---

## Goal
A two-module Gradle build (`core`, `worldedit`) that builds, tests, formats and
publishes, so the region code can be ported into it.

## Scope
- Kotlin Kotlin DSL multi-project: `include(":core", ":worldedit")`.
- `core`: group `dev.rooster.region`, archives name `rooster-region`, package
  `dev.rooster.region`. Dependencies: `compileOnly(paper-api)`,
  `implementation(joml)`.
- `worldedit`: group `dev.rooster.region`, archives name
  `rooster-region-worldedit`, package `dev.rooster.region.worldedit`.
  Dependencies: `api(project(":core"))` plus the WorldEdit API (`compileOnly`,
  via the FAWE artifacts / IntellectualSites BOM).
- Kotlin `2.2.0`, Java `21` toolchain, Paper API `1.21.4` (`compileOnly`),
  `joml`, JUnit 5 + MockBukkit (`mockbukkit-v1.21`).
- ktlint via the Gradle plugin, configured by `.editorconfig`.
- `maven-publish` publishing both modules to `mavenLocal`.
- Gradle wrapper, `justfile` (`build`, `test`, `format`, `publish`).
- One smoke test that both modules assemble and the core module's classpath has
  no Rooster/Exposed/WorldEdit dependency.

## Acceptance criteria
- `just build`, `just test`, `just format` succeed.
- `just publish` publishes `rooster-region` and `rooster-region-worldedit` to
  `mavenLocal`.
- The published `rooster-region` POM declares only `joml` (plus the
  `compileOnly` Paper API that must not leak as a runtime dependency) — no
  Rooster, Exposed, WorldEdit, or Adventure.
- `./gradlew :core:dependencies --configuration runtimeClasspath` shows no
  framework/ORM/WorldEdit entries.

## Out of scope
- Any region code (tickets 010/020).

## Notes
- Reference structures: `rooster-core/build.gradle.kts` (single module),
  `rooster-ui/settings.gradle.kts` (composite), `rooster-monolith/worldedit`
  (WorldEdit deps + BOM).
- Keep the `worldedit` module free of a `plugin.yml`; it is a library.
- Version deviations (resolved against the installed Gradle `9.7.0`):
  - Kotlin `2.4.20` instead of `2.2.0`: Kotlin 2.2.0 predates Gradle 9 and
    cannot configure a Gradle 9.7 build.
  - ktlint Gradle plugin `14.2.0` (the ticket did not pin a version).
  - MockBukkit `4.45.0`: the last `mockbukkit-v1.21` release built against
    Paper `1.21.4-R0.1-SNAPSHOT`; later 4.x releases target 1.21.5+.
  - JUnit `5.12.1` via `org.junit:junit-bom`, matching MockBukkit's
    `junit-jupiter`.
  - joml `1.10.9`; WorldEdit BOM `com.intellectualsites.bom:bom-newest:1.52`
    (resolves from Maven Central and pins FAWE `2.12.3`).
- `kotlin.stdlib.default.dependency=false` plus `compileOnly(kotlin("stdlib"))`
  keeps the published `rooster-region` POM to `joml` only, as required.
- `paper-api` is also a `testImplementation` because MockBukkit needs it at test
  runtime; neither it nor Adventure appears in any published POM.
- The wrapper was bootstrapped from the reference wrapper: the system Gradle
  distribution (`GRADLE_HOME=/usr/share/java/gradle`) is missing modules, so
  `gradle wrapper` cannot run in this environment.
- Toolchain additions beyond the scope:
  - `settings.gradle.kts` applies `org.gradle.toolchains.foojay-resolver-convention`
    `0.8.0` so `jvmToolchain(21)` can locate or download a JDK 21.
  - `gradle.properties` sets `org.gradle.jvmargs=-Xmx2g` for the build daemon.
- `worldedit` applies the IntellectualSites BOM as `compileOnly` so its
  constraints reach only the compile classpath, and marks
  `FastAsyncWorldEdit-Bukkit` `isTransitive = false` because only its
  `com.sk89q.worldedit.bukkit` API is needed; neither leaks into a published POM.
- Smoke coverage: `core` has `ClasspathSmokeTest` (forbidden FQCNs absent) and
  `MockBukkitHarnessTest` (pinned MockBukkit boots), and the `check`-bound
  `verifyCoreDependencies` task asserts the generated POM and `runtimeClasspath`
  are `joml`-only and that `compileClasspath` has no `dev.rooster`/Exposed/
  WorldEdit leak. `worldedit` is verified by the `check`-bound
  `verifyWorldEditClasspath` task, which resolves the FAWE/BOM compile classpath
  (asserting `dev.rooster.region:core` and FAWE-Core) and asserts the FAWE-Core
  and FAWE-Bukkit jars ship `CuboidRegion` and `BukkitAdapter`; it has no JUnit
  test until ticket 020 adds the adapter under test.
- Symbol-level exercise of `worldedit`'s `api(project(":core"))` is deferred to
  ticket 020: `core` has no public symbol to reference until ticket 010 lands.
