# Roadmap posterior a T74 — Capas reales

## Estado tras T74

Las capas narrativas ya no dependen de placeholders productivos. La app puede asignar voz/estilo reales desde la biblioteca y exige assets reales para imagen/audio.

## T75 — Storyboard como capa del documento

Con las capas de imagen apuntando a assets reales, T75 debe consolidar la relación:

```text
texto seleccionado → imagen real → duración según audio del fragmento
```

Debe soportar una misma imagen asociada a varios fragmentos sin duplicar el asset.

## T76 — Video/render contract

Debe convertir el storyboard en plan de video: duración, frame, resolución, audio vinculado, FFmpeg y estado de render.

## T77 — Integridad/reparación

Debe validar que las capas apuntan a targets existentes, detectar assets faltantes, checksums obsoletos y audio/capas en revisión tras refrescar fuente.
