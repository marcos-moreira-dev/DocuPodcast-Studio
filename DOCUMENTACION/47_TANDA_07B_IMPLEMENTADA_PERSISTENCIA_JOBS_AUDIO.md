# Tanda 7B implementada — Persistencia temprana de jobs de audio

## Objetivo

Cerrar la deuda inmediata de la Tanda 7: el job mock ya generaba WAVs por segmento, manifest y progreso, pero la cola seguía siendo principalmente en memoria. Esta tanda agrega persistencia temprana de jobs para que la futura integración TTS real no empiece sin contrato de recuperación.

## Implementado

- `AudioSegmentSnapshot`: estado persistible por segmento.
- `AudioJobSnapshot`: snapshot persistible de job completo.
- `AudioJobRepository`: puerto de aplicación para guardar/listar/cargar jobs.
- `AudioJobFileRepository`: adaptador filesystem que escribe:
  - `jobs/JOB-*/job.json`
  - `jobs/JOB-*/segments-status.json`
- `ListPersistedAudioJobsUseCase`: caso de uso para consultar historial persistido.
- `MockAudioGenerationGateway`: ahora persiste snapshots durante la ejecución, no solo al final.
- `AudioWorkspaceView`: muestra historial persistido de jobs y permite actualizarlo.
- `DocuPodcastShellViewModel`: al reabrir un proyecto intenta recuperar el último estado persistido de audio.

## Archivos persistidos

Cada job queda así:

```text
jobs/JOB-xxxx/
├── job.json
├── segments-status.json
├── audio/
│   ├── SEG-001.wav
│   └── SEG-002.wav
├── final/
│   └── podcast-mock.wav
├── audio-manifest.json
└── logs/
    └── generation-log.jsonl
```

## Regla de recuperación inicial

Si un job fue encontrado como `running` al reabrir el proyecto, la UI no lo trata como activo. Lo muestra como interrumpido/cancelado para evitar bloquear cierre o cambios de proyecto. La reanudación real queda para la siguiente tanda opcional.

## Validación

Se agregaron tests para:

- Snapshot de job y duplicados de segmento.
- Escritura/lectura de `job.json` y `segments-status.json`.
- Persistencia automática desde `MockAudioGenerationGateway`.

## Límite consciente

Esta tanda no implementa reanudación real ni reintento de pendientes. Solo deja el contrato persistente y la lectura de historial. La continuación natural es Tanda 7C.
