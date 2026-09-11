---
name: WorldEdit adapter module
status: todo
parent: MVP
depends-on: ["010"]
reviewers: [tester, correctness, architecture]
---

## Goal
Port the WorldEdit bridge into the optional `worldedit` module, targeting the
generic WorldEdit API and returning `null` instead of throwing when there is no
selection.

## Source of truth
- `/home/cyp/repos/rooster-monolith/worldedit/src/main/kotlin/dev/cypdashuhn/rooster/worldedit/adapter/Adapter.kt`
- `/home/cyp/repos/BuildPluginV4/src/main/kotlin/dev/cypdashuhn/build/worldedit/Adapter.kt`

## Scope
`dev.rooster.region.worldedit`:
- `Region.toWorldEditRegion(): CuboidRegion`
- `WERegion.toRegion(world: World): Region`
- `WERegion.toRegion(player: Player): Region`
- `BlockVector3.toLocation(world: World): Location`
- `Player.worldEditSelection(): WERegion?` (uses
  `WorldEdit.getInstance().sessionManager`; `null` when no selection)
- Import `com.sk89q.worldedit.regions.Region as WERegion`.

## Acceptance criteria
- `worldedit` compiles against the WorldEdit API (`compileOnly`) and depends on
  `core` only.
- `Player.worldEditSelection()` returns `null` (not an exception) when the
  player has no selection.
- Conversion functions use the selection's min/max points and world.
- Unit tests cover what is testable without a live WorldEdit session; anything
  not testable is recorded in `docs/manual-test.md`.
- No FAWE-specific (`com.fastasyncworldedit.*`) imports.

## Out of scope
- CommandAPI argument types (`WESelectionArgument`, `worldEditRegionArgument`) —
  framework- and version-locked.
- Any `plugin.yml` or hard runtime dependency on a WorldEdit implementation.

## Notes
- A library cannot declare `depend`; the consumer chooses WorldEdit or FAWE.
- If the WE API is awkward to test, keep the conversion functions pure over
  `BlockVector3`/min/max and test those.
