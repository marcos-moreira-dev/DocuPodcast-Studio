# Tanda 8 - Ribbon y comandos

## Regla operativa

El ribbon queda como superficie de producto, no como inventario tecnico. El contrato estable es:

- `AppCommandId`: identidad estable del comando.
- `AppCommandRegistry`: texto, owner, superficies permitidas e implementacion.
- `RibbonDefinitionCatalog`: unica estructura visual del ribbon.
- `CommandAvailabilityPolicy`: visible/disabled y razon humana por modo o estado.
- `CommandAuditInspector`: ningun comando visible queda sin handler.

No se cambia schema, persistencia, `ProjectMode`, handlers productivos ni exportaciones reales. Tanda 9 cerrara readiness/exportacion.

## Matriz de ribbon vigente

| Pestana | Grupo | Comandos | Modo | Handler | Decision |
| --- | --- | --- | --- | --- | --- |
| Inicio | Fuente documental | `OPEN_SOURCE_DOCUMENT` | todos | `handleImportWord` | conservar |
| Inicio | Proyecto | `NEW_PROJECT`, `OPEN_PROJECT`, `SAVE_PROJECT` | todos | handlers de proyecto | conservar |
| Lectura | Preparar lectura | `PREPARE_DOCUMENT_READING`, `GENERATE_AUDIO`, `CANCEL_AUDIO_JOB` | todos con documento/script segun estado | lectura/audio | conservar |
| Lectura | Video narrativo | `OPEN_NARRATIVE_VISUAL_PRODUCTION`, `IMPORT_NARRATIVE_VIDEO_GRAMMAR`, `EXPORT_NARRATIVE_VIDEO_GRAMMAR_TEMPLATE` | `NARRATIVE_VIDEO` | workspace/gramatica | conservar |
| Vista | Vistas principales | `SHOW_WELCOME`, `OPEN_DOCUMENT_READER`, `OPEN_VOICE_LIBRARY` | todos | workspace | conservar |
| Vista | Ventana | `TOGGLE_FULLSCREEN` | todos | shell | conservar |
| Vista | Soporte | `OPEN_SETTINGS`, `OPEN_GUIDE` | todos | settings/guia | conservar |
| Teatro | Guion | `OPEN_THEATRE_SCRIPT` | `THEATRE_PRODUCTION` | workspace teatral | conservar |
| Teatro | Gramatica | `IMPORT_THEATRE_GRAMMAR`, `EXPORT_THEATRE_GRAMMAR_TEMPLATE` | `THEATRE_PRODUCTION` | gramatica teatral | conservar |
| Teatro | IA visual | `OPEN_THEATRE_IMAGE_GENERATION` | `THEATRE_PRODUCTION` guardado | workspace IA teatral | conservar |
| Exportar | Centro | `OPEN_EXPORT_CENTER`, `INSPECT_EXPORT_READINESS` | proyecto abierto | centro/readiness | conservar |
| Exportar | Salidas creativas | `EXPORT_PODCAST_WAV`, `EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO`, `EXPORT_SIMPLE_VIDEO_PACKAGE`, `EXPORT_THEATRE_WORK`, `EXPORT_THEATRE_SPATIAL_VIEW`, `EXPORT_THEATRE_PORTION` | por modo | abre Centro con destino preseleccionado | mover aqui |
| Exportar | Destino | `OPEN_EXPORTS_FOLDER` | proyecto guardado | shell | conservar |

## Decisiones sobre botones cuestionables

| Comando | Decision | Razon |
| --- | --- | --- |
| `IMPORT_BRIDGE_IMAGE_FOR_SELECTION` | ocultar del ribbon | Es microaccion contextual del rail visual, no flujo transversal. |
| `IMPORT_IMAGE_FOR_SELECTION`, `ASSOCIATE_IMAGE_TO_SELECTION` | ocultar del ribbon | Pertenecen al rail derecho/seleccion. |
| `IMPORT_AUDIO_FOR_SELECTION`, `EXTRACT_VIDEO_AUDIO_FOR_SELECTION` | ocultar del ribbon | Pertenecen al sidebar/seleccion. |
| `ASSIGN_AI_VOICE_TO_SELECTION`, `ASSIGN_HUMAN_RECORDING_TO_SELECTION` | ocultar del ribbon | Pertenecen al sidebar o vista Voces. |
| `IMPORT_VOICE_SAMPLE` | ocultar del ribbon | Pertenene a Vista Voces/Configuracion, no al ribbon global. |
| `LISTEN_DOCUMENT`, `PLAY_SELECTION`, `CLEAR_SELECTION` | ocultar del ribbon | Playback y seleccion viven en playbar/teclas/superficie contextual. |
| `OPEN_STORYBOARD`, `OPEN_AUDIO_JOBS` | ocultar legacy | No son workspaces vigentes. |
| `EXPORT_PROJECT_BUNDLE`, `EXPORT_DIAGNOSTIC_REPORT`, `INSPECT_PROJECT_INTEGRITY`, `EXPORT_AI_RESOURCES` | mantener fuera del ribbon | Soporte avanzado o diagnostico. |
| `TOGGLE_RIBBON_COLLAPSED` | no catalogar | Es chrome interno del ribbon, no boton de producto. |

## Disponibilidad

`CommandAvailabilityPolicy` sigue siendo la unica fuente para:

- ocultar herramientas teatrales fuera de `THEATRE_PRODUCTION`;
- ocultar produccion/exportacion narrativa fuera de `NARRATIVE_VIDEO`;
- ocultar video documental `EXPORT_DOCUMENT_TEXT_AUDIO_VIDEO` fuera de `DOCUMENTARY_STUDIO`;
- deshabilitar salidas si no hay proyecto abierto o proyecto guardado;
- explicar en tooltip por que un boton visible no esta disponible.

## Pruebas

Resultado final:

- PASS: `mvn -q "-Dtest=AppCommandRegistryTest,AppCommandDispatcherTest,CommandAvailabilityPolicyTc1SourceTest,WorkspaceCapabilityCommandMapperTest,RibbonCatalogRf1SourceTest,RibbonFinalContractT120BSourceTest,RibbonFinalApplicationT120CSourceTest,CommandAuditRc1SourceTest,RibbonBaseT101SourceTest,RibbonCommandContractTanda8Test,ExportCenterCoordinatorTest,InspectExportReadinessUseCaseTest" test`
- PASS: `mvn -q test`

Guardas agregadas:

- `RibbonCommandContractTanda8Test`: valida catalogo, registry, handlers, ausencia de duplicados y comandos excluidos del ribbon.
- `CommandAvailabilityPolicyTc1SourceTest`: protege el binding de razon humana para tooltips.
- Tests historicos de ribbon ajustados para leer `RibbonDefinitionCatalog`, no comentarios de compatibilidad en `RibbonView`.
