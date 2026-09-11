# Review reports

One directory per ticket, one file per review role:

```
docs/reviews/<ticket-id>/
  tester.md
  correctness.md
  architecture.md
  readability.md
  ux.md
```

- Each reviewer owns exactly one file and appends a `## Round <n>` section.
- A reviewer reads the earlier reports it is given and does not re-report their
  findings (concur or dissent instead).
- Findings carry no severity labels; every finding is fixed or explicitly
  deferred to a named ticket/gate with a reason.
- Format and pipeline: `docs/workflow.md`.
- Roles are omitted when irrelevant, so a directory need not contain all files.
