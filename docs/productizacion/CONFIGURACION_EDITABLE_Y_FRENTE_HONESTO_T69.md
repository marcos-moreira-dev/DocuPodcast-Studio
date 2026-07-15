# Configuración editable y frente honesto — T69

## Problema corregido

La configuración operativa persistía a nivel de cerebro, pero visualmente seguía pareciendo una ficha técnica. Eso dañaba la confianza porque el usuario veía valores sin forma clara de editarlos.

## Decisión

`SettingsDialog` pasa a ser superficie editable real. Los ajustes operativos viven fuera del documento fuente y se guardan en `operational-settings.properties`.

## Capacidades visibles

- Editar tamaño de lectura e interlineado.
- Editar prebuffer y lookahead.
- Editar modo de motor TTS, comando, idioma, voz, timeout y reintentos.
- Editar rutas Whisper/STT.
- Editar FFmpeg y resolución.
- Editar carpetas de modelos/exportación.
- Editar flags de diagnóstico.
- Guardar cambios.
- Restaurar predeterminados en pantalla.

## Honestidad de frente

Desde T70, el frente dice `Abrir documento` solo para formatos realmente abribles: DOCX, PDF con texto u OCR local, Markdown y TXT. PDF escaneado o sin texto extraible puede generar bloques narrables con OCR local; si OCR falla, se conserva fallback visual con aviso.
