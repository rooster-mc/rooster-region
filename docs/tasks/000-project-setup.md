---
name: Gradle setup for core and worldedit modules
status: todo
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
