# Tanda 60 — Auditoría ejecutable del cerebro y Documento narrable raíz

## Tipo

Documental/arquitectura protegida con guardarraíles fuente. No cambia comportamiento productivo.

## Decisión principal

El objeto padre de DocuPodcast V1 es el **Documento narrable**.

El guion sigue existiendo como proyección interna/avanzada para segmentar, importar/exportar Markdown compatible y depurar narración, pero no debe ser presentado como paso obligatorio ni raíz paralela para el usuario normal.

## Motivo

El propósito superficial y principal de la app es abrir un Word/DOCX, PDF, Markdown/MD o TXT para leerlo y escucharlo. Luego, de manera opcional, el usuario puede asociar voz, audio, imágenes o storyboard a textos concretos. En video, una imagen asociada dura lo que dure el texto hablado.

## Archivos agregados

```text
docs/productizacion/CONTRATO_DOCUMENTO_NARRABLE_RAIZ_V1.md
docs/productizacion/AUDITORIA_EJECUTABLE_CEREBRO_T60.md
docs/productizacion/ROADMAP_POST_T60_CEREBRO_DOCUMENTO_RAIZ.md
src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/NarratedDocumentRootContractSourceTest.java
src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/ExecutableBrainAuditSourceTest.java
```

## Archivos actualizados

```text
docs/productizacion/MAPA_CEREBRO_APP.md
docs/productizacion/AUDITORIA_CEREBRO_PRIORITARIA.md
README.md
AI_HANDOFF.md
VALIDATION.md
```

## Criterios protegidos

- Documento narrable como raíz V1.
- Guion como proyección interna/avanzada, no padre visible obligatorio.
- Fuente Word/PDF/Markdown/TXT solo lectura.
- Storyboard opcional ligado a texto; duración de imagen = duración del texto hablado.
- Auditoría del cerebro antes de rediseño visual aplicado.
- Refactor incremental en T61, no renombrado masivo peligroso.
