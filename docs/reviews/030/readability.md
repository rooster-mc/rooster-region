# Readability review — 030 (README, publishing, and consumption)

## Round 1
### Verdict
The README is well organised and quickly followable, the new snippet test matches
the existing test style, and the two `verify*` task additions read consistently
with the tasks they extend. I found two small clarity gaps — a README example
that presents non-mutating calls as bare statements, and a build-script local
named after the `mavenLocal()` repository concept — plus one phrasing nit I am
not raising as work.

### Findings
#### 1. The `enlarge`/`shrink` example calls the transforms as bare statements, which reads as mutation
- Location: `README.md:118-120`
- Problem: `region.enlarge(2, Face.EAST)` / `region.shrink(1, Axis.Y)` /
  `region.enlarge(1)` are shown as statements with comments that describe the
  result ("grows only the east face", "pulls both Y faces in by 1", "grows every
  face by 1"), in the same block as the `region.min`/`region.max` property reads
  above them. Because the calls are not bound to anything, they are no-ops, and
  the comments imply `region` changed — which the prose two lines later then
  contradicts ("return a new `Region` and never mutate the receiver"). A reader
  copying the snippet gets nothing happening and has to re-read to see why. The
  snippet test already models the clearer form (`ReadmeExamplesTest.kt:39-53`
  binds `east`/`y`/`all`).
- Suggested fix: bind each result to a local, mirroring the test, e.g.
  `val east = region.enlarge(2, Face.EAST)` and comment on the bound value, or
  keep one call and make the "returns a new `Region`" point visually explicit.

#### 2. The root task names its Maven directory `mavenLocal`, which reads as the repository function
- Location: `build.gradle.kts:18-22` (used at `:37-40`)
- Problem: `val mavenLocal = file(...)` holds a `File` directory, but
  `mavenLocal` is the well-known Gradle repository function (and the name the
  README uses for that repository). At a glance the local reads like a
  repository declaration rather than "the path under which installed POMs live".
  The nested `"${System.getProperty("user.home")}/.m2/repository"` quoting also
  makes the expression denser than it needs to be.
- Suggested fix: rename to something that states the role, e.g.
  `localMavenRepo`/`m2Repository`, and build the fallback path from parts
  (`File(System.getProperty("user.home"), ".m2/repository")`) to drop the
  nested string quotes.

### Non-findings
- **README structure is quickly followable.** Title → intro → `## Modules` table
  → `## Install` (`mavenLocal`, composite) → `## Usage` (`Region`, geometry,
  WorldEdit) → `## Publishing` → `## Development` is a natural read order. Every
  code block is fenced with a language (`sh`/`kotlin`), and each snippet's
  imports cover the symbols it uses (`README.md:93-96,130-138,160-165`). The
  `// build.gradle.kts` / `// settings.gradle.kts` file labels are consistent and
  helpful.
- **Phrasing is clear throughout.** `README.md:74` ("the same coordinates as the
  `mavenLocal` path") uses "path" for a section rather than a filesystem path,
  but the surrounding text leaves no real ambiguity, so I did not raise it.
  The snippets use `world` without defining it (`README.md:98,140`), which is
  conventional in Bukkit examples and not confusing.
- **New test is consistent with existing tests.** `ReadmeExamplesTest` extends
  `WorldTestSupport` like `GeometryTest`, uses backticked sentence names, imports
  only what it uses (`assertTrue`/`assertFalse`/`assertEquals` all exercised),
  has no comments, and its small `location(...)` helper removes repetition
  without hiding anything. The class name is broad (the README WorldEdit example
  is covered by MT-003, not here), but the test lives in `core` and the two
  examples it does cover are named after the README sections they mirror, so I
  did not raise a rename.
- **Gradle task additions read consistently with their siblings.**
  `verifyPublishedArtifacts` (`build.gradle.kts:11-53`) uses the same
  `group`/`description`/`doLast` shape and the same nested-helper style as
  `verifyCoreDependencies`; the extended `verifyWorldEditClasspath`
  (`worldedit/build.gradle.kts:51-120`) keeps its description accurate for the
  added POM check (`:53-55`) and places the new assertion before the existing
  classpath checks. Error messages name the artifact and the unexpected value.
  No dead locals, no unnecessary indirection.
- **Formatting is clean.** No trailing whitespace or tabs in `README.md` or
  `ReadmeExamplesTest.kt`; the new test's longest line is under the 100-column
  budget. The three lines over 100 in the build scripts
  (`build.gradle.kts:14,29`, `worldedit/build.gradle.kts:54`) are single-string
  description/regex lines, which ktlint's `MaxLineLengthRule` deliberately skips
  (`isLineOnlyContainingSingleTemplateString`); they match the pre-existing
  `core/build.gradle.kts:42` precedent, so this is not a formatter failure. The
  trailing commas in `file(...)`/`Regex(...)` are allowed because
  `.editorconfig` disables the trailing-comma rules.
- **Duplication observation — concur with architecture.** The POM-dependency
  regex now appears in `core/build.gradle.kts:51-59`,
  `worldedit/build.gradle.kts:63-71` and `build.gradle.kts:27-35`; as the
  architecture report says, extracting a shared build helper is not warranted
  for a two-module repo and is not this ticket's work.
- **Prior reports — concur.** I concur with `docs/reviews/030/correctness.md`
  (no findings) including its reading that "any WorldEdit region type works" is
  imprecise but not false; I do not re-raise it. I concur with
  `docs/reviews/030/architecture.md` (no findings) on the task placement, the
  composite mapping and the `AGENTS.md`/`docs/manual-test.md` state.
