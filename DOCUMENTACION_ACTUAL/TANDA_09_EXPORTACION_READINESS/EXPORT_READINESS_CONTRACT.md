# Tanda 9 - Exportacion y readiness por modalidad

## Regla operativa

Exportacion queda como capacidad transversal que consulta el estado real del proyecto. La UI no decide por su cuenta si una salida esta lista: el Centro de exportacion consume `InspectExportReadinessUseCase` y filtra los targets creativos permitidos por `ProjectMode`.

La exportacion no instala motores, no genera imagenes, no crea mapas, no sintetiza audio ni rellena assets faltantes en silencio. La unica excepcion operacional existente es audio faltante: si el handler ya muestra confirmacion humana para generar y exportar, el Centro puede habilitar ese target, pero el readiness sigue reportando el faltante.

## Matriz por modalidad

| Modalidad | Salidas creativas visibles | Readiness de aplicacion | Comando UI |
| --- | --- | --- | --- |
| `DOCUMENTARY_STUDIO` | Audio final | `PODCAST_WAV` | `EXPORT_PODCAST_WAV` |
| `DOCUMENTARY_STUDIO` | Video documental texto+audio | `DOCUMENT_TEXT_AUDIO_VIDEO` | `EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO` |
| `NARRATIVE_VIDEO` | Audio final | `PODCAST_WAV` | `EXPORT_PODCAST_WAV` |
| `NARRATIVE_VIDEO` | Video narrativo final MP4 | `FINAL_VIDEO_MP4` | `EXPORT_SIMPLE_VIDEO_PACKAGE` |
| `THEATRE_PRODUCTION` | Audio final | `PODCAST_WAV` | `EXPORT_PODCAST_WAV` |
| `THEATRE_PRODUCTION` | Obra teatral | `THEATRE_WORK_VIDEO` | `EXPORT_THEATRE_WORK` |
| `THEATRE_PRODUCTION` | Mapa teatral espacial | `THEATRE_SPATIAL_MAP_VIDEO` | `EXPORT_THEATRE_SPATIAL_VIEW` |
| `THEATRE_PRODUCTION` | Acto o escena teatral | `THEATRE_PORTION_VIDEO` | `EXPORT_THEATRE_PORTION` |

## Artefactos de soporte

Siguen en el reporte completo y en paquetes auditables, pero no aparecen como salidas creativas principales del Centro:

- `PROJECT_BUNDLE`
- `DIAGNOSTIC_REPORT`
- `STORYBOARD_SUMMARY`
- `SIMPLE_VIDEO_PACKAGE`

`SIMPLE_VIDEO_PACKAGE` queda como paquete tecnico/auditable. En modo narrativo, el boton visible `EXPORT_SIMPLE_VIDEO_PACKAGE` representa el MP4 final y se alimenta de `FINAL_VIDEO_MP4`.

## Blockers y decisiones

| Caso | Decision |
| --- | --- |
| Proyecto sin guardar | Bloquear salidas que necesitan carpeta de proyecto. |
| Lectura o guion ausente | Bloquear exportacion creativa hasta preparar lectura. |
| Audio faltante | Mostrar blocker en readiness; habilitar solo si el handler tiene confirmacion explicita para generar y exportar. |
| Imagen principal faltante en Narrativa | Bloquear MP4 narrativo; no generar imagen en exportacion. |
| Mapa teatral sin placements/posiciones/acciones | Bloquear mapa teatral; no crear mapa en exportacion. |
| Porcion teatral sin acto/escena/intervenciones | Bloquear porcion; no inferir alcance silenciosamente. |
| FFmpeg/runtime | No se repara en esta tanda; los handlers siguen usando readiness/setup existente. |

## Cambios implementados

- `ProjectExportTargetCatalog` define targets creativos por `ProjectMode` en `application.export`, sin dependencia a `presentation`.
- `InspectExportReadinessUseCase` ahora trata audio final como salida que requiere proyecto guardado cuando necesita carpeta/jobs.
- `ExportCenterState` transporta `ExportReadinessReport` y procesos relacionados; ya no transporta flags paralelos de audio/visuales.
- `ExportCenterCoordinator` filtra el reporte completo usando el catalogo de targets creativos y mapea cada `ExportableArtifactKind` al comando UI correspondiente.
- Las variantes teatrales existentes quedan protegidas: obra completa, mapa espacial y porcion acto/escena.

## Pruebas

Resultado:

- PASS: `mvn -q "-Dtest=ProjectExportTargetCatalogTest,ExportCenterCoordinatorTest,ExportCenterCoordinatorProcessJobsTest,InspectExportReadinessUseCaseTest,ExportReadinessTanda9SourceTest,TheatreFullFunctionalSurfaceTanda4SourceTest" test`
- PASS: `mvn -q "-Dtest=InspectExportReadinessUseCaseTest,ExportCenterCoordinatorTest,ProjectExportTargetCatalogTest,ProjectExportFormatPolicyTest,ExportPodcastWavUseCaseTest,InspectFinalAudioExportReadinessUseCaseTest,BuildNarrativeVideoPlanUseCaseTest,BuildTheatreWorkVideoPlanUseCaseTest,BuildTheatreSpatialVideoPlanUseCaseTest,ExportSimpleVideoPackageUseCaseTest,ExportReadinessUx2SourceTest,ExportBrainReadinessSourceTest,RibbonCommandContractTanda8Test,CommandAvailabilityPolicyTc1SourceTest,ExportReadinessTanda9SourceTest" test`
- PASS: `mvn -q test`
