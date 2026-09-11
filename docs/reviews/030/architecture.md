# Architecture review — 030 (README, publishing, and consumption)

## Round 1
### Verdict
The 030 change is documentation and build-verification only: no module boundary,
package or public API moved, `core` stays free of Rooster/Exposed/WorldEdit, the
WorldEdit API stays `compileOnly` in `worldedit`, and the new root
`verifyPublishedArtifacts` task is deliberately outside `check` so `check` has no
publishing side effect. The composite-build guidance matches the real
artifact-id/project-name split, `AGENTS.md`'s status matches the tree, and
`docs/architecture.md`/`docs/design.md` are not invalidated by 030. I found no
new module-boundary, extendability or documentation-staleness work.

### Findings
#### No new findings.

### Non-findings
- **`core` boundary is unchanged and clean.** The diff adds no dependency:
  `core/build.gradle.kts:19-29` still declares only `compileOnly(paper-api)`,
  `api(joml)`, `compileOnly(kotlin("stdlib"))` and test deps, and the new
  `ReadmeExamplesTest.kt` is a `core` test using existing MockBukkit support. No
  source references `dev.rooster`, `org.jetbrains.exposed`, `com.sk89q` or
  `net.kyori`.
- **`worldedit` keeps WorldEdit `compileOnly` and the generic API target.**
  `worldedit/build.gradle.kts:22-32` is untouched; `Adapter.kt:3-6,11` imports
  only `com.sk89q.worldedit.*`, no `com.fastasyncworldedit.*`. The extended
  `verifyWorldEditClasspath` (`worldedit/build.gradle.kts:51-119`) adds a
  generated-POM assertion and keeps the existing compile-present/runtime-absent
  classpath checks, so it still enforces the boundary it owned before 030.
- **The build changes do not put publishing into `check`.** The new
  `verifyPublishedArtifacts` (`build.gradle.kts:11-53`) depends on
  `:core:publishToMavenLocal`/`:worldedit:publishToMavenLocal` but is registered
  only on the root project, which has no `check`; the only `check` wiring is
  `core/build.gradle.kts:96-98` and `worldedit/build.gradle.kts:122-124` to the
  in-module verification tasks that read the *generated* POM. So `./gradlew check`
  cannot write to `~/.m2`. Root is the right home for a cross-module check of the
  *installed* POMs, and the per-module checks stay in their own modules.
- **Composite-build guidance is architecturally sound.** `README.md:62-71` maps
  `dev.rooster.region:rooster-region` → `project(":core")` and
  `dev.rooster.region:rooster-region-worldedit` → `project(":worldedit")`. The
  Gradle project names are `core`/`worldedit` (`settings.gradle.kts:7`) while the
  published artifact ids are `rooster-region`/`rooster-region-worldedit`
  (`core/build.gradle.kts:8-12`, `worldedit/build.gradle.kts:10-14`), so
  automatic substitution would not match and the explicit
  `dependencySubstitution` is required — the README states exactly this
  (`README.md:58-60`). The coordinates it declares match the publication
  `artifactId`s and the root `group`/`version` (`build.gradle.kts:6-9`), and
  `:worldedit`'s `api(project(":core"))` resolves within the included build, so
  no extra mapping is needed for the adapter's core dependency.
- **`AGENTS.md` "Current status" matches the tree.** `AGENTS.md:87-91` records
  000/010/020 as done and 030 as implemented/in review, and correctly calls 030
  the last MVP ticket (`docs/tasks/README.md:10-13`). No stale claim remains in
  that section (the earlier `justfile`-targets and 010/020 status gaps are gone).
- **`docs/architecture.md` and `docs/design.md` are in step.** Their module maps
  (`architecture.md:7-10`), dependency paragraphs (`architecture.md:26-30,40-45`),
  WorldEdit seam (`architecture.md:32-45,49-52`) and decisions
  (`design.md:15-27,42-47`, including the `maven-publish` to `mavenLocal` decision)
  remain accurate. 030 adds no module, package, public symbol or dependency, so it
  invalidates nothing there. The root `verifyPublishedArtifacts` task is build
  infrastructure, not a module seam, and the per-module verification tasks were
  likewise never listed in `architecture.md`.
- **`docs/manual-test.md` MT-003 belongs there and names 030.**
  `docs/manual-test.md:13` records the throwaway consumer build against
  `mavenLocal` and the composite build plus the real-server selection contract,
  and the "How to run" note (`:17-20`) explains what it needs. It is the
  non-automatable consumer counterpart to the snippet test, matching the ticket's
  "snippet test or a documented manual step" criterion.
- **Prior correctness report — concur.** I concur with
  `docs/reviews/030/correctness.md`'s verdict (no new findings) and its
  non-findings on POM contents, the composite mapping and MT-003. Its Adventure
  nuance is correct: Paper puts `net.kyori.adventure-*` on `core`'s compile
  classpath transitively, but `core` declares no direct Adventure dependency and
  the published POM is `joml`-only, so the boundary claim holds and there is no
  actionable build or doc change.
- **Orchestrator-owned in-flight state — not reported.**
  `docs/tasks/README.md:13` and the ticket frontmatter still read `todo`; as
  established in the 000/020 architecture reviews, that is queue state the
  orchestrator sets at commit/done, not documentation staleness.
- **Duplication observation, not raised as work.** The POM-dependency regex now
  appears in `core/build.gradle.kts:51-59`, `worldedit/build.gradle.kts:62-74` and
  `build.gradle.kts:27-35`. This is the same build-script duplication the 000
  round-2 review accepted for a two-module repo; it is not a module/package
  boundary, and extracting a shared build helper is not warranted by this ticket.
