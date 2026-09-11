# Readability review — 010 (Core region, face, and geometry helpers)

## Round 1
### Verdict
The port is clean and followable: one class per file, no comments, no dead code,
and formatting is within the 100-column budget with no tabs or trailing
whitespace. Three naming/structure items are worth a pass — a misspelled public
helper, a boolean-flag private function whose call sites read as bare
`true`/`false`, and a zero-vararg overload pair that forces `*emptyArray<Face>()`
incantations at call sites.

### Findings
#### 1. Public helper `closesDistanceTo` is misspelled and confusable with `closestDistanceToAxis`
- Location: `core/src/main/kotlin/dev/rooster/region/Region.kt:267`
- Problem: `closesDistanceTo` reads as the verb "closes", not the adjective
  "closest", and it sits two definitions away from the correctly spelled
  `closestDistanceToAxis` (`Region.kt:274`). The two near-identical names
  describe different measurements (nearest defining corner vs. signed offset on
  an axis), so the typo makes the pair harder to tell apart at a glance. This is
  a permanent public API name in a library whose point is a clean extraction.
- Suggested fix: Rename to `closestDistanceTo` and update the two call sites in
  `RegionTest.kt:346-347`. The source of truth carries the same typo, so record
  the rename as a deliberate deviation from the "keep the public shape close"
  note; the library is unpublished, so no consumer churn is at risk.

#### 2. `changeBorders`'s `enlarge` flag shadows the method name and is passed positionally as `true`/`false`
- Location: `core/src/main/kotlin/dev/rooster/region/Region.kt:188-206`
  (declaration at `:202`; calls at `:190`, `:193`, `:197`, `:200`)
- Problem: the four call sites read `changeBorders(amount, true, ...)` and
  `changeBorders(amount, false, ...)`, so a reader has to jump to the signature
  to learn what the boolean means. The parameter is named `enlarge`, which
  shadows the public `enlarge(...)` methods, so inside the body `if (enlarge)`
  and `if (enlarge xor ...)`-style reads look like a reference to the method
  rather than the flag.
- Suggested fix: Rename the parameter to something unambiguous (`growing`,
  `expand`) and pass it by name at the call sites (`enlarge = true`), or split
  the private helper into `expandBorders`/`contractBorders`. It is private, so
  there is no API cost.

#### 3. Zero-vararg overload ambiguity forces `*emptyArray<Face>()` at call sites
- Location: `core/src/main/kotlin/dev/rooster/region/Region.kt:188-200`;
  `core/src/test/kotlin/dev/rooster/region/RegionTest.kt:202,210`
- Problem: `enlarge(amount, vararg axes: Axis)` and
  `enlarge(amount, vararg faces: Face)` are both applicable when no vararg is
  supplied, so the natural `region.enlarge(1)` does not compile. The tests
  therefore have to write `region.enlarge(1, *emptyArray<Face>())`, which reads
  as an incantation and hides the "every face" intent the empty vararg encodes.
  A reader has to know the overload-resolution subtlety to parse the line.
- Suggested fix: Add a one-argument overload
  `fun enlarge(amount: Int): Region = changeBorders(amount, true)` and the
  matching `shrink(amount: Int)`. It is additive and source-compatible, resolves
  the ambiguity in favour of the intended call, and lets the tests say
  `region.enlarge(1)`.

### Non-findings
- **Formatting is clean.** No line exceeds 100 columns in main or test sources,
  there are no tabs, no trailing whitespace, and every file ends with a newline.
  `Face.kt`'s trailing comma and the multiline `dimensions`/`Location(...)`
  constructions are consistent with the `.editorconfig` (trailing-comma rules
  disabled, max line 100).
- **Comments.** None in main or test sources, matching the "no comments unless a
  non-obvious why" rule; the message in `Region.kt:22` is user-facing validation,
  not a comment.
- **File hygiene and structure.** One responsibility per file
  (`Region.kt`, `Face.kt`, `util/Geometry.kt`, `util/Vector3dMath.kt`), names
  match their contents, no dead code, and the `util`→`Region` import for
  `Box.region` is prescribed by `docs/architecture.md`.
- **`Math.floorDiv` vs Kotlin's `floorDiv` (`Region.kt:62-65`).** Java-style but
  unambiguous and correct; not worth churn.
- **Redundant `this.` qualifiers (`Region.kt:249-251`, `Geometry.kt:9,18-23,25-30`).**
  Noise-level only; the receivers are obvious from context.
- **`isCorner`/`isEdge`/`isFace` magic counts (`Region.kt:241-245`).** The method
  names document the 3/2/1 mapping, so the bare literals do not slow a reader.
- **`chunks`/`chunksFull` duplicate their double loop (`Region.kt:156-177`).** A
  helper could unify them, but both are short and adjacent; not worth the
  indirection.
- **`@Suppress("unused")` on `Region` (`Region.kt:16`).** Source-faithful and
  harmless for a public-API class.
- **Prior findings — concur.** I have no dissent from the tester, correctness, or
  architecture findings. None are within the readability column (test
  assertions, math/API semantics, module boundaries/doc staleness), so I do not
  re-report them; in particular I agree with correctness's note that
  `closesDistanceTo` is source-faithful, which is why finding 1 frames the typo
  as a naming decision rather than a defect in the port.

## Round 2
### Verdict
All three round-1 readability findings are resolved in `f3ce7ac`: the misspelled
helper is now `closestDistanceTo`, `changeBorders` no longer takes an ambiguous
boolean (the `expandBorders`/`contractBorders` split names the direction), and
one-argument `enlarge(Int)`/`shrink(Int)` overloads let call sites drop the
`*emptyArray<Face>()` incantation. The round-2 edits are formatting-clean and add
no new readability issues, so I have no new findings.

### Findings
No new findings. The changed code is followable, the new overloads read cleanly,
and formatting still conforms: no line over 100 columns, no tabs, no trailing
whitespace, no comments, and every file ends with a newline.

### Non-findings
- **Round-1 finding 1 resolved.** `closesDistanceTo` is renamed to
  `closestDistanceTo` (`core/src/main/kotlin/dev/rooster/region/Region.kt:277`),
  now consistent with `closestDistanceToAxis`, and the two test call sites and the
  test name are updated (`RegionTest.kt:379,382-383`). I concur with architecture
  round-2 finding 1 that the deliberate rename should be recorded in a non-review
  doc; that record is architecture's scope, so I do not re-report it.
- **Round-1 finding 2 resolved.** `changeBorders` now takes a signed `shift: Int`
  (`Region.kt:213`) with no boolean flag, and the direction is named by
  `expandBorders`/`contractBorders` (`Region.kt:207-211`); the `enlarge`/`shrink`
  overloads call those directly, so no call site reads as a bare `true`/`false`.
- **Round-1 finding 3 resolved.** The one-argument `enlarge(amount: Int)`
  (`Region.kt:189`) and `shrink(amount: Int)` (`Region.kt:198`) overloads
  disambiguate the zero-vararg case, and the test now calls `region.enlarge(1)` /
  `region.shrink(1)` (`RegionTest.kt:207,215`) instead of
  `*emptyArray<Face>()`.
- **Formatting and hygiene unchanged.** No line exceeds 100 columns in main or
  test sources, there are no tabs or trailing whitespace, every file ends with a
  newline, and no comments were added. `import kotlin.math.absoluteValue` sits
  last in `Region.kt:15`, consistent with ktlint's trailing `kotlin` import group.
- **New code reads cleanly.** The signed `shift` is applied once per face via
  `faceShift` (`Region.kt:219`), the three overload families sit adjacent in
  `enlarge`/`shrink` order, and nothing new is indirect or dead.
- **Prior reports — concur.** I concur with tester round-2 finding 1 (the new
  `entities` exclusion test is vacuous under MockBukkit's max-exclusive query
  box) and correctness round 2 (no new findings); both are outside the
  readability column, so I do not re-report them. I concur with architecture
  round-2 finding 1 on the unrecorded rename and leave the recording to that
  scope.
