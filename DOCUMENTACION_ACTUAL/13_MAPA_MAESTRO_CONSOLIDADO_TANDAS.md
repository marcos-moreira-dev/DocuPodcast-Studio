# Mapa maestro consolidado de tandas

Fecha de consolidacion: 2026-06-24

## Proposito

Este documento une dos fuentes que hasta ahora convivian en paralelo:

- El Word maestro de recuperacion y alineacion funcional con Teatro.
- El plan operativo vigente hacia RC personal en `DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/`.

La regla nueva es simple: primero se cierra la alineacion funcional con el Word maestro y el respaldo; despues el respaldo y la documentacion historica dejan de gobernar decisiones nuevas. Desde ese punto, el producto actual, sus pruebas y `DOCUMENTACION_ACTUAL/` mandan.

## Base ya cerrada

Las siguientes tandas quedan como base de partida y no deben reabrirse salvo bug concreto:

| Tanda | Estado | Evidencia vigente |
| --- | --- | --- |
| Tanda 1 - Auditoria comparativa de Teatro | Cerrada documentalmente | `TANDA_01_AUDITORIA_TEATRO/` |
| Tanda 2 - Arquitectura objetivo | Cerrada documentalmente | `TANDA_02_ARQUITECTURA_OBJETIVO/` |
| Tanda 3 - Ciclo de vida de proyectos y fuentes | Implementada | `TANDA_03_CICLO_PROYECTO/PROJECT_LIFECYCLE_SPEC.md` |

G sigue siendo la base de trabajo. El respaldo `C:\Users\MARCOS MOREIRA\Downloads\docupodcast studio respaldo estudiar` queda como lectura historica y no como fuente de copia directa.

## Bloque A - Cierre de alineacion funcional

Estas tandas completan la alineacion del Word maestro contra el codigo actual. Al terminar la Tanda 10, se cierra la puerta de alineacion con respaldo.

| Tanda | Nombre | Objetivo | Entregable principal | Gate |
| --- | --- | --- | --- | --- |
| 4 | Nucleo teatral consolidado | Consolidar dominio, gramatica, semantica, persistencia y roundtrip teatral. | `TANDA_04_NUCLEO_TEATRAL/` | Teatro crea/importa/guarda/reabre estructura sin perdida. |
| 5 | Experiencia teatral operativa | Dejar workspace teatral, sidebars, seleccion, actos/escenas, personajes, objetos y mapas en flujo usable. | `TANDA_05_EXPERIENCIA_TEATRAL/` | Usuario recorre y edita una obra de extremo a extremo. |
| 6 | Voces transversales | Cerrar contrato comun de voces para Estudio, Narrativa y Teatro. | `TANDA_06_VOCES_TRANSVERSALES/` | Teatro usa alias/personajes hacia perfiles, no motores propios. |
| 7 | Visuales transversales | Separar capacidad visual y generacion local de Teatro, conservando adaptadores teatrales. | `TANDA_07_VISUALES_TRANSVERSALES/` | Visuales funciona como capacidad compartida. |
| 8 | Ribbon y comandos | Ordenar ribbon con `AppCommandId`, `RibbonDefinitionCatalog` y `CommandAvailabilityPolicy`. | `TANDA_08_RIBBON_COMANDOS/` | Cada boton visible tiene hogar, condicion y handler o queda retirado/deshabilitado. |
| 9 | Exportacion y readiness | Unificar salidas por modalidad, readiness honesto y exportaciones teatrales. | `TANDA_09_EXPORTACION_READINESS/` | Centro de exportacion muestra salidas validas por modo. |
| 10 | Compatibilidad y cierre de alineacion | Proyectos antiguos, migraciones explicitas y decision final sobre respaldo/historicos. | `TANDA_10_CIERRE_ALINEACION/` | No quedan capacidades solo en respaldo sin decision documentada. |

## Puerta para dejar de alinear

La alineacion contra Word/respaldo termina al cerrar la Tanda 10. Desde la Tanda 11:

- El respaldo queda solo como archivo de consulta.
- Los roadmaps historicos no gobiernan decisiones nuevas.
- Cualquier decision nueva debe estar resumida en `DOCUMENTACION_ACTUAL/`.
- No se recupera codigo por nostalgia: se implementa solo si hay gap funcional probado.

Criterios obligatorios para cerrar la puerta:

- Teatro crea, importa, guarda y reabre estructura completa.
- Voces y Visuales estan tratadas como capacidades compartidas.
- Ribbon refleja comandos reales estabilizados.
- Exportacion/readiness cubre salidas por modalidad.
- Proyectos antiguos abren o muestran migracion explicita.
- No quedan capacidades "solo en respaldo" sin clasificacion: conservar, adaptar, sustituir, recuperar, retirar o incierto.

## Bloque B - RC, runtime y limpieza

Estas tandas ya no alinean contra respaldo. Su autoridad principal es el producto actual, los estandares RC y las pruebas.

| Tanda | Nombre | Objetivo | Gate |
| --- | --- | --- | --- |
| 11 | Runtime, procesos y descargas | Centralizar rutas, procesos externos, artefactos de motores y descargas gestionadas. | No reintroducir `ProcessBuilder` directo ni rutas operativas duplicadas. |
| 12 | Settings, motores y microcopy | Configuracion por pasos humanos, readiness real y mensajes no tecnicos en UI comun. | Ningun motor aparece usable sin prueba real. |
| 13 | Persistencia RC y smokes reales | Guardar/reabrir proyecto completo con documento, voces, visuales, audio, jobs y exportaciones. | Smoke real audio/video/persistencia aprobado o pendiente declarado. |
| 14 | Limpieza de tests | Clasificar tests vivos, historicos, arquitectura, UI vigente y source guardrails. | Tests protegen producto vigente, no arqueologia accidental. |
| 15 | Limpieza documental | README, AI_HANDOFF y VALIDATION cortos; historicos indexados o archivados. | Documentacion vigente no marea ni contradice el plan actual. |
| 16 | Anti-placeholder, diagnostico, packaging y RC gate | Sin comandos visibles muertos, diagnostico por perfiles, heap portable, licencias si aplica y RC final. | RC personal honesta con pendientes visibles. |

## Regla de limpieza documental

No ejecutar limpieza documental antes de limpiar tests.

Orden obligatorio:

1. `TEST-CLEAN1`: clasificar y consolidar pruebas que todavia apuntan a Markdown historico.
2. `DOCS-CLEAN1`: reducir documentacion operativa a pocos puntos de entrada.
3. Mantener historicos como trazabilidad con avisos claros.
4. Borrar o mover documentos solo si no hay pruebas, referencias ni decisiones vigentes que dependan de ellos.

## Gates de prueba por tanda

| Tanda | Validacion minima |
| --- | --- |
| 4 | Roundtrip JSON teatral, parser de gramatica, semantica y persistencia. |
| 5 | Pruebas de coordinadores teatrales, seleccion, mapas y workspace. |
| 6 | `VoiceCapabilityPolicy`, asignaciones por modalidad y guardas de motor. |
| 7 | Proyeccion visual, asignacion de imagenes y adaptadores teatrales. |
| 8 | `AppCommandRegistry`, `CommandAvailabilityPolicy`, `RibbonDefinitionCatalog` y handlers. |
| 9 | `InspectExportReadinessUseCase`, Centro de exportacion y exportaciones por modo. |
| 10 | Migracion/compatibilidad, apertura de proyectos antiguos y decision de cierre de respaldo. |
| 11-13 | Smokes runtime, procesos, audio/video, persistencia y diagnostico operativo. |
| 14-16 | `mvn -q test`, diagnostico RC, no placeholders visibles y documentacion minima. |

## Reglas permanentes

- No ejecutar builds ni tests dentro del respaldo.
- No copiar carpetas del respaldo sobre G.
- No cambiar schema sin migracion y pruebas de roundtrip.
- No borrar documentacion historica antes de `TEST-CLEAN1`.
- No poner motores concretos dentro de modalidades.
- No redisenar ribbon antes de tener comandos reales estabilizados.
- No cerrar RC si un motor real fue omitido sin declararlo.
