# Word/DOCX como entrada principal

Decisión clave: **Word/DOCX es entrada prioritaria desde el MVP**.

El usuario tiene sus notas en Word. Por eso la pantalla de inicio, toolbar, guía y roadmap deben arrancar desde DOCX.

## Objetivo del importador DOCX

El importador debe extraer:

- párrafos;
- estilos Word;
- títulos y subtítulos;
- listas simples;
- tablas simples;
- imágenes detectadas;
- texto alternativo/descripción de imagen si existe;
- metadatos básicos.

## Qué no promete

- No modifica el Word original.
- No interpreta visualmente imágenes sin descripción.
- No arregla automáticamente documentos mal formateados.
- No garantiza orden perfecto en archivos corruptos o extraños.

## Modelo destino

El DOCX se convierte a:

```text
ReadableDocument
  ├── metadata
  ├── sourceRef
  └── blocks[]
```

Cada `DocumentBlock` puede ser:

```text
DOCUMENT_TITLE
HEADING
SUBHEADING
PARAGRAPH
LIST_ITEM
TABLE_NOTICE
IMAGE_NOTICE
IGNORED
```

## Perfil de lectura

Como los documentos Word pueden estar mal formateados, debe existir `ReadingProfile`:

- estilos Word equivalentes a título;
- tamaño de fuente;
- negrita;
- numeración 1.1, 1.2;
- reglas de imagen;
- reglas de tabla;
- frases de transición para título/subtítulo.

## Flujo mínimo

```text
Abrir DOCX
  → importar bloques
  → mostrar documento con scroll
  → aplicar perfil de lectura
  → corregir estructura si hace falta
  → generar guion narrable
```

## Tests prioritarios

- `DocxDocumentImporterTest`.
- `DocumentStructureDetectionTest`.
- `WordFirstProductContractSourceTest`.
