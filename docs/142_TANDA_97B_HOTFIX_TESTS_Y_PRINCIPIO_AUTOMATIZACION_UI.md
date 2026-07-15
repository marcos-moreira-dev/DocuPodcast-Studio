# Tanda 97B — Hotfix tests y principio de automatización UI

Tanda correctiva sobre T97.

## Cambios

- Alinea `DocuPodcastProjectFileRepositoryTest` con `formatVersion = 2`.
- Limpia `T90G_SMOKE_MODULAR_MOTORES.md` para no reintroducir `Audio a texto` como etiqueta visible.
- Documenta el principio de automatización asistida de pasos previos: una acción principal debe poder preparar requisitos obvios con confirmación humana.

## Validación focal

- `DocuPodcastProjectFileRepositoryTest.writtenJsonContainsStableTopLevelSections`
- `ModularRealEnginesSmokeT90GSourceTest.t90gProvidesModularSmokeScriptsAndRunsOnlySelectedEngines`

## Siguiente paso

T98 — Contrato GUI con el usuario antes del rediseño fuerte.
