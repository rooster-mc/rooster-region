---
name: World-scoped worldEditSelection
status: done
parent: MVP
depends-on: ["020"]
reviewers: [tester, correctness, architecture]
---

## Goal
Make `Player.worldEditSelection()` world-scoped: return `null` when the
session's selection belongs to a different world than the player's current
world, instead of handing back a selection that would be misread at the wrong
coordinates.

## Scope
- In `worldedit/Adapter.kt`, compare the selection's world to the player's world
  and return `null` on mismatch.
- Keep returning `null` when there is no selection.
- Tests where possible; otherwise record a manual-test entry.

## Acceptance criteria
- A stale selection from another world yields `null`.
- A selection in the player's current world is returned unchanged.
- `just build`/`test`/`format` green.

## Out of scope
- Changing `WERegion.toRegion(world)`.

## Notes
- `mc-ui-designer` currently guards this itself; once this lands it can drop the
  guard.
