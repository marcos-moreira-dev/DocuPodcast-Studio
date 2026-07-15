# Tanda 71 — Refresco completo de fuente y roadmap profundo

## Propósito

T71 corrige el fallo local detectado en T70 y completa el contrato del botón **Refrescar contenido** para todas las fuentes documentales V1: DOCX, PDF con texto nativo, Markdown/MD y TXT.

## Hotfix incluido desde T70

El log local de T70 reportó un único fallo en `DocumentNarratedExperienceSourceTest`: el test aceptaba Word/DOCX, Markdown y TXT, pero no el nuevo texto de Welcome con **PDF con texto nativo**. T71 actualiza ese guardarraíl sin cambiar comportamiento productivo.

## Cambio de cerebro

`RefreshSourceDocumentUseCase` ya no depende directamente de un importador DOCX genérico. Ahora usa `DocumentSourceImportService`, el mismo canal unificado de entrada documental creado en T70.

Esto significa:

```text
Refrescar contenido
→ detectar tipo de fuente
→ aplicar contrato V1 por tipo
→ reimportar si cumple requisitos
→ comparar snapshot anterior vs snapshot nuevo
→ reportar vigencia u obsolescencia
```

## Reglas por tipo

- DOCX: reimporta y compara.
- Markdown/MD: reimporta y compara.
- TXT: reimporta y compara.
- PDF: solo reimporta si conserva texto nativo extraíble.
- PDF escaneado/imagen: no intenta OCR, no crea documento parcial y marca el refresco como no soportado para esa fuente actual.

## Resultado de refresco

- Sin cambios: audio, capas y storyboard siguen vigentes.
- Con cambios: audio queda obsoleto; capas y storyboard quedan en revisión.
- Fuente faltante: no se reemplaza el documento actual.
- Fuente incompatible: no se reemplaza el documento actual y se informa la razón.

## Métricas de comparación

`SourceDocumentChangeReport` ahora puede exponer:

- delta de bloques;
- delta de palabras;
- cambio de formato;
- cambio estructural;
- resumen detallado para diagnóstico.

## Roadmap profundo

Se agrega `docs/productizacion/TANDAS_PENDIENTES_PROFUNDAS_POST_T71.md` con todas las tandas pendientes hasta RC, descritas a profundidad para evitar pérdida de contexto.
