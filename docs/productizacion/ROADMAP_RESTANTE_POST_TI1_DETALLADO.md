# Roadmap restante post-TI1 — documentación exhaustiva

Este documento queda como handoff largo para evitar pérdida de contexto. La ventana de chat está cerca del límite, por lo que este roadmap debe considerarse el contrato de continuidad después de TI1.

## Estado base tras TI1

Base vigente: TI1 — RenderUnit end-to-end.

Ya están completadas las fases:

1. GUI prometida reconstruida y pulida: T110R, T108, T111, T112.
2. Ejemplos internos: T113.
3. Productización base: TP1, TP2, TP3, TP4, TP5 y TP6.
4. Primer contrato técnico de integración profunda: TI1.

El proyecto ya tiene:

- documento fuente solo lectura;
- proyecto portable `.docupodcast`;
- assets relativos;
- guía/configuración/ejemplos;
- voces como biblioteca;
- overlay compacto de procesos largos;
- preflight humano de motores;
- runtime layout;
- packaging portable/MSI base;
- manifest de terceros;
- `RenderUnitPlan` como nuevo contrato puente.

Quedan tres familias de trabajo: integración técnica, refactor de presentación y limpieza final.

---

# TI2 — Audio jobs desde RenderPlan

## Estado

Implementada en TI2. Se conserva esta sección como contrato histórico y criterios de validación.

## Objetivo

Cambiar la generación de audio para que ya no dependa exclusivamente de `NarrationScriptDocument` por segmento completo. La entrada conceptual debe ser `RenderUnitPlan`, usando solo las unidades habladas.

## Problema actual

`AudioGenerationRequest` todavía lleva un guion completo y los jobs generan por `NarrationSegment`. Eso ignora parte de la riqueza de capas por oración/unidad:

- voz por rango;
- emoción por rango;
- audio humano por rango;
- unidades omitidas;
- visual silencioso que no debe pedir audio.

## Implementación esperada

1. Crear un request nuevo o extender el existente:
   - `AudioGenerationRequest` puede aceptar `RenderUnitPlan` opcional;
   - o crear `RenderPlanAudioGenerationRequest`.
2. Mapear `RenderUnit` hablado a unidades de trabajo:
   - `SPOKEN_ONLY` y `SPOKEN_WITH_VISUAL` generan o usan audio;
   - `VISUAL_SILENT` no genera audio;
   - `OMITTED` no genera audio.
3. Preservar compatibilidad con jobs antiguos:
   - proyectos viejos pueden seguir usando segmentos;
   - nuevos jobs deben registrar `unitId`.
4. `AudioSegmentSnapshot` o nuevo snapshot debe registrar unidad:
   - `unitId`;
   - `segmentId` legacy;
   - texto efectivo;
   - voz efectiva;
   - estilo efectivo;
   - asset de audio externo si aplica.
5. Respetar audio humano:
   - si una unidad usa `audioAssetId`, no debe invocar TTS;
   - debe crear cue/estado apuntando a ese asset.

## Tests obligatorios

- unidad con voz/emoción genera request TTS correcto;
- unidad con audio humano no llama TTS;
- unidad visual silenciosa no crea tarea de audio;
- reanudación conserva unidades completadas;
- manifest de job mantiene `unitId`.

## Riesgos

- romper jobs legacy;
- mezclar `segmentId` y `unitId` sin política clara;
- duplicar audio para unidades que usan clip externo.

## Criterio de éxito

Un documento con dos oraciones, una con voz A y otra con voz B, debe producir dos unidades de audio diferenciadas. Una imagen/tabla visual silenciosa no debe pedir TTS.

---

# TI3 — Storyboard/video desde RenderPlan

## Objetivo

Hacer que el plan de video nazca de `RenderUnitPlan`, no de todos los segmentos del guion.

## Problema actual

`BuildSimpleVideoPlanUseCase` crea un frame por segmento. Eso genera frames sin imagen o exige audio en situaciones donde el producto quiere silencio.

## Implementación esperada

1. Crear ruta nueva:
   - `BuildSimpleVideoPlanFromRenderUnitPlanUseCase`, o
   - extender `BuildSimpleVideoPlanUseCase` con overload de `RenderUnitPlan`.
2. Reglas:
   - `SPOKEN_ONLY` → no entra al video porque no tiene visual;
   - `SPOKEN_WITH_VISUAL` → frame con imagen + audio;
   - `VISUAL_SILENT` → frame con imagen + silencio;
   - `OMITTED` → no entra.
3. Modificar `SimpleVideoFrame` o crear campo explícito:
   - `silentFrame`;
   - `renderUnitId`;
   - `sourceKind` o `RenderUnitKind`;
   - duración silenciosa efectiva.
4. Actualizar FFmpeg commands:
   - imagen + audio;
   - imagen + silencio generado;
   - no bloquear por falta de audio si el frame es silencioso.
5. Export readiness:
   - video listo si todas las unidades visuales tienen imagen;
   - audio solo requerido para unidades habladas con visual.

## Tests obligatorios

- segmento sin imagen no produce frame;
- unidad visual silenciosa produce frame de 5 segundos;
- unidad con imagen+audio produce frame normal;
- video plan no advierte por falta de audio en visual silencioso;
- FFmpeg command plan genera comando o placeholder correcto para silencio.

## Criterio de éxito

Si el documento tiene cinco párrafos y solo dos tienen imagen asignada, el video debe tener dos frames, no cinco.

---

# TI4 — Bloques visuales no narrables

## Objetivo

Cerrar el tratamiento real de imágenes, tablas y LaTeX/fórmulas como bloques fuente visuales, no como texto a narrar.

## Problema actual

`IMAGE_NOTICE` y `TABLE_NOTICE` existen, pero nacieron como avisos textuales. El sistema puede intentar narrarlos. Falta una semántica más limpia.

## Implementación esperada

1. Revisar `DocumentBlockType`:
   - decidir si se renombran a `SOURCE_IMAGE`, `SOURCE_TABLE`, `SOURCE_FORMULA`;
   - o mantener nombres por compatibilidad y cambiar semántica.
2. `narratableByDefault()`:
   - imágenes, tablas y fórmulas no deben narrarse por defecto;
   - si el usuario quiere una narración, debe agregarse como nota/capa o texto alternativo explícito.
3. Selección de bloque visual:
   - el usuario debe poder seleccionar el bloque fuente;
   - sidebar izquierdo debe mostrar que es visual/no narrable;
   - puede asignar imagen de storyboard u otro asset.
4. Crear `RenderUnit VISUAL_SILENT` cuando:
   - el bloque fuente es visual/no narrable;
   - el usuario asignó visual;
   - hay duración configurada.
5. LaTeX:
   - documentar y preparar tipo de bloque;
   - no vender render visual real hasta implementarlo.

## Tests obligatorios

- DOCX con imagen no genera segmento narrable automático;
- tabla fuente no entra a TTS por defecto;
- bloque visual con imagen asignada crea `VISUAL_SILENT`;
- bloque visual sin imagen asignada se omite del video;
- duración se lee desde configuración.

## Criterio de éxito

Una tabla de un documento contable se ve en la hoja, no se lee como “tabla detectada” por defecto, y solo aparece en video si el usuario decide asignarle un visual.

---

# TI5 — TextAnchor fuerte y reconciliación

## Objetivo

Hacer que las capas sobrevivan mejor cuando el documento fuente se refresca o cambia.

## Problema actual

`TextAnchor` existe, pero muchas capas usan anchors legacy con baja confianza. Los offsets son frágiles.

## Implementación esperada

1. Crear anchors ricos al seleccionar:
   - texto seleccionado;
   - hash del texto;
   - contexto antes/después;
   - snapshot hash;
   - confidence.
2. Al refrescar fuente:
   - intentar localizar la selección original;
   - si no coincide, buscar por contexto;
   - si hay ambigüedad, marcar `NEEDS_REVIEW`.
3. UI:
   - mostrar aviso de capas que requieren revisión;
   - no borrarlas silenciosamente;
   - permitir reasignar.
4. Persistencia:
   - evaluar si requiere `formatVersion 3`;
   - mantener migración desde anchors legacy.

## Tests obligatorios

- anchor exacto se reconcilia;
- texto movido se detecta por contexto;
- texto repetido marca baja confianza;
- capa no se pierde al refrescar;
- round-trip preserva anchor rico.

## Criterio de éxito

Si el usuario refresca un Word con cambios menores, las voces/imágenes asignadas no desaparecen sin explicación.

---

# TI6 — FFmpeg como job persistente/cancelable

## Objetivo

Convertir render de video en proceso largo real, con progreso, cancelación segura, logs, manifest y bloqueo operativo.

## Problema actual

El video genera paquete/comandos, pero no ejecuta render MP4 integrado como job persistente.

## Implementación esperada

1. Crear `VideoRenderGateway`:
   - submit;
   - cancel;
   - status;
   - resume/retry si aplica.
2. Crear snapshot:
   - jobId;
   - estado;
   - etapa;
   - progreso;
   - output esperado;
   - logs stdout/stderr;
   - errores humanos.
3. UI:
   - usar overlay compacto ya existente;
   - bloquear nuevo render mientras hay render activo;
   - permitir ocultar/mostrar desde status bar;
   - permitir cancelar seguro.
4. FFmpeg:
   - usar `RuntimePathResolver`;
   - no depender de PATH global;
   - manejar imagen+audio e imagen+silencio.
5. Persistencia:
   - jobs en `jobs/video/<jobId>` o esquema equivalente.

## Tests obligatorios

- ejecutable fake FFmpeg genera output esperado;
- cancelación destruye proceso fake;
- logs se escriben;
- no permite dos renders simultáneos;
- smoke sin FFmpeg deja paquete auditable.

## Criterio de éxito

El usuario pulsa exportar video, ve progreso, puede ocultarlo, puede cancelar, y al terminar obtiene MP4 o reporte claro.

---

# TI7 — Playback por oración/unidad

## Objetivo

Hacer que la reproducción pueda operar por unidad real, no solo por segmento completo.

## Problema actual

El playback ya tiene `unitId` en `PlaybackCue`, pero la experiencia visible sigue muy asociada al segmento.

## Implementación esperada

1. Manifest desde `RenderUnitPlan`:
   - cues por unidad hablada;
   - audio externo como cue;
   - TTS generado por unidad.
2. Selección en documento:
   - seleccionar oración;
   - buscar unidad;
   - reproducir desde esa unidad.
3. Playbar:
   - mostrar unidad activa;
   - seguir buffer por chunks/unidades;
   - no intentar reproducir visual silencioso como audio.
4. Sincronización:
   - sidebar izquierdo actualiza voz/audio/imagen de la unidad;
   - rail derecho resalta visual relacionado.

## Tests obligatorios

- selección de oración reproduce cue correcto;
- texto repetido no toma cue incorrecto si hay anchor;
- audio externo se reproduce como cue;
- visual silencioso no entra al playback de audio;
- buffer cuenta unidades listas.

## Criterio de éxito

Reproducir selección reproduce exactamente la oración elegida, no el bloque completo ni el primer texto parecido.

---

# RF1 — Dividir `DocuPodcastShellViewModel`

## Objetivo

Reducir el ViewModel gigante y separar responsabilidades por flujo.

## Problema actual

El `DocuPodcastShellViewModel` concentra proyecto, documento, audio, playback, exportación, media, voces, estado, navegación y jobs.

## Implementación esperada

Extraer gradualmente:

- `DocumentMediaWorkflowCoordinator`;
- `DocumentPlaybackWorkflowCoordinator`;
- `VoiceWorkspaceWorkflowCoordinator`;
- `ExampleProjectWorkflowCoordinator`;
- `ExportWorkflowCoordinator`;
- `LongProcessWorkflowCoordinator`;
- `RenderUnitWorkflowCoordinator`.

## Regla

No hacer un refactor masivo que rompa UX. Debe hacerse por tandas pequeñas, con tests fuente y funcionales.

---

# RF2 — Coordinadores de presentación por flujo

## Objetivo

Que cada superficie visual despache intención y delegue a un coordinador claro.

## Flujos candidatos

- abrir/importar documento;
- guardar guiado;
- reproducir/generar audio;
- asignar imagen/audio/voz;
- gestionar voces;
- crear proyecto demo;
- exportar audio/bundle/video;
- mostrar/ocultar procesos.

## Criterio de éxito

Las vistas JavaFX no deben contener lógica de negocio ni manipular directamente demasiados servicios.

---

# RF3 — Use cases de orquestación de producto

## Objetivo

Mover secuencias de producto desde presentación hacia aplicación.

## Use cases candidatos

- `OpenDocumentAsProjectUseCase`;
- `PrepareListeningSessionUseCase`;
- `AssignMediaToSelectionUseCase`;
- `CreateDemoProjectUseCase` más completo;
- `ExportUserFacingPackageUseCase`;
- `BuildEndToEndRenderReadinessUseCase`.

## Criterio de éxito

La UI debe decir “quiero escuchar selección” y la aplicación debe resolver precondiciones, proyecto, guion, render plan, audio y estado.

---

# RF4 — Limpieza de workspaces heredados

## Objetivo

Eliminar residuos semánticos de Guion, Audio Jobs, Storyboard y Observabilidad como vistas principales.

## Regla vigente

Workspaces principales visibles:

- Inicio;
- Documento;
- Voces.

El resto vive como panel, diálogo, reporte o capa interna.

## Tareas

- revisar enums;
- revisar rutas internas;
- limpiar mensajes visibles;
- mantener compatibilidad si algún test/documentación histórica lo necesita;
- no reabrir `Audio Jobs` como pantalla de usuario.

---

# RF5 — Limpieza final STT/Whisper de superficies activas

## Objetivo

Mantener STT/Whisper como deuda histórica/futura, pero no en producto core visible.

## Regla vigente

Motores visibles del producto core:

- Coqui/XTTS;
- Piper;
- FFmpeg;
- mock diagnóstico/desarrollo.

Whisper/STT no debe aparecer en Menú, Ribbon, Guía de uso ni flujo normal.

## Tareas

- revisar docs activos;
- revisar Configuración;
- revisar tests históricos;
- mover menciones a roadmap o documentación histórica;
- impedir que nuevos agentes vuelvan a vender “audio a texto” como funcionalidad core.

---

# Orden recomendado después de TI1

1. TI2 — Audio jobs desde RenderPlan. **Implementada.**
2. TI3 — Storyboard/video desde RenderPlan.
3. TI4 — Bloques visuales no narrables.
4. TI5 — TextAnchor fuerte y reconciliación.
5. TI6 — FFmpeg como job persistente/cancelable.
6. TI7 — Playback por oración/unidad.
7. RF1 — Dividir `DocuPodcastShellViewModel`.
8. RF2 — Coordinadores de presentación por flujo.
9. RF3 — Use cases de orquestación de producto.
10. RF4 — Limpieza de workspaces heredados.
11. RF5 — Limpieza final STT/Whisper.

## Nota final

No avanzar a RF antes de cerrar TI3/TI4 si el objetivo inmediato es que el cerebro render/audio/video quede coherente. El refactor grande debe apoyarse en contratos estables, no reemplazarlos a medio camino.

---

# Índice de documentos individuales

Además de este documento maestro, cada tanda pendiente tiene un markdown individual en:

```text
docs/productizacion/roadmap_restante_post_ti1/
```

Archivos:

- `TI2_AUDIO_JOBS_DESDE_RENDERPLAN.md`
- `TI3_STORYBOARD_VIDEO_DESDE_RENDERPLAN.md`
- `TI4_BLOQUES_VISUALES_NO_NARRABLES.md`
- `TI5_TEXTANCHOR_RECONCILIACION.md`
- `TI6_FFMPEG_JOB_PERSISTENTE_CANCELABLE.md`
- `TI7_PLAYBACK_ORACION_UNIDAD.md`
- `RF1_DIVIDIR_SHELL_VIEW_MODEL.md`
- `RF2_COORDINADORES_PRESENTACION_FLUJO.md`
- `RF3_USE_CASES_ORQUESTACION_PRODUCTO.md`
- `RF4_LIMPIEZA_WORKSPACES_HEREDADOS.md`
- `RF5_LIMPIEZA_STT_WHISPER_SUPERFICIES.md`
```


## Actualización TI3

TI3 queda implementada: el video debe nacer de RenderUnitPlan y no de todos los segmentos del guion. Quedan pendientes TI4, TI5, TI6, TI7 y los refactors RF.


### TI4 completada

Bloques visuales no narrables implementados: imagen, tabla y fórmula/matemática se identifican como fuente visual, no narración automática.


## Actualización TI4

TI4 queda implementada: imágenes, tablas y fórmulas se reconocen como bloques visuales fuente no narrables. LaTeX/OMML queda identificado-only; render visual real sigue fuera de alcance.

## Cierre TI7

TI7 queda implementada como playback por oración/unidad. Ya existe manifiesto unit-aware desde `RenderUnitPlan` y `seekUnit(...)`. Las tandas técnicas restantes pasan a refactor de presentación/orquestación: RF1–RF5.
