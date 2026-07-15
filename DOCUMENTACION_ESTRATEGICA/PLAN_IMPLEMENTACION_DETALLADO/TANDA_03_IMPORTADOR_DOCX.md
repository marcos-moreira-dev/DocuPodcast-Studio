# Tanda 3 — Importador DOCX mínimo

## Objetivo

Abrir Word/DOCX y convertirlo a `ReadableDocument`.

## Dependencia candidata

Apache POI para DOCX.

## Dominio

- `ReadableDocument`;
- `DocumentMetadata`;
- `DocumentBlock`;
- `DocumentBlockType`;
- `DocumentImageNotice`;
- `DocumentTableNotice`.

## Infrastructure

- `DocxDocumentImporter`.

## Alcance MVP

Extraer:

- texto de párrafos;
- estilos;
- títulos por estilo Word;
- listas simples como texto;
- tablas simples como aviso/resumen;
- imágenes como `IMAGE_NOTICE`;
- alt text si existe.

## No alcance

- OCR;
- interpretar imágenes;
- fidelidad visual Word completa;
- editar DOCX original.

## Tests

Crear fixtures DOCX pequeños:

- párrafos;
- heading 1/2;
- imagen con/sin alt text;
- tabla simple;
- lista.

Validar que el original no se modifica.
