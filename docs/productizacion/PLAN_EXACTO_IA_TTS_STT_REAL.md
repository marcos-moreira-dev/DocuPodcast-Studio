# Plan exacto para integrar IA real de voz y transcripción

Este documento responde a la pregunta central: qué falta para que DocuPodcast Studio produzca voz real desde texto y transcriba audio real desde sonido.


## Cierre de decisión T83: pocos motores oficiales

Después de la congelación del cerebro V1 y del rediseño T81, la ruta oficial queda reducida a pocos motores:

```text
TTS promedio/liviano: Piper
TTS avanzado/muy realista: XTTS / Coqui mediante wrapper externo
STT oficial: whisper.cpp
Media auxiliar: FFmpeg
```

La aplicación no debe mostrar diez motores ni abrir rutas incompletas. Primero debe cerrar una demo funcional con Piper y whisper.cpp. XTTS/Coqui queda como motor avanzado, potente y más pesado.

Ver también: `docs/productizacion/T83_PLAN_CERRADO_MOTORES_IA_REAL.md`.

## Estado actual honesto

La aplicación ya tiene una parte importante del cerebro lista:

- `AudioGenerationGateway` como puerto de generación de audio.
- `MockAudioGenerationGateway` para pruebas y smoke.
- `LocalTtsProcessAudioGenerationGateway` para ejecutar un motor TTS externo mediante plantilla de comando.
- Persistencia de jobs de audio, segmentos WAV, manifests, logs y diagnóstico.
- Configuración operativa persistente.
- Políticas CPU/GPU y placeholders de dispositivo para procesos externos.
- `SpeechToTextGateway` como puerto STT.
- `WhisperCppSpeechToTextGateway` para ejecutar Whisper local por proceso.
- Inspección de carpetas de modelos.
- Asistente/catalogación de modelos en la UI.

Pero todavía falta convertir esto en una experiencia de usuario completamente real y guiada. El usuario normal no debe instalar motores por consola ni escribir comandos complejos.

## Decisión recomendada de motores V1

Para no abrir demasiadas líneas a la vez, conviene establecer una ruta oficial V1:

### TTS liviano oficial

```text
Piper
```

Uso esperado: lectura local rápida con voces predescargadas. Es más liviano que XTTS y sirve como primer motor real para “Escuchar documento”.

### TTS potente opcional

```text
XTTS / Coqui compatible por wrapper externo
```

Uso esperado: voces de mayor calidad y estilos más ricos. Debe tratarse como motor avanzado por el peso de modelos y dependencias.

### STT oficial

```text
whisper.cpp
```

Uso esperado: transcripción local de audio a texto usando modelos `ggml`/compatibles, con ejecutable local y modelos en carpeta configurada.

## Lo que falta implementar exactamente

### 1. Catálogo real de motores instalables

Crear o madurar un catálogo con entradas verificables:

```text
TTS_PIPER
TTS_XTTS_WRAPPER
STT_WHISPER_CPP
FFMPEG
```

Cada entrada debe declarar:

```text
nombre visible
propósito
carpeta esperada
ejecutable esperado
archivos de modelo esperados
checksum opcional
comando de prueba
compatibilidad CPU/GPU
estado: instalado / incompleto / no encontrado / error
```

### 2. Instalador/asistente de motores

La UI ya tiene un asistente conceptual. Falta volverlo operativo.

Debe permitir:

```text
Seleccionar carpeta local del motor
Seleccionar ejecutable
Seleccionar carpeta o archivo de modelo
Verificar preflight
Ejecutar prueba corta
Guardar configuración
```

Opcional para futuro:

```text
Descargar desde un manifiesto firmado/verificable
```

No conviene hardcodear enlaces externos dentro de la lógica central. Mejor usar un `models-catalog.json` externo o versionado.

### 3. Piper real

Implementar una ruta oficial para Piper:

```text
runtime/tts/piper/piper.exe
runtime/tts/piper/voices/<voz>.onnx
runtime/tts/piper/voices/<voz>.onnx.json
```

Comando típico a parametrizar por configuración:

```text
piper.exe --model <voice.onnx> --output_file <output.wav>
```

Se necesita un adapter específico o plantilla robusta que pase texto por archivo/stdin y genere WAV por segmento.

Tareas concretas:

```text
PiperTtsConfiguration
PiperTtsPreflight
PiperTtsAudioGenerationGateway o perfil oficial dentro de LocalTtsProcessAudioGenerationGateway
PiperVoiceCatalogReader
Prueba real de una frase corta
Documentación de instalación offline
```

### 4. XTTS / Coqui por wrapper

XTTS no debería meterse directamente en Java como dependencia pesada. Conviene usar un wrapper externo.

Estructura sugerida:

```text
runtime/tts/xtts/tts_worker.exe o wrapper Python local administrado por DocuPodcast
models/tts/xtts/
voices/samples/
```

Tareas concretas:

```text
XttsWrapperProfile
verificación de Python/worker o ejecutable empaquetado
comando estándar con {textFile}, {outputFile}, {voice}, {language}, {computePolicy}, {gpuIndex}
prueba corta
registro de logs por segmento
cancelación fuerte del proceso
```

XTTS debe quedar como motor potente/avanzado. Piper debe ser el camino más directo para V1.

### 5. Whisper.cpp real

Ya existe `WhisperCppSpeechToTextGateway`, pero falta la experiencia completa.

Estructura esperada:

```text
runtime/stt/whisper/whisper-cli.exe
models/stt/whisper/ggml-base.bin u otro modelo
```

Tareas concretas:

```text
WhisperCppPreflight ejecutable desde UI
selector de modelo
selector de idioma
prueba corta de transcripción
mensajes humanos si falta ejecutable o modelo
logs stdout/stderr accesibles desde diagnóstico
```

### 6. FFmpeg real para extracción de audio y video simple

FFmpeg afecta:

```text
extraer audio desde video
normalizar audio
exportar video simple
```

Tareas concretas:

```text
FFmpeg preflight real desde Configuración
selector de ejecutable FFmpeg
preferencia por FFmpeg embebido si existe
prueba de versión `ffmpeg -version`
validación de extracción de audio desde un video corto
registrar ruta efectiva en manifest
```

### 7. Integrar motores con Documento sin exponer técnica

En Documento debe verse lenguaje humano:

```text
Preparando audio…
Generando voz…
Audio listo.
No se encontró motor de voz. Configúralo en Configuración.
```

No mostrar en Documento:

```text
CUDA
wrapper
commandTemplate
model.bin
stdout
stderr
```

Eso queda en Configuración/Diagnóstico.

### 8. Mejorar estados de error

Cada motor debe devolver errores accionables:

```text
Falta ejecutable
Falta modelo
Modelo incompatible
No se pudo escribir WAV
El motor tardó demasiado
El proceso fue cancelado
GPU solicitada no disponible; se usó CPU
```

### 9. Pruebas automáticas y manuales

Pruebas unitarias/smoke:

```text
Piper preflight sin ejecutable
Piper preflight con ejecutable falso controlado
Whisper preflight sin modelo
FFmpeg preflight sin binario
LocalTtsProcessAudioGenerationGateway con worker fake
WhisperCppSpeechToTextGateway con worker fake
```

Pruebas manuales obligatorias:

```text
DOCX simple → voz Piper → reproducir
TXT largo → buffer/generación por segmentos
MP3 asignado a oración
Video MP4 → extraer audio → asignar a oración
Audio corto → Whisper → texto
Exportar paquete con audio/model logs/manifiestos
Guardar/cerrar/reabrir y reproducir
```

## Orden de implementación recomendado actualizado tras T85

### T83 — Plan cerrado de motores IA

Objetivo: reducir la matriz de motores a Piper, Coqui XTTS, whisper.cpp y FFmpeg.

### T84 — Preflight operativo de motores

Objetivo: que Configuración diga con claridad qué está instalado y qué falta, con Coqui XTTS marcado como obligatorio para calidad alta.

### T85 — Piper TTS real end-to-end

Objetivo: producir WAV real desde texto usando Piper como motor liviano, sin depender todavía de XTTS pesado.

### T86 — Coqui XTTS wrapper real

Objetivo: cerrar el motor de voz de calidad alta obligatorio.

### T87 — Whisper.cpp como STT real

Objetivo: transcribir audio real desde archivo.

### T88 — FFmpeg operativo completo

Objetivo: extraer audio desde video y validar render simple.

### T89 — UX de motores para usuario normal

Objetivo: mensajes humanos, botones de prueba, asistente de modelos y errores accionables.

### T90 — Smoke real de IA local y Release Candidate

Objetivo: probar en máquina Windows real con motores y modelos seleccionados, y cerrar RC solo cuando Documento pueda leer con voz real y STT funcione al menos con ruta oficial.

## Decisión crítica pendiente

Hay que decidir si la aplicación:

1. **No incluye modelos** y solo guía al usuario a instalarlos.
2. **Incluye un motor liviano y una voz mínima** para que funcione out-of-the-box.
3. **Descarga modelos desde catálogo verificable**.

Recomendación: para V1 práctica, usar opción 2 con Piper liviano si las licencias/tamaño lo permiten, y opción 1 para XTTS/Whisper grandes.

## Resumen ejecutivo

El cerebro ya está preparado para motores externos. Lo que falta no es rehacer toda la app, sino cerrar la cadena completa:

```text
Configurar motor → verificar modelo → probar una frase/audio → generar/transcribir → registrar logs → mostrar estado humano → guardar/reabrir/exportar
```

Sin esa cadena, la app puede verse lista, pero no cumplirá su promesa principal.

## Actualización T84 — Coqui obligatorio y preflight

T84 agrega preflight ejecutable para Coqui XTTS, Piper, whisper.cpp y FFmpeg. La regla de producto queda cerrada: Coqui es obligatorio para calidad alta; Piper solo acelera la primera demo. Los modelos viven fuera del `.docupodcast`, en carpetas locales verificables, y Configuración debe guiar descarga/importación/prueba sin exponer comandos en Documento.


## T85 — Piper TTS real end-to-end

Piper queda como primer motor funcional: `engineMode=piper` puede derivar un comando hacia `scripts/tts/piper-file-to-wav.ps1`, `tools/piper/piper.exe` y `models/tts/piper/voices/<voz>.onnx`. La siguiente tanda debe cerrar Coqui XTTS como motor de calidad alta.

## Actualización T86 — Coqui XTTS con voz por defecto

Coqui XTTS se mantiene como motor de calidad alta obligatorio. Piper sirve como demo/respaldo, pero el objetivo del producto es una voz más realista.

T86 agrega:

```text
scripts/tts/xtts-file-to-wav.ps1
tools/xtts-wrapper/synthesize_xtts.py
models/tts/xtts/speakers/voz-por-defecto.wav
samples/voices/default/source/voz-por-defecto.mp4
```

La fuente recibida se llama `.mp4`, pero contiene audio. Se conserva como fuente original y se convierte a WAV mono 24 kHz para XTTS.

El wrapper no descarga modelos automáticamente. Deben instalarse Python, Coqui TTS, PyTorch y el modelo XTTS en carpetas locales verificables. La UX debe esconder esos detalles en Configuración/Preflight.


## Actualización T87 — STT real con whisper.cpp

T87 agrega el camino ejecutable de STT: audio local → normalización WAV → `whisper.cpp` → transcript TXT → logs dentro del proyecto.

El smoke real pendiente requiere un `whisper-cli.exe` real y un modelo local en `models/stt/whisper/`. La interfaz futura debe mostrar mensajes humanos y no flags técnicos.
