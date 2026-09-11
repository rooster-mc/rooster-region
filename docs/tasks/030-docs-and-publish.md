---
name: README, publishing, and consumption
status: done
parent: MVP
depends-on: ["000", "010", "020"]
reviewers: [correctness, architecture, readability]
---

## Goal
Make the library usable: a README with usage examples, verified publishing, and
documented consumption paths.

## Scope
- `README.md`: what it is, the two modules, install (Gradle coordinates),
  usage examples for `Region`/`Face`/geometry helpers and for
  `Player.worldEditSelection()`.
- Verify `just publish` installs both artifacts to `mavenLocal` and that the
  POMs are correct (no framework/ORM/WorldEdit leaking into `rooster-region`).
- Document consuming via `mavenLocal` and via a Gradle composite build
  (`includeBuild`).
- Confirm the `core` module has no dependency on Rooster, an ORM, WorldEdit or
  Adventure, and that `worldedit` pulls WorldEdit only as `compileOnly`.

## Acceptance criteria
- README examples compile against the published artifacts (checked by a small
  snippet test or a documented manual step).
- Published `rooster-region` POM lists only `joml` as a runtime dependency.
- Consumption instructions are accurate for both `mavenLocal` and composite.

## Out of scope
- Publishing to a remote repository / CI.

## Notes
- This is documentation-heavy; `correctness` checks the claims against the
  build, `architecture` checks module boundaries, `readability` checks clarity.
