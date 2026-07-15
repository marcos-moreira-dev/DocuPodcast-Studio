# Tanda 83 — Plan cerrado de motores IA reales para voz y transcripción

## Propósito de esta tanda

La Tanda 82 dejó un handoff amplio: referencias visuales, notas de conversación, plan general de IA real y continuidad del proyecto. La Tanda 83 baja ese plan a una decisión más operativa: **no abrir demasiados motores** y concentrar la implementación real en pocos caminos oficiales.

La meta del producto no es presumir diez motores, sino que DocuPodcast Studio pueda demostrar valor real:

```text
abrir documento → escuchar con voz real → guardar/reabrir → asignar audio/imagen → transcribir audio → exportar evidencia
```

Para eso, esta tanda documenta con detalle qué falta para cerrar la promesa central de la aplicación: **texto a voz real y audio a texto real**.

## Decisión de producto

La aplicación tendrá una ruta oficial reducida:

1. **Piper** como motor TTS promedio/liviano y operativo.
2. **XTTS / Coqui compatible mediante wrapper externo** como motor TTS avanzado y de mayor realismo.
3. **whisper.cpp** como motor STT local.
4. **FFmpeg** como herramienta auxiliar para extracción/normalización de audio y video simple.

No se deben agregar más motores oficiales hasta que esta ruta funcione de extremo a extremo.

## Por qué pocos motores

Agregar demasiados motores antes de cerrar la experiencia real genera estos problemas:

- más preflight que mantener;
- más configuraciones en la UI;
- más rutas de error;
- más documentación para el usuario;
- más packaging;
- más casos de prueba;
- más deuda cuando todavía no está probado el flujo básico.

Por eso la regla queda así:

```text
Primero: Piper + whisper.cpp + FFmpeg funcionando.
Después: XTTS/Coqui como avanzado.
Nunca: diez motores incompletos en la UI.
```

## Qué debe significar “funciona”

No basta con que exista una clase Java o un campo de configuración. Para declarar un motor como funcional se exige:

1. detectar ejecutable;
2. detectar modelo;
3. ejecutar preflight;
4. generar/transcribir una muestra corta;
5. guardar logs;
6. informar errores humanos;
7. producir artefactos dentro del proyecto;
8. sobrevivir guardar/cerrar/reabrir;
9. aparecer en export bundle/diagnóstico;
10. tener smoke automático o manual documentado.

## Motor 1: Piper — TTS promedio/liviano oficial

### Rol en el producto

Piper debe ser el primer motor real de voz. No necesariamente será el más realista, pero sí debe ser el más directo para demostrar que DocuPodcast sirve.

Uso esperado:

```text
Documento → segmentos → Piper → WAV por segmento → manifest → reproducción
```

### Por qué Piper

- Es local.
- Es relativamente liviano frente a XTTS.
- Puede trabajar con modelos de voz descargables.
- Encaja con un flujo offline-first.
- Es ideal para una primera prueba real de “Escuchar documento”.

### Estructura recomendada en carpeta del proyecto/app

```text
runtime/
  tts/
    piper/
      piper.exe
      voices/
        es_<voz>.onnx
        es_<voz>.onnx.json
```

O, si no se embebe en la app:

```text
Configuración → Motores y modelos → Piper
  Ejecutable: C:\...\piper.exe
  Voz: C:\...\es_<voz>.onnx
  Config voz: C:\...\es_<voz>.onnx.json
```

### Preflight requerido

Crear o madurar:

```text
PiperTtsPreflight
PiperTtsConfiguration
PiperVoiceDescriptor
PiperVoiceCatalogReader
```

Checks mínimos:

```text
existe piper.exe
piper.exe es ejecutable
existe .onnx
existe .onnx.json
ejecutable responde a prueba corta
carpeta output del proyecto es escribible
idioma seleccionado coincide con voz o se advierte
```

### Comando esperado

La implementación exacta puede variar por versión, pero el contrato de DocuPodcast debe soportar:

```text
entrada de texto por archivo o stdin
salida WAV por archivo
idioma/voz seleccionada
logs stdout/stderr
cancelación de proceso
```

Plantilla conceptual:

```text
piper.exe --model {voiceModel} --output_file {outputFile} < {textFile}
```

### UI mínima

En Configuración:

```text
Motor de voz: Piper
Estado: Listo / Incompleto / Error
[Elegir ejecutable]
[Elegir voz]
[Probar voz]
```

En Documento:

```text
Preparando audio…
Generando voz…
Audio listo.
No se encontró motor de voz. Configúralo en Configuración.
```

No mostrar en Documento:

```text
piper.exe
.onnx
stdout
stderr
commandTemplate
```

## Motor 2: XTTS / Coqui — TTS avanzado de mayor realismo

### Rol en el producto

XTTS/Coqui no debe bloquear la primera versión funcional. Debe quedar como motor avanzado para obtener voces más realistas, estilo y clonación bajo consentimiento.

Uso esperado:

```text
Documento → segmentos → wrapper XTTS → WAV por segmento → manifest → reproducción
```

### Decisión técnica

No conviene incrustar XTTS como dependencia Java directa. Es mejor usar un **wrapper externo**:

```text
runtime/
  tts/
    xtts/
      worker.exe
      models/
      voices/
        samples/
```

O un wrapper Python/EXE configurado:

```text
xtts-worker --text {textFile} --out {outputFile} --voice {voiceSample} --lang {language} --device {computeDevice}
```

### Preflight requerido

```text
XttsWrapperPreflight
XttsWrapperConfiguration
XttsVoiceSamplePolicy
XttsLicenseAcknowledgement
```

Checks mínimos:

```text
existe worker o script
existe modelo/carpeta de modelo
existe muestra de voz si se usa clonación
se aceptó/registró licencia si aplica
puede generar WAV corto
GPU solicitada disponible o fallback CPU registrado
```

### Riesgos que deben quedar explícitos

- Dependencias pesadas.
- Licencia de modelo específica.
- Posible necesidad de GPU para experiencia fluida.
- Uso responsable de voces de terceros.
- No usar voces de personas sin permiso.

### UI mínima

En Configuración, modo avanzado:

```text
Motor avanzado: XTTS / Coqui compatible
Estado: No configurado / Listo / Error
[Elegir wrapper]
[Elegir carpeta de modelo]
[Elegir muestra de voz]
[Probar]
```

En Documento solo aparece como opción simple:

```text
Origen del audio: Voz IA
Voz: <voz configurada>
```

## Motor STT: whisper.cpp

### Rol en el producto

whisper.cpp debe ser el motor oficial para audio a texto local. No debe competir con la lectura del documento, pero sí permitir:

```text
audio del computador → transcripción → texto revisable
```

### Estructura recomendada

```text
runtime/
  stt/
    whisper/
      whisper-cli.exe
      models/
        ggml-base.bin
```

O configuración manual:

```text
Configuración → STT
  Ejecutable: C:\...\whisper-cli.exe
  Modelo: C:\...\ggml-base.bin
  Idioma: es
```

### Preflight requerido

```text
WhisperCppPreflight
WhisperModelDescriptor
WhisperLanguagePolicy
```

Checks mínimos:

```text
existe whisper-cli.exe
existe modelo ggml/bin compatible
ejecutable responde a prueba de versión o ayuda
puede transcribir audio corto
output queda dentro del proyecto
logs stdout/stderr quedan guardados
```

### UI mínima

En Configuración:

```text
Motor de transcripción: whisper.cpp
Modelo: base / small / medium / ruta personalizada
Idioma: es / auto
[Probar transcripción]
```

En flujo de usuario:

```text
Elegir audio → Transcribir → revisar texto
```

## FFmpeg

### Rol

FFmpeg no es IA, pero es herramienta obligatoria para:

- extraer audio desde video;
- normalizar audio;
- preparar video simple;
- validar formatos.

### Preflight requerido

```text
FfmpegPreflight
FfmpegVersionProbe
FfmpegAudioExtractionProbe
```

Checks mínimos:

```text
existe ffmpeg.exe
ffmpeg -version responde
puede convertir/extraer muestra corta
ruta efectiva queda en manifest
```

## Tareas exactas para cerrar IA real

### T83A — Documentar y congelar shortlist

Hecho por esta tanda: Piper, XTTS/Coqui wrapper, whisper.cpp y FFmpeg como ruta oficial.

### T84 — Preflight operativo de motores

Implementar:

```text
PiperPreflight
XttsWrapperPreflight
WhisperCppPreflight ampliado
FfmpegPreflight ampliado
EngineReadinessReport
```

UI:

```text
Configuración → Motores y modelos
muestra instalado / incompleto / error
botón probar motor
```

### T85 — Piper real end-to-end

Implementar:

```text
PiperAudioGenerationGateway
PiperVoiceCatalogReader
Piper test phrase
Piper smoke con worker real o fake controlado
```

Debe lograr:

```text
DOCX/TXT → preparar lectura → generar WAV real → reproducir
```

### T86 — whisper.cpp real end-to-end

Implementar:

```text
WhisperCppPreflight
WhisperCpp test audio
TranscribeAudioToTextUseCase conectado a UI normal
```

Debe lograr:

```text
WAV/MP3 → transcribir → guardar texto/logs
```

### T87 — FFmpeg real para media

Implementar:

```text
FfmpegVideoAudioExtractionGateway real
normalización de audio
prueba de extracción desde MP4 corto
```

Debe lograr:

```text
MP4 → audio extraído WAV → asignable al fragmento
```

### T88 — UX humana de motores

Implementar mensajes:

```text
Falta configurar motor de voz.
Falta elegir una voz.
No se encontró el modelo de transcripción.
Se usará CPU porque no se detectó GPU compatible.
Audio listo para reproducir.
```

### T89 — Smoke real de usuario

Checklist:

```text
abrir DOCX
escuchar con Piper
guardar/reabrir
asignar audio del computador
extraer audio de video
transcribir audio con whisper.cpp
exportar paquete
```

### T90 — RC real

Solo cuando todo lo anterior funcione en Windows real.

## Criterio de “valió la pena”

La aplicación debe poder mostrar una demo corta:

```text
1. Abrir un Word.
2. Presionar Escuchar documento.
3. Oír una voz real generada por Piper.
4. Seleccionar una oración.
5. Asignar una imagen o audio.
6. Transcribir un audio corto con whisper.cpp.
7. Exportar un paquete auditable.
```

Si eso funciona, DocuPodcast deja de ser scaffolding y pasa a ser producto funcional, aunque todavía sea borrador.

## Fuentes externas a revisar antes de empaquetar

- Piper: repositorio oficial y catálogo de voces.
- XTTS/Coqui: modelo XTTS-v2, licencia y requerimientos.
- whisper.cpp: releases, modelos ggml y uso de `whisper-cli`.
- FFmpeg: binarios Windows y licencias.

Las fuentes no deben mezclarse con el flujo Documento. Solo deben aparecer en documentación, Configuración o guía avanzada.
