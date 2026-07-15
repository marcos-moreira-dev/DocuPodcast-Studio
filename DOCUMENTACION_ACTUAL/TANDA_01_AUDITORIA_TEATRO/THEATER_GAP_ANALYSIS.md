# Tanda 1 - Gap Analysis Teatro

Fecha de auditoria: 2026-06-24

## Criterio

Clasificacion usada:

| Decision | Significado |
| --- | --- |
| Conservar actual | G tiene la capacidad vigente y no debe reemplazarse con respaldo. |
| Adaptar | La capacidad existe, pero necesita orden, integracion o validacion en tandas futuras. |
| Sustituir | Requiere reemplazo por una pieza nueva o una abstraccion mejor. |
| Recuperar | Algo falta en G y debe recuperarse del respaldo. |
| Retirar | Algo debe salir o quedar oculto por obsoleto. |
| Incierto | Falta evidencia funcional suficiente. |

## Hallazgos por capacidad

| Capacidad | Evidencia | Decision | Razon | Tanda futura |
| --- | --- | --- | --- | --- |
| Dominio teatral | `TheatreProjectLayer` igual en G y respaldo. | Conservar actual | El contrato de datos no parece perdido ni degradado. | T2-T3 solo si se cambia persistencia. |
| Planes de dominio teatral | `domain/theatre/plan/*` igual. | Conservar actual | No hay gap contra respaldo. | Ninguna inmediata. |
| Persistencia JSON teatral | Reader/writer cubren intervenciones, personajes, alias, imagenes, actos, escenas, posiciones, acciones, placements y objetos. | Conservar actual | La prueba de roundtrip pasa. | T3 si se agrega migracion de schema. |
| Inferencia de modo legacy | `ProjectModePolicy` infiere teatro si hay datos teatrales. | Conservar actual | Protege proyectos antiguos sin modo explicito. | T3. |
| Capacidades por modo | `ProjectModeCapabilities` habilita teatro para `THEATRE_PRODUCTION`. | Conservar actual | Es la base para visibilidad de comandos y exportacion por modalidad. | T8 ribbon si se ajusta UX. |
| Gramatica transversal | `application/grammar/*` solo existe en G. | Conservar actual | Es avance vigente, no presente en respaldo. | T4-T5. |
| Parser/template theatrical application | `application/theatre/grammar/*` solo existe en G. | Conservar actual | La responsabilidad esta mejor ubicada fuera de presentation. | T4. |
| Parser/template theatrical presentation | Misma ruta en ambos, pero hash distinto. | Adaptar | Debe quedar como wrapper/superficie compatible o retirarse si ya no aporta. No copiar version del respaldo. | T4 o T8. |
| Semantica de proyecto | `ProjectSemanticsDocument`, repositorio e infraestructura solo en G. | Adaptar | Capacidad nueva util, pero necesita reglas claras de persistencia y uso. | T4-T5. |
| Materializacion de gramatica en proyecto | `GrammarWorkflowCoordinator` solo en G. | Conservar actual | Une Markdown, proyecto, documento/script y workspace teatral. | T4-T5. |
| Proyeccion de produccion teatral | `BuildTheatreProductionProjectionUseCase` solo en G. | Conservar actual | Readiness/exportacion dependen de una proyeccion application. | T5. |
| Readiness teatral | `InspectExportReadinessUseCase` cambio en G y sus pruebas pasan. | Conservar actual | El respaldo no tiene el estado actual de exportables teatrales. | T5. |
| Centro de exportacion | `presentation/export/*` solo en G. | Conservar actual | Es la superficie transversal vigente para exportar por modalidad. | T5. |
| Exportacion obra/mapa/porcion | `ExportWorkflowCoordinator`, `ExportableArtifactKind`, `BuildTheatreSpatialVideoPlanUseCase` cambiaron. | Conservar actual | Son cambios funcionales vinculados a exportacion teatral actual. | T5-T6. |
| Video de mapa espacial | `BuildTheatreSpatialVideoPlanUseCase` cambio en G. | Conservar actual | La geometria de exportacion debe seguir alineada con la UI actual. | T5. |
| Workspace `Guion teatral` | `WorkspaceDescriptorCatalog` declara `THEATRE_SCRIPT` como superficie primaria. | Adaptar | Mantener flujo, pero revisar agrupacion de comandos en ribbon sin cambiar logica ahora. | T8. |
| Ribbon pestana `Teatro` | `RibbonDefinitionCatalog` agrupa Guion, Gramatica, Obra e IA visual. | Adaptar | La estructura existe, pero el usuario marco que algunos botones pueden ser cuestionables. | T8. |
| Ribbon pestana `Exportar` | Incluye Centro, Estado exportacion, Audio, Video, Obra, Video mapa, Porcion obra y Exportaciones. | Adaptar | Buen eje transversal; puede necesitar limpieza de salidas por modo. | T8. |
| Politica de disponibilidad | `CommandAvailabilityPolicy` oculta comandos teatrales si el modo no tiene teatro. | Conservar actual | Es la barrera contra mezclar modalidades. | T8 solo para UX/mensajes. |
| Imagen IA teatral | `TheatreImageGenerationWorkspaceView` y workflows teatrales existen en G. | Adaptar | Capacidad real, pero la auditoria visual transversal queda fuera de Tanda 1. | T7. |
| Perfiles/personajes/objetos | Coordinadores de escenas, personajes y objetos existen en G. | Conservar actual | Son parte del workspace teatral actual. | T2-T5 segun cambios. |
| Alias de voz teatral | `TheatreProjectLayer.VoiceRoleAlias` referencia perfiles de voz. | Adaptar | Debe seguir desacoplado del motor de voz y alinearse con voces transversales. | T6. |
| Tests de producto/ribbon/export | Hay pruebas nuevas solo en G para centro, ribbon y gramatica. | Conservar actual | Funcionan como guardas de comportamiento vigente. | Todas las tandas. |
| Recuperacion desde respaldo | No hay archivos solo en respaldo dentro del filtro auditado. | Retirar como estrategia | No hay evidencia de pieza teatral perdida que deba recuperarse copiando. | Ninguna. |

## Gaps reales

1. La comparacion no muestra ausencia de codigo teatral en G; muestra que G avanzo sobre el respaldo.
2. El mayor riesgo no es recuperar codigo, sino no romper la coordinacion actual entre modo, ribbon, gramatica, workspace y exportacion.
3. El ribbon necesita una tanda propia: su catalogo es estable, pero las decisiones de agrupacion y visibilidad tienen impacto directo en como el usuario entiende cada modalidad.
4. Exportacion ya es transversal por tipo de proyecto. La deuda futura esta en limpiar opciones, readiness y mensajes, no en crear un exportador paralelo para teatro.
5. Imagenes y voces deben tratarse como dependencias transversales, pero sin sacar todavia logica teatral que necesita contexto de escena/personaje/objeto.

## Decisiones para Tandas 2-9

| Tanda futura | Decision derivada de Tanda 1 |
| --- | --- |
| T2 | Trabajar sobre G como fuente principal. El respaldo solo sirve para contraste historico. |
| T3 | No cambiar schema teatral sin migracion y pruebas de roundtrip. |
| T4 | Consolidar gramatica en application; revisar si las clases de presentation son wrappers, adaptadores o deuda. |
| T5 | Mantener Centro de exportacion y readiness por modalidad como eje de salidas. |
| T6 | Revisar voces teatrales como aliases hacia perfiles, no como motor propio. |
| T7 | Revisar imagen IA teatral dentro de una estrategia visual transversal, conservando contexto teatral. |
| T8 | Auditar ribbon/barra de opciones con `AppCommandId` como contrato, no con texto visual suelto. |
| T9 | Usar pruebas de producto actuales como guardas antes de retirar legacy interno. |

## Guardrails

1. No copiar archivos del respaldo sobre G.
2. No revertir archivos cambiados de exportacion/video/gramatica por comparacion de hash.
3. No mover responsabilidad de gramatica de application hacia presentation.
4. No exponer comandos teatrales fuera de `THEATRE_PRODUCTION`.
5. No duplicar exportacion por workspace; usar Centro de exportacion y readiness.
6. No tocar persistencia teatral sin ampliar `DocuPodcastProjectTheatreJsonTest`.
7. No redisenar ribbon en esta tanda; solo registrar comandos y decisiones para Tanda 8.

## Validacion

Pruebas ejecutadas en G:

```powershell
mvn -q "-Dtest=DocuPodcastProjectTheatreJsonTest,TheatreGrammarMarkdownParserTest,BuildTheatreProductionProjectionUseCaseTest,BuildTheatreSpatialVideoPlanUseCaseTest,InspectExportReadinessUseCaseTest" test
```

Resultado: PASS.

No se ejecutaron builds ni pruebas en el respaldo.
