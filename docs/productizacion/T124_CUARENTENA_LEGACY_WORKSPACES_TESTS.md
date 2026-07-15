# T124 — Cuarentena legacy workspaces/tests

## Objetivo

T124 separa de forma explícita las superficies reales del producto de las superficies heredadas que todavía pueden existir en código por compatibilidad. La regla de producto queda fija: DocuPodcast Studio solo debe navegar como producto a Inicio, Documento y Voces. La preparación interna de lectura, los procesos de audio y la secuencia visual no deben competir como workspaces principales.

## Contexto

Después de T114-HF1, T114-HF2 y T123-CAT01, el producto ya está alineado con estas reglas:

- DocuPodcast abre documentos, no guiones.
- Markdown entra como documento fuente.
- Whisper/STT no pertenece al producto.
- Guion no es categoría visible del usuario.

Sin embargo, el código aún conserva clases históricas como `ScriptWorkspaceView`, `AudioWorkspaceView`, `StoryboardWorkspaceView` y `MainToolbarView`. No conviene borrarlas a ciegas en esta tanda porque varios tests históricos y compatibilidad interna todavía las inspeccionan. Lo correcto en esta etapa es ponerlas en cuarentena: pueden existir, pero no pueden ser superficies primarias ni persistirse como vista activa.

## Cambios de la tanda

- `WorkspaceSurfacePolicy` ahora expone explícitamente `isLegacyInternalSurface(...)` como nombre de producto para distinguir superficies heredadas.
- `WorkspaceNavigationCoordinator` ya normaliza cualquier workspace heredado hacia `DOCUMENT_READER`.
- `DocuPodcastShellViewModel.showPlaceholder(...)` usa el workspace resuelto, no el solicitado, para evitar mensajes que aparenten activar superficies internas.
- `WorkspaceDescriptorCatalog` refuerza que `SCRIPT_EDITOR`, `AUDIO_JOBS` y `STORYBOARD` son superficies internas heredadas y no navegación primaria.
- Los comandos heredados de storyboard se reetiquetan hacia lenguaje de visuales/secuencia visual cuando son accesos de compatibilidad.
- La Configuración usa “Visuales / video” en lugar de “Storyboard / video”.
- La guía categoriza el tópico visual como “Visuales”.
- Se agrega `LegacyWorkspaceQuarantineT124SourceTest`.

## Superficies de producto

Las únicas superficies principales del producto son:

- `WELCOME_HOME` — Inicio.
- `DOCUMENT_READER` — Documento.
- `VOICE_LIBRARY` — Voces.

## Superficies heredadas en cuarentena

- `SCRIPT_EDITOR` — preparación interna heredada; no es vista de producto.
- `AUDIO_JOBS` — procesos internos; deben ir hacia overlay/panel de progreso, no workspace.
- `STORYBOARD` — secuencia visual interna; el usuario la ve como rail/panel visual dentro del Documento, no como módulo principal.

## Reglas para siguientes tandas

- No registrar `SCRIPT_EDITOR`, `AUDIO_JOBS` ni `STORYBOARD` en `DocuPodcastShellView.initialiseWorkspaces()`.
- No hacer `activeWorkspace.set(...)` directo hacia superficies legacy.
- No guardar `activeWorkspace` como superficie legacy.
- Si un proyecto antiguo trae una superficie legacy, debe restaurarse como `DOCUMENT_READER`.
- No reetiquetar Storyboard como pestaña principal hasta la tanda de Ribbon final; de momento se trata como visuales internos.

## Validación esperada

Ejecutar:

```bat
scripts\99-diagnostico-completo.bat
```

Esperado: Maven compile OK, Maven tests OK, smoke automático cerebro OK, preflight arranque motores OK y Piper/FFmpeg locales OK o reporte humano de preparación.
