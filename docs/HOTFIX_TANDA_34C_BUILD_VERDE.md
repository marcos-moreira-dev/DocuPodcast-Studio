# Hotfix Tanda 34C — Build verde y tests fuente alineados

## Objetivo

La Tanda 34B corrigió el error de compilación causado por el accessor `updateSegmentText()`. Después de esa corrección el proyecto ya compila código productivo y tests, pero Maven quedó rojo por seis tests fuente/UX desalineados con el contrato real del código.

Esta tanda es un hotfix quirúrgico para dejar el proyecto en verde sin introducir funcionalidades nuevas.

## Alcance aplicado

Se actualizaron los tests fuente para validar el contrato vigente en vez de nombres o ubicaciones históricas:

- `AudioPlaybackManifestUiSourceTest`: ahora valida `playbackCues`, `state.playbackCues()` y los métodos de sincronización del `DocuPodcastShellViewModel`.
- `ExportBundleAuditSourceTest`: ahora valida `reports/BUNDLE_FILE_INDEX.tsv`, `MANIFEST_EXPORTACION.md`, `README_EXPORTACION.md`, `BundleArtifactMetadata`, SHA-256 e índice auditable vigente.
- `StoryboardPlaybackSyncSourceTest`: ahora valida que el estado activo vive en `StoryboardScenePresentation.cardStateCssClass()` y que el CSS contiene `storyboard-scene-active`.
- `VoiceSampleImportUiSourceTest`: ahora valida que la acción contextual `Importar voz` vive en `WorkspaceToolbarActionProvider`, no hardcodeada en `MainToolbarView`.

También se agregó un microcopy visible en `VoiceLibraryWorkspaceView`:

> Muestra de voz: importa o graba únicamente voces propias o autorizadas. La muestra queda como referencia/capacidad del motor, no como promesa de clonación automática.

Este microcopy mejora la UX sin prometer clonación ni síntesis emocional garantizada.

## Archivos modificados

```text
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java
src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/audio/AudioPlaybackManifestUiSourceTest.java
src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/export/ExportBundleAuditSourceTest.java
src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/storyboard/StoryboardPlaybackSyncSourceTest.java
src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleImportUiSourceTest.java
docs/HOTFIX_TANDA_34C_BUILD_VERDE.md
```

## Fuera de alcance

No se modificaron:

```text
ExportPodcastWavUseCase
PcmWavConcatenator
FileSystemProjectBundleExporter
ProjectBundleExportResult
AudioWorkspaceView
StoryboardWorkspaceView
WorkspaceToolbarActionProvider
DocuPodcastShellViewModel
```

## Validación realizada en este entorno

No se ejecutó Maven porque este entorno no tiene `mvn` instalado.

Sí se validó mediante auditoría estática que las cadenas exigidas por los tests actualizados existen en las fuentes correspondientes, y se verificó la integridad del ZIP generado con `unzip -t`.

## Validación pendiente en Windows

Desde la carpeta `scripts`:

```bat
.\02-ejecutar-tests.bat
```

Luego:

```bat
.\01-ejecutar-app.bat
```
