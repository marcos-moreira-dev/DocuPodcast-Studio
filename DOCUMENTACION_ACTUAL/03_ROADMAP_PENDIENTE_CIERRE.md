# Roadmap pendiente de cierre — estado actual tras demo estable

## ESTANDARES-PENDIENTES-RC1

La base validada por el usuario tiene diagnóstico completo OK, demo teatral estable y app ejecutable. El roadmap pendiente real ya no debe reconstruirse desde documentación histórica dispersa. La referencia vigente para continuar es:

```text
DOCUMENTACION_ACTUAL/08_ESTANDARES_PENDIENTES_RC.md
DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/08_ESTANDARES_PENDIENTES_Y_TANDAS_RESTANTES.md
docs/productizacion/ESTANDARES_PENDIENTES_RC1_MAPA_DE_CONTINUIDAD.md
```

Siguiente tanda recomendada:

```text
RF-TX1-CONSOLIDACION-DOCUMENTAL
```

Motivo: las rutas de runtime y procesos externos ya quedaron centralizados; ahora corresponde reducir ruido documental, fijar criterios de refactor consciente y extraer responsabilidades transversales sin rediseñar la app.

## PDF avanzado — alcance futuro

PDF ya es un formato aceptado por DocuPodcast Studio como documento fuente. La deuda no es “aceptar PDF”, sino darle un soporte tan profundo y confiable como el trabajado para Word/DOCX.

Línea futura recomendada:

- Conservar el flujo actual de PDF como entrada válida.
- Mejorar lectura estructural: páginas, encabezados, bloques, saltos y selección estable.
- Mejorar extracción visual: imágenes, tablas y relación con el rail Visual cuando el PDF lo permita.
- Diagnosticar PDFs escaneados o sin texto nativo con un mensaje claro, sin prometer lectura profunda si falta OCR.
- Mantener Word/DOCX como referencia de profundidad actual; PDF avanzado se planifica después de cerrar RC operativo.

---

## Estado tras XTTS-MODEL-PATH-HF1

Voz IA avanzada ya corrige configuraciones/rutas que apuntan al archivo `model.pth` en vez de la carpeta del modelo. La siguiente tanda en piedra sigue siendo **EXPORT-READINESS-UX1**.

## Estado tras FIRST-USE-ONBOARDING1

La primera experiencia ya prioriza escuchar rápido con Voz local simple. La siguiente tanda en piedra es **EXPORT-READINESS-UX1**, para mostrar estado humano antes de exportar audio/video.

## Estado tras DOCUMENT-SIDEBAR-VOICE-UX1

El sidebar izquierdo de Documento ya distingue **Voz generada** y **Audio del computador**. Documento usa nombres simples en el ComboBox de `Tono` y solo ofrece tonos registrados para la voz seleccionada. La siguiente tanda en piedra es **FIRST-USE-ONBOARDING1**.

## Estado tras VOICE-LIBRARY-SYNC1

La biblioteca de voces ya sincroniza con Documento y las muestras de referencia viven en almacenamiento de app/runtime. La siguiente tanda en piedra es **DOCUMENT-SIDEBAR-VOICE-UX1**, para pulir el sidebar de Documento y diferenciar con claridad Voz IA generada vs Audio del computador.

## Ajuste vigente — VOICE-GPU-DEVICE-HF2

El selector de CPU/GPU debe entenderse como selector para procesos de voz. Voz local simple y Voz IA avanzada reciben el dispositivo elegido; cada runtime decide si puede usar GPU. Voz IA avanzada exige smoke CUDA local para usar NVIDIA.

- VOICE-CHUNKS-HF1 aplicado: queda pendiente VOICE-LIBRARY-SYNC1 como siguiente tanda funcional de voces.


## Roadmap actualizado tras VOICE-UX-POLISH1A

1. Revalidación VOICE-UX-POLISH1A con `scripts\99-diagnostico-completo.bat`.
2. **VOICE-REGISTRATION-WIZARD1** — subvista Nueva voz con grabación Java, muestra Neutral obligatoria y muestras por tono/emoción.
3. **VOICE-LIBRARY-SYNC1** — Documento ve voces/tonos recién registrados y solo muestra emociones existentes para la voz elegida.
4. **DOCUMENT-SIDEBAR-VOICE-UX1** — pulir sidebar de Documento distinguiendo Voz IA generada vs audio local.
5. Continuar el orden del plan maestro actualizado en `DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/01_PLAN_MAESTRO_IMPLEMENTACION.md`.

---

## Roadmap actualizado tras VOICE-PLAN-REFERENCIAS1

1. Revalidación documental del plan de voces si se desea, sin necesidad de Maven porque no hay código.
2. **VOICE-UX-POLISH1A** — limpiar Vista Voces: quitar acciones muertas, microcopy confuso, resumen innecesario y labels excesivos.
3. **VOICE-REGISTRATION-WIZARD1** — subvista Nueva voz con grabación Java, muestra Neutral obligatoria y muestras por emoción.
4. **VOICE-LIBRARY-SYNC1** — Documento ve voces/tonos recién registrados y solo muestra emociones existentes para la voz elegida.
5. **DOCUMENT-SIDEBAR-VOICE-UX1** — pulir sidebar de Documento distinguiendo Voz IA generada vs audio local.
6. Continuar el orden del plan maestro actualizado en `DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/01_PLAN_MAESTRO_IMPLEMENTACION.md`.

---

## Roadmap actualizado tras PLAYBACK-SPEED-HF9

1. Revalidación PLAYBACK-SPEED-HF9 con `scripts\99-diagnostico-completo.bat`.
2. **VOICE-UX-POLISH1** — pulido puntual de Vista Voces.
3. **DOCUMENT-SIDEBAR-VOICE-UX1** — simplificar voz/tono/audio en sidebar de Documento.
4. Continuar el orden del plan maestro en `DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/01_PLAN_MAESTRO_IMPLEMENTACION.md`.

---

# Roadmap pendiente de cierre — nota de autoridad vigente

La lectura masiva final consolidó el roadmap operativo en:

`DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/01_PLAN_MAESTRO_IMPLEMENTACION.md`

Las secciones históricas de este archivo se conservan por trazabilidad, pero para continuar implementación debe usarse el plan en piedra.

---

## Roadmap actualizado tras DOC-INDEX-PLAYBACK-HF1

1. Revalidación DOC-INDEX-PLAYBACK-HF1 con `scripts\99-diagnostico-completo.bat`.
2. **PLAYBACK-SPEED-HF9** — transición sin silencio artificial a 1.5x/1.75x.
3. **VOICE-UX-POLISH1** — pulido puntual de Vista Voces.
4. Continuar el orden del plan maestro en `DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/01_PLAN_MAESTRO_IMPLEMENTACION.md`.

## Roadmap actualizado tras MOTOR-GPU-SMOKE1

1. Revalidación MOTOR-GPU-SMOKE1 con `scripts\99-diagnostico-completo.bat`.
2. **DOC-INDEX-PLAYBACK-HF1** — índice seleccionado debe generar/reproducir desde ese fragmento.
3. **PLAYBACK-SPEED-HF9** — transición sin silencio artificial a 1.5x/1.75x.
4. Continuar el orden del plan maestro en `DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/01_PLAN_MAESTRO_IMPLEMENTACION.md`.

## Roadmap actualizado tras DOC-INDEX-HF13

1. Revalidación DOC-INDEX-HF13 con `scripts\99-diagnostico-completo.bat`.
2. **DOC-PERF / DOC-MEM** — optimizar documentos grandes, imágenes, tablas y memoria.
3. **VIDEO-EXPORT1-HF** — progreso/cancelación más fino si el render MP4 bloquea.
4. **RF-TX1** — refactor transversal GUI/técnico.
5. **PF9** — anti-placeholder global.
6. **Revisión final de Voz IA avanzada post-descarga** — queda para cuando el usuario entregue diagnóstico/reporte final.
7. **PF10 / RC** — release candidate personal.

## Roadmap actualizado tras MOTOR-SMOKE4R-HF1

1. Revalidación MOTOR-SMOKE4R-HF1 con `scripts\99-diagnostico-completo.bat`.
2. **MOTOR-PLAYCONF1** — confirmar reproducción de la prueba WAV dentro de la app o integrar botón de reproducción en Configuración.
3. **MOTOR-PERF1 / MOTOR-GPU1** — CPU/GPU realista con prueba de uso, no solo detección.
4. **FFMPEG-PREP1** — FFmpeg interno/preparable con `ffmpeg.exe`, `ffprobe.exe` y `libx264`.
5. **VIDEO-EXPORT1** — exportación `.mp4` final con ventana de calidad 4K/2K/1080p/720p.
6. **AUDIO-COMPRESS1** — exportación MP3/AAC manteniendo chunks WAV internos.
7. **EXPORT-CLEAN1**, **DOC-INDEX-HF13**, **DOC-PERF/DOC-MEM**, **RF-TX1**, **PF9**, **PF10/RC**.

## Roadmap actualizado tras MOTOR-SMOKE4R / COQUI-DL1

1. Revalidación MOTOR-SMOKE4R / COQUI-DL1 con `scripts\99-diagnostico-completo.bat`.
2. **MOTOR-PLAYCONF1** — confirmar reproducción de la prueba WAV dentro de la app o integrar botón de reproducción en Configuración.
3. **MOTOR-PERF1 / MOTOR-GPU1** — CPU/GPU realista con prueba de uso, no solo detección.
4. **FFMPEG-PREP1** — FFmpeg interno/preparable con `ffmpeg.exe`, `ffprobe.exe` y `libx264`.
5. **VIDEO-EXPORT1** — exportación `.mp4` final con ventana de calidad 4K/2K/1080p/720p.
6. **AUDIO-COMPRESS1** — exportación MP3/AAC manteniendo chunks WAV internos.
7. **EXPORT-CLEAN1** — mover diagnósticos/paquetes técnicos fuera del flujo común.
8. **DOC-INDEX-HF13**, **DOC-PERF/DOC-MEM**.
9. **RF-TX1** — extracción transversal GUI/técnica final.
10. **PF9** — anti-placeholder global.
11. **PF10 / RC** — release candidate personal.

## Roadmap actualizado tras VOZ-UX4R-3D

1. Revalidación VOZ-UX4R-3D con `scripts\99-diagnostico-completo.bat`.
2. **MOTOR-SMOKE4R / COQUI-DL1** — Voz IA avanzada robusta: descarga, verificación, selección, WAV generado y reproducido.
3. **MOTOR-PERF1 / MOTOR-GPU1** — CPU/GPU realista con prueba de uso, no solo detección.
4. **FFMPEG-PREP1** — FFmpeg interno/preparable con `ffmpeg.exe`, `ffprobe.exe` y `libx264`.
5. **VIDEO-EXPORT1** — exportación `.mp4` final con ventana de calidad 4K/2K/1080p/720p.
6. **AUDIO-COMPRESS1** — exportación MP3/AAC manteniendo chunks WAV internos.
7. **EXPORT-CLEAN1** — mover diagnósticos/paquetes técnicos fuera del flujo común.
8. **DOC-INDEX-HF13**, **DOC-PERF/DOC-MEM**.
9. **RF-TX1** — extracción transversal GUI/técnica final.
10. **PF9** — anti-placeholder global.
11. **PF10 / RC** — release candidate personal.

## Roadmap actualizado tras VOZ-TTS5B

1. Revalidación VOZ-TTS5B con `scripts\99-diagnostico-completo.bat`.
2. **VOZ-UX4R-3D** — Gestión teatral de tonos en Vista Voces: grabar/importar/reemplazar/eliminar/exportar muestras por emoción con UI sobria.
3. **MOTOR-SMOKE4R / COQUI-DL1** — Voz IA avanzada robusta: descarga, verificación, selección, WAV generado y reproducido.
4. **MOTOR-PERF1 / MOTOR-GPU1** — CPU/GPU realista con prueba de uso, no solo detección.
5. **FFMPEG-PREP1** — FFmpeg interno/preparable con `ffmpeg.exe`, `ffprobe.exe` y `libx264`.
6. **VIDEO-EXPORT1** — exportación `.mp4` final con ventana de calidad 4K/2K/1080p/720p.
7. **AUDIO-COMPRESS1** — exportación MP3/AAC manteniendo chunks WAV internos.
8. **EXPORT-CLEAN1** — mover diagnósticos/paquetes técnicos fuera del flujo común.
9. **DOC-INDEX-HF13**, **DOC-PERF/DOC-MEM**.
10. **RF-TX1** — extracción transversal GUI/técnica final.
11. **PF9** — anti-placeholder global.
12. **PF10 / RC** — release candidate personal.

## Estado tras VOZ-TTS5A

Documento ya filtra opciones de voz/tono con honestidad: Voz IA avanzada solo muestra voces con muestra Neutral registrada y los tonos provienen de las muestras realmente registradas para esa voz. La siguiente tanda funcional lógica es `VOZ-TTS5B`, para que la generación TTS use efectivamente voz + tono real al renderizar audio. Mantener como prioridad paralela posterior `MOTOR-SMOKE4R / COQUI-DL1`, porque la descarga de Voz IA avanzada sigue siendo el mayor riesgo operativo real.

## Estado tras DOC-UX-HF10H

La estabilidad de HF10G queda pulida en progreso visual y copia de Video local. Próximo bloque recomendado: revalidación HF10H, luego PLAYBACK-HF9 o DOC-PERF-HF10I si aparecen costos de proyección/selección en documentos muy largos. MOTOR-PERF1 sigue pendiente para GPU real.

# Roadmap pendiente de cierre

Orden recomendado tras DOC-UX-HF10G.

## 1. Revalidación DOC-UX-HF10G

- Correr `scripts\99-diagnostico-completo.bat`.
- Confirmar Maven compile/test verde.
- Abrir documento grande y generar chunks.
- Confirmar que el overlay se puede ocultar y reabrir.
- Confirmar que el contador/ETA se mantiene actualizado.
- Confirmar que las tablas se expanden hacia abajo sin puntos suspensivos innecesarios.

## 2. DOC-PERF-HF10H — proyecciones y selección eficientes

- Evitar reconstrucciones completas de proyecciones para imagen seleccionada.
- Indexar rangos/segmentos/asignaciones visuales.
- Mantener una fuente única para rail derecho y panel Imagen.

## 3. AUDIO-UI-HF10I — política final de refresco de progreso

- Definir cuándo refrescar solo estado/ETA y cuándo reconstruir manifest.
- Garantizar que generación pura no compita con playback.
- Proteger overlay ocultable ante miles de estados.

## 4. DOC-MEM-HF10J — memoria de importadores y visuales fuente

- Revisar imágenes DOCX grandes y Base64.
- Evaluar assets internos/lazy decode.
- Mantener tablas legibles sin cargar datos innecesarios.

## 5. PLAYBACK-HF9 — WAV persistido y reproducir desde aquí robusto

- Reusar WAV existente sin regenerar.
- Reconstruir manifest desde jobs persistidos.
- Evitar “no hay audio” si sí existe WAV.
- Asegurar que reproducir desde un pivote/frase respeta unidad/cue exacta.

## 6. DOC-INDEX-HF13 — índice navegable del documento

- TreeView desde encabezados/estilos para Word/Markdown.
- TXT con navegación plana o deshabilitada con motivo humano.
- Saltar a bloque/frase sin depender de múltiples scrollbars.

## 7. VOZ-UX4R — Vista Voces final por sub-tandas

- Lista sobria transversal.
- Mini workspace Voces.
- Voz local simple con textbox editable.
- Voz IA avanzada con creación/renombrado/eliminación de voces.
- Tonos por voz.
- Prueba de voz.

## 8. VOZ-TTS5 — usar voz y tono reales en Documento

- Documento usa voces creadas.
- Combo de tono solo con tonos registrados.
- Voz local simple sin tonos.

## 9. MOTOR-SMOKE4R — motores reales usables

- Descargado.
- Verificado.
- Seleccionable.
- Prueba WAV generada.
- WAV reproducido dentro de la app.

## 10. MOTOR-PERF1 — concurrencia/GPU realista para motores

- Validar con evidencia si el motor seleccionado puede usar GPU; los controles `1x`/`1.5x`/`1.75x` solo aceleran playback de WAV ya generados con time-stretch local, no generación TTS.
- Revisar ejecución real del wrapper Python de Voz IA avanzada.
- Evitar promesas placeholder de GPU/concurrencia.
- Proteger orden del manifest y cancelación.

## 11. VIDEO-EXPORT1 / FFMPEG-PREP1

- Asistente de exportación de video.
- Calidad 4K/2K/1080p/720p.
- FFmpeg interno/preparable.
- Renderizar solo `.mp4` final visible al usuario.

## 12. PF7B — WAV final usable

- Unir segmentos.
- Mostrar ubicación.
- Abrir carpeta.

## 13. DOC-IMG5-SMOKE

- Imágenes Word.
- Tablas Word.
- Fórmulas detectadas.
- Capas de imagen por usuario.
- Guardar/reabrir.

## 14. SETTINGS-RF2 / RF-TX1 — refactor sin cambio visible

- Extraer operaciones largas de Configuración.
- Descargas.
- Procesos externos.
- Assets.
- Checksums.
- WAV inspector.
- Progreso/cancelación transversal.

## 15. PF9 — anti-placeholder global

- Todo visible funciona o se deshabilita con motivo humano.

## 16. PF10 — release candidate personal final

- Diagnóstico completo verde.
- Motores probados.
- Audio y video generados.
- App portable/app-image.


## PLAYBACK-CORE1 — diagnóstico runtime de reproducción

Se agrega diagnóstico visible de manifest/cue/WAV/player/cola en el status bar desplazable para aislar por qué la continuidad se detiene tras el primer fragmento.

## Estado después de PLAYBACK-CORE3

- Pendiente revalidar la continuidad real en Windows con documentos que tengan varios cues por segmento.
- Si CORE3 queda estable, se retoma el roadmap funcional: compresión/exportación de audio, performance documental, progreso de audio, memoria de importadores e índice de documento.


## PLAYBACK-CORE4 — cola exacta guiada por final real del reproductor

- Reproduce por cola exacta de cues, pero prioriza el callback real del reproductor antes de avanzar.
- El watchdog de cola ya no corta audio si Java Sound todavía reporta reproducción activa.
- Corrige inconsistencias observadas al cambiar velocidad, pausar/reanudar y reproducir mientras se generan chunks.
- Documento técnico: `docs/productizacion/PLAYBACK_CORE4_PLAYER_CALLBACK_QUEUE.md`.

## Roadmap actualizado tras VOZ-UX4R-1

1. Revalidación VOZ-UX4R-1.
2. VOZ-UX4R-2 — selector de motor activo dentro de Voces con persistencia real.
3. VOZ-UX4R-3 — Voz local simple completa y mínima.
4. VOZ-UX4R-4 — crear, renombrar y eliminar voces avanzadas con borrado seguro de muestras gestionadas.
5. VOZ-UX4R-5 — tonos por voz y filtrado en Documento.
6. VOZ-UX4R-6 — prueba generada simple/avanzada, separada de reproducir muestra.
7. VOZ-TTS5 — usar voz/tono reales en Documento.
8. MOTOR-SMOKE4R — descarga, verificación, selección y prueba WAV real de Voz IA avanzada.
9. MOTOR-PERF1 — concurrencia/GPU realista.
10. AUDIO-COMPRESS1 — exportación MP3/AAC con FFmpeg interno.
11. VIDEO-EXPORT1 y FFMPEG-PREP1.
12. DOC-PERF-HF10H, AUDIO-UI-HF10I, DOC-MEM-HF10J, DOC-INDEX-HF13.
13. PF7B, DOC-IMG5-SMOKE, SETTINGS-RF2, RF-TX1, PF9 y PF10.

## Roadmap alineado tras VOZ-UX4R-DOC1

La Vista Voces queda redefinida como microaplicación administrativa sobria. El contrato completo está en `docs/productizacion/VOZ_UX4R_CONTRATO_FINAL_MODULOS.md`.

Reglas cerradas:

- Solo tres módulos principales: **Inicio**, **Configurar motor** y **Gestionar voces**.
- La UI debe usar filas sobrias, no tarjetas futuristas ni estilo web/SaaS.
- Inicio lista voces, estados y emociones registradas; solo ofrece **Editar voz** y **Eliminar** como acciones rápidas.
- Configurar motor permite elegir motor, probar texto corto y seleccionar dispositivo de renderizado real para todos los motores.
- Gestionar voces permite crear, editar, eliminar, importar, grabar, reemplazar y exportar muestras por emoción.
- Una voz es entidad única: `Pepito`; las emociones son muestras asociadas, no voces separadas.
- Neutral es obligatoria para que una voz aparezca en Documento.
- Deben soportarse muchas emociones; no hardcodear solo feliz/triste/enojado.
- Eliminar voz debe mostrar message box y advertir que se eliminarán archivos/muestras asociados a esa voz.
- Documento solo debe listar voces con Neutral y emociones registradas para la voz seleccionada.

Roadmap inmediato actualizado:

1. **Revalidación VOZ-UX4R-DOC1** — documentación alineada y guardarraíl fuente verde.
2. **VOZ-UX4R-2A — shell modular simple de Voces** — reemplazar el layout actual por módulos Inicio / Configurar motor / Gestionar voces.
3. **VOZ-UX4R-2B — Configurar motor real en Voces** — selector de motor, selector CPU/GPU detectado para todos los motores, estado real y prueba con textbox.
4. **VOZ-UX4R-3 — Gestionar voces real** — crear/editar/eliminar voces, importar/grabar/reemplazar muestras por emoción, reproducir y exportar muestras.
5. **VOZ-TTS5 — Documento consume voces/emociones reales** — combos filtrados por voces con Neutral y emociones registradas.
6. **MOTOR-SMOKE4R / COQUI-DL1** — descarga/verificación/prueba real de Voz IA avanzada.
7. **MOTOR-PERF1 / MOTOR-GPU1** — detección y uso honesto de CPU/GPU.
8. **AUDIO-COMPRESS1** — exportación MP3/AAC con FFmpeg interno.
9. **VIDEO-EXPORT1 / FFMPEG-PREP1**.
10. **DOC-PERF-HF10H**, **AUDIO-UI-HF10I**, **DOC-MEM-HF10J**, **DOC-INDEX-HF13**.
11. **PF7B**, **DOC-IMG5-SMOKE**, **SETTINGS-RF2**, **RF-TX1**, **PF9**, **PF10**.

## Estado tras VOZ-UX4R-2A

La Vista Voces ya tiene shell modular sobrio con tres módulos: **Inicio**, **Configurar motor** y **Gestionar voces**. Se elimina el enfoque de `SplitPane` para esta vista y queda preparada la microaplicación administrativa de voces.

Siguiente orden recomendado:

1. **Revalidación VOZ-UX4R-2A** — diagnóstico completo y smoke visual de los tres módulos.
2. **VOZ-UX4R-2B** — selector real de motor en Voces, selector CPU/GPU detectado para todos los motores, estado real y prueba con textbox.
3. **VOZ-UX4R-3** — Gestionar voces real: crear, editar, eliminar con message box, importar/grabar/reemplazar emociones, reproducir y exportar muestras.
4. **VOZ-TTS5** — Documento consume voces con Neutral y emociones registradas por voz.
5. **MOTOR-SMOKE4R / COQUI-DL1** — descarga/verificación/prueba real de Voz IA avanzada, postergado por prioridad de UX.

## VOZ-UX4R-2B — Configurar motor real dentro de Voces

Se completa el módulo Configurar motor de la microaplicación Voces con selector de motor activo, selector de dispositivo de renderizado CPU/GPU detectado, estado honesto y prueba por textbox. Las selecciones se persisten en `OperationalSettings`, la misma configuración interna usada por Configuración. No se resuelve todavía la descarga de Voz IA avanzada/Coqui; queda para `MOTOR-SMOKE4R / COQUI-DL1`.

## VOZ-UX4R-3B — navegación modular sobria de Voces

Corrige la navegación interna de Vista Voces: los módulos Inicio, Configurar motor y Gestionar voces cambian correctamente sin que el refresco de selección devuelva la vista a Gestionar voces. El sidebar ahora muestra solo el nombre de cada módulo, con estilo administrativo sobrio tipo escritorio/Teams, sin degradados ni descripciones dentro del botón.

## Roadmap actualizado tras MOTOR-PLAYCONF1

1. Revalidación MOTOR-PLAYCONF1.
2. MOTOR-PERF1 / MOTOR-GPU1 — CPU/GPU realista con prueba de uso real.
3. FFMPEG-PREP1 — FFmpeg interno/preparable con verificación de `ffmpeg.exe`, `ffprobe.exe` y `libx264`.
4. VIDEO-EXPORT1 — exportación directa a `.mp4` final con ventana de calidad.
5. AUDIO-COMPRESS1 — exportar MP3/AAC manteniendo chunks WAV internos.
6. EXPORT-CLEAN1 — mover diagnóstico/paquetes técnicos fuera del flujo común.
7. DOC-INDEX-HF13, DOC-PERF/DOC-MEM.
8. RF-TX1 — refactor transversal GUI/técnico.
9. PF9 y PF10/RC.

## Actualización posterior a VIDEO-EXPORT1-HF

- Exportación MP4 final: flujo visible, progreso y cancelación implementados.
- Pendiente: RF-TX1, PF9, revisión final de Voz IA avanzada y RC.

## Nota implementada — MOTOR-GPU-SMOKE1-HF2

Tras la prueba visual en Configuración, el smoke CUDA fue corregido para no ejecutar el probe con `python -c`. El gateway ahora escribe un `.py` temporal UTF-8 y lo ejecuta con el Python autocontenido, evitando falsos negativos por `SyntaxError: unterminated string literal` en Windows.



### Estado tras VOICE-REGISTRATION-WIZARD1

Nueva voz ya tiene subvista operativa. La siguiente tanda en piedra es **VOICE-LIBRARY-SYNC1**, para refrescar Documento y cerrar la política de voces/tonos registrados.


## THEATER-COLON-READ1

Documento puede omitir etiquetas breves antes de dos puntos para guiones teatrales, por ejemplo `Vaquero: texto` → `texto` en la generación de audio. El Word original no cambia y los chunks deben rehacerse.
