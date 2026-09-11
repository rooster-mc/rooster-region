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
