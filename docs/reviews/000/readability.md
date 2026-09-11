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
