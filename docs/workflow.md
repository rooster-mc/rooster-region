# Workflow

How work is planned, executed, reviewed and committed in this repository.

## Roles

Two orchestrator tiers, then workers. Nesting is two levels deep; opencode's
`subagent_depth` defaults to `1`, so this repo sets it to `2` in
`.opencode/opencode.json`.

| Agent | Tier | Job | Reports only on |
|---|---|---|---|
| `orchestrator` | primary | Owns the queue, picks unblocked tickets, manages branches/worktrees, launches one ticket-orchestrator per ticket. Never implements or reviews. | — |
| `ticket-orchestrator` | subagent | Runs one ticket end to end by delegating to the workers. The only agent that may spawn them. | — |
| `implementor` | worker | Implements the ticket and its tests. The only agent that edits source. | — |
| `tester` | worker | Test quality (missing, excessive, brittle) and test-environment fidelity. | test files and harness limits only |
| `correctness` | worker | Bugs, wrong behaviour, spec mismatches, API/contract fit. | logic, math, API, integration only |
| `architecture` | worker | Module boundaries, extendability, and doc staleness. | module/package structure and docs only |
| `readability` | worker | Clarity, file hygiene, formatting, followability. | source structure/naming/format only |
| `ux` | worker | Player-facing loop and feedback. Omit for a pure library. | loop, feedback truthfulness, discoverability only |

Scopes are **exclusive**: a reviewer reports only within its column.

Subagents cannot spawn subagents unless their agent config declares a `task`
permission; `ticket-orchestrator` does, scoped to the workers. Every reviewer
owns exactly one report file, bound to its role (see [Review reports](#review-reports)).

## Meta-orchestration

Run by `orchestrator` (primary):

- Pick unblocked tickets: `status: todo` whose `depends-on` are all `done`.
- Launch one `ticket-orchestrator` subagent per ticket. Run several in parallel
  when they are file-disjoint; serialize or isolate in worktrees otherwise.
- Merge finished ticket branches back in dependency order and update the queue.

## Per-ticket pipeline

Run by `ticket-orchestrator`:

1. Read the ticket and its `reviewers` list.
2. `implementor` implements it. Keep its `task_id`.
3. For each reviewer in order, spawn it with the ticket id, the round number,
   and the paths of every earlier report in this round. It writes its own report.
4. Hand **all** reports to `implementor`. Every finding must be fixed, or
   explicitly deferred to a named ticket/gate with a reason.
5. **Commit** the resulting state.
6. Round 2: **resume** the same sessions via `task_id` and repeat steps 3–5.
7. Record any acceptance criterion that cannot be automated in
   `docs/manual-test.md` (see [Manual gate](#manual-gate)).
8. Mark the ticket `done` and commit.

Two rounds total per ticket. Omit stages that do not apply, but keep the order
of those that remain.

## Reviewer handoff (statefulness)

- The ticket-orchestrator passes the paths of every earlier report in the round
  into each reviewer's prompt.
- A reviewer must **not** re-report a prior finding. It may **concur** or
  **dissent**, and may add findings only within its own scope.
- Findings carry **no severity labels**. A finding is work: fixed, or deferred
  with a named target and a reason.

## Manual gate

Some criteria cannot be automated (a real WorldEdit selection, an in-game
check). Those go in `docs/manual-test.md`, each with a status and, when
unverified, a reason.

- A ticket may be `done` with an unverified manual entry, but the entry must
  exist and name the ticket.
- `tester` flags paths the harness cannot exercise (e.g. WorldEdit sessions
  under MockBukkit); those become manual-test entries.
- The `orchestrator` surfaces open entries when reporting a phase done.

## Verification ownership

- The **implementor** is the only agent that runs the build, the test suite, and
  the formatter. It must leave the tree green and report the exact commands.
- Reviewers do **not** run long or slow tools. They assume a green tree.
- If a reviewer suspects a failure, it states the suspicion and scenario; the
  ticket-orchestrator sends it to the implementor to reproduce.
- Quick read-only inspection (reading files, `git diff`) is fine.

## Commits

- Conventional commits: `type(domain): description`, imperative, lowercase,
  ≤72 chars, no trailing period.
- Types: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `perf`, `revert`.
- One commit per unit of completed work; stage precisely, never `git add -A`.
- No authorship or tool trailers. Never push unless asked.

## Ticket format

Tickets live in `docs/tasks/` as `NNN-slug.md` with YAML frontmatter:

```markdown
---
name: Human readable title
status: todo | in-progress | review | done | blocked | backlog
parent: MVP
depends-on: [000]
reviewers: [tester, correctness, architecture, readability]
---

Body: goal, scope, acceptance criteria, out of scope, notes.
```

## Review reports

Each reviewer writes to exactly one file, bound to its role:
`docs/reviews/<ticket-id>/<role>.md`, appending a `## Round <n>` section.

```markdown
# <Role> review — <ticket id> (<short title>)

## Round 1
### Verdict
One or two sentences.

### Findings
#### 1. <title>
- Location: path:line
- Problem: what is wrong and why it matters
- Suggested fix: concrete direction

### Non-findings
Checked and fine, and any prior finding you concur with.
```

No severity labels: a finding is work. The reviewer also returns a one-paragraph
summary in its reply.

## Definition of done for a ticket

- Acceptance criteria met.
- Build, tests and lint pass.
- Both review rounds completed; every finding fixed or explicitly deferred.
- Every non-automatable criterion recorded in `docs/manual-test.md`.
- Ticket status set to `done` and committed.
