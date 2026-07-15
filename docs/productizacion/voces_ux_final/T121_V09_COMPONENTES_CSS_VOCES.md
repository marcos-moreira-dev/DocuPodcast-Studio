# T121-V09 — Componentes GUI y CSS para Voces

## Objetivo

Crear o reutilizar componentes transversales para que la vista Voces no sea JavaFX suelto ni CSS heredado.

## Componentes sugeridos

- `VoiceProfileCard`
- `VoiceSampleRow`
- `VoiceToneBadge`
- `VoiceEngineModeSelector`
- `VoiceGeneratedTestPanel`
- `VoiceRecordingWizardView`
- `VoiceSampleActionBar`
- `VoiceSampleDownloadDialog`
- `DestructiveActionDialog`

## Componentes existentes a reutilizar

- `ActionButtonFactory`
- `SectionHeader`
- `InfoBadge`
- `MetricBadge`
- `DiagnosticCard`
- `EmptyStateView`
- `ActionBar`

## CSS

Archivos sugeridos:

```text
css/components/voice-cards.css
css/components/voice-samples.css
css/workspaces/voice-library.css
```

Si se conserva un archivo único, debe estar organizado por secciones y no crecer sin control.

## Tokens

Usar tokens globales ya existentes. Si faltan, crearlos en CSS de tokens, no hardcodear colores en Java.

## Corrección de warnings CSS

La consola mostró warnings de JavaFX relacionados con:

- `-dp-text`
- `-dp-primary`
- `-dp-muted`
- `examples-dialog`
- `example-project-card`
- `settings-engine-status-badge`

Se recomienda una tanda hotfix:

```text
T120D-HF1 — Corrección CSS warnings JavaFX
```

antes o junto con el rediseño visual.

## Tests recomendados

- `NoInlineStyleVoiceLibrarySourceTest`
- `VoiceLibraryUsesReusableComponentsSourceTest`
- `NoLegacyScriptCssInVoiceLibrarySourceTest`
- `CssTokensResolveForVoiceLibrarySourceTest`

## Criterios de aceptación

- Sin estilos inline.
- Sin CSS heredado de script.
- Sin warnings CSS por tokens faltantes.
- Componentes reutilizables.
