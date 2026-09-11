# AGENTS.md

Operating manual for agents working in `rooster-region`.

## What this is

A standalone Kotlin library for Bukkit 3D-space regions, plus an optional
WorldEdit adapter. Extracted from the region code in `rooster-core` and the
WorldEdit bridge in `rooster-monolith/worldedit` / `BuildPluginV4/worldedit`.

Read these before working:

- `docs/design.md` — goal, scope, decisions, naming.
- `docs/architecture.md` — module layout and seams.
- `docs/workflow.md` — the review pipeline and report/ticket formats.
- `docs/manual-test.md` — the gate for non-automatable criteria.
- `docs/tasks/` — the tickets.

## Stack

Kotlin `2.4.20`, Java `21`, Gradle Kotlin DSL. Paper API `1.21.4`
(`compileOnly`), `joml`, WorldEdit API (`compileOnly`, in the `worldedit`
module), JUnit 5 + MockBukkit. Full rationale in `docs/design.md`.

## Commands

```sh
just build      # assemble both modules
just test       # JUnit suite
just format     # ktlint
just publish    # publishToMavenLocal
```

## Orchestration model

Two orchestrator tiers, then workers:

- **Meta-orchestrator** (`orchestrator`, primary): owns the ticket queue, picks
  unblocked tickets, manages branches/worktrees, and launches one
  **ticket-orchestrator** per ticket. It never implements or reviews.
- **Ticket-orchestrator** (subagent): runs one ticket end to end by delegating.
  It is the only agent allowed to spawn the workers.
- **Workers** (subagents): `implementor`, `tester`, `correctness`,
  `architecture`, `readability`, `ux` (ux is omitted for this pure library).

Nesting is two levels deep: `orchestrator` → `ticket-orchestrator` → worker.
`subagent_depth` is `2` in `.opencode/opencode.json`, and `ticket-orchestrator`
declares a `task` permission (subagents otherwise get `task` denied).

Rules:

- `implementor` is the only agent that edits source.
- Reviewers are read-only except for their own report file,
  `docs/reviews/<ticket-id>/<role>.md`; they never touch source.
- Per ticket: implement, then run the ticket's `reviewers` in order, hand the
  reports back to `implementor`, commit, and repeat for a second round.
- Reviewers are stateful: each is given the earlier reports for the round and
  must not re-report their findings. Round 2 resumes the same sessions.
- Findings carry no severity labels; a finding is work — fixed, or deferred to a
  named ticket/gate with a reason.
- Omit reviewers that are irrelevant; keep the order of those that remain.
- Only `implementor` runs slow verification (build, `just test`, `just format`)
  and must leave the tree green. Reviewers read and reason.
- Non-automatable acceptance criteria are recorded in `docs/manual-test.md`; the
  meta-orchestrator surfaces open entries when reporting a phase done.
- Parallel tickets must be file-disjoint; otherwise the meta-orchestrator
  serializes them or isolates them in separate worktrees.

## Git

- You own the repository: branches, worktrees, staging.
- Conventional commits: `type(domain): description`, imperative, lowercase,
  ≤72 chars, no trailing period, no tool/authorship trailers.
- Stage precisely; never `git add -A`.
- Commit after each completed work unit. Never push unless asked.

## Conventions

- No code comments unless they explain a non-obvious *why*.
- `core` must stay free of Rooster/Exposed/WorldEdit/Adventure dependencies;
  WorldEdit lives only in the `worldedit` module.
- Update docs in the same commit as the change that invalidates them.
- Format with ktlint per `.editorconfig`.

## Current status

Tickets 000 (two-module Gradle setup), 010 (core `Region`, `Face` and geometry
helpers) and 020 (WorldEdit adapter) are done. Ticket
[`docs/tasks/030-docs-and-publish.md`](docs/tasks/030-docs-and-publish.md), the
README, publishing and consumption work, is implemented and in review. It is the
last ticket in the MVP queue; once it is done there is no next ticket.
