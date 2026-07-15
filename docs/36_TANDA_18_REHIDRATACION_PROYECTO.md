# Tanda 18 — Rehidratación completa del proyecto

## Objetivo

Abrir un proyecto `.docupodcast.json` debe reconstruir la sesión editable cuando existen artefactos materializados en la carpeta del proyecto. La app ya no debe abrir solo metadata, voz y estado visual dejando documento, guion y storyboard en `null`.

## Cambios principales

- Se agregó `ProjectWorkspaceHydration` para representar los artefactos recuperados.
- Se agregó `LoadProjectWorkspaceArtifactsUseCase` como punto de entrada de aplicación.
- `ImportedDocumentWorkspaceRepository`, `NarrationScriptWorkspaceRepository` y `StoryboardWorkspaceRepository` ahora tienen método `load(Path projectFile)`.
- `ReadableDocumentWorkspaceRepository` carga `document/document.json`.
- `NarrationScriptWorkspaceFileRepository` carga `script/narration-script.json`.
- `StoryboardWorkspaceFileRepository` carga `storyboard/storyboard.json`.
- `ProjectSession` puede hidratar documento, guion y storyboard sin marcar dirty.
- `DocuPodcastShellViewModel.openProject(...)` usa la hidratación y reconstruye el manifest de playback si hay guion y jobs persistidos.

## Contrato de producto

```text
guardar proyecto
cerrar app
abrir proyecto
continuar con documento, guion y storyboard cargados si existen los snapshots materializados
```

Esta tanda no convierte todavía la validación en estricta: si falta un snapshot, la apertura continúa con el artefacto ausente. La validación dura de archivos físicos, rutas, checksums y payload incompleto queda para la Tanda 19.

## Tests agregados o ampliados

```text
LoadProjectWorkspaceArtifactsUseCaseTest
ProjectSessionHydrationTest
ReadableDocumentWorkspaceRepositoryTest
NarrationScriptWorkspaceFileRepositoryTest
StoryboardWorkspaceFileRepositoryTest
```

## Validación en este entorno

No se ejecutó Maven porque `mvn` no está instalado.

Sí se validó con `javac --release 21`:

```text
domain + application + infrastructure + ProjectSession
DocuPodcastShellViewModel con stubs JavaFX mínimos
tests nuevos/modificados con stubs JUnit
```

## Siguiente tanda recomendada

Tanda 19 — Validación fuerte del proyecto y assets.
