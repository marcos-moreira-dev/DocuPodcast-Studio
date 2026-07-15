# Estrategia — Tanda 13: Exportaciones

La capa de exportación sigue la referencia de DMS: salida activa, formatos reales, rutas normalizadas y paquetes con manifiesto.

## Salidas actuales

| Artefacto | Salida implementada |
|---|---|
| Guion | Markdown |
| Audio | WAV |
| Proyecto | Carpeta portable |
| Diagnóstico | Markdown |

## Salidas pendientes

- MP3 con encoder real.
- PDF/DOCX del guion.
- PNG preview de storyboard.
- Paquete storyboard enriquecido.
- Video simple futuro.

## Riesgo mitigado

Evitar promesas falsas: exportación solo si hay guion, audio o proyecto guardado.
