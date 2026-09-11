# Readability review — 000 (Gradle setup for core and worldedit modules)

## Round 1
### Verdict
The scaffolding is small, consistent and easy to follow: the root build script is
minimal, both module scripts share the same shape, the justfile mirrors the four
documented commands, and the smoke test is plainly named. I found four clarity
gaps, all of them "the why is not written down" or a duplicated literal, not
structural rewrites.

### Findings
#### 1. `gradle.properties` disables stdlib with no reason written down
- Location: `gradle.properties:1`
- Problem: `kotlin.stdlib.default.dependency=false` is a load-bearing,
  non-obvious switch: it is the mechanism that keeps Kotlin stdlib out of the
  published `rooster-region` POM (acceptance criterion 3). A newcomer opening
  `gradle.properties` sees a bare flag with no hint of its purpose or of the
  consumer-visible consequence (a Java-only consumer must supply stdlib). The
  reason currently lives only in the ticket Notes and in
  `core/build.gradle.kts:19`, not next to the flag. This is distinct from the
  architecture finding about `docs/design.md`/`docs/architecture.md`: it is the
  in-file rationale, not the module-boundary documentation.
- Suggested fix: add a one-line `#` comment above the property explaining that
  stdlib is kept out of the published POM and is instead a `compileOnly`
  dependency consumers provide at runtime.

#### 2. The artifact name is written twice per module
- Location: `core/build.gradle.kts:8` and `:40`; `worldedit/build.gradle.kts:9`
  and `:45`
- Problem: each module declares the same name twice — once as
  `base { archivesName.set("rooster-region") }` (the jar name) and again as
  `artifactId = "rooster-region"` (the Maven coordinate). Both are genuinely
  needed, because the Gradle project is named `core`/`worldedit` and neither
  value defaults from the other, but that subtlety is invisible at a glance and
  a rename that touches only one of the two literals produces a jar whose file
  name disagrees with its published coordinate.
- Suggested fix: hoist a single `val artifactName = "rooster-region"` near the
  top of each script and use it for both `archivesName.set(artifactName)` and
  `artifactId = artifactName`; alternatively add a short comment saying why both
  assignments exist.

#### 3. The `compileOnly` BOM and non-transitive Bukkit artifact are unexplained
- Location: `worldedit/build.gradle.kts:21-23`
- Problem: `compileOnly(platform(...bom-newest...))` plus
  `FastAsyncWorldEdit-Core` (transitive) and `FastAsyncWorldEdit-Bukkit` with
  `{ isTransitive = false }` is an asymmetric arrangement a reader cannot decode
  from the code. The BOM is applied in `compileOnly` so its constraints only
  reach the compile classpath, and Bukkit is made non-transitive while Core is
  not; neither choice is self-evident, and the ticket Notes do not cover them.
  Someone wiring ticket 020's imports will stop here and have to reverse-engineer
  it.
- Suggested fix: add a brief comment on the BOM/`isTransitive` lines (why the
  platform is `compileOnly` and why only the Bukkit adapter is non-transitive),
  or record the same rationale as a Note on ticket 000.

#### 4. `.editorconfig` is not marked as the config root
- Location: `.editorconfig:1`
- Problem: without `root = true`, EditorConfig walks up past the repository and
  would silently merge any ancestor `.editorconfig` (there are sibling repos
  under `/home/cyp/repos/` with their own), so the formatter contract is not
  self-contained and a reader cannot tell where the effective settings stop.
  Today no ancestor config exists, so the impact is latent, but the fix is one
  line and makes the formatting rules authoritative.
- Suggested fix: add `root = true` as the first line.

### Non-findings
- **justfile is readable.** One recipe per documented command
  (`build`/`test`/`format`/`publish`), each with a short purpose comment, plus a
  `default` that lists them; `set shell := ["bash", "-uc"]` is unusual but
  harmless with no interpolated variables. No `clean` recipe is expected —
  `AGENTS.md` and the ticket scope list exactly these four commands.
- **Root `build.gradle.kts` is minimal and correct.** Plugins are declared
  `apply false` and applied per module, which is the standard multi-project
  shape; setting `group`/`version` via `allprojects` also hits the root project,
  which is harmless and not worth changing.
- **No leftover or dead files.** The wrapper (`gradlew`, `gradlew.bat`,
  `gradle/wrapper/*`) is present and `gradlew` is executable; `.gitignore`
  ignores `build/`/`.gradle/` and re-includes the wrapper jar. No stray sources,
  no `plugin.yml` in `worldedit` (correct for a library).
- **Test file hygiene is fine.** `ClasspathSmokeTest.kt` has a clear backtick
  name, a matching package, no comments, and its `forbidden` list and wrapping
  are ktlint-clean per `.editorconfig`.
- **No comments anywhere in the build scripts or test**, so there is no
  comment-noise problem; findings 1 and 3 are the only places where a
  non-obvious *why* is unwritten.
- **Per-module duplication of plugin/dependency blocks is not raised as work.**
  I concur with the architecture reviewer that repeating the Paper/JUnit/
  MockBukkit coordinates and the `repositories` blocks across the two module
  scripts is acceptable for a two-module library and is not required by the
  ticket.
- **Prior reports.** I concur with architecture's three findings (stale
  `AGENTS.md` status, undocumented stdlib handling in `docs/design.md`/
  `docs/architecture.md`, unrecorded foojay/JVM-args additions); they are
  docs/module-boundary matters and I do not re-report them. I concur with
  correctness's clean verdict and with the tester's three findings, which are
  test-quality matters outside my scope. The tester's and correctness's shared
  observation that `worldedit` has no sources is not a readability defect — the
  module is scaffolding for ticket 020.

## Round 2
### Verdict
All four Round 1 findings are resolved cleanly — the stdlib rationale is now
inline, both artifact names are single-sourced, the BOM/`isTransitive` choices
are commented, and `.editorconfig` is rooted. The new `check`-bound verification
tasks are followable and ktlint-clean; I found two small clarity gaps in how they
describe and name their own checks.

### Findings
#### 1. `verifyCoreDependencies`' description omits one of its three checks
- Location: `core/build.gradle.kts:40`
- Problem: the description says the task "Asserts the published core POM and
  runtime classpath expose only joml", but the task also scans
  `compileClasspath` for forbidden WorldEdit/Exposed/Rooster leaks
  (`core/build.gradle.kts:74-86`). That third check is the only guard for
  `compileOnly` leaks, which the POM and runtime assertions cannot see, so it is
  the most important part to advertise. A reader skimming the task (or
  `./gradlew tasks`) would not know it exists, and a future maintainer could drop
  it believing the description is complete.
- Suggested fix: broaden the description, e.g. "Asserts the published core POM
  and runtime classpath are joml-only and the compile classpath has no
  framework/ORM/WorldEdit leaks."

#### 2. The `moduleNames` helper silently drops the project's own component, and its name does not say so
- Location: `core/build.gradle.kts:62-67` (filter at `:66`);
  `worldedit/build.gradle.kts:57-62` (filter at `:61`)
- Problem: `moduleNames` returns module coordinates from
  `incoming.resolutionResult.allComponents`, which includes this project's own
  component, so the helper filters out `dev.rooster.region:core` /
  `dev.rooster.region:worldedit`. The name `moduleNames` suggests a plain
  enumeration, so a reader cannot tell that the result is "all resolved modules
  except me" or why the exclusion is there; a caller comparing the result to an
  expected list has to know the implicit filter.
- Suggested fix: rename to something that states the intent (e.g.
  `externalModuleNames`) or add a one-line comment above the `filterNot`
  explaining that `allComponents` includes this project itself.

### Non-findings
- **R1 resolved.** `gradle.properties:1-2` now carries a two-line `#` comment
  tying `kotlin.stdlib.default.dependency=false` to the published-POM/stdlib
  story; the flag is no longer unexplained.
- **R2 resolved.** `val artifactName` is hoisted in both scripts
  (`core/build.gradle.kts:7`, `worldedit/build.gradle.kts:10`) and feeds both
  `archivesName` (`:10`, `:13`) and `artifactId` (`:98`, `:103`), so the jar name
  and Maven coordinate are single-sourced.
- **R3 resolved.** `worldedit/build.gradle.kts:25-26` explains the `compileOnly`
  BOM and `:29-30` the non-transitive Bukkit adapter, and the same rationale is
  recorded in `docs/tasks/000-project-setup.md:69-72`.
- **R4 resolved.** `.editorconfig:1` now sets `root = true`, so the formatter
  contract is self-contained.
- **New verification tasks are structurally consistent and followable.** Both
  register with `group = "verification"` and a description, capture
  configurations at configuration time, do the work in `doLast`, and are wired
  into `check` via `tasks.named("check")` (`core/build.gradle.kts:38-92`,
  `worldedit/build.gradle.kts:49-97`). `verifyWorldEditClasspath`'s description
  ("WorldEdit API is on the compile classpath only") adequately covers its
  compile-presence plus runtime-absence checks, so finding 1 is scoped to the
  core task.
- **ktlint/formatting is clean.** No line in either build script exceeds the
  `.editorconfig` `max_line_length = 100`, and the nested helpers, regex call
  and `ZipFile` block follow the wrapping style already used elsewhere; the
  `java.util.zip.ZipFile` import in `worldedit/build.gradle.kts:1` is the only
  import needed and is placed correctly.
- **justfile unchanged and still readable.** Same four recipes with purpose
  comments; the new verification tasks ride on `just build` via `check`, which is
  consistent with the ticket's command list.
- **No leftover or dead files.** The tree adds only the two verification tasks
  and two tests; wrapper, `.gitignore` and module layout are unchanged.
- **Prior reports.** I concur with the tester's Round 2 findings 1–3 — they are
  test-quality matters outside my scope. On tester finding 1 I add only the
  readability facet: `WorldEditCompileClasspathTest`'s name promises a
  compile-classpath assertion while its body asserts test-runtime absence, so
  whichever resolution is chosen the name should follow the assertion. I concur
  with architecture's Round 2 findings (stale `AGENTS.md` parenthetical and the
  ticket Note that will need updating with tester finding 1); both are docs
  matters. I concur with correctness's Round 2 verdict (no findings), including
  that the `artifactName` hoist and the new tasks leave the published contract
  unchanged.
