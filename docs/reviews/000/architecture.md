# Architecture review — 000 (Gradle setup for core and worldedit modules)

## Round 1
### Verdict
Module boundaries, group/artifact/package naming and the WorldEdit-only-in-`worldedit`
separation are all correct, and the Kotlin `2.4.20` edits to `docs/design.md` and
`AGENTS.md` match the build. Two docs gaps remain: `AGENTS.md`'s status section is now
stale, and the `core` dependency description in `docs/design.md`/`docs/architecture.md`
no longer covers the Kotlin-stdlib handling that is load-bearing for the POM-purity
criterion; the ticket Notes also omit the added foojay toolchain-resolver plugin.

### Findings
#### 1. `AGENTS.md` "Current status" is stale after ticket 000's build scaffolding
- Location: `AGENTS.md:87-90` (specifically line 89)
- Problem: it still reads "Planning/setup only. No code has been written yet. Start
  with ticket 000." Ticket 000 has now written the two-module build, the wrapper, the
  `justfile` and the smoke test, so "No code has been written yet" is false. The
  implementor already touched this file for the Kotlin version, so the invalidating
  change and the doc edit are in the same working tree; per `AGENTS.md` ("Update docs in
  the same commit as the change that invalidates them") this should not be left behind.
- Suggested fix: update the status paragraph in the 000 commit (e.g. point at ticket
  010 as the next step and note that 000's setup is in review), or explicitly defer the
  status rewrite to the commit that marks 000 `done`, with that reason recorded.

#### 2. `core`'s documented dependency set omits the Kotlin-stdlib handling
- Location: `docs/design.md:17-18`, `docs/architecture.md:26` vs `core/build.gradle.kts:19`
  and `gradle.properties:1`
- Problem: design says core "depends only on the Bukkit API (`compileOnly`) and `joml`",
  and architecture lists "`compileOnly(paper-api)`, `implementation(joml)`". The build
  also declares `compileOnly(kotlin("stdlib"))` and relies on
  `kotlin.stdlib.default.dependency=false` to keep stdlib out of the published POM —
  the exact mechanism behind acceptance criterion 3. Because the published POM omits
  stdlib, a consumer of `rooster-region` must supply Kotlin stdlib itself (normally
  automatic for Kotlin consumers, not for a Java-only one). That consumer-visible seam
  is only in the ticket Notes, not in the two docs that describe the module's
  dependency boundary, so the docs are out of step with the code they describe.
- Suggested fix: add the stdlib line and the `kotlin.stdlib.default.dependency=false`
  rationale to `docs/architecture.md`'s `core` dependencies (and adjust
  `docs/design.md:17-18`), noting that stdlib is `compileOnly` so the POM stays
  `joml`-only and that consumers provide stdlib at runtime.

#### 3. Ticket Notes do not record the foojay toolchain-resolver settings plugin
- Location: `settings.gradle.kts:2`; `docs/tasks/000-project-setup.md:48-64`
- Problem: the Notes enumerate version deviations but not
  `org.gradle.toolchains.foojay-resolver-convention:0.8.0`, which is an addition beyond
  the ticket scope and changes build behaviour (Gradle may download a JDK 21 toolchain
  from a remote resolver when none is installed). Nor is
  `org.gradle.jvmargs=-Xmx2g` (`gradle.properties:2`) recorded. The review focus asks
  whether deviation notes are complete; the two most visible unrecorded build additions
  are these.
- Suggested fix: add a Note bullet for the foojay resolver (why it is needed given
  `jvmToolchain(21)`) and the JVM args, or drop the resolver if the build must not
  reach a toolchain service.

### Non-findings
- **`core` boundary is clean.** `core/build.gradle.kts:16-26` declares only Paper API
  and joml (plus stdlib and test deps); no Rooster, Exposed, WorldEdit or Adventure.
  WorldEdit/FAWE (`worldedit/build.gradle.kts:20-23`) lives only in the `worldedit`
  module, matching `docs/design.md:21-25` and `AGENTS.md:82-83`.
- **Naming matches the prescribed coordinates.** `build.gradle.kts:7-8` sets group
  `dev.rooster.region` and version `1.0-SNAPSHOT`; `core/build.gradle.kts:8,40` gives
  `rooster-region`, `worldedit/build.gradle.kts:9,45` gives
  `rooster-region-worldedit`; the core test package is `dev.rooster.region`
  (`ClasspathSmokeTest.kt:1`). This matches `docs/design.md:37-40` and
  `docs/architecture.md:6-10`.
- **`worldedit` uses the right boundary primitives.** It applies `java-library` and
  `api(project(":core"))` (`worldedit/build.gradle.kts:3,19`) while Paper/FAWE are
  `compileOnly`, so the published `worldedit` POM exposes only `core` — the correct
  optional-adapter shape.
- **Kotlin version edits are consistent.** `docs/design.md:35` and `AGENTS.md:21` now
  read `2.4.20`, matching `build.gradle.kts:2`. No other doc still cites `2.2.0` except
  the ticket Scope (`docs/tasks/000-project-setup.md:22`), which is the original
  requirement with the deviation correctly recorded under Notes
  (`docs/tasks/000-project-setup.md:49-50`) — that placement is intentional and fine.
- **`architecture.md`'s `.editorconfig` reference is accurate** (`docs/architecture.md:50`;
  `.editorconfig` exists with `max_line_length = 100`).
- **`core` uses `implementation(joml)` although its future public API exposes
  `org.joml.Vector3d`, and does not apply `java-library`.** I concur with the
  correctness non-finding: the ticket scope explicitly mandates `implementation(joml)`,
  and Paper API currently supplies joml at compile scope, so this is the specified
  modelling, not a defect for 000. It is a seam worth revisiting in ticket 010 if the
  adapter/consumer story changes.
- **No version catalog / convention plugin.** The per-module duplication of plugin
  blocks and the repeated Paper/JUnit/MockBukkit coordinates is acceptable for a
  two-module library and is not required by the ticket; not raised as work.
- **Ticket `status: todo` in `docs/tasks/000-project-setup.md:3` and
  `docs/tasks/README.md:10`** is orchestrator-owned queue state for an in-flight
  ticket, not documentation staleness.
- **Prior reports.** I concur with correctness's verdict (no correctness finding) and
  note that the tester's three findings are test-quality matters outside my scope, so I
  do not re-report them. The tester's observation that `worldedit` has no test/main
  source means its FAWE/BOM compile classpath is not exercised by `just build`; that is
  a test-coverage point, not a module-boundary one, so it stays with the tester.

## Round 2
### Verdict
All three Round 1 findings are resolved: `AGENTS.md`'s status is rewritten,
`docs/design.md` and `docs/architecture.md` now record the stdlib seam, and the ticket
Notes document the foojay resolver and JVM args. Module boundaries and naming are
unchanged and correct, and the new `check`-bound verification tasks stay inside their
own modules; two documentation items still need attention.

### Findings
#### 1. `AGENTS.md`'s "justfile targets do not exist yet" parenthetical is now stale
- Location: `AGENTS.md:34`
- Problem: it still reads "(If `justfile` targets do not exist yet, they are created by
  ticket 000.)". The `justfile` exists at the repo root with `build`, `test`, `format`
  and `publish`, so the caveat is false and contradicts the `just` commands listed
  directly above it. The Round 1 status rewrite updated the `Current status` section
  but missed this line in the same file.
- Suggested fix: delete the parenthetical in the 000 commit (the targets exist), or
  replace it with a note that the `justfile` was created by ticket 000.

#### 2. Ticket Notes will go stale when tester Round 2 finding 1 is resolved
- Location: `docs/tasks/000-project-setup.md:73-80` (specifically lines 77-78)
- Problem: the "Smoke coverage" Note names `worldedit`'s
  `WorldEditCompileClasspathTest` and describes it as "WorldEdit absent at runtime".
  Tester Round 2 finding 1 asks for that test to be deleted or retargeted because it
  asserts the wrong seam and will obstruct ticket 020. Whichever resolution is chosen,
  the Note as written will no longer match the tree, so the same commit that touches
  the test must update this list. This is the one docs-in-step dependency introduced by
  the Round 2 findings.
- Suggested fix: when fixing tester Round 2 finding 1, drop or rewrite the
  `WorldEditCompileClasspathTest` clause in `docs/tasks/000-project-setup.md:77-78` so
  the Note names only the tests/tasks that remain.

### Non-findings
- **Round 1 A1 resolved.** `AGENTS.md:87-92` now states ticket 000 is implemented and
  in review, and points at ticket 010 — accurate against the tree.
- **Round 1 A2 resolved.** `docs/design.md:18-20` and `docs/architecture.md:26-30` both
  now state stdlib is `compileOnly`, cite `kotlin.stdlib.default.dependency=false`, and
  say consumers supply stdlib at runtime — accurate against
  `core/build.gradle.kts:21` and `gradle.properties:3`.
- **Round 1 A3 resolved.** `docs/tasks/000-project-setup.md:65-68` records the foojay
  resolver (`settings.gradle.kts:2`) and `org.gradle.jvmargs=-Xmx2g`
  (`gradle.properties:4`), with the reason for each.
- **Module boundaries hold.** `core/build.gradle.kts:18-28` still declares only Paper
  API, joml, stdlib and test deps — no Rooster, Exposed, WorldEdit or Adventure.
  WorldEdit/FAWE remains confined to `worldedit/build.gradle.kts:22-39`, and the new
  `verifyCoreDependencies`/`verifyWorldEditClasspath` tasks live in their respective
  module files and scan only that module's own configurations. The `worldedit` test
  (`WorldEditCompileClasspathTest.kt:10`) references the WorldEdit API only as a
  `Class.forName` string, so it adds no compile dependency.
- **Naming and artifact coordinates unchanged and correct.**
  `core/build.gradle.kts:7,98` and `worldedit/build.gradle.kts:10,103` feed the same
  `artifactName` into `archivesName` and the publication `artifactId`, matching
  `docs/design.md:39-42` and `docs/architecture.md:6-10`.
- **Extendability unchanged.** The two `moduleNames` helpers
  (`core/build.gradle.kts:62-67`, `worldedit/build.gradle.kts:57-62`) and the
  per-module verification blocks are duplicated, but with exactly two modules and no
  ticket requirement for a convention plugin or version catalog this remains acceptable;
  the duplication is the natural refactor point if a third module lands.
- **`AGENTS.md` Stack line still accurate** (`AGENTS.md:21`: Kotlin `2.4.20` matches
  `build.gradle.kts:2`; Paper `1.21.4`, joml, WorldEdit `compileOnly`, JUnit +
  MockBukkit all match).
- **Ticket `status: todo`** (`docs/tasks/000-project-setup.md:3`,
  `docs/tasks/README.md:10`) is still orchestrator-owned queue state; I note the
  inconsistency with `AGENTS.md:89` ("in review") but leave it to the
  ticket-orchestrator to set at commit/done, as in Round 1.
- **Prior reports.** I concur with correctness's Round 2 verdict (no findings) and with
  its agreement on the tester's three findings. The tester's findings are test-quality
  and I do not re-report them; my finding 2 only tracks the documentation consequence
  of resolving tester finding 1.
