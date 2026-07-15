# Tanda 3 implementada — Importador Word/DOCX con diagnóstico documental

Esta tanda cierra la primera versión funcional del importador Word/DOCX.

## Implementado

- Lectura de `word/document.xml` en orden de cuerpo.
- Lectura opcional de `word/styles.xml` para mapear `styleId` a nombre de estilo humano.
- Detección de:
  - títulos y subtítulos por estilos Word (`Heading`, `Título`, `Ttulo`, etc.);
  - listas mediante `w:numPr`;
  - párrafos;
  - imágenes mediante `w:drawing`/`w:pict` y `wp:docPr`;
  - tablas simples mediante `w:tbl`.
- Diagnóstico de importación con severidades `INFO`, `WARNING`, `ERROR`.
- Advertencias por:
  - imágenes sin descripción;
  - tablas que deben revisarse;
  - documento sin estructura clara;
  - párrafos demasiado largos.
- `document/document.json` ahora guarda resumen, bloques, metadatos e issues.

## Decisión técnica

El importador sigue usando solo APIs del JDK (`ZIP + XML`) para evitar agregar Apache POI antes de necesitarlo. Si el extractor queda corto con Word reales complejos, POI puede reemplazar el adaptador manteniendo el puerto `DocumentImporter`.

## Limitaciones conscientes

- No interpreta imágenes visualmente.
- No extrae tablas como estructura completa todavía.
- No procesa headers/footers ni notas al pie.
- No resuelve relaciones de imágenes ni copia binarios internos de DOCX.
- No aplica todavía un perfil de lectura editable; eso queda para la Tanda 5.
