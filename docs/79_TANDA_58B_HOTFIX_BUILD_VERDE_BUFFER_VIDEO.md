# Tanda 58B — Hotfix build verde: buffer y video

## Objetivo

Dejar la base Tanda 58 en condiciones de revalidación local antes de avanzar a auditoría GUI, cambios de scaffolding o refactor grande.

La tanda es deliberadamente pequeña: corrige dos contratos fuente que dejaron el build rojo en la validación local, sin introducir nuevas capacidades de producto.

## Cambios aplicados

### 1. Mensaje de espera de buffer centralizado

Antes, `DocuPodcastShellViewModel` escribía manualmente una frase de espera distinta a la política de dominio:

```text
Esperando el siguiente fragmento de audio.
```

`PlaybackBufferPolicy.waitingLabel()` ya tenía la frase canónica:

```text
Preparando el siguiente fragmento de audio; la lectura continuará automáticamente.
```

Ahora el ViewModel reutiliza `playbackBufferPolicy.waitingLabel()`. Esto evita dos textos para el mismo estado y mantiene el contrato de streaming/prebuffer en una sola política.

### 2. Contrato honesto de video simple

Tanda 58 preparó video simple 2K, presets y contrato FFmpeg, pero la exportación todavía genera paquete renderizable/auditable, no MP4 final directo.

Se actualizó `ExportSimpleVideoPackageUseCase` para decirlo explícitamente:

```text
paquete renderizable/auditable
MP4 final pendiente de conectar el render por frames con FFmpeg
```

El script `render-video-simple.bat` deja de presentar el TODO como promesa difusa y declara que debe conectarse el render por frames desde `frames.csv` para producir `video-simple.mp4`.

## Tests alineados

```text
StreamingPlaybackBufferSourceTest
SimpleVideoExportSourceTest
```

Los tests ahora protegen el contrato correcto:

- el mensaje de buffer vive en `PlaybackBufferPolicy` y el shell lo consume;
- video simple se describe como paquete renderizable/auditable;
- MP4 final no se promete mientras no exista render real completo.

## Qué no cambia

Esta tanda no implementa:

- render MP4 real por FFmpeg;
- rediseño de UI;
- migración de botones hardcodeados;
- refactor de `DocuPodcastShellViewModel`;
- smoke real de usuario.

Esos temas pasan a las siguientes tandas.

## Validación esperada

En Windows, desde `scripts`:

```bat
.\02-ejecutar-tests.bat
```

Resultado esperado:

```text
Tests OK
0 failures
0 errors
```
