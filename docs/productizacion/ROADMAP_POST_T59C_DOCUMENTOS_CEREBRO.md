# Roadmap post T59C — fuentes inmutables y cerebro auditable

## Contexto

T59B separó el catálogo de componentes transversales del mapa del cerebro. T59C agrega una regla de producto que debe respetar cualquier refactor posterior: los documentos fuente se abren en modo solo lectura durante esta versión.

## Decisión agregada

```text
Word/PDF/Markdown/TXT fuente = solo lectura en V1.
Las ediciones viven como artefactos del proyecto DocuPodcast, no dentro del archivo original.
```

## Orden recomendado actualizado

| Tanda | Foco | Tipo |
|---|---|---|
| T59C | Contrato de documentos fuente solo lectura + referencia ofimática para rediseño futuro. | Documental/arquitectura protegida. |
| T60 | Auditoría ejecutable del cerebro + Documento narrable como raíz. | Source audit + tests + mapa de responsabilidades. |
| T61 | Refactor prioritario de coordinadores. | Código sin cambio visual profundo. |
| T62 | Round-trip funcional real. | Persistencia, jobs, capas, assets, reapertura. |
| T63 | Configuración operativa. | Motores, modelos, buffer, FFmpeg, STT, preferencias. |
| T64 | Rediseño UI aplicado. | Documento limpio + componentes con criterios ya definidos. |
| T65 | Smoke integral y RC. | Pruebas manuales, packaging, límites conocidos. |

## Efecto sobre T60

La auditoría del cerebro debe verificar que ningún flujo trate el documento fuente como editable. Debe buscar:

- acciones visibles que sugieran edición de fuente;
- persistencia inversa hacia Word/PDF/Markdown/TXT fuente;
- confusión entre guion editable y documento fuente;
- textos de UI que prometan modificar el original;
- falta de separación entre `ReadableDocument` y artefactos del proyecto.

## Efecto sobre T64

El rediseño visual puede tomar inspiración de interfaces de ofimática, especialmente en página centrada, grupos de acciones, zoom y barra de estado, pero debe respetar:

```text
inspiración de lectura sí;
edición ofimática completa no.
```


## Ajuste agregado por T60

La auditoría no debe partir de “Word → guion”. Debe partir de “fuente solo lectura → Documento narrable”. El guion compatible queda como proyección interna/avanzada para TTS, edición avanzada e importación/exportación Markdown.
