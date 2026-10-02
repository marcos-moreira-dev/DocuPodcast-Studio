# Lector PDF semántico

**Estado:** estable y congelado desde 2026-08-10.

Este documento es la autoridad arquitectónica del lector PDF. Reabrirlo exige una decisión explícita, un corpus versionado y actualizar pruebas, firma y documentación.

## Flujo canónico

```mermaid
flowchart LR
 A["PDFBox: texto y render"] --> B["Qwen PAGE_SEMANTIC_READING"]
 B --> C["Block V1"] --> D["Parser y validación Java"]
 D --> E["Cobertura PDFBox"]
 E -->|duda| F["Máximo un verificador"]
 F -->|huecos| G["Máximo una recuperación: 3 ROI a 300 DPI"]
 E -->|aceptada| H["Merge determinista"]
 G --> H --> I["PreparedPdfPage / PdfRegion"]
 I --> J["Persistencia atómica"] --> K["NarrationSegment"]
 K --> L["TTS incremental, playback y resaltado"]
```

La ruta productiva es única. No existen una ruta JSON alternativa, GBNF, servidor llama.cpp paralelo ni perfiles experimentales de concurrencia. `PreparedPdfPage`/`PdfRegion` son canónicos para PDF. `ReadableDocument`/`DocumentBlock` lo son para Word; un `DocumentBlock` sintético PDF solo es compatibilidad. `NarrationSegment` converge audio y `DocumentContentItem`/`DocumentContentProjection`, audiovisual.

## Block V1

El contrato usa `PAGE / BEGIN / SOURCE / SPEECH / END / DONE`. Un delimitador es control solo en la línea y posición exactas de la gramática. `SOURCE` admite libremente JSON, código, llaves, corchetes, fórmulas y texto ordinario.

Java posee parsing, validación, normalización, IDs, `readingOrder`, revisiones, JSON y persistencia. Qwen no genera el JSON canónico. Sin `DONE`, una respuesta inválida o truncada no sustituye una página válida.

Alias congelados:

| Alias | Canónico |
|---|---|
| `HEADLINE`, `SECTION_TITLE` | `HEADING` |
| `SUBTITLE`, `SUBSECTION_TITLE` | `SUBHEADING` |
| `PROSE`, `TEXT` | `PARAGRAPH` |
| `LIST_ITEM` | `LIST` |
| `NOTE`, `CALLOUT`, `BOX`, `DEFINITION` | `SIDEBAR` |
| `EQUATION`, `FORMULA` | `MATH` |
| `FIGURE`, `GRAPH`, `DIAGRAM` | `IMAGE` |

`FIGURE` solo se convierte en caption con evidencia estrecha de pie. La confianza Qwen se valida como campo de transporte V1, pero no decide aceptación: internamente es `0.0` y `modelConfidence=not-reported`.

## Validación y recuperación

La autoridad se aplica por capas: sintaxis, coherencia semántica/local, cobertura contra texto nativo fiable y verificación visual si hace falta. Umbrales empíricos congelados:

- tokens globales: `0.84`;
- bloque largo: `0.58`;
- bloque corto: `0.80`.

`0.80` es el baseline real para bloques cortos. El histórico `0.68` quedó obsoleto por permitir omisiones. Recalibrar requiere cambio explícito y nueva evidencia.

Hay como máximo un verificador y una ronda de recuperación de hasta tres ROI a 300 DPI. No hay retry automático idéntico al faltar presupuesto o `DONE`. Transporte transitorio, timeout/stall, OOM, truncación, protocolo inválido y cancelación son estados distintos.

La evidencia insuficiente rechaza sin publicar parcialmente. Tampoco dispara automáticamente native/OCR: la preparación estándar nativa/OCR es un flujo explícito separado.

## Persistencia e identidad

Manifest y página usan schema V3, `sourceSha256` y `pdf-semantic-reader-block-v1-frozen-2026-08-10`. Una firma anterior regenera una vez; una compatible se reutiliza. Las escrituras son atómicas y el cleanup best-effort no oculta el error original.

IDs de región y `contentId` expresan identidad semántica estable. Reinterpretar el mismo contenido preserva identidad y ediciones; contenido realmente nuevo obtiene otro ID. Fingerprints de narración, voz y estilo invalidan solo los WAV afectados.

## Runtime y recursos

El perfil probado de esta operación usa Qwen3-VL 4B Instruct Q8_0, contexto 8192, KV Q8_0, Flash Attention, batch 512 y GPU-first con offload a RAM/CPU. No es regla universal. Cada página tiene contexto independiente; el modelo puede residir entre páginas, sin compartir historial. Ollama permanece serial (`parallel=1`, `max loaded models=1`).

La GTX 1650 4 GB es soportada con derrame al host: baseline aproximado 2–3+ tok/s y varios minutos en páginas densas. `MediaCapabilityService` y la instancia global única de `PriorityResourceScheduler` gobiernan recursos. La admisión efectiva es uno; los crops CPU internos no readquieren el scheduler.

## Observabilidad y alcance

Se registran aceptación/rechazo, cobertura, regiones, verificador, ROI, duración y categoría técnica. La UI muestra mensajes accionables sin umbrales ni trazas. Cancelación, evidencia insuficiente, fallo técnico y formato no soportado no se mezclan. Temporales pertenecen al request y se limpian al finalizar.

FAST_LISTEN, prefetch, TTS incremental, reproducción y resaltado están cerrados en el núcleo. El checklist JavaFX manual es de release, no deuda arquitectónica. Fuera de alcance: generación ilustrativa, MP4 incremental, concurrencia Qwen, paginación Word exacta y cambios al modelo Word. Solo se reabre por defecto reproducible, runtime/modelo incompatible, corpus con fallos materiales o decisión arquitectónica expresa.
