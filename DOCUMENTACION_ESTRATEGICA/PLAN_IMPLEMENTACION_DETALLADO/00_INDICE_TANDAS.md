## Actualización

- Tanda 9 implementada: `TANDA_09_ESTADO_IMPLEMENTACION.md`.
- Siguiente: Tanda 10 — Voice Library.

# Índice de tandas de implementación

Este directorio describe las tandas técnicas planificadas con detalle. Las tandas no son dogma, pero sí una ruta segura para no perder el alcance.

## Orden recomendado

1. `TANDA_01_ONBOARDING_SCAFFOLDING.md`
2. `TANDA_02_PROYECTO_DOCUPODCAST_JSON.md`
3. `TANDA_03_IMPORTADOR_DOCX.md`
4. `TANDA_04_DOCUMENT_WORKSPACE.md`
5. `TANDA_05_READING_PROFILE.md`
6. `TANDA_06_SCRIPT_NARRABLE.md`
7. `TANDA_07_AUDIO_JOB_MOCK.md`
8. `TANDA_08_AUDIO_REAL_GATEWAY.md`
9. `TANDA_09_REANUDACION_JOBS.md`
10. `TANDA_10_VOICE_LIBRARY.md`
11. `TANDA_11_STORYBOARD_BASICO.md`
12. `TANDA_12_PLAYBACK_SINCRONIZADO.md`
13. `TANDA_13_EXPORTACIONES.md`
14. `TANDA_14_GUIA_RECURSOS_IA.md`
15. `TANDA_15_PACKAGING_RELEASE.md`

Cada tanda debe terminar con tests, documentación actualizada y criterios de aceptación claros.


## Actualización posterior a Tanda 2

Se implementó la base `.docupodcast.json` + assets relativos. Antes del importador DOCX se agrega una tanda intermedia:

```text
Tanda 2.5 — Integración UI de proyecto/session
```

Razón: la capa interna de persistencia ya existe, pero el usuario todavía no puede crear, abrir, guardar ni cerrar proyectos desde la interfaz. Esa sesión es necesaria antes de cargar Word/DOCX en un proyecto real.

Después de Tanda 2.5 continúa:

```text
Tanda 3 — Importador Word/DOCX real
```

## Tanda 3 parcial — DOCX operativo

Adelantada parcialmente: existe importer DOCX mínimo, workspace documental y materialización `source/` + `document/document.json` al guardar. Falta estructura lateral, diagnóstico y perfil de lectura.


## Tanda 3/4 — DOCX diagnóstico y Document Workspace

Se implementó el cierre de la primera versión del importador DOCX y se avanzó el workspace Documento con SideDock mínimo: Estructura, Propiedades y Diagnóstico. El JSON documental materializado ahora incluye resumen, metadatos e issues de importación.


## Tanda 3 cerrada / Tanda 4 avanzada

Tanda 3 queda cerrada en primera versión: DOCX importer con diagnóstico. Tanda 4 queda avanzada: Document Workspace con SideDock inicial, pendiente de extracción de paneles y filtros.


## Actualización de índice — Tandas 3 y 4

- Tanda 3: importador DOCX mejorado con orden de cuerpo, listas, tablas, imágenes, metadata y diagnósticos.
- Tanda 4: workspace Documento avanzado con estructura lateral, diagnóstico, métricas y selección de bloques.
- Validación parcial: compilación `javac --release 21` de domain/application/infrastructure sin JavaFX.
- Validación completa pendiente en entorno local con Maven Toolchain Temurin 21.


## Actualización Tanda 4 / avance Tanda 5

- Tanda 4 implementada: Document Workspace modular con SideDock reutilizable, filtros y acciones manuales de bloque.
- Tanda 5 avanzada: dominio Reading Profile, perfil académico Word-first y aplicación de reglas sobre bloques.
- Sigue pendiente cerrar Tanda 5 con editor/persistencia de perfiles y puente formal hacia guion narrable.

## Actualización posterior — Tanda 5B implementada

Reading Profile queda cerrado en primera versión: editor visible, persistencia en `.docupodcast.json`, previsualización no destructiva y aplicación al documento importado. La siguiente tanda recomendada es **Tanda 6 — Guion narrable**, donde se construirá `NarrationScriptDocument` desde los bloques ya clasificados.

Tandas pendientes actualizadas:

1. Tanda 6 — Guion narrable.
2. Tanda 7 — Audio job mock con progreso/ETA.
3. Tanda 8 — Gateway TTS real.
4. Tanda 9 — Reanudación de jobs.
5. Tanda 10 — Voice Library.
6. Tanda 11 — Storyboard básico.
7. Tanda 12 — Playback sincronizado.
8. Tanda 13 — Exportaciones.
9. Tanda 14 — Guía integrada + recursos IA.
10. Tanda 15 — Packaging / release candidate.

## Actualización tras Tanda 6

- Tanda 6: implementada. Ver `TANDA_06_ESTADO_IMPLEMENTACION.md`.
- Siguiente: Tanda 7 — Audio job mock con progreso/ETA.

## Actualización posterior a Tanda 7

- Tanda 7 implementada: audio job mock con progreso/ETA.
- Siguiente: Tanda 8 — Gateway TTS real.
- Posible tanda agregada: 7B — persistencia temprana de jobs, si se quiere antes de TTS real.

---

## Actualización Tanda 7B — Persistencia temprana de jobs de audio

Se implementó persistencia temprana de jobs de audio sobre el mock:

- `AudioJobSnapshot` y `AudioSegmentSnapshot`.
- `AudioJobRepository` + `AudioJobFileRepository`.
- Escritura de `jobs/JOB-*/job.json`.
- Escritura de `jobs/JOB-*/segments-status.json`.
- El `MockAudioGenerationGateway` persiste estado durante la generación.
- El workspace Audio muestra historial persistido.
- Al reabrir proyecto, la UI puede recuperar el último estado persistido sin tratarlo como job activo fantasma.

Queda agregada como recomendación la **Tanda 7C opcional — Recuperación operativa/reintento de jobs persistidos**, antes de conectar TTS real si se desea máxima robustez.

Tandas pendientes actualizadas:

```text
Tanda 7C opcional — Recuperación operativa de jobs persistidos
Tanda 8           — Gateway TTS real
Tanda 9           — Reanudación avanzada de jobs
Tanda 10          — Voice Library
Tanda 11          — Storyboard básico
Tanda 12          — Playback sincronizado
Tanda 13          — Exportaciones
Tanda 14          — Guía integrada + recursos IA
Tanda 15          — Packaging / release candidate
```



## Actualización Tanda 7C

- `TANDA_07C_ESTADO_IMPLEMENTACION.md` implementada.
- `TANDA_07D_OPCIONAL_RECORDING_PLAYBACK_CONTRACTS.md` agregada como recomendación opcional antes de Tanda 8.

## Actualización Tanda 7D

- `TANDA_07D_ESTADO_IMPLEMENTACION.md` — implementada.
- Tanda 8 queda como siguiente paso natural: Gateway TTS real.


- [Tanda 8 — Gateway TTS real](TANDA_08_ESTADO_IMPLEMENTACION.md)


- [Tanda 10 — Estado de implementación](TANDA_10_ESTADO_IMPLEMENTACION.md)
- Tanda 10B opcional — Importación de muestras de voz y assets VOICE_SAMPLE.
- Tanda 11 — Storyboard básico.
- Tanda 12 — Playback sincronizado real.
- Tanda 13 — Exportaciones.
- Tanda 14 — Guía integrada + recursos IA.
- Tanda 15 — Packaging / release candidate.

- [Tanda 10B opcional — Importación de muestras de voz](TANDA_10B_OPCIONAL_IMPORTACION_MUESTRAS_VOZ.md)


- `TANDA_10B_ESTADO_IMPLEMENTACION.md` — importación de muestras de voz y assets `VOICE_SAMPLE`.


- Tanda 11 — Storyboard básico: implementada. Ver `TANDA_11_ESTADO_IMPLEMENTACION.md`.
- Siguiente: Tanda 12 — Playback sincronizado real.


## Tanda 12

Implementada. Ver `TANDA_12_ESTADO_IMPLEMENTACION.md`.

Pendientes: Tanda 13, 14 y 15.

- [Tanda 13 — Estado de implementación](TANDA_13_ESTADO_IMPLEMENTACION.md)
- Tanda 14 — Guía integrada + recursos IA.
- Tanda 15 — Packaging / release candidate.


## Actualización tras Tanda 14

Implementada guía integrada + recursos IA. Queda Tanda 15 — Packaging / release candidate.

## Hotfix scripts Maven root-safe

Se corrigieron los scripts para que funcionen tanto desde la raíz del repositorio como desde la carpeta `scripts\`. Todos los scripts que ejecutan Maven ahora hacen `pushd` a la raíz y usan `call mvn`, evitando el error `MissingProjectException` cuando se ejecutan desde `scripts\`.

También se añadió `ScriptsRootSafeSourceTest` como guardarraíl.



## Hotfix tests Windows v2

Corrección previa a Tanda 15: rutas Windows, warning DOCX y rebaseline de tests fuente de UI.

## Tanda 15 completada

Ver `TANDA_15_ESTADO_IMPLEMENTACION.md`.

## Tanda 16 sugerida

Polish visual/UX posterior al cierre técnico: toolbar contextual por workspace, iconos, statusbar, textos de producto, pantalla de inicio y smoke visual.


## Tanda 16 — Polish visual / UX

- Controles visibles de ventana.
- Ventana centrada y ajustada a pantalla.
- Toolbar contextual por workspace.
- Statusbar con retroalimentación más clara.
- Pantalla de inicio con azul suave y tarjetas refinadas.
