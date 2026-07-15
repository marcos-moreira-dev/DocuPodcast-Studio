# Avance Tanda 3 — Word/DOCX operativo

Esta actualización adelanta la prioridad Word-first:

1. El usuario puede seleccionar un `.docx` desde menú o toolbar.
2. La aplicación extrae bloques con un importer mínimo basado en APIs JDK.
3. El documento se muestra en un workspace propio.
4. Al guardar el proyecto, se crea `source/` y `document/document.json`.
5. El `.docupodcast.json` referencia ambos como assets relativos.

El extractor es intencionalmente conservador. La decisión de usar Apache POI o mantener el extractor JDK se revisará después de probar documentos Word reales del usuario.
