# Word/DOCX como entrada prioritaria

El usuario tiene sus notas principalmente en Word. Por eso DOCX debe ser entrada de primera clase desde el MVP.

## Flujo principal

```text
Abrir DOCX
  → extraer bloques
  → detectar estructura
  → mostrar documento
  → ajustar perfil de lectura
  → crear guion narrable
```

## Qué debe extraer el importador DOCX

- Párrafos.
- Títulos por estilo Word.
- Subtítulos por estilo Word.
- Listas simples.
- Tablas simples como aviso o resumen textual.
- Imágenes como aviso.
- Alt text o descripción de imagen si existe.
- Metadatos básicos del documento.

## Reglas de detección

Primera prioridad:

- Estilos Word: `Heading 1`, `Heading 2`, `Título 1`, `Título 2`.

Fallback:

- Tamaño de fuente.
- Negrita.
- Texto corto.
- Numeración tipo `1.`, `1.1`, `2.3`.

## Lo que no debe prometer

- No corregir automáticamente un Word mal formateado.
- No describir imágenes sin alt text.
- No alterar el archivo Word original.
- No garantizar que PDF tenga estructura tan buena como DOCX.

## UI requerida

- Botón destacado “Abrir Word/DOCX” en Welcome.
- Acción global “Abrir documento”.
- Diagnóstico de importación.
- Perfil de lectura configurable.
- Guía integrada “Importar notas desde Word”.

## Tests mínimos

- `DocxDocumentImporterTest`.
- `DocumentStructureDetectionTest`.
- `WordFirstProductContractSourceTest`.
