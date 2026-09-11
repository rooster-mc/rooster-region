# Manual test gate

Acceptance criteria that cannot be automated, owned by the ticket that
introduced them. A ticket may be `done` with an `unverified` entry, but the entry
must exist (see `docs/workflow.md` → Manual gate).

Status: `unverified` | `passed` | `failed`.

| Id | Ticket | Check | Status |
|---|---|---|---|
| MT-001 | 020 | On a real server with FastAsyncWorldEdit (or WorldEdit), make a selection, call `Player.worldEditSelection()`, and confirm it converts to the expected `Region`; repeat with no selection and confirm `null`. | unverified |
| MT-002 | 010 | On a live server, confirm the fidelity-sensitive Bukkit members (`blocks`/`blocksArray`/`contains`/`entities`/`players`/`chunks`/`chunksFull`) behave with real chunks, block states and entity tracking (MockBukkit drives them against a mock world). | unverified |

## How to run

MT-001 requires a Paper server with a WorldEdit implementation loaded; MT-002
only needs a Paper server. There is no run server in this library repo; verify
from a consumer plugin's dev server.
