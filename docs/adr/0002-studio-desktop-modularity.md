# ADR 0002: keep `studio-desktop` physically intact during tranche three

- Status: accepted for tranche three
- Date: 2026-07-20

## Context

The logical boundaries of Documentary, Theatre, Narrative, shared presentation, application and infrastructure are clearer than the physical Maven boundary of `studio-desktop`. The refactor plan requires a measured decision and explicitly forbids adding more Maven modules during this tranche.

## Measurement snapshot

The source snapshot at the start of tranche three shows:

- direct presentation imports: Document -> Theatre 6, Document -> Narrative 1, Theatre -> Document 2, Theatre -> Narrative 0, Narrative -> Document/Theatre 0;
- five primary composition classes above 1,500 non-empty lines; the shell view model is about 2,966 and the technical-problem dialog about 2,498 after the first extraction;
- `studio-desktop` still requires 16 JPMS modules and exports only its root and bootstrap packages;
- a targeted desktop compilation currently recompiles roughly 1,272 production sources and 347 test sources;
- normal architecture tests pass, but the remaining cross-product imports and unstable controller surface mean a new physical API would either export implementation packages or duplicate transversal infrastructure.

These figures are reproducible with `rg`, line counts and Maven compiler output. They are architectural inputs, not quality targets by themselves.

## Decision

Do not split `studio-desktop` in tranche three. First remove product cycles/imports, replace the shell facade with stable controller APIs and narrow JPMS requirements. A future module candidate is accepted only when all of these hold:

1. its package graph is cycle-free;
2. its API has contract tests and no provider/runtime names;
3. it reduces JPMS dependencies or measured build/test time;
4. it does not require exporting internal persistence or JavaFX implementation packages;
5. transversal ink, media administration, side-docks and operability remain single implementations.

## Consequences

Logical package rules and constructor injection remain the enforcement mechanism for this tranche. Compilation time will not improve merely through a new Maven boundary, but the refactor avoids freezing unstable APIs. Measurements must be repeated after the shell/controller cutover; only then may a separate ADR propose physical modules.
