# Smoke automático del cerebro — T79

- Escenario: `docupodcast-brain-smoke-v1`
- Estado: OK
- Inicio UTC: 2026-07-20T02:08:20.833651400Z
- Fin UTC: 2026-07-20T02:08:22.632563100Z
- Pasos OK: 10
- Advertencias: 0
- Fallos: 0

## Pasos

| Paso | Estado | Duración | Detalle |
|---|---|---:|---|
| `DOC-SOURCES` Importar fuentes y abrir PDF visual si OCR no esta disponible | OK | 75 ms | DOCX/TXT/Markdown/PDF nativo importados; PDF escaneado abierto como visual por OCR no disponible |
| `NARRATION` Construir narración interna desde Documento | OK | 1 ms | 3 segmentos narrables generados |
| `LAYERS-STORYBOARD` Asignar imagen real y construir storyboard | OK | 8 ms | 1 asset real + 1 binding storyboard desde capa IMAGE |
| `AUDIO-MOCK` Generar audio mock persistido | OK | 105 ms | 3/3 segmentos WAV listos |
| `PLAYBACK` Construir manifest de reproducción sincronizada | OK | 0 ms | 3 cues de playback construidos |
| `ROUNDTRIP` Guardar y reabrir proyecto completo | OK | 188 ms | Proyecto, documento, guion, storyboard y audio rehidratados |
| `INTEGRITY` Inspeccionar integridad del proyecto reabierto | OK | 21 ms | Integridad OK sin reparación requerida |
| `EXPORT-READINESS` Inspeccionar preparación de exportaciones | OK | 4 ms | 7 salidas exportables inspeccionadas |
| `EXPORT-BUNDLE` Exportar paquete auditable | OK | 1.34 s | Bundle exportado con EXPORT_READINESS.md y manifiesto |
| `VIDEO-PACKAGE` Exportar paquete de video simple | OK | 21 ms | 3 frames en paquete de video simple |

## Evidencia generada

- `bundle/smoke_cerebro_t79_export_20260719-210821`
- `simple-video`
- `project-tree.txt`
- `EXPORT_READINESS.md`
- `PROJECT_INTEGRITY.md`

## Alcance

Este smoke automático valida el cerebro sin JavaFX: importación documental, narración interna, audio mock, round-trip, integridad, export readiness, paquete auditable y paquete de video simple. No reemplaza el smoke visual manual.
