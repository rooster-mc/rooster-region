# Tasks

Tickets for this repository. Each ticket is one unit that goes through the
review pipeline in `docs/workflow.md`.

## Index

| Id | Title | Parent | Depends on | Status |
|---|---|---|---|---|
| [000](000-project-setup.md) | Gradle setup for core and worldedit modules | MVP | — | done |
| [010](010-core-region.md) | Core region, face, and geometry helpers | MVP | 000 | done |
| [020](020-worldedit-adapter.md) | WorldEdit adapter module | MVP | 010 | done |
| [030](030-docs-and-publish.md) | README, publishing, and consumption | MVP | 000, 010, 020 | done |
| [040](040-blockpos-and-blockat.md) | BlockPos and Region.blockAt | MVP | 010 | done |
| [050](050-world-scoped-selection.md) | World-scoped worldEditSelection | MVP | 020 | todo |

## Template

```markdown
---
name: Human readable title
status: todo
parent: MVP
depends-on: []
reviewers: [tester, correctness, architecture, readability]
---

## Goal
What this ticket delivers, in one or two sentences.

## Scope
- Bullet list of what is included.

## Acceptance criteria
- Verifiable statements.

## Out of scope
- Explicit non-goals.

## Notes
- Decisions, references, gotchas.
```
