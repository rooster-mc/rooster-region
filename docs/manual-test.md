# Manual test gate

Acceptance criteria that cannot be automated, owned by the ticket that
introduced them. A ticket may be `done` with an `unverified` entry, but the entry
must exist (see `docs/workflow.md` → Manual gate).

Status: `unverified` | `passed` | `failed`.

| Id | Ticket | Check | Status |
|---|---|---|---|
| MT-001 | 020 | On a real server with FastAsyncWorldEdit (or WorldEdit), make a selection, call `Player.worldEditSelection()`, and confirm it converts to the expected `Region`; repeat with no selection and confirm `null`. | unverified |
| MT-002 | 010 | Confirm `Region.blocks`/`contains`/`enlarge` behave on a live world with real chunks (MockBukkit covers the pure math only). | unverified |

## How to run

These require a Paper server with a WorldEdit implementation loaded. There is no
run server in this library repo; verify from a consumer plugin's dev server.
