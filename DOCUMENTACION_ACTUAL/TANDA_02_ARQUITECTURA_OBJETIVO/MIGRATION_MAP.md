# Tanda 2 - Mapa de Migracion

Fecha de implementacion documental: 2026-06-24

## Resumen

Este mapa traduce la auditoria de Tanda 1 en una ruta de migracion por capacidad. No ordena copiar codigo del respaldo. Indica que conservar, adaptar o posponer dentro de G.

## Mapa por capacidad

| Capacidad | Estado actual | Destino objetivo | Decision | Tanda |
| --- | --- | --- | --- | --- |
| Modalidad oficial | `ProjectMode` con tres modos oficiales. | Mantener en `domain/project`. | Conservar. | T2-T3 |
| Compatibilidad legacy | `ProjectKind` y `ProjectModePolicy.inferLegacy(...)`. | Mantener como compatibilidad, no como UI principal. | Adaptar solo si T3 requiere ciclo de proyecto. | T3 |
| Capabilities por modo | `ProjectModeCapabilities`. | Fuente unica para visibilidad/permiso por modalidad. | Conservar. | T2-T8 |
| Dominio teatral | `TheatreProjectLayer` igual a respaldo. | Mantener en `domain/theatre`. | Conservar. | T4 |
| Planes de dominio teatral | `domain/theatre/plan/*`. | Mantener. | Conservar. | T4 |
| Persistencia teatral | JSON reader/writer cubren capa teatral. | Mantener schema; ampliar solo con migracion explicita. | Conservar. | T3-T4 |
| Gramatica transversal | `application/grammar/*`. | Owner de plantillas/importacion/semantica. | Conservar. | T4 |
| Gramatica teatral application | `application/theatre/grammar/*`. | Parser/template teatral oficial. | Conservar. | T4 |
| Gramatica teatral presentation | `presentation/theatre/TheatreGrammar*`. | Wrapper o compatibilidad; no source of truth. | Adaptar/retirar con evidencia. | T4/T8 |
| Semantica de proyecto | `ProjectSemanticsDocument` y repositorio. | Trazabilidad de importacion y materializacion. | Adaptar con reglas de persistencia. | T4-T5 |
| Workflow de gramatica | `GrammarWorkflowCoordinator`. | Presentation coordinator que llama application. | Conservar; no crecer. | T4-T5 |
| Proyeccion teatral | `BuildTheatreProductionProjectionUseCase`. | Read model application para UI/readiness/export. | Conservar. | T4-T5 |
| Escenas/personajes/objetos | Coordinadores presentation shell. | Mantener como coordinadores UI, con dominio en `TheatreProjectLayer`. | Adaptar si crecen. | T5 |
| Seleccion y placements | `TheatreCanvasSelectionSupport`, `TheatreTextActionPlacementSaveWorkflow`. | Mantener como UI/coordination, sin duplicar geometria. | Conservar con guardas. | T5 |
| Video mapa teatral | `BuildTheatreSpatialVideoPlanUseCase`. | Application video; debe compartir criterio con UI. | Conservar. | T5/T9 |
| Alias de voz teatral | `TheatreProjectLayer.VoiceRoleAlias`. | Referencia teatral a `VoiceProfile`, no motor. | Adaptar en contrato de Voces. | T6 |
| Biblioteca de voces | `VoiceLibrary`, `VoiceApplicationServices`. | Capacidad transversal. | Conservar y adaptar por modalidad. | T6 |
| Generacion de voz | Audio/render/job use cases + gateways de infraestructura. | Capacidad transversal con readiness; modalidades solo solicitan por referencias. | Adaptar. | T6 |
| Visual por fragmento | `FragmentAssetBinding`, `BuildVisualProductionProjectionUseCase`. | Capacidad transversal por `FragmentId`. | Conservar. | T7 |
| Imagen teatral global | Character/object/map images en `TheatreProjectLayer`. | Significado teatral; validacion/generacion desde Visuales. | Adaptar. | T7 |
| Imagen IA teatral | `TheatreImageGenerationWorkspaceView` y workflows. | Extraer motor/cola/generacion hacia Visuales; dejar contexto teatral como adaptador. | Adaptar despues. | T7 |
| Centro de exportacion | `presentation/export/*`. | Superficie transversal por modo. | Conservar. | T9 |
| Readiness exportable | `InspectExportReadinessUseCase`. | Fuente application de verdad para salidas. | Conservar y ampliar por capacidades. | T9 |
| Ribbon | `RibbonDefinitionCatalog`, `AppCommandRegistry`, `CommandAvailabilityPolicy`. | Reorganizar solo cuando capacidades reales esten claras. | Posponer. | T8 |
| Respaldo | 0 archivos relevantes solo en respaldo. | Fuente historica de contraste. | No copiar. | Todas |

## Ruta de migracion por tandas

| Tanda | Trabajo arquitectonico derivado |
| --- | --- |
| T3 | Formalizar ciclo de proyecto sin cambiar `ProjectMode`; definir fuente principal y compatibilidad legacy. |
| T4 | Consolidar nucleo teatral: gramatica application, semantica, invariantes y persistencia. |
| T5 | Restaurar experiencia teatral sobre proyecciones application; no mover motores a Teatro. |
| T6 | Unificar Voces como capacidad transversal con adaptadores por modalidad. |
| T7 | Unificar Visuales como capacidad transversal; extraer generacion local fuera de Teatro. |
| T8 | Reordenar ribbon cuando comandos reales esten estabilizados. |
| T9 | Reforzar exportacion/readiness comun por modalidad y salidas teatrales. |
| T10 | Migracion, limpieza y retiro de compatibilidad solo con evidencia. |

## Reglas de compatibilidad

1. Cualquier cambio de `ProjectMetadata`, `DocuPodcastProject` o `TheatreProjectLayer` exige prueba de roundtrip JSON.
2. Los proyectos con datos teatrales deben seguir resolviendo `THEATRE_PRODUCTION`.
3. Las referencias a assets deben seguir resolviendose por `ProjectAssetCatalog`.
4. Las referencias de voz deben seguir usando ids de biblioteca/perfil/estilo, no rutas de motor.
5. Las exportaciones existentes de Estudio y Narrativa no se pueden degradar por recuperar Teatro.
6. El respaldo no se usa como fuente de reemplazo directo salvo evidencia posterior de perdida real.

## Elementos sin migracion inmediata

| Elemento | Razon |
| --- | --- |
| Redisenar ribbon | Depende de Tandas 4-7 y esta planificado para Tanda 8. |
| Mover generacion IA teatral | Depende del contrato completo de Visuales en Tanda 7. |
| Cambiar schema teatral | Tanda 1 encontro contrato conservado; no hay necesidad en Tanda 2. |
| Crear interfaces Java nuevas | La arquitectura actual ya tiene servicios/casos de uso suficientes para documentar contratos primero. |
