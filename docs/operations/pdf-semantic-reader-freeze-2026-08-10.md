# Acta de congelamiento del lector PDF semántico

Fecha: 2026-08-10  
Resultado: **FROZEN = YES**.

## Baseline

- Único transporte: Block V1; publicación atómica solo tras `DONE`.
- Qwen3-VL 4B Instruct Q8_0, 8K, KV Q8, Flash, batch 512, GPU-first, serial.
- PDF canónico: `PreparedPdfPage`/`PdfRegion`; Word permanece `ReadableDocument`/`DocumentBlock`.
- Cobertura: `0.84` global, `0.58` bloque largo, `0.80` bloque corto.
- Máximo un verificador y una recuperación de tres ROI a 300 DPI.
- Manifest/página V3, SHA-256 de fuente, firma congelada y escritura atómica.
- Corpus: 12 páginas; 4 aceptadas seguras, 8 rechazadas seguras, cero omisiones silenciosas conocidas entre aceptadas.

## Limpieza

Se retiraron parser JSON alternativo, pruebas/smoke asociados y harnesses experimentales de concurrencia=2/replay. Se conservaron diagnósticos y smokes del camino productivo. Producción no admite flags para elevar paralelismo.

Se endurecieron firma de compatibilidad, persistencia `INSUFFICIENT_EVIDENCE`, temporales, JSON atómico, mensajes UI y logs. La confianza del modelo no es autoridad.

## Deuda aplazada

- Checklist visual/manual JavaFX en hardware real.
- Evolución del scheduler físico y preservación de encoder antes de concurrencia GPU.
- Generación ilustrativa y render/exportación audiovisual final.

No reabren el lector. Un nuevo trabajo PDF exige defecto reproducible o decisión expresa con corpus nuevo. Autoridades: [arquitectura](../architecture/pdf-semantic-reader.md) y [smokes](pdf-semantic-reader-smokes.md).

La corrección post-freeze de presentación y outcomes está documentada en [resultados por página](pdf-post-freeze-page-outcomes-2026-08-10.md). No modificó Block V1, firma, prompts, cobertura, recovery ni runtime.

## Verificación final

La regresión Maven completa terminó con 1.272 pruebas vigentes, 0 fallos, 0 errores y 13 smokes opt-in omitidos. También pasaron en aislamiento lifecycle de Ollama, arquitectura PDF/Qwen, persistencia, Word, audio, video y scheduler. Un primer intento solapado falló de forma transitoria en `ComfyUiTransportTest`; repetido en aislamiento y luego dentro de una ejecución completa limpia quedó verde. `git diff --check` no detectó errores, solo avisos de política LF/CRLF del working tree.
