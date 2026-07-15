# Tanda 9 — Reanudación de jobs

## Objetivo

Persistir y reanudar trabajos largos.

## Archivos

- `job.json`;
- `segments-status.json`;
- `audio-manifest.json`;
- `generation-log.jsonl`.

## Casos

- cancelar;
- fallar en segmento;
- reabrir proyecto;
- reintentar fallidos;
- continuar pendientes.

## Criterios

- No regenera segmentos completados.
- Mantiene audio parcial.
- Exporta audio parcial con advertencia.
- Marca job incompleto.
