# Memoria — Tanda 13 implementada: Exportaciones

La Tanda 13 agrega salidas reales al flujo DocuPodcast.

## Cadena cubierta

```text
Guion narrable → Markdown
Audio final de job → WAV
Proyecto activo → paquete portable
Estado técnico → reporte diagnóstico Markdown
```

## Decisiones

- DOCX fuente viaja como `input/` cuando existe.
- `.docupodcast.json` y snapshots viajan como `editable/`.
- Audio final y guion exportado viajan como `output/`.
- Jobs persistidos viajan como `jobs/`.
- README/MANIFEST viajan como `reports/`.
- MP3 queda pendiente hasta tener encoder/FFmpeg integrado.
- Video/storyboard exportable queda pendiente hasta salida visual/video real.

## Guardarraíl

No se debe mostrar una exportación si no hay cadena real. Los tests de `ProjectExportFormatPolicy` cubren el criterio inicial.
