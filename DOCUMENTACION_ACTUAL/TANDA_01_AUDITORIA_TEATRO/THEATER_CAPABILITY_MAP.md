# Tanda 1 - Mapa de Capacidades Teatrales

Fecha de auditoria: 2026-06-24

## Lectura general

La modalidad teatral en G no es solo una vista. Es un flujo completo que atraviesa dominio, gramatica, persistencia, UI, seleccion, imagenes, voces, mapas y exportacion. El respaldo conserva el nucleo de dominio y buena parte de la UI, pero no tiene la capa transversal de gramatica ni el Centro de exportacion actual.

## Mapa por capa

| Capa | Archivos principales en G | Capacidad teatral |
| --- | --- | --- |
| Modo de proyecto | `domain/project/ProjectMode.java`, `application/project/ProjectModeCapabilities.java`, `application/project/ProjectModePolicy.java` | Distingue `THEATRE_PRODUCTION`, permite inferir modo teatral si un proyecto legacy contiene datos de teatro y habilita capacidades teatrales. |
| Dominio teatral | `domain/theatre/TheatreProjectLayer.java` | Modelo persistible de intervenciones, personajes, alias de voz, imagenes, actos, escenas, posiciones, acciones, placements, objetos e imagenes de objetos. |
| Persistencia JSON | `infrastructure/json/DocuPodcastProjectJsonReader.java`, `DocuPodcastProjectJsonWriter.java` | Roundtrip de toda la capa teatral. La prueba `DocuPodcastProjectTheatreJsonTest` valida el contrato. |
| Gramatica application | `application/grammar/*`, `application/theatre/grammar/*` | Importa Markdown de gramatica teatral, genera plantilla oficial y reporta diagnosticos. Es nuevo en G frente al respaldo. |
| Semantica de proyecto | `ProjectSemanticsDocument`, `ProjectSemanticsRepository`, `FileSystemProjectSemanticsRepository` | Guarda una representacion semantica derivada de la gramatica para trazabilidad futura. |
| Coordinacion de gramatica | `presentation/shell/workflow/GrammarWorkflowCoordinator.java` | Orquesta importacion/exportacion de plantillas, materializa teatro en proyecto, crea documento/script y abre la superficie teatral. |
| Workspace teatral | `presentation/theatre/*`, `presentation/shell/DocuPodcastShellView.java`, `presentation/workspace/WorkspaceDescriptorCatalog.java` | Superficie de `Guion teatral` con herramientas teatrales sobre el documento. |
| Escenas/personajes/objetos | `TheatreSceneCoordinator`, `TheatreCharacterProfileCoordinator`, `TheatreObjectProfileCoordinator` | Administra actos, escenas, perfiles de personaje y objetos asociados a la obra. |
| Seleccion y placements | `TheatreTextActionPlacementSaveWorkflow`, `presentation/theatre/TheatreTextualMapPanel.java`, `presentation/theatre/TheatreCanvasSelectionSupport.java` | Vincula fragmentos narrativos con ubicaciones/acciones teatrales y conserva seleccion visual. |
| Imagen teatral | `TheatreImageGenerationWorkspaceView`, `TheatreImageGenerationWorkflow`, `TheatreCharacterImageCoordinator`, `TheatreObjectImageCoordinator` | Prepara contexto visual teatral por intervencion, personaje y objeto. Depende de assets del proyecto. |
| Voces | `VoiceProfileAdministrationCoordinator`, `VoiceSampleWorkflowCoordinator`, `TheatreProjectLayer.VoiceRoleAlias` | Teatro referencia voces por alias/perfil, pero no debe acoplarse al motor de voz. |
| Video teatral | `application/video/BuildTheatreSpatialVideoPlanUseCase.java`, `ExportFinalVideoUseCase.java`, `TheatreExportScope` | Construye video de mapa espacial y exportacion de obra/porcion. |
| Exportacion application | `application/export/InspectExportReadinessUseCase.java`, `ExportableArtifactKind.java` | Revisa si obra, mapa y porcion teatral son exportables. |
| Centro de exportacion | `presentation/export/*` | Presenta salidas creativas filtradas por tipo de proyecto. En teatro agrega obra, mapa y porcion. |
| Ribbon/barra de opciones | `presentation/ribbon/RibbonDefinitionCatalog.java`, `presentation/command/AppCommandRegistry.java`, `CommandAvailabilityPolicy.java` | Expone comandos estables y los oculta/deshabilita segun modo y estado. Es una pieza critica para no mezclar modalidades. |

## Modalidad teatral

En `THEATRE_PRODUCTION`, el programa habilita:

1. Documento con capa teatral (`THEATRE_SCRIPT`).
2. Importacion de gramatica teatral desde Markdown.
3. Exportacion de plantilla de gramatica teatral.
4. Administracion de personajes, objetos, actos, escenas y mapa textual/espacial.
5. Generacion IA teatral como microaplicacion visual.
6. Readiness y exportacion de obra, mapa teatral y porcion de obra.

La modalidad teatral comparte capacidades con lectura/documento y audio, pero sus comandos principales deben estar condicionados por `ProjectModeCapabilities.theatreProduction()`.

## Ribbon / barra de opciones

El ribbon se define en `RibbonDefinitionCatalog`. Sus pestanas oficiales son:

| Pestana | Grupos | Comandos relevantes |
| --- | --- | --- |
| `Inicio` | Fuente documental, Proyecto | Abrir fuente, Nuevo, Abrir, Guardar. |
| `Lectura` | Preparar lectura, Video narrativo | Preparar lectura, Generar audio, Cancelar, Visual narrativa, Importar narrativa, Plantilla narrativa, Imagen puente. |
| `Vista` | Vistas principales, Ventana, Soporte | Inicio, Documento, Voces, Pantalla completa, Configurar, Guia. |
| `Teatro` | Guion, Gramatica, Obra, IA visual | Guion, Importar gramatica, Plantilla gramatica, Obra, Video mapa, Generacion IA. |
| `Exportar` | Centro, Salidas creativas, Destino | Centro, Estado exportacion, Audio, Video, Obra, Video mapa, Porcion obra, Exportaciones. |

Comandos teatrales del ribbon:

| Comando | Rol | Disponibilidad |
| --- | --- | --- |
| `OPEN_THEATRE_SCRIPT` | Abre el documento con herramientas teatrales. | Visible solo si el modo tiene produccion teatral. |
| `IMPORT_THEATRE_GRAMMAR` | Importa Markdown de gramatica teatral para crear/materializar obra, actos y escenas. | Visible solo en teatro. |
| `EXPORT_THEATRE_GRAMMAR_TEMPLATE` | Exporta plantilla Markdown de gramatica teatral. | Visible solo en teatro. |
| `EXPORT_THEATRE_WORK` | Exporta obra teatral completa. | Visible solo en teatro; requiere proyecto abierto. |
| `EXPORT_THEATRE_SPATIAL_VIEW` | Exporta video de mapa teatral. | Visible solo en teatro; requiere proyecto abierto. |
| `EXPORT_THEATRE_PORTION` | Exporta acto o escena. | Visible solo en teatro; requiere proyecto abierto. |
| `OPEN_THEATRE_IMAGE_GENERATION` | Abre generacion IA teatral. | Visible solo en teatro; requiere proyecto guardable. |

Comandos transversales que tambien afectan teatro:

| Comando | Rol transversal |
| --- | --- |
| `OPEN_SOURCE_DOCUMENT` | Carga fuente documental. |
| `REFRESH_SOURCE_DOCUMENT` | Relee la fuente. |
| `PREPARE_DOCUMENT_READING` | Construye lectura/script base. |
| `LISTEN_DOCUMENT`, `PLAY_SELECTION`, `PAUSE_PLAYBACK`, `RESUME_PLAYBACK`, `STOP_PLAYBACK` | Controlan reproduccion. |
| `ASSIGN_AI_VOICE_TO_SELECTION`, `ASSIGN_HUMAN_RECORDING_TO_SELECTION`, `IMPORT_VOICE_SAMPLE`, `OPEN_VOICE_LIBRARY` | Gestionan voces y muestras. |
| `OPEN_EXPORT_CENTER`, `INSPECT_EXPORT_READINESS`, `OPEN_EXPORTS_FOLDER` | Centralizan exportacion/readiness/destino. |

Punto de cuidado: algunos botones pueden ser discutibles por ubicacion o agrupacion, pero el contrato real no esta en el texto visual del boton sino en `AppCommandId`, `AppCommandRegistry`, `RibbonDefinitionCatalog` y `CommandAvailabilityPolicy`.

## Flujos reales en G

### Importacion de gramatica teatral

1. El usuario activa `IMPORT_THEATRE_GRAMMAR`.
2. `DocuPodcastShellView` despacha el comando.
3. `GrammarWorkflowCoordinator` lee el Markdown.
4. `ImportProjectGrammarMarkdownUseCase` usa `application/theatre/grammar/TheatreGrammarMarkdownParser`.
5. Se materializan actos, escenas, personajes, objetos, intervenciones y documento/script.
6. Se guarda semantica via `ProjectSemanticsRepository`.
7. El shell muestra `THEATRE_SCRIPT`.

### Roundtrip de proyecto teatral

1. `DocuPodcastProject` contiene `TheatreProjectLayer`.
2. `DocuPodcastProjectJsonWriter` escribe todos los campos teatrales.
3. `DocuPodcastProjectJsonReader` lee esos campos y conserva modo o infiere `THEATRE_PRODUCTION` si detecta datos teatrales.
4. `DocuPodcastProjectTheatreJsonTest` valida que el contrato sobreviva guardar/cargar.

### Mapa y seleccion teatral

1. La UI de teatro usa fragmentos/intervenciones del script y datos de `TheatreProjectLayer`.
2. Los placements guardan posicion/accion para fragmentos.
3. El mapa textual/espacial se alimenta de esas asociaciones.
4. El video de mapa usa `BuildTheatreSpatialVideoPlanUseCase`, por lo que UI y exportacion deben conservar la misma geometria logica.

### Imagen teatral

1. `OPEN_THEATRE_IMAGE_GENERATION` abre la microaplicacion teatral.
2. El workflow arma contexto con escena, personajes, objetos e imagenes existentes.
3. La salida se registra como asset del proyecto y se vincula a personaje/objeto/intervencion segun el caso.
4. Esta capacidad debe migrarse con cuidado en tandas visuales futuras porque mezcla assets, contexto teatral y motor local.

### Exportacion teatral

1. `OPEN_EXPORT_CENTER` construye estado desde `DocuPodcastShellViewModel`.
2. `ExportCenterCoordinator` lista destinos segun `ProjectMode`.
3. En `THEATRE_PRODUCTION`, se agregan `EXPORT_THEATRE_WORK`, `EXPORT_THEATRE_SPATIAL_VIEW` y `EXPORT_THEATRE_PORTION`.
4. `INSPECT_EXPORT_READINESS` usa `InspectExportReadinessUseCase`.
5. `ExportWorkflowCoordinator` ejecuta la salida seleccionada.

## Capacidades transversales: exportacion

La exportacion actual esta filtrada por tipo de proyecto:

| Modo | Salidas esperadas por Centro de exportacion |
| --- | --- |
| Documental | Audio y video documental texto+audio cuando aplica. |
| Video narrativo | Audio y video narrativo/simple cuando aplica. |
| Teatro | Audio, obra teatral, video de mapa y porcion de obra. |

La conclusion transversal para Tanda 1 es que la exportacion ya se comporta como centro comun con targets por modalidad. No conviene duplicar exportadores por workspace ni volver al modelo anterior del respaldo. El trabajo futuro debe reforzar readiness, mensajes y opciones por tipo de proyecto.
