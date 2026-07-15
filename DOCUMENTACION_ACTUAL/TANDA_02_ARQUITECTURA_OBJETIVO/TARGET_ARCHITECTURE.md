# Tanda 2 - Arquitectura Objetivo

Fecha de implementacion documental: 2026-06-24

## Resumen

La arquitectura objetivo de DocuPodcast Studio separa dos ejes:

1. Modalidades verticales de producto: `DOCUMENTARY_STUDIO`, `NARRATIVE_VIDEO`, `THEATRE_PRODUCTION`.
2. Capacidades transversales: Documento/Lectura, Voces, Visuales, Exportacion, Settings/Runtime y Procesos.

Esta tanda no introduce clases, interfaces Java ni cambios de schema. Su salida es un contrato documental para que Tandas 3-9 implementen cambios incrementales sin reescritura masiva ni copia desde el respaldo.

## Autoridad y hechos verificados

| Hecho | Evidencia en G | Decision |
| --- | --- | --- |
| La modalidad oficial es `ProjectMode`. | `domain/project/ProjectMode.java` | Mantener como vocabulario de producto. |
| `ProjectKind` sigue existiendo. | `ProjectMetadata.kind()` | Tratarlo como compatibilidad/lifecycle legacy, no como modalidad principal. |
| Las capacidades por modalidad ya existen. | `application/project/ProjectModeCapabilities.java` | Reusar; no crear un segundo sistema de capabilities. |
| La inferencia legacy ya existe. | `application/project/ProjectModePolicy.java` | Centralizar nuevas inferencias aqui o detras de este criterio. |
| Los servicios ya se agrupan por familias. | `application/ApplicationServices.java`, `application/services/*` | Usar como fachada interna, no convertir presentation en ensamblador. |
| Hay guardas de capas. | `ArchitectureBoundaryTest` | No relajar estas reglas. |
| Teatro no debe recuperar codigo por copia. | Tanda 1: 0 archivos relevantes solo en respaldo. | G es fuente principal; respaldo solo contraste. |

## Modelo de producto

| Modalidad | Responsabilidad vertical | Capacidades transversales que puede usar |
| --- | --- | --- |
| Estudio documental | Leer, navegar, escuchar y producir material desde una fuente documental. | Documento/Lectura, Voces, Exportacion; Visuales solo cuando existan evidencias o futuras asignaciones aprobadas. |
| Video narrativo | Producir video por fragmentos narrativos, con imagen principal, puente opcional y audio. | Documento/Lectura, Voces, Visuales, Exportacion. |
| Produccion teatral | Formalizar obra con actos, escenas, personajes, intervenciones, objetos, mapas y salidas teatrales. | Documento/Lectura, Voces, Visuales, Exportacion. |

Regla: una modalidad define el significado de una asociacion; una capacidad transversal define la operacion reusable. Por ejemplo, Teatro puede asociar una voz a un personaje/intervencion, pero la biblioteca, el motor y la generacion de voz siguen siendo capacidad de Voces.

## Capas y dependencias permitidas

| Capa | Responsabilidad | Puede depender de | No debe depender de |
| --- | --- | --- | --- |
| `domain` | Modelos, identidades, invariantes y reglas puras. | Java base y otros modelos de domain. | `application`, `infrastructure`, `presentation`, JavaFX. |
| `application` | Casos de uso, politicas, puertos, proyecciones y readiness. | `domain`, librerias Java puras. | `infrastructure`, `presentation`, JavaFX. |
| `infrastructure` | Filesystem, procesos, settings, repositorios concretos, gateways externos. | `domain`, `application`. | JavaFX y decisiones de UI. |
| `presentation` | JavaFX, workspaces, coordinadores de shell, dialogs y command dispatch. | `domain`, `application`. | Adaptadores concretos de `infrastructure`. |
| `bootstrap` | Ensamblaje de dependencias concretas. | Todas las capas necesarias para wiring. | Logica de producto nueva. |

Regla de implementacion futura: si una clase de presentation necesita operar con filesystem, procesos, audio engine o settings concretos, debe hacerlo por servicios/casos de uso ya ensamblados, no importando infraestructura directamente.

## Familias de servicios objetivo

| Familia | Estado actual | Rol objetivo |
| --- | --- | --- |
| `ProjectApplicationServices` | Existente. | Abrir, guardar, crear, validar proyecto y compatibilidad. |
| `DocumentApplicationServices` / `ReadingProfileApplicationServices` / `ScriptApplicationServices` | Existentes. | Entrada documental, preparacion de lectura y guion narrativo base. |
| `VoiceApplicationServices` | Existente. | Biblioteca de voces, muestras, tonos, asignacion y pruebas/generacion ligadas a la capacidad de Voces. |
| `VisualProductionApplicationServices` | Existente. | Proyeccion visual transversal por `FragmentId`, prompts y diagnostico de assets. |
| `StoryboardApplicationServices` | Existente. | Compatibilidad y materializacion de visuales narrativos existentes. Debe converger gradualmente con Visuales, no duplicarse. |
| `GrammarApplicationServices` | Existente. | Plantillas, importacion Markdown y semantica de proyecto. |
| `TheatreApplicationServices` | Existente. | Proyeccion/readiness teatral y reglas de enlace teatral. No debe alojar motores de voz/imagen. |
| `ExportApplicationServices` | Existente. | Readiness, formato, audio final, video final y paquetes. |
| `SettingsApplicationServices` / `ProcessApplicationServices` | Existentes. | Runtime, motores, procesos largos y configuracion tecnica. |

## Propiedad de datos

| Dato | Owner objetivo | Persistencia actual / direccion |
| --- | --- | --- |
| Modalidad del proyecto | `ProjectMetadata.mode()` + `ProjectModePolicy` | Conservar. Inferir solo para legacy. |
| Assets del proyecto | `ProjectAssetCatalog` | Conservar como catalogo comun. |
| Biblioteca de voces | `VoiceLibrary` | Capacidad transversal; asignaciones especificas viven en cada dominio/script. |
| Asignacion de voz a segmento | `NarrationSegment.withVoice(...)` | Conservar como contrato de lectura/script. |
| Alias de voz teatral | `TheatreProjectLayer.VoiceRoleAlias` | Adaptar como referencia teatral hacia perfiles de voz, no motor. |
| Visual por fragmento | `FragmentAssetBinding` y roles visuales | Mantener como contrato transversal por fragmento. |
| Imagenes teatrales globales | `TheatreProjectLayer.CharacterImage`, `ObjectImage`, `Scene.spatialMapAssetId()` | Mantener en Teatro porque su significado es teatral; validar assets via Visuales. |
| Gramatica importada | `ProjectSemanticsDocument` + repositorio | Mantener como semantica derivada; definir reglas en Tanda 4. |
| Readiness/exportacion | `InspectExportReadinessUseCase`, `ExportReadinessReport` | Mantener como consulta comun por modalidad. |

No se cambia el formato `.docupodcast` en Tanda 2.

## Fronteras por capacidad

### Documento y lectura

Documento produce `ReadableDocument`, proyecciones y `NarrationScriptDocument`. Las modalidades consumen fragmentos/segmentos preparados, pero no deben reimplementar importadores ni lectura.

### Voces

Voces administra biblioteca, perfiles, tonos, muestras y readiness de motor. Estudio, Narrativa y Teatro solo guardan referencias semanticas a voces/estilos. Ninguna modalidad debe conocer XTTS, Piper, Java Sound o command templates.

### Visuales

Visuales administra proyecciones visuales por fragmento, diagnostico de assets, prompts y estado de imagen. Teatro puede conservar personaje/objeto/mapa como significado, pero la validacion de asset y la generacion local deben migrar hacia capacidad Visuales en Tanda 7.

### Exportacion

Exportacion consulta modo, lectura, audio, visuales y teatro para declarar salidas disponibles. No debe sintetizar audio, generar imagenes ni crear mapas faltantes de forma silenciosa.

### Ribbon y shell

El ribbon representa tareas, no arquitectura interna. Para Tanda 2 solo se documenta la frontera: `AppCommandId`, `AppCommandRegistry`, `RibbonDefinitionCatalog` y `CommandAvailabilityPolicy` son el contrato actual. La redistribucion queda para Tanda 8.

## Reglas para implementaciones futuras

1. No crear un segundo enum de modalidad.
2. No copiar archivos del respaldo sobre G.
3. No agregar dependencias directas de Teatro a motores de voz o imagen.
4. No duplicar readiness en presentation.
5. No mover responsabilidades a `DocuPodcastShellViewModel`; debe reducirse o delegar.
6. No cambiar schema sin prueba de roundtrip y plan de migracion.
7. Mantener UI y exportacion teatral alineadas en geometria de mapas.
8. Preferir casos de uso pequenos y servicios existentes antes de nuevas fachadas globales.
