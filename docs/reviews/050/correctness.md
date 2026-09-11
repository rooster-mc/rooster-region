# Correctness review — 050 (World-scoped worldEditSelection)

## Round 1

### Verdict
No correctness findings. The added world guard in `Adapter.kt:38` compares the
selection's world against the player's current world using `BukkitWorld.equals`,
which resolves to underlying Bukkit-world identity, so distinct worlds compare
unequal and the same world compares equal. The branch is placed after the
`selectionWorld == null` guard and before `isSelectionDefined`, introduces no new
NPE or class-loading failure beyond what `Adapter.kt:33` already requires, and
leaves the same-world return path byte-for-byte equivalent. Both acceptance
criteria are satisfied by the code as written.

### Findings
None.

### Non-findings
- **World comparison is correct for both WorldEdit and FAWE — verified.**
  `Adapter.kt:38` evaluates `selectionWorld.equals(BukkitAdapter.adapt(this.world))`.
  In the FAWE-Bukkit jar this library compiles against,
  `BukkitAdapter.adapt(org.bukkit.World)` returns `new BukkitWorld(world)` and
  `BukkitWorld.equals(Object)` first matches the other `BukkitWorld` and compares
  the underlying `org.bukkit.World` values (`ref.equals(otherWorld)`), only
  falling back to name comparison when the other side is a non-`BukkitWorld`
  `com.sk89q.worldedit.world.World`. Plain WorldEdit's `BukkitWorld.equals` has
  the same shape. `LocalSession.getSelectionWorld()` returns the world the region
  was created with (`LocalSession.java:846-852`), which in a Bukkit server is a
  `BukkitWorld`, so both operands are `BukkitWorld`s and the identity branch is
  taken: the same Craft world compares equal, two distinct worlds compare
  unequal. No name collision can make distinct worlds equal, and a still-loaded
  world's strong server reference keeps the `WeakReference` alive so the same
  world cannot compare unequal.
- **Ordering and null handling are sound.** `Adapter.kt:37` already returns on a
  null `selectionWorld`, so line 38 never dereferences null. `BukkitAdapter.adapt`
  never returns null (`IBukkitAdapter.java:322-325`), and a null `this.world`
  would already have failed at `Adapter.kt:33`/`36`; line 38 adds no new NPE
  path. Running the world check before `isSelectionDefined` cannot change any
  observable result: a same-world incomplete selection still reaches
  `isSelectionDefined` and returns null there, and a different-world selection
  returns null either way. `isSelectionDefined(selectionWorld)` is still called
  with the same argument it was before.
- **Same-world behaviour is preserved exactly.** For a matching world the new
  guard falls through to the unchanged `localSession.isSelectionDefined(selectionWorld)`
  and `localSession.getSelection(selectionWorld)` at `Adapter.kt:39-40`, so the
  returned `WERegion` is the same object as before and still carries its own
  world. `WERegion.toRegion(world)` (`Adapter.kt:20-27`) is untouched, matching
  the ticket's out-of-scope note (`docs/tasks/050-world-scoped-selection.md:26-27`).
- **Acceptance criteria met.** "A stale selection from another world yields
  null" follows from the mismatch branch returning null before any region is
  produced. "A selection in the player's current world is returned unchanged"
  follows from the fall-through above. The remaining criterion (`just
  build`/`test`/`format` green) is the implementor's verification, not a
  correctness property I can judge from reading.
- **The new early return does not change the returned type or the documented
  `null`-not-exception contract** (`README.md:190-195`): a cross-world stale
  selection is now null instead of a region, which is exactly what callers such
  as `player.worldEditSelection()?.toRegion(player.world)` (`README.md:184-185`)
  want.
- **The tester's finding is outside correctness scope and I take no position on
  it.** Its subject is the `docs/manual-test.md` gate entry, which `tester` owns;
  it does not assert a logic or API defect. I do not re-report it.
- **README wording and the `AGENTS.md`/task-index status drift are documentation
  scope.** `README.md:190-192` still lists only "a stale selection was cleared
  after a world change" and does not spell out the new world-mismatch case, and
  `AGENTS.md:91` says all MVP tickets are done while
  `docs/tasks/README.md:15` still shows 050 as `todo`. Those are doc-staleness
  items for `architecture`, not correctness.

## Round 2

### Verdict
The implementation is unchanged since Round 1: the only source delta on the
branch is still the single guard at `Adapter.kt:38` from `207d012`, and the
round-2 commits (`ec0445e..HEAD`) touch only `README.md`, `docs/manual-test.md`
and review files. The README now states the world-mismatch case, matching the
code, so my Round 1 "no findings" verdict stands. No new correctness work.

### Findings
None.

### Non-findings
- **Implementation unchanged — confirmed.** `git diff 207d012..HEAD --
  worldedit/src/main/kotlin/dev/rooster/region/worldedit/Adapter.kt` is empty,
  `git diff main...HEAD` still shows exactly one added line in `Adapter.kt`, and
  `git diff ec0445e..HEAD --name-only` contains no `.kt`/`.kts` file. All Round 1
  reasoning (world comparison via `BukkitWorld.equals`, ordering after the
  `selectionWorld == null` guard and before `isSelectionDefined`, same-world
  fall-through, unchanged `toRegion(world)`) therefore still applies verbatim; I
  do not repeat it as findings.
- **The README contract now matches the implementation.** The round-2 edit
  (`README.md:190-194`) replaces the vague "a stale selection was cleared after a
  world change" with "the selection belongs to a world other than the player's
  current one (for example a stale selection made before a world change)", which
  is exactly what `Adapter.kt:38` does. The runnable example
  (`README.md:184-185`) is unchanged and its `?.` still absorbs the new `null`, so
  no caller-facing contract regressed.
- **No source or test change means no new logic/API surface to review.** The
  round-2 commits are documentation only, so there is no new math, nullability,
  adapter-conversion or caller-integration behaviour to assess.
- **The tester's Round 2 report is outside correctness scope and I take no
  position on it.** It resolves the manual-test-entry finding and judges no test
  code; nothing in it asserts a logic or API defect I would need to concur with or
  dissent from.
- **Round 1's documentation-scope notes are resolved.** The README staleness
  noted at Round 1 is fixed, and the `AGENTS.md`/task-index status drift was
  already handled by `ec0445e`; neither is a correctness matter.
