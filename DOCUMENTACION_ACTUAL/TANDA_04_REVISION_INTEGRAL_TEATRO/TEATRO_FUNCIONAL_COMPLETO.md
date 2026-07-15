# Tanda 4: Revision integral de Teatro

## Proposito

Esta tanda reemplaza el enfoque estrecho de "nucleo teatral" por una revision funcional completa de Teatro. El objetivo no es copiar el respaldo ni redisenar la experiencia, sino dejar inventariado y protegido todo lo que ya existe en el codigo actual antes de pasar a extracciones transversales.

Base de trabajo: `C:\Users\MARCOS MOREIRA\Downloads\g`.

Restricciones:
- El respaldo es solo lectura.
- No se cambia schema JSON.
- No se elimina ningun comando, modulo o dialog teatral visible.
- Visuales transversales se cierran en Tanda 7, pero la IA teatral actual queda protegida desde esta tanda.
- Exportacion final se cierra en Tanda 9, pero las variantes teatrales existentes quedan inventariadas aqui.

## Clasificacion usada

| Estado | Significado |
| --- | --- |
| funcional | La capacidad tiene entrada visible, flujo conectado y pruebas o guardas vigentes. |
| parcial | La capacidad existe y esta conectada, pero no usa aun toda la opcion visible o depende de cierre posterior. |
| stub visible | El usuario ve una accion o control, pero el comportamiento real aun no esta implementado. |
| legacy | Existe por compatibilidad o recuperacion de proyectos antiguos. |
| pendiente | No debe implementarse en esta tanda, pero queda como deuda explicita. |

## Matriz de capacidades teatrales

| Capacidad | Estado | Evidencia actual | Decision Tanda 4 |
| --- | --- | --- | --- |
| Modalidad oficial `THEATRE_PRODUCTION` | funcional | `ProjectMode`, `ProjectModePolicy`, `ProjectModeCapabilities`, `WorkspaceKind.THEATRE_SCRIPT`, `WorkspaceKind.THEATRE_IMAGE_GENERATION`. | Conservar como entrada oficial de Teatro. |
| Gramatica teatral v1 | funcional | `application.theatre.grammar.TheatreGrammarMarkdownParser`, `TheatreGrammarTemplate`, `ProjectGrammarKind.THEATRE_PRODUCTION`. | `application` es la fuente de verdad. |
| Fachadas de gramatica en presentation | legacy | `presentation.theatre.TheatreGrammarMarkdownParser` y `TheatreGrammarTemplate` delegan a `application.theatre.grammar`. | Mantener como compatibilidad; no duplicar parser. |
| Importacion y sidecar semantico | funcional | `ImportProjectGrammarMarkdownUseCase` materializa `ProjectSemanticsDocument` y reporta diagnostics teatrales. | Fortalecer con pruebas cuando se toquen parser o lifecycle. |
| Persistencia teatral | funcional | `TheatreProjectLayer` y `DocuPodcastProjectTheatreJsonTest` cubren bloque opcional `theatre`. | No cambiar schema sin prueba de perdida y aprobacion. |
| Workspace `Teatro > Guion` | funcional | `DocuPodcastShellView` registra `WorkspaceKind.THEATRE_SCRIPT` con `DocumentWorkspaceMode.THEATRE_SCRIPT`. | Conservar como reutilizacion del lector con capa teatral. |
| Sidebar teatral derecho | funcional | `TheatreSideDock` registra `Fragmentos visuales`, `Personajes`, `Mapa textual`, `Mapa espacial y acciones`, `Objetos`. | Proteger los cinco modulos como superficie vigente. |
| Fragmentos visuales | funcional | Usa `DocumentImageContextPanel`, `DocumentMediaRailView` y `CollapsibleModuleSplitPane`. | Mantener dentro del dock teatral, no devolver al Documento limpio. |
| Personajes | funcional | `TheatreCharactersPanel`, `TheatreCharacterProfileCoordinator`, `TheatreCharacterImageCoordinator`. | Conservar perfiles e imagenes por personaje/escena. |
| Objetos | funcional | `TheatreObjectsPanel`, `TheatreObjectProfileCoordinator`, `TheatreObjectImageCoordinator`. | Conservar utileria/escenografia e imagenes por escena. |
| Mapa textual | funcional | `TheatreTextualMapPanel`, `TheatreTextSequenceCanvas`, `IntervencionBoundaryStore`. | Mantener numeracion e identificadores de intervencion. |
| Mapa espacial y acciones | funcional | `TheatreSpatialActionMapPanel`, `TheatreCanvasSelectionSupport`, placements y acciones. | Mantener alineado con `BuildTheatreSpatialVideoPlanUseCase`. |
| Workspace `Generacion IA teatral` | funcional | `TheatreImageGenerationWorkspaceView` abre desde `OPEN_THEATRE_IMAGE_GENERATION`. | Proteger como workspace real, no como pendiente abstracto. |
| Modulos IA teatral | funcional | `TheatreAiModuleId`: `HOME`, `ENGINE`, `GENERATE`, `JOBS`. | Mantener navegacion modular. |
| Prueba de motor local de imagen | funcional | `RunLocalTheatreImageSmokeUseCase`, `InspectLocalTheatreImageEngineUseCase`, `ImageEngineSmokeRequest`. | No mover a Visuales hasta Tanda 7. |
| Generacion de candidatos por intervencion | funcional | `TheatreImageGenerationWorkflow`, `generateTheatreImageCandidate`, `approveTheatreGeneratedImageCandidate`. | Conservar aprobacion como asignacion visual teatral. |
| Generacion de frames | funcional | `TheatreFrameGenerationWorkflow`, `TheatreFrameGenerationRequest`, `FrameGenerationMode`. | Conservar cola, scope y cancelacion. |
| Exportacion de paquetes IA/contexto | funcional | `TheatreInterventionContextExportWorkflow`, `TheatreBulkInterventionContextExportWorkflow`. | Conservar como soporte de generacion por contexto. |
| Centro de exportacion por modo | funcional | `ExportCenterCoordinator` muestra salidas teatrales solo en `THEATRE_PRODUCTION`. | Conservar como superficie transversal. |
| Readiness teatral | funcional | `InspectExportReadinessUseCase` agrega `THEATRE_WORK_VIDEO`, `THEATRE_SPATIAL_MAP_VIDEO`, `THEATRE_PORTION_VIDEO`. | Mantener en application, no duplicar en presentation. |
| `EXPORT_THEATRE_SPATIAL_VIEW` | funcional | `ExportWorkflowCoordinator.exportTheatreSpatialVideo` llama `ExportFinalVideoUseCase.exportTheatreSpatialMap`. | Conservar como video mapa de obra completa. |
| `EXPORT_THEATRE_PORTION` | funcional | `TheatrePortionExportOptions` permite `THEATRE_MAP` o `FINAL_VIDEO` por scope. | Conservar doble salida acto/escena. |
| `EXPORT_THEATRE_WORK` | funcional | `TheatreWorkExportOptionsDialog` recoge opciones teatrales y `BuildTheatreWorkVideoPlanUseCase` compone frames de obra con imagen, texto y mapa lateral opcional antes del MP4. | Conservar como compositor teatral dedicado. |
| MP4 final comun en modo Teatro | parcial | `exportFinalVideo(... TheatreExportScope ...)` filtra script por scope para porciones. | Conservar como salida comun, separada de `Exportar obra`. |
| Ribbon teatral | parcial | Comandos teatrales existen y se filtran por `ProjectModeCapabilities`; reorganizacion completa queda en Tanda 8. | Proteger comandos visibles hasta la tanda de ribbon. |

## Exportacion teatral existente

Salidas inventariadas:

| Comando | Salida | Estado | Nota |
| --- | --- | --- | --- |
| `EXPORT_THEATRE_WORK` | `THEATRE_WORK_VIDEO` / `docupodcast-obra.mp4` | funcional | Usa las opciones de imagen, texto, tipografia, mapa, personajes y desplazamientos para construir frames teatrales antes del render MP4. |
| `EXPORT_THEATRE_SPATIAL_VIEW` | `THEATRE_SPATIAL_MAP_VIDEO` / `docupodcast-mapa-teatral.mp4` | funcional | Usa mapa espacial y fragmentos sincronizados. |
| `EXPORT_THEATRE_PORTION` | `THEATRE_PORTION_VIDEO` / `docupodcast-porcion-obra.mp4` | funcional | Permite acto o escena, con salida mapa teatral o video final. |
| `EXPORT_SIMPLE_VIDEO_PACKAGE` / `FINAL_VIDEO_MP4` | video comun | parcial en Teatro | Sirve como ruta comun de video, pero no sustituye el compositor teatral completo. |

## Botones y controles cuestionables registrados

| Superficie | Control | Estado | Decision |
| --- | --- | --- | --- |
| `TheatreWorkExportOptionsDialog` | `Usar imagenes de fragmentos` | funcional | Alimenta el compositor de obra teatral. |
| `TheatreWorkExportOptionsDialog` | `Mostrar texto`, `Tipografia`, `Efecto del texto`, `Posicion` | funcional | Alimentan la region de texto del frame teatral. |
| `TheatreWorkExportOptionsDialog` | `Mapa espacial lateral`, `Mostrar personajes`, `Mostrar desplazamientos` | funcional | Alimentan el panel lateral de mapa cuando la escena tiene mapa preparado. |
| `TheatreSideDock` | id legacy `THEATRE_ACTIONS` | legacy | Acciones estan fusionadas en `THEATRE_SPATIAL_MAP`; conservar por compatibilidad de estado. |
| Ribbon Teatro | exportaciones teatrales visibles | parcial | No limpiar antes de Tanda 8. |

## Guardas de no regresion

La tanda agrega `TheatreFullFunctionalSurfaceTanda4SourceTest` para fijar:
- comandos y availability por modalidad teatral;
- workspaces `THEATRE_SCRIPT` y `THEATRE_IMAGE_GENERATION`;
- modulos del sidebar teatral;
- fachada de gramatica delegando a application;
- modulos y acciones de la vista IA teatral;
- exportaciones teatrales y readiness;
- compositor dedicado de `EXPORT_THEATRE_WORK`.

## Resultado de pruebas

Ejecutado en `G`, sin tocar el respaldo:

```powershell
mvn -q "-Dtest=DocuPodcastProjectTheatreJsonTest,ImportProjectGrammarMarkdownUseCaseTest,TheatreGrammarMarkdownParserTest,BuildTheatreProductionProjectionUseCaseTest,TheatreFragmentVisualAssignmentServiceTest,BuildTheatreSpatialVideoPlanUseCaseTest,InspectExportReadinessUseCaseTest,ExportCenterCoordinatorTest,TheatreScriptRf16SourceTest,TheatreImageGenerationWorkspaceSourceTest,TheatreAiGenerationModularWorkspaceSourceTest,TheatreImageGenerationWorkflowTest,TheatreFrameGenerationWorkflowTest,TheatreCharactersModuleSourceTest,TheatreObjectsModuleSourceTest,TheatreTextMapsSourceTest,TheatreSpatialSpeakerIndicatorSourceTest,TheatrePlaybarLayoutSourceTest,TheatreFullFunctionalSurfaceTanda4SourceTest" test
```

Resultado: PASS.

Despues de conectar `Exportar obra` al compositor dedicado:

```powershell
mvn -q "-Dtest=BuildTheatreWorkVideoPlanUseCaseTest,TheatreFullFunctionalSurfaceTanda4SourceTest" test
mvn -q "-Dtest=DocuPodcastProjectTheatreJsonTest,ImportProjectGrammarMarkdownUseCaseTest,TheatreGrammarMarkdownParserTest,BuildTheatreProductionProjectionUseCaseTest,TheatreFragmentVisualAssignmentServiceTest,BuildTheatreSpatialVideoPlanUseCaseTest,BuildTheatreWorkVideoPlanUseCaseTest,InspectExportReadinessUseCaseTest,ExportCenterCoordinatorTest,TheatreScriptRf16SourceTest,TheatreImageGenerationWorkspaceSourceTest,TheatreAiGenerationModularWorkspaceSourceTest,TheatreImageGenerationWorkflowTest,TheatreFrameGenerationWorkflowTest,TheatreCharactersModuleSourceTest,TheatreObjectsModuleSourceTest,TheatreTextMapsSourceTest,TheatreSpatialSpeakerIndicatorSourceTest,TheatrePlaybarLayoutSourceTest,TheatreFullFunctionalSurfaceTanda4SourceTest" test
mvn -q test
```

Resultado: PASS.

Guardarrail RF2: `DocuPodcastShellViewModel` queda en 2599 lineas, dentro del limite `<= 2600`.
