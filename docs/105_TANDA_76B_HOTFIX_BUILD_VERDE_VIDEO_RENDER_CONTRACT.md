# Tanda 76B — Hotfix build verde del contrato de video

## Objetivo

T76B corrige el fallo local detectado en `VideoRenderContractSourceTest` sin cambiar lógica productiva ni UX. La base T76 ya tenía el contrato de video simple correcto, pero el source test era sensible a mayúsculas/minúsculas al validar la frase `cancelación segura` dentro de `docs/productizacion/VIDEO_RENDER_CONTRACT_T76.md`.

## Cambio aplicado

- `VideoRenderContractSourceTest` normaliza el contenido documental con `toLowerCase(Locale.ROOT)` antes de validar `bloqueo operativo` y `cancelación segura`.
- Se conserva la validación de `docupodcast-simple-video-render-v1`, `renderableAsMp4`, `RENDER_MANIFEST.json`, `render-commands.txt`, `RENDER_STATE.md` y `MP4 final`.
- No se modifica `VideoRenderCommandPlan`, `BuildVideoRenderCommandPlanUseCase` ni `ExportSimpleVideoPackageUseCase`.

## Decisión

El problema era de guardarraíl textual, no de cerebro funcional. La corrección se hace en el test porque el documento ya contenía el concepto correcto como encabezado `Cancelación segura`.

## Validación esperada

En entorno local con Maven/Toolchain:

```bat
scripts\02-ejecutar-tests.bat
```

Resultado esperado: build verde sobre la misma base funcional de T76.

## Continuidad

Después de T76B, el siguiente bloque debe seguir centrado en cerebro:

1. T77 — Integridad y reparación del proyecto.
2. T78 — Exportaciones del cerebro.
3. T79 — Smoke automático del cerebro.
4. T80A — Dispositivo de inferencia CPU/GPU y rendimiento.
5. T80B — Entrada flexible de media: MP3/WAV/video a audio.
6. T80 — Congelación del cerebro V1.
