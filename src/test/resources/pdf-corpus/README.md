# Matriz del corpus PDF V2

`PdfV2CorpusEndToEndTest` genera los fixtures con PDFBox para evitar almacenar
documentos de terceros.

| Caso | Evidencia exigida |
|---|---|
| Digital | texto nativo fiable, geometría estable y cero OCR |
| Escaneado | preparación OCR y fuente original intacta |
| Híbrido | evidencias nativas y OCR reconciliadas sin duplicar texto |
| Rotado | geometría dentro de los límites de página |
| Multicolumna | `columnIndex` separado de `readingOrder` |
| Tabla | región conservada, visible y buscable |
| Fórmula | región conservada, visible y buscable |
| OCR defectuoso | región `UNCERTAIN`, buscable y excluida del TTS |

La ejecución escribe overlays PNG y
`target/pdf-v2-corpus/pdf-v2-corpus-report.json`. Son artefactos de diagnóstico
no canónicos y no deben incorporarse al proyecto guardado.

Los fixtures reales no versionados pueden colocarse temporalmente aquí para el
smoke local. No deben contener material con licencia incompatible.
