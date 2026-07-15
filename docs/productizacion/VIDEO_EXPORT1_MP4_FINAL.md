# VIDEO-EXPORT1 — Exportación MP4 final

## Objetivo

El flujo normal de **Exportar video** debe producir un archivo `.mp4` final, no un paquete técnico visible para el usuario.

## Contrato implementado

- El comando visible queda como **Exportar video**.
- La UI muestra una ventana secundaria para elegir calidad: **4K, 2K, 1080p o 720p**.
- Después se usa `FileChooser` para escoger el archivo `.mp4` de salida.
- El caso de uso `ExportFinalVideoUseCase` valida que Video local esté listo mediante `FfmpegRuntimeProbeUseCase.readyForFinalVideo()`.
- Si faltan imágenes/audio en la secuencia visual, se detiene con mensaje humano antes de renderizar.
- El paquete técnico histórico queda disponible como infraestructura/diagnóstico, no como flujo principal.

## Alcance

Esta tanda no modifica Voz IA avanzada ni revisa la descarga del modelo. Esa revisión queda pospuesta hasta que se aporte el diagnóstico/reporte post-descarga.
