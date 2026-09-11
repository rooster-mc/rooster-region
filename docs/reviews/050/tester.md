# Tester review — 050 (World-scoped worldEditSelection)

## Round 1

### Verdict
I concur with the implementor's assessment: the new world-scoping branch is
genuinely unreachable under MockBukkit, so no automatable test is missing. The
one piece of work is that ticket 050 has no `docs/manual-test.md` entry naming
it, and the manual gate requires one for its non-automatable acceptance criteria.
No tests were added, so there are no new excessive or brittle tests to report.

### Findings

#### 1. Ticket 050's non-automatable acceptance criteria have no manual-test entry naming the ticket
- Location: `docs/manual-test.md:9-13` (table); `docs/tasks/050-world-scoped-selection.md:22-23`
- Problem: Both of 050's acceptance criteria ("a stale selection from another
  world yields `null`" and "a selection in the player's current world is returned
  unchanged") are exercised only through `Player.worldEditSelection()`, which the
  harness cannot run (see the non-finding below). The manual gate
  (`docs/workflow.md:65-75`) says the entry "must exist and name the ticket", and
  none of MT-001/002/003 names 050 — MT-001 is 020's, MT-003 is 030's. MT-001's
  phrase "after switching worlds" (`docs/manual-test.md:11`) groups world
  switching with *incomplete* selections; it does not state the specific 050
  assertion that a still-defined selection made in world A is rejected once the
  player is in world B. With 050 marked done in `AGENTS.md:91` and the ticket
  frontmatter/index still `todo` (`docs/tasks/README.md:15`), the missing entry
  is the only gate item outstanding.
- Suggested fix: add a row to `docs/manual-test.md` and name 050, e.g.

  ```
  | MT-004 | 050 | On a real server with FastAsyncWorldEdit (or WorldEdit), make a selection in world A, then move the player to world B and confirm `Player.worldEditSelection()` returns `null`; move back to world A and confirm the same selection is returned unchanged. | unverified |
  ```

  Record the reason alongside it. The table currently has no reason column even
  though `docs/workflow.md:69` asks for one on unverified entries; either add a
  `Reason` column or extend the `## How to run` paragraph (the convention MT-003
  follows at `docs/manual-test.md:13,15-20`) with: "MT-004 requires a live
  WorldEdit/FAWE plugin and session; under MockBukkit `BukkitAdapter` is absent
  from the test classpath and its enum initializer needs
  `WorldEditPlugin.getInstance()`, so the world-scoping branch cannot be
  exercised."

### Non-findings
- **The `worldEditSelection()` path is correctly not automated — concur.**
  `com.sk89q.worldedit.bukkit.BukkitAdapter` ships only in the FAWE-Bukkit jar
  (`worldedit/build.gradle.kts:106-109`), which is `compileOnly` and is *not* on
  the test classpath: `testImplementation` (`worldedit/build.gradle.kts:34-40`)
  declares FAWE-Core, Paper, and MockBukkit but no FAWE-Bukkit, so any test
  calling `Adapter.kt:32-40` would fail to link before asserting anything. Adding
  FAWE-Bukkit to the test classpath would not fix it: I disassembled the
  BOM-resolved FAWE-Bukkit 2.15.3 `BukkitAdapter.class` and its enum constructor
  (run while the `INSTANCE` constant initializes) invokes
  `WorldEditPlugin.getInstance().getBukkitImplAdapter()`; with no real enabled
  WorldEdit/FAWE plugin `getInstance()` is null and class initialization throws.
  The function additionally needs `WorldEdit.getInstance().sessionManager`
  (`Adapter.kt:34`). No MockBukkit seam reaches the new `Adapter.kt:38` branch, so
  the ticket's "Tests where possible; otherwise record a manual-test entry"
  (`docs/tasks/050-world-scoped-selection.md:19`) is satisfied by a manual entry,
  not by a missing unit test.
- **No new or changed tests to judge as excessive or brittle.** `207d012` touched
  only `Adapter.kt` and `docs/architecture.md`; `AdapterTest.kt` is byte-for-byte
  the 020 suite (four tests, `AdapterTest.kt:31-84`). Nothing over-mocks or pins
  internal detail, and the four cases still target distinct functions.
- **Adding a test that merely expects `NoClassDefFoundError` would be worse than
  no test.** It would pin a classpath accident (main `compileOnly` not flowing to
  test runtime) rather than 050's behaviour, and would break the moment anyone
  legitimately puts FAWE-Bukkit on the test classpath. Deferring is right.
- **`verifyWorldEditClasspath` is unaffected.** It reads the main
  `compileClasspath`/`runtimeClasspath` (`worldedit/build.gradle.kts:58-59,
  111-118`), which no `testImplementation` change touches, so the compile-only /
  published-runtime seam stays guarded. No test-quality implication.
- **`Region.toWorldEditRegion()` and the rest of `worldEditSelection()` remain
  gated by MT-001 as before.** Those are 020-owned paths and unchanged by 050; I
  do not re-report them.
- **The `AGENTS.md` and `docs/architecture.md` edits are outside tester scope.**
  `AGENTS.md:87-91` and `architecture.md:53-55` are documentation; I note only
  that neither adds the manual entry finding 1 asks for.
- **No prior reports in this round to concur with or dissent from** — tester is
  first in the 050 reviewer list (`docs/tasks/050-world-scoped-selection.md:6`).

## Round 2

### Verdict
Round 1 finding 1 is resolved: `MT-004` names ticket 050, covers both of its
non-automatable acceptance criteria (selection in world A, player moves to world
B → `null`, back to world A → unchanged), is marked `unverified`, and the harness
reason is recorded in `## How to run`. The round-2 commits change no source or
test file, so nothing test-related regressed and there is no new test-quality or
harness-fidelity work.

### Findings
#### No new findings.

### Non-findings
- **Round 1 finding 1 is resolved as requested.** `docs/manual-test.md:14` adds
  `MT-004 | 050` with exactly the two-sided check (world A → world B → `null`;
  back → unchanged), and `docs/manual-test.md:20-23` records the reason ("requires
  a live WorldEdit/FAWE plugin and session: under MockBukkit `BukkitAdapter` is
  absent from the test classpath and its enum initializer needs
  `WorldEditPlugin.getInstance()`"), satisfying the manual gate's
  entry-names-the-ticket requirement (`docs/workflow.md:65-75`). The reason lives
  in the `## How to run` prose rather than a table column, matching the MT-003
  precedent (`docs/manual-test.md:13,18-20`); the table's lack of a `Reason`
  column remains a pre-existing format choice, not new work for this ticket.
- **No test file changed anywhere on the branch.** `git diff main...HEAD --
  '*/src/test/*'` is empty and `ec0445e..HEAD` (the round-2 commits) touches no
  `.kt` or `.kts` file, so `AdapterTest.kt` is byte-for-byte the round-1 suite.
  There is no new test to judge as excessive, brittle, or mis-layered, and none
  was removed.
- **The README fix does not disturb any test.** The architecture finding's fix at
  `README.md:190-194` is prose; the runnable snippet above it
  (`README.md:184-185`, `player.worldEditSelection()?.toRegion(player.world)`) is
  unchanged, so `ReadmeExamplesTest` is unaffected and the WorldEdit example
  remains correctly excluded and gated by `MT-001`/`MT-004`.
- **The MT-004 check is a good manual test, not a stiff or ambiguous one.** It
  asserts observable public behaviour at both ends of the transition and does not
  pin an implementation detail (e.g. it does not name `BukkitAdapter` or
  `selectionWorld`), so it survives internal refactors of the guard.
- **No earlier report in this round to concur with or dissent from** — tester is
  again first in the 050 reviewer list
  (`docs/tasks/050-world-scoped-selection.md:6`).
