# Rooster Region — Design

A standalone Kotlin library for 3D-space region math on Bukkit, with an optional
WorldEdit adapter. Extracted from the region code that lives inside
`rooster-core` (`dev.rooster.core.region`) and the WorldEdit bridge in
`rooster-monolith/worldedit` / `BuildPluginV4/worldedit`.

## Goal

Package a reusable region/geometry utility that any Paper plugin can depend on
without pulling in the Rooster framework, its ORM, or WorldEdit.

## Decisions (taken without asking)

- **Standalone core.** The `core` module must be importable without
  `rooster-core`, without the Rooster framework, and without any ORM/Exposed
  (`rooster-sql`/`RoosterDb`). It depends only on the Bukkit API (`compileOnly`)
  and `joml`. Kotlin stdlib is also `compileOnly`
  (`kotlin.stdlib.default.dependency=false`), keeping the published POM
  `joml`-only; consumers supply stdlib at runtime.
- **No geometry/bukkit split.** Geometry helpers and the Bukkit `Region` live in
  the same `core` module. Splitting them adds friction for no benefit today.
- **WorldEdit is optional and separate.** The WE bridge lives in its own
  `worldedit` module so a consumer can use `core` without WorldEdit on the
  classpath. It targets the generic WorldEdit API (`com.sk89q.worldedit.*`), not
  FAWE-specific classes, and is `compileOnly`. A library has no `plugin.yml`, so
  the consumer decides whether to depend on WorldEdit or FastAsyncWorldEdit.
- **Excluded from the extraction:**
  - `compareVectors` player-messaging helper (localization/Adventure) — not
    geometry.
  - The CommandAPI/rooster-commands argument helpers (`WESelectionArgument`,
    `worldEditRegionArgument`) — framework- and version-locked.
- **Coordinates:** `Region` is a wrapper between two Bukkit `Location` edges,
  matching the existing behaviour. `Face` and the geometry helpers
  (`toVector3d`, `Vector3d.toLocation`, `Box`, `value`, `Vector3d.distance`) come
  along because `Region` uses them.
- **Stack:** Kotlin `2.4.20`, Java `21`, Paper API `1.21.4` (`compileOnly`),
  `joml`, JUnit 5 + MockBukkit. Matches the Rooster library family.
- **Coordinates / naming:** group `dev.rooster.region`; artifacts
  `rooster-region` (core) and `rooster-region-worldedit`; packages
  `dev.rooster.region` and `dev.rooster.region.worldedit`; version
  `1.0-SNAPSHOT`.
- **Publishing:** `maven-publish` to `mavenLocal` so consumers (and our own
  composite builds) can resolve it.

## Out of scope

- CommandAPI argument types for selections.
- Any dependency on `rooster-core`, `rooster-sql`, `rooster-localization`, or
  `rooster-ui`.
- Non-cuboid region types (WorldEdit `CuboidRegion` only).
