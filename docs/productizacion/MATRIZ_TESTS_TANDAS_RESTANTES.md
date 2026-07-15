# Matriz de tests obligatorios por tanda pendiente

Toda tanda futura debe agregar o actualizar tests. Si la tanda es visual o documental, puede usar source tests; si toca dominio/aplicación/infraestructura, debe agregar tests funcionales reales además de guardarraíles fuente.

| Tanda | Tests mínimos |
|---|---|
| 49 Auditoría scaffolding + FFmpeg | `VisualScaffoldingAuditDocumentationTest`, `EmbeddedFfmpegContractSourceTest`, `Video2KContractSourceTest` |
| 50 Modernización visual | `ModernVisualThemeSourceTest`, `CssTokenCoverageSourceTest`, `NoLegacyXpLookSourceTest`, `NoExcessiveNestedPanelsSourceTest` |
| 51 Documento operativo | `DocumentOperationalWorkspaceSourceTest`, `DocumentPageRenderingSourceTest`, `DocumentPrimaryActionVisibilityTest` |
| 52 Word → escuchar | `ListenDocumentWorkflowTest`, `DocumentToScriptToAudioFlowTest`, `PlaybackActiveSentenceFollowTest` |
| 53 Selección/asignación | `DocumentRangeAssignmentWorkflowTest`, `NarrativeLayerConflictPolicyTest`, `AssignmentDoesNotModifyOriginalWordTest` |
| 54 Mini rail | `MiniMediaRailInteractionTest`, `RailItemHighlightsDocumentRangeTest`, `CollapsibleRailBehaviorSourceTest` |
| 55 Motor de voz | `VoiceEngineCapabilityPolicyTest`, `VoiceSpeedSettingsTest`, `VoiceEnginePreflightSourceTest` |
| 56 Modelos | `ModelInstallAssistantWorkflowTest`, `ModelChecksumVerificationTest`, `ManualModelImportTest` |
| 57 Streaming/prebuffer | `StreamingAudioGenerationWorkflowTest`, `PlaybackWaitsForMissingChunkTest`, `LongDocumentChunkingTest` |
| 58 Video 2K + FFmpeg | `EmbeddedFfmpegDiscoveryTest`, `VideoExportUsesBundledFfmpegTest`, `SimpleVideo2KExportPolicyTest` |
| 59 Round-trip | `DocuPodcastFullProjectRoundTripTest`, `NarrativeAssignmentsRoundTripTest`, `MediaRailRoundTripTest` |
| 60 Refactor | `ShellViewModelSizeGuardTest`, `CoordinatorResponsibilitySourceTest`, `NoPresentationInfrastructureLeakTest` |
| 61 Configuración | `SettingsSectionsCoverageTest`, `SettingsDoNotPolluteMainWorkspaceTest`, `FfmpegSettingsCoverageTest` |
| 62 Smoke | `EndToEndUserFlowTest` + docs de smoke mínimo/largo/video 2K |
| 63 RC | `ReleasePackagingScriptsSourceTest`, `InstallerManifestSourceTest`, `EmbeddedToolsPackagingSourceTest` |


## Tanda 55 — Tests agregados

```text
VoiceEngineUsabilityPolicyTest
VoiceSynthesisSettingsTest
VoiceEngineSettingsSourceTest
```

## Tandas restantes — Tests mínimos

```text
56 ModelInstallAssistantWorkflowTest / ModelChecksumVerificationTest
57 StreamingAudioGenerationWorkflowTest / LongDocumentChunkingTest
58 EmbeddedFfmpegDiscoveryTest / SimpleVideo2KExportPolicyTest / VideoRenderProgressSourceTest
59 DocuPodcastFullProjectRoundTripTest
60 ShellViewModelSizeGuardTest / CoordinatorResponsibilitySourceTest
61 SettingsSectionsCoverageTest
62 EndToEndUserFlowTest + smokes documentados
63 ReleaseCandidateChecklistSourceTest
```


## Tanda 56 — Asistente real de modelos

Agrega contratos de carpeta local, verificación de archivos mínimos y reconocimiento de checksum para XTTS/Coqui, Piper y Whisper. Mantiene importación manual obligatoria y evita depender de URLs fijas. Tests: `InspectLocalModelFolderUseCaseTest`, `RealModelAssistantSourceTest`.

## Tanda 57 — Streaming/prebuffer robusto

- Estado: implementada en esta base.
- Contrato: 5 fragmentos iniciales, 10 de lookahead, espera visible y continuidad automática si falta audio.
- Tests: `StreamingPlaybackWindowTest`, `StreamingPlaybackRobustnessSourceTest`.

## Tanda 58 — Video simple 2K + FFmpeg embebido

- Video simple es capacidad del producto cuando el usuario usa storyboard/imágenes.
- Resolución predeterminada: 2K (2560x1440); opciones: 720p, 1080p, 2K y 4K.
- FFmpeg se busca primero como herramienta embebida en `tools/ffmpeg/bin/ffmpeg.exe` y no debe exigir PATH global.
- Durante render, la pantalla operativa entra en modo render con progreso y bloqueo temporal de lectura/edición/nuevas exportaciones.
