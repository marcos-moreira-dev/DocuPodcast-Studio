# Tanda 5B implementada — Reading Profile editable, persistible y previsualizable

## Estado

La Tanda 5B cierra la primera versión funcional del **Reading Profile**. El perfil deja de ser una regla interna fija y pasa a ser una configuración visible, editable y persistible dentro del proyecto `.docupodcast.json`.

## Qué se implementó

- `DocuPodcastProject` ahora conserva `ReadingProfile` como parte del agregado raíz.
- `.docupodcast.json` ahora escribe y lee el bloque `readingProfile`.
- El reader mantiene compatibilidad: si el bloque no existe, usa el perfil académico Word por defecto.
- `document/document.json` materializado incluye el perfil activo que se usó al guardar.
- Se agregó previsualización no destructiva con:
  - `ReadingProfilePreview`;
  - `ReadingProfilePreviewItem`;
  - `PreviewReadingProfileUseCase`.
- `DocumentReadingProfilePanel` ahora permite editar:
  - nombre;
  - descripción;
  - palabras clave de título principal;
  - palabras clave de título;
  - palabras clave de subtítulo;
  - máximo de palabras para negrita breve;
  - política de imágenes;
  - política de tablas.
- El panel permite:
  - previsualizar impacto;
  - guardar perfil en el proyecto;
  - guardar y aplicar al documento.

## Regla importante

La previsualización no modifica el documento. Solo calcula qué bloques cambiarían si se aplica el perfil.

## Persistencia

El formato raíz ahora contiene una sección como:

```json
"readingProfile": {
  "id": "reading-profile-academic-default",
  "name": "Documento académico Word",
  "description": "...",
  "imagePolicy": "READ_DESCRIPTION_OR_OMIT",
  "tablePolicy": "ANNOUNCE_SUMMARY",
  "headingRules": {
    "titleStyleKeywords": [],
    "headingStyleKeywords": [],
    "subheadingStyleKeywords": [],
    "maxShortBoldWords": 14,
    "treatShortBoldParagraphAsSubheading": true
  }
}
```

## Validación parcial realizada

En este entorno no hay Maven. Se validó compilación parcial con:

```bat
javac --release 21
```

para las capas sin JavaFX:

- domain;
- application;
- infrastructure.

También se ejecutaron smokes manuales de reader/writer JSON y materialización documental con perfil.

## Pendiente para la siguiente tanda

La siguiente tanda ya puede entrar a **Guion narrable**, porque el documento Word ya tiene:

- estructura importada;
- diagnóstico;
- reclasificación manual;
- perfil de lectura editable;
- previsualización;
- persistencia.
