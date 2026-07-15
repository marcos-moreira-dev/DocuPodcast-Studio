## Nota DOC-UX-HF10H

Pendiente transversal: extraer política común de progreso/tamaño generado para audio y video, y cerrar descarga automática de Video local en FFMPEG-PREP1. GPU debe validarse con evidencia operativa en MOTOR-PERF1; no basta con detectar dispositivo.

# Deuda técnica y refactor transversal pendiente

Esta deuda no debe mezclarse con cambios de UX salvo que sea estrictamente necesario.

## Criterios de refactor consciente

- Favorecer cohesión: cada clase debe tener una razón principal para cambiar.
- Aplicar SOLID de forma pragmática: interfaces nuevas solo cuando reduzcan acoplamiento real, duplicación real o permitan testear una frontera concreta.
- Separar transversal de específico: procesos externos, rutas embebidas, diagnóstico, settings, dependencias, logging y errores de usuario son transversales; XTTS, Piper, FFmpeg/video, lectura, playback, voces y proyectos son específicos de dominio.
- No crear abstracciones especulativas. Si una extracción no mejora una tanda real o un test concreto, queda pendiente.
- Presentation orquesta interacción y message boxes; application coordina casos de uso; infrastructure implementa detalles externos.
- No cambiar comportamiento visible, schema de settings ni formato `.docupodcast.json` durante refactors RF, salvo migración explícita.

## Consolidación documental

`DOCUMENTACION_ACTUAL/` gobierna el cierre. Las carpetas históricas se conservan como archivo/contexto, pero no deben ser usadas como roadmap activo si contradicen esta carpeta. La limpieza documental inicial es de autoridad y señalización, no de borrado masivo.

## RF-TX1 — extracción transversal sin cambio visible

Candidatos:

- Servicio común de descargas HTTP.
- Runner común de procesos externos.
- Inspector WAV común.
- Gestión de assets internos.
- Checksums y nombres seguros.
- Preparación de dependencias locales.
- Operaciones largas con progreso y cancelación.
- Capa común para mensajes humanos de error.
- Humanización de mensajes técnicos en Configuración y asistentes de dependencias.

## Reglas

- No cambiar formato `.docupodcast.json` salvo migración explícita.
- No cambiar comportamiento visible.
- No romper tests de producto.
- No borrar documentación histórica que aún esté cubierta por guardarraíles; marcarla como histórica y apuntar a `DOCUMENTACION_ACTUAL/`.

## Riesgos actuales

- `DocuPodcastShellViewModel` sigue grande y debe vigilar límite RF2.
- Documentos grandes requieren virtualización real.
- Configuración aún concentra muchas operaciones largas.
- Motores reales requieren separación clara entre descargado, verificado y probado.
- La auditoría vigente de orquestadores y legacy documental vive en `10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md`; usarla antes de tocar `SettingsDialog`, `VoiceLibraryWorkspaceView`, `DocuPodcastShellViewModel` o gateways grandes.

## Nota sobre concurrencia de audio y GPU

La generación de chunks de audio no debe ejecutarse en el hilo JavaFX. El flujo actual debe mantenerse como trabajo de fondo mediante el gateway de audio; la vista solo recibe estados de progreso. La optimización de GPU/concurrencia pertenece al motor real de síntesis —principalmente procesos Python para Voz IA avanzada— y no a JavaFX. Java debe orquestar jobs, límites de concurrencia, manifest, cancelación y orden de salida, pero no debe prometer GPU si el comando real no la usa.

Para una futura tanda `MOTOR-PERF1`, cualquier paralelización debe proteger:

- orden estable de segmentos/chunks en el manifest;
- límites de CPU/RAM/VRAM;
- cancelación cooperativa;
- reanudación desde chunks existentes;
- diferenciación entre motores que soportan GPU y motores que solo corren CPU.


## Componentes transversales recientes

- `SourceTableGridView`: grilla visual reutilizable para tablas fuente DOCX/Markdown. Debe permanecer como componente transversal y no como código duplicado dentro de `DocumentWorkspaceView`.

## Nota DOC-UX-HF10G

La virtualización del rail derecho y el throttle de estados de audio son correcciones de estabilidad, no refactor final. Para RF-TX1 quedan pendientes extractores transversales más limpios para progreso, jobs largos, inspección de WAV, descargas y procesos externos. `SettingsDialog` y `DocuPodcastShellViewModel` siguen siendo hotspots de deuda controlada.


## PLAYBACK-SPEED1 / PLAYBACK-SPEED2 — deuda observada

`PLAYBACK-SPEED2` reemplaza la aceleración por sample rate con un time-stretch PCM local por overlap-add para conservar mejor el tono en `1.5x` y `1.75x`. Sigue siendo una solución portable sin dependencias externas; si más adelante se exige calidad DSP superior, evaluar un backend multimedia especializado.


## PLAYBACK-SPEED3 — deuda residual

La continuidad de velocidad queda corregida sin aumentar `DocuPodcastShellViewModel` sobre el límite vigente, pero la solución sigue viviendo demasiado cerca del Shell. En `RF-TX1` conviene extraer un coordinador de transporte continuo que concentre cursor, cue activo, cambio de velocidad, espera de buffer y transición al siguiente WAV.

## PLAYBACK-SPEED6 — controlador estructural de continuidad

Se extrajo la continuidad de playback a `PlaybackContinuationController` y se agregó callback real `SegmentAudioPlayer.setOnPlaybackFinished(Consumer<Path>)`. El avance entre chunks ya no depende solo del polling de `Timeline`: Java Sound notifica el final natural del WAV activo y el ViewModel transiciona al siguiente cue validando que el archivo terminado corresponde al cue vigente.
