# FFMPEG-PREP1 — Video local descargable y verificable

## Objetivo

Convertir **Preparar video local** en una acción real de producto: descargar el paquete configurado, extraerlo dentro de la carpeta de la aplicación, copiar los componentes necesarios a `tools/ffmpeg/bin` y verificar que sirven para exportar video final.

## Alcance

- Se agrega `DownloadFfmpegPortableRuntimeUseCase`.
- Se agrega `FfmpegRuntimeDownloadReport`.
- `SettingsApplicationServices` expone `downloadFfmpegPortableRuntime`.
- `ApplicationServicesFactory` cablea el caso de uso.
- En Configuración > Motores de voz y media > Video local, el botón **Preparar** descarga desde la URL guardada y verifica el runtime.
- **Importar carpeta** queda como alternativa manual/soporte.

## Contrato técnico

El runtime final vive en:

```text
tools/ffmpeg/bin/ffmpeg.exe
tools/ffmpeg/bin/ffprobe.exe
```

La descarga temporal vive bajo:

```text
tools/ffmpeg/downloads/video-local-runtime.zip
tools/ffmpeg/downloads/extracted/
```

La verificación usa `FfmpegRuntimeProbeUseCase.readyForFinalVideo()` y exige:

- ejecutable de video local presente;
- herramienta de inspección de video presente;
- soporte de `libx264` como fallback CPU para video final.

## UX

La UI normal habla de **Video local** y **componentes de video**. Los nombres técnicos quedan en código, reportes o documentación técnica, no como operación principal.

## Fuera de alcance

- No renderiza todavía `.mp4` final directo.
- No implementa selector de calidad de exportación final.
- No cambia el paquete técnico/auditable existente.

Eso queda para `VIDEO-EXPORT1`.
