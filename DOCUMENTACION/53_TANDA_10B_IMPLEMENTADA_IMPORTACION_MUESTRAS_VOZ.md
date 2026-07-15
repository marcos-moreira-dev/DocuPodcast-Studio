# Tanda 10B — Importación de muestras de voz y assets VOICE_SAMPLE

## Estado

Implementada.

Esta tanda permite importar una muestra de voz seleccionada por el usuario y copiarla dentro de la carpeta portable del proyecto, sin incrustar audio binario en `.docupodcast.json`.

## Decisión importante

Los archivos de audio estáticos reales quedan fuera del repositorio fuente. El usuario subió muestras como `grabacion.wav`, `grabacion_converted.wav` y `speaker.wav`; sirven para pruebas manuales locales, pero no se incluyen en el ZIP del código para evitar peso y privacidad.

## Cadena implementada

```text
Archivo WAV/MP3/FLAC/OGG/M4A elegido por el usuario
→ ImportVoiceSampleUseCase
→ LocalVoiceSampleFileRepository
→ voices/samples/<asset>.wav
→ ProjectAssetReference kind=VOICE_SAMPLE
→ VoiceProfile.sampleAssetId
→ VoiceLibrary actualizada
→ .docupodcast.json + voices/voice-library.json al guardar
```

## Archivos nuevos principales

```text
application/voice/VoiceSampleImportRequest.java
application/voice/VoiceSampleImportResult.java
application/voice/VoiceSampleRepository.java
application/voice/ImportVoiceSampleUseCase.java
infrastructure/voice/LocalVoiceSampleFileRepository.java
```

## UI

Se agregó acción visible en:

```text
Menú Voz → Importar muestra para Mi voz…
Toolbar → Importar muestra
Workspace Voces → Importar muestra para Mi voz…
```

La UI importa por defecto al perfil inicial `VOC-OWN-PLACEHOLDER`, que representa “Mi voz”.

## Formatos aceptados

```text
.wav
.mp3
.flac
.ogg
.m4a
```

WAV sigue siendo el formato recomendado para muestras de voz, especialmente si se conectará a motores TTS o STT/Whisper en tandas posteriores.

## Assets

La muestra se registra como:

```text
ProjectAssetKind.VOICE_SAMPLE
```

con ruta relativa controlada:

```text
voices/samples/VOICE-SAMPLE-VOC-OWN-PLACEHOLDER-<archivo>.wav
```

Se calcula checksum SHA-256 y se evita guardar rutas absolutas.

## Ética y consentimiento

La tanda mantiene la regla de producto:

```text
- voz propia: puede importarse como muestra propia;
- voz autorizada/importada: requiere nota de consentimiento;
- no se fomenta clonar voces sin permiso.
```

## Lo que no implementa todavía

```text
- grabación desde micrófono;
- edición/corte de muestras;
- normalización de audio;
- validación de duración/calidad;
- conversión automática de sample rate;
- selección de múltiples perfiles destino;
- uso real de speaker_wav por el worker TTS;
- Whisper/STT real.
```

Esto queda para tandas posteriores.

## Tests agregados

```text
ImportVoiceSampleUseCaseTest
LocalVoiceSampleFileRepositoryTest
VoiceSampleImportUiSourceTest
```

Cubren:

```text
- importación de muestra propia;
- registro de asset VOICE_SAMPLE;
- enlace con VoiceProfile.sampleAssetId;
- copia a voices/samples/;
- checksum SHA-256;
- rechazo de extensión no soportada;
- existencia de acciones UI visibles.
```

## Tandas pendientes relacionadas

```text
Tanda 11  — Storyboard básico
Tanda 12  — Playback sincronizado real
Tanda 14  — Guía integrada + recursos IA
Futura     — Grabación real desde micrófono
Futura     — Whisper/STT real para audio→texto
Futura     — Normalización/conversión de muestras
```
