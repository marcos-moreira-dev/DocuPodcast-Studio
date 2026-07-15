## FIRST-USE-ONBOARDING1 — implementada

La primera experiencia prioriza Voz local simple para escuchar rápido. Voz IA avanzada y Video local quedan como mejoras posteriores desde Configuración. Ver `docs/productizacion/FIRST_USE_ONBOARDING1_ESCUCHAR_RAPIDO.md`.

## DOCUMENT-SIDEBAR-VOICE-UX1 — implementada

Documento distingue Voz generada y Audio del computador; el campo visible queda como Tono y el ComboBox muestra solo tonos simples realmente registrados para la voz elegida. Ver `docs/productizacion/DOCUMENT_SIDEBAR_VOICE_UX1_VOZ_IA_AUDIO_LOCAL.md`.

### 7. VOICE-LIBRARY-SYNC1

**Estado:** implementada. Documento: `docs/productizacion/VOICE_LIBRARY_SYNC1_APP_LIBRARY_DOCUMENTO.md`.

**Resultado:** muestras de voz en biblioteca de app/runtime, Documento refresca voces/tonos y solo muestra emociones registradas para la voz seleccionada.

- VOICE-CHUNKS-HF1 agregado como hotfix operativo antes de VOICE-LIBRARY-SYNC1: GPU honesta, render desde fragmento y progreso del documento.


# Plan maestro de implementación en piedra

Este documento fija el orden de implementación posterior al cierre de lecturas masivas. El orden no debe cambiarse salvo por una falla local nueva y bloqueante detectada en `scripts\99-diagnostico-completo.bat` o en un smoke real de motores/exportación.

## Actualización de autoridad — bloque de voces

Tras la retroalimentación visual de Vista Voces y la decisión sobre muestras de referencia, el bloque `VOICE-UX-POLISH1` se divide en tres tandas antes de pulir Documento:

1. `VOICE-UX-POLISH1A`
2. `VOICE-REGISTRATION-WIZARD1`
3. `VOICE-LIBRARY-SYNC1`

La razón es operativa: limpiar la vista actual, luego construir la subvista de grabación y finalmente sincronizar la biblioteca con Documento. El detalle completo está en `07_VOCES_VISTA_Y_WIZARD_REFERENCIAS.md`.

## Orden resumido actualizado

1. `MOTOR-ADV-READY-GATE1` — implementada
2. `MOTOR-GPU-SMOKE1` — implementada
3. `DOC-INDEX-PLAYBACK-HF1` — implementada
4. `PLAYBACK-SPEED-HF9` — implementada
5. `VOICE-UX-POLISH1A`
6. `VOICE-REGISTRATION-WIZARD1`
7. `VOICE-LIBRARY-SYNC1`
8. `DOCUMENT-SIDEBAR-VOICE-UX1`
9. `FIRST-USE-ONBOARDING1` — implementada
10. `EXPORT-READINESS-UX1`
11. `SETTINGS-RUNTIME-UX1`
12. `RF-TX2A`
13. `RF-TX2B`
14. `RF-TX2C`
15. `RF-TX2D`
16. `AUDIO-EXPORT-FINAL-HF1`
17. `VIDEO-RUNTIME-SMOKE1`
18. `PERSISTENCE-RC1`
19. `TEST-CLEAN1`
20. `DOCS-CLEAN1`
21. `PF9`
22. `DIAGNOSTIC-SCRIPTS-RC1`
23. `PACKAGING-MEMORY-RC1`
24. `LEGAL-THIRD-PARTY-RC1` — solo si se distribuye fuera del entorno personal/local.
25. `RC-GATE1`

## Bloque 1 — producto funcional y confianza del usuario

### 1. MOTOR-ADV-READY-GATE1

**Estado:** implementada. Documento: `docs/productizacion/MOTOR_ADV_READY_GATE1_USABLE_REAL.md`.

**Problema que resuelve:** Voz IA avanzada puede estar descargada o seleccionable, pero no necesariamente probada para generar audio real.

**Resultado esperado:** la app diferencia con claridad `descargado`, `verificado`, `probado`, `reproducido` y `usable para documento`.

**No negociable:** no permitir generación de chunks largos con Voz IA avanzada si no hay prueba WAV real o confirmación equivalente.

### 2. MOTOR-GPU-SMOKE1

**Estado:** implementada. Documento: `docs/productizacion/MOTOR_GPU_SMOKE1_CUDA_RUNTIME_LOCAL.md`. Hotfix adicional: el probe CUDA escribe un `.py` temporal para evitar errores de comillas en Windows.

**Problema que resuelve:** la UI puede sugerir GPU, pero el runtime Python autocontenido no ha demostrado CUDA.

**Resultado esperado:** GPU solo se habilita para Voz IA avanzada cuando el Python local confirma `torch.cuda.is_available() == true`.

### 3. DOC-INDEX-PLAYBACK-HF1

**Estado:** implementada. Documento: `docs/productizacion/DOC_INDEX_PLAYBACK_HF1_GENERAR_DESDE_INDICE.md`.

**Problema que resuelve:** al seleccionar una sección en el índice de un documento largo, reproducir puede iniciar/generar desde el principio.

**Resultado esperado:** índice seleccionado implica pivote real de generación/reproducción.

### 4. PLAYBACK-SPEED-HF9

**Estado:** implementada. Documento: `docs/productizacion/PLAYBACK_SPEED_HF9_TIMING_REAL.md`.

**Problema que resuelve:** a `1.5x` o `1.75x` el audio suena rápido, pero puede quedar silencio como si esperara duración `1x`.

**Resultado esperado:** el siguiente chunk inicia cuando termina el WAV acelerado, no cuando vence un watchdog conservador.

### 5. VOICE-UX-POLISH1A

**Problema que resuelve:** Vista Voces contiene elementos sin función suficiente, microcopy excesivo, labels visuales mejorables y estados que pueden parecer botones.

**Resultado esperado:** Vista Voces queda sobria y operativa: Inicio explica el propósito, Configurar motor permite seleccionar/probar, Gestionar voces lista voces y emociones reales.

**Reglas clave:**

- No dashboard decorativo.
- No botón `Ver estado de voces` si no tiene acción real.
- No `Resumen de operación` si solo duplica `Configurar motor`.
- ComboBox de emociones con una palabra: `Neutral`, `Feliz`, `Enojado`, `Triste`.
- Lista de voces con tags solo de emociones realmente asignadas.

### 6. VOICE-REGISTRATION-WIZARD1

**Problema que resuelve:** crear voces con muestras no debe ser un formulario improvisado dentro de la lista.

**Resultado esperado:** subvista `Nueva voz` con nombre de voz, emoción, frase sugerida, grabación Java, detener, reproducir, eliminar, asignar muestra y guardar.

**No negociable:** una voz avanzada no se guarda sin muestra `Neutral`.

**Regla técnica:** la grabación se implementa con Java/API de audio, no con Python.

**Regla de almacenamiento:** las muestras viven en la biblioteca de voces de la app/runtime/repositorio local, no dentro de cada proyecto.

### 7. VOICE-LIBRARY-SYNC1

**Problema que resuelve:** Documento debe ver inmediatamente las voces/tonos recién registrados.

**Resultado esperado:** al guardar `María · Neutral/Feliz/Triste`, Documento permite elegir `María` y solo muestra esos tonos.

**Regla clave:** las muestras son referencias para Coqui/XTTS; no son clips finales fijos.

### 8. DOCUMENT-SIDEBAR-VOICE-UX1

**Problema que resuelve:** Documento debe distinguir con claridad Voz IA generada vs audio local elegido por el usuario.

**Resultado esperado:** el sidebar ofrece `Voz`, `Tono` y `Audio` con labels simples. El tono depende de la voz seleccionada y solo muestra muestras registradas.

### 9. FIRST-USE-ONBOARDING1

**Problema que resuelve:** primera experiencia puede quedar dominada por la descarga pesada de Voz IA avanzada.

**Resultado esperado:** camino rápido: Voz local simple → abrir documento → escuchar.

### 10. EXPORT-READINESS-UX1

**Problema que resuelve:** audio/video pueden fallar tarde, cuando el usuario ya eligió exportar.

**Resultado esperado:** la app muestra antes qué exportaciones están listas y qué falta.

### 11. SETTINGS-RUNTIME-UX1

**Problema que resuelve:** Configuración es funcional, pero mezcla pasos, descargas, importaciones, prueba, reproducción y uso.

**Resultado esperado:** configuración por pasos humanos: preparar → probar → reproducir prueba → usar.

## Bloque 2 — refactor transversal y exportaciones reales

### 12. RF-TX2A — runtime paths + labels de motores

Centraliza rutas de XTTS/Piper/FFmpeg y labels humanos de motores.

### 13. RF-TX2B — mensajes humanos transversales

Saca `friendlyEngineText` y equivalentes de vistas grandes; crea política compartida de mensajes.

### 14. RF-TX2C — ExternalProcessRunner

Unifica ejecución de Python, FFmpeg, Piper, GPU smoke y probes.

### 15. RF-TX2D — ManagedDownloadService

Unifica descargas de XTTS, Piper y FFmpeg.

### 16. AUDIO-EXPORT-FINAL-HF1

Exportación WAV/MP3/AAC en segundo plano, con progreso y cancelación.

### 17. VIDEO-RUNTIME-SMOKE1

Smoke real de MP4 final con FFmpeg local.

### 18. PERSISTENCE-RC1

Guardar/reabrir proyecto completo con documento, voces, tonos, imágenes, audio, jobs, manifest y exportaciones.

## Bloque 3 — limpieza, gates y RC

### 19. TEST-CLEAN1

Clasifica tests vivos, históricos y source guardrails. Reduce ruido sin perder protección.

### 20. DOCS-CLEAN1

Reduce documentación operativa a `DOCUMENTACION_ACTUAL/`, README/AI_HANDOFF/VALIDATION cortos y docs históricos archivados.

### 21. PF9

Anti-placeholder global. Todo botón visible funciona o se deshabilita con motivo humano.

### 22. DIAGNOSTIC-SCRIPTS-RC1

Separa diagnóstico base, motores reales, smoke CUDA y perfiles RC.

### 23. PACKAGING-MEMORY-RC1

Asegura heap explícito para app-image/portable, no solo para ejecución Maven.

### 24. LEGAL-THIRD-PARTY-RC1

Solo si se distribuye públicamente. Cierra licencias/checksums de motores, modelos y herramientas.

### 25. RC-GATE1

Release candidate personal con diagnóstico, motores, audio, video, persistencia, docs y tests en estado aprobado.

## Regla final

No adelantar refactor grande ni limpieza documental antes de cerrar el bloque de voces. Vista Voces y Documento forman una unidad operativa: primero se registran voces/muestras correctamente, luego Documento puede asignarlas con honestidad.


### Actualización VOICE-REGISTRATION-WIZARD1

La tanda queda implementada como subvista operativa de Nueva voz. Mantiene Java Sound para grabación y compuerta Neutral. La sincronización completa hacia Documento queda en VOICE-LIBRARY-SYNC1.
