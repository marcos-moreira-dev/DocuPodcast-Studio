# Tanda 7 implementada — Audio job mock con progreso y ETA

## Objetivo

Crear la primera cadena funcional de generación de audio sin conectar todavía XTTS, Piper ni otro motor TTS real. La tanda sirve para probar la arquitectura de jobs largos, progreso, ETA, cancelación cooperativa, salida por segmentos y workspace de audio.

## Resultado

La aplicación ya puede tomar un `NarrationScriptDocument` y lanzar una generación mock. El mock crea WAVs silenciosos válidos por segmento y un WAV final también silencioso. Esto permite validar la UX de progreso sin depender aún del motor real.

## Flujo implementado

```text
Word/DOCX
→ Documento normalizado
→ Reading Profile
→ Guion narrable
→ Generar audio mock
→ jobs/JOB-*/audio/SEG-*.wav
→ jobs/JOB-*/final/podcast-mock.wav
→ jobs/JOB-*/audio-manifest.json
```

## Piezas agregadas

### Dominio

- `AudioJobState`
- `AudioGenerationStage`
- `AudioSegmentStatus`
- `AudioClipReference`
- `AudioManifest`

### Aplicación

- `AudioGenerationGateway`
- `AudioGenerationRequest`
- `AudioJobStatusDto`
- `SubmitAudioGenerationJobUseCase`
- `CancelAudioGenerationJobUseCase`
- `ListAudioGenerationJobsUseCase`
- `AudioApplicationServices`

### Infraestructura

- `MockAudioGenerationGateway`
- `InMemoryAudioJobQueue`
- `MockWavWriter`

### Presentación

- `AudioWorkspaceView`
- menú `Audio`
- toolbar con `Generar audio mock`
- `DocuPodcastShellViewModel` con estado de job activo

## Comportamiento de progreso

El DTO expone:

- estado;
- etapa;
- segmentos completados/totales;
- progreso porcentual;
- segmento actual;
- título del segmento actual;
- ETA aproximada;
- carpeta del job;
- audio final;
- manifest.

## Decisiones

- La UI no invoca un motor concreto; llama a `AudioGenerationGateway`.
- El mock usa `ExecutorService` de un hilo para no bloquear JavaFX.
- La cancelación es cooperativa.
- El proyecto debe guardarse antes de generar audio para poder crear `jobs/` junto al `.docupodcast.json`.
- Al completar, se registran assets `AUDIO_FINAL` y `AUDIO_MANIFEST`.

## Limitaciones

- No hay TTS real.
- No hay reanudación persistente de jobs.
- No hay retry por segmento.
- El manifest es mínimo.
- No se registra cada clip como asset individual todavía.

## Validación

Se agregó cobertura para:

- manifest y clips de audio;
- DTO de progreso/ETA;
- mock gateway generando WAVs y manifest;
- workspace de audio con progreso, ETA y cancelación.

La validación Maven debe ejecutarse localmente con Java 21 Eclipse Temurin y Maven Toolchain.
