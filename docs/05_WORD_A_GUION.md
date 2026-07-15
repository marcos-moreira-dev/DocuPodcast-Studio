# Word a guion narrable

## Decisión

Word/DOCX es entrada primaria del MVP porque las notas del usuario están ahí.

## Flujo

```text
DOCX → ReadableDocument → DocumentBlock[] → ReadingProfile → NarrationScriptDocument
```

## Qué debe extraerse

- Párrafos.
- Títulos y subtítulos por estilos Word.
- Fallback por tamaño, negrita y numeración.
- Listas simples.
- Tablas simples.
- Imágenes como avisos.
- Texto alternativo/descripción de imagen si existe.

## Qué no debe prometer

- No modifica el Word original.
- No interpreta imágenes automáticamente.
- No corrige estructura mal hecha sin revisión.
- No garantiza fidelidad perfecta de PDF.

## Próxima implementación relevante

Crear `DocxDocumentImporter` con Apache POI en una tanda posterior.
