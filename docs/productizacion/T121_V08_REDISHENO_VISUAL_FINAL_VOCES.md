# T121-V08 — Rediseño visual final de Voces

## Objetivo

Cerrar la estructura visual de la vista **Voces** como pantalla de producto: moderna, limpia y sobria, centrada en preparar voces y muestras, no en asignar fragmentos ni exponer nombres técnicos de motores.

## Cambios implementados

- `VoiceLibraryWorkspaceView` agrega un hero de producto con el contrato: **Documento asigna · Voces prepara**.
- Se agrega sección **Modo activo y alcance** con tarjetas estilizadas para el modo de voz actual, muestras y asignación.
- Se reemplaza el lenguaje de wizard por **Registro de muestras por tono**.
- El detalle de voz seleccionada muestra una matriz de muestras registradas por tono.
- Cada muestra se muestra como fila visual con badge de tono, destacando neutral como obligatoria.
- Se conservan componentes transversales: `ActionButtonFactory.primary` y `ActionButtonFactory.secondary`.
- No se introducen nombres técnicos de motores en la GUI: la interfaz mantiene **Voz IA avanzada**, **Voz local simple** y **Modo de prueba**.

## CSS

Se agregan clases en `voice-library.css`:

- `voice-library-hero`
- `voice-library-eyebrow`
- `voice-mode-overview`
- `voice-engine-card`
- `voice-engine-card-active`
- `voice-sample-grid`
- `voice-sample-row`
- `voice-tone-badge`
- `voice-tone-badge-required`

## Tests

- `VoiceLibraryFinalRedesignT121V08SourceTest`
- Corrección de tests fuente heredados para que validen `VoiceReferenceTone` en Documento en vez de `PerformanceStyle`.
- Corrección de tests que todavía buscaban lógica de muestras/pruebas directamente en `DocuPodcastShellViewModel` después de la extracción a `VoiceSampleWorkflowCoordinator`.

## Próxima tanda

T121-V09 — Componentes/CSS de Voces.
