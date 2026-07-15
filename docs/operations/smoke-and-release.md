# Smoke tests and release

## Local validation

1. Run `mvn -q test`.
2. Run `mvn clean package`.
3. Audit local runtimes.
4. Start a fresh application instance.
5. Open one official example in each category.
6. Verify document selection, audio playback and project save/reopen.
7. Export Documentary, Narrative and Theatre video.
8. Inspect SideDocks, dialogs, scroll and canvas layout at common desktop sizes.

Useful scripts include:

- `scripts/13-revalidacion-local-completa.bat`
- `scripts/19-smoke-motores-reales.bat`
- `scripts/38-smoke-video-final.bat`
- `scripts/99-diagnostico-completo.bat`

Packaging scripts under `scripts/14-*`, `15-*` and `16-*` create app-image, MSI and release-candidate outputs. Third-party manifests must be generated and reviewed before distribution.

An application instance launched from old `target/classes` must be closed before validating a new build.

