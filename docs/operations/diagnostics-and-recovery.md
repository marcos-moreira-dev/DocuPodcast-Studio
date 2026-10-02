# Diagnostics, support and job recovery

All observability is local. The launcher installs Logback as the runtime provider while production code depends only on SLF4J.

## Logging

Logs are written to `~/.docupodcast-studio/logs` by default. Override the directory with `-Ddocupodcast.log.dir=<path>` and the root level with `-Ddocupodcast.log.level=DEBUG` for a diagnostic run.

The active file is `studio.log`. Rotation is daily and also occurs at 10 MB; retention is 14 days with a 200 MB total cap. MDC may carry `operationId`, `jobId`, `capabilityId`, `engineId`, `projectId`, `workspace` and `actionId`.

Never log full prompts, document contents, transcripts, credentials, sensitive query strings or personal absolute paths. An error is logged once at the boundary that translates it into an application result.

## Support bundle

The support exporter creates a ZIP locally and never uploads it. It may contain:

- application/runtime versions and engine readiness;
- non-sensitive configuration and structured job metrics;
- job manifests and recent text logs;
- an omissions manifest explaining files that were excluded.

It excludes projects, source documents, prompts, generated media, models, weights and runtimes. Home and project roots are replaced with `<HOME>` and `<PROJECT>`. Input is bounded and only approved text formats are copied.

## Normal shutdown

Shutdown participants run in this order:

1. reject new jobs;
2. request cancellation;
3. wait no more than five seconds;
4. persist job/session state;
5. close ink providers, managed runtimes, executors and logging.

Every participant must be idempotent. A failure is recorded and does not prevent the remaining participants from closing.

## Abrupt-close recovery

At startup, neutral generation jobs in `RUNNING` or `STAGING` are reconciled to `INTERRUPTED` and marked recoverable. Legacy jobs remain immutable. Cleanup is confined to the staging directory owned by the job; valid resumable partials are retained. Retrying a legacy job reconstructs a neutral request and creates a new job ID.

## Local support procedure

1. Reproduce once with diagnostic logging enabled only if needed.
2. Export the support ZIP from the diagnostic action.
3. Inspect `diagnostics.txt` and `omitted.txt` before sharing.
4. Confirm that no project or source content was included.
5. Share the ZIP manually through the support channel selected by the user.

## Public operational entrypoints

- `scripts\00-diagnosticar.bat` reports toolchain, reactor, roots and required helpers.
- `scripts\06-exportar-diagnosticos.bat` writes only under `target/diagnostics` and includes the read-only parity report.
- `scripts\04-smoke-capacidades-reales.bat` executes real media only in the current workspace. A reference tree is never used as a working directory.

The complete catalog is documented in [`scripts/README.md`](../../scripts/README.md).
