# Tanda 34 — Podcast final real

## Objetivo

Convertir la exportación de podcast en una salida real de producto: si el job ya tiene WAV final, se copia; si el TTS real solo generó WAVs por segmento, la app concatena esos segmentos PCM WAV en un único archivo final.

## Cambios principales

- `ExportPodcastWavUseCase` ahora devuelve `PodcastFinalWavExportResult`.
- Se agrega `PcmWavConcatenator` para unir WAV PCM compatibles sin depender de motores externos.
- Se genera reporte lateral `*.export-report.md` con job fuente, modo de exportación, segmentos y fuentes usadas.
- `ProjectExportFormatPolicy` considera exportable un job completado con segmentos WAV aunque no tenga `finalAudioPath`.
- El export bundle usa la misma cadena de exportación y puede producir `output/podcast.wav` desde segmentos.
- `DocuPodcastShellViewModel` informa si exportó copiando WAV final o concatenando segmentos.

## Alcance

La unión exige WAV PCM con el mismo formato entre segmentos. No remezcla, no normaliza volumen ni cambia sample rate; esas mejoras quedan para una fase posterior de motores avanzados/audio mastering.
