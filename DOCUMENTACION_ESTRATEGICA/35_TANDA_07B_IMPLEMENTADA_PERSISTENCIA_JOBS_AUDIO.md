# Estrategia — Tanda 7B persistencia temprana de jobs

## Por qué se agregó esta tanda opcional

La Tanda 8 pretende conectar un gateway TTS real. Antes de eso, conviene asegurar que el producto ya sabe guardar y leer jobs. Sin esto, cualquier falla del motor real podría dejar audios parciales sin trazabilidad.

## Contrato persistente mínimo

- `job.json`: estado agregado del job.
- `segments-status.json`: estado de cada segmento.
- `audio-manifest.json`: clips generados y audio final.
- `generation-log.jsonl`: eventos técnicos del job.

## Valor de producto

Permite explicar al usuario:

- qué segmentos ya fueron completados;
- qué segmento estaba activo;
- dónde están los WAVs parciales;
- si el job se completó, falló o quedó interrumpido;
- qué se podría reintentar en una tanda posterior.

## Siguiente tanda opcional recomendada

Tanda 7C — Recuperación UI y reintento desde jobs persistidos. Recomendable antes de TTS real si se quiere máxima robustez.
