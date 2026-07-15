# Tanda 10 - Compatibilidad y cierre de alineacion

## Decision

La alineacion activa contra el Word maestro/respaldo queda cerrada al terminar esta tanda. Desde Tanda 11 en adelante, el producto actual, sus pruebas y `DOCUMENTACION_ACTUAL/` mandan. El respaldo `C:\Users\MARCOS MOREIRA\Downloads\docupodcast studio respaldo estudiar` queda como archivo historico de consulta, no como fuente para copiar codigo ni reabrir decisiones.

No se cambia schema en esta tanda. La compatibilidad se valida leyendo proyectos legacy y materializando `project.mode` al guardar de nuevo.

## Comparacion de dominio

Comparacion de solo lectura entre:

- Actual: `C:\Users\MARCOS MOREIRA\Downloads\g\src\main\java\com\marcosmoreiradev\docupodcaststudio\domain`
- Respaldo: `C:\Users\MARCOS MOREIRA\Downloads\docupodcast studio respaldo estudiar\src\main\java\com\marcosmoreiradev\docupodcaststudio\domain`

Resultado:

| Metrica | Valor |
| --- | ---: |
| Current | 102 |
| Backup | 95 |
| Same | 89 |
| Changed | 6 |
| OnlyCurrent | 7 |
| OnlyBackup | 0 |

Resumen estable para guardas: `OnlyBackup: 0`.

## Diferencias clasificadas

| Archivo o grupo | Clasificacion | Decision |
| --- | --- | --- |
| `project/ProjectMode.java` | Evolucion vigente | Conservar. Es la modalidad oficial del producto actual. |
| `fragment/*` | Evolucion vigente | Conservar. Fragmentos son ancla transversal para audio, visuales, teatro y exportacion. |
| `project/ProjectMetadata.java` | Compatibilidad legacy + evolucion vigente | Conservar `ProjectKind` y agregar/usar `ProjectMode`; proyectos viejos sin `mode` se infieren. |
| `project/DocuPodcastProject.java` | Compatibilidad legacy + evolucion vigente | Conservar assets, voces, capas narrativas, teatro y view state sin dependencias externas. |
| `assignment/NarrativeLayerKind.java` | Evolucion vigente | Conservar capas narrativas actuales; no volver a Storyboard legacy. |
| `process/ProcessJobKind.java`, `process/ProcessJobState.java` | Evolucion vigente | Conservar procesos persistidos y recuperables para runtime/exportacion. |
| `reading/TableNarrationPolicy.java` | Evolucion vigente | Conservar reglas actuales de lectura. |
| Archivos solo en respaldo | No aplica | No hay archivos `domain` solo en respaldo. |

## Compatibilidad validada

Reglas cerradas:

- JSON sin `project.mode` abre sin error.
- `ProjectKind.DOCUMENT_ONLY` sin datos teatrales infiere `DOCUMENTARY_STUDIO`.
- `ProjectKind.FULL_PROJECT` sin datos teatrales infiere `NARRATIVE_VIDEO`.
- Cualquier proyecto con datos en `TheatreProjectLayer` infiere `THEATRE_PRODUCTION`.
- Al guardar de nuevo, `mode` queda materializado y `kind` no se pierde.
- Proyectos sin fuente primaria siguen abriendo como proyectos sin fuente; no hay migracion destructiva.

## Puerta cerrada

Los criterios del mapa maestro quedan satisfechos para cerrar la alineacion:

- Teatro crea/importa/guarda/reabre estructura completa por las tandas 4 y 5.
- Voces y Visuales quedaron como capacidades compartidas en tandas 6 y 7.
- Ribbon refleja comandos reales estabilizados en Tanda 8.
- Exportacion/readiness cubre salidas por modalidad en Tanda 9.
- Proyectos antiguos sin `mode` abren con inferencia explicita.
- No quedan capacidades `domain` solo en respaldo sin decision documentada.

## Pruebas

Ejecutadas en `C:\Users\MARCOS MOREIRA\Downloads\g`. No se ejecutaron builds ni tests dentro del respaldo.

| Comando | Resultado | Nota |
| --- | --- | --- |
| `mvn -q "-Dtest=ProjectModeTest,ProjectModeCapabilitiesTest,DocuPodcastProjectFileRepositoryTest,DocuPodcastProjectTheatreJsonTest,DocuPodcastProjectNarrativeLayersJsonTest,DocuPodcastProjectVoiceLibraryJsonTest,ProjectRoundTripUseCaseTest,LoadProjectWorkspaceArtifactsUseCaseTest,ValidateProjectWorkspaceIntegrityUseCaseTest,RunMigrationClosureChecklistUseCaseTest,ArchitectureBoundaryTest,MigrationClosureTanda10SourceTest" test` | PASS | Valida modos oficiales, compatibilidad JSON legacy, roundtrip, frontera de arquitectura, checklist de migracion y ausencia de archivos `domain` solo en respaldo. |
| `mvn -q test` | PASS | Gate completo del proyecto actual para cerrar alineacion. |
