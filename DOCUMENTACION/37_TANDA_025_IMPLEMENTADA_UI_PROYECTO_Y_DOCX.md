# Tanda 2.5 implementada — UI de proyecto, sesión y entrada DOCX inicial

Esta tanda conecta la persistencia `.docupodcast.json` con la interfaz JavaFX.

## Implementado

- Menú Archivo con Nuevo proyecto, Abrir proyecto, Guardar, Guardar como, Cerrar y Salir.
- Toolbar con Nuevo, Abrir proyecto, Guardar y Abrir Word/DOCX.
- `ProjectSession` y `ProjectSessionCoordinator` para sesión activa, ruta de archivo y dirty state.
- Título de ventana con `*` cuando hay cambios sin guardar.
- Confirmación al cerrar o reemplazar proyecto si hay cambios sin guardar.
- `DocumentWorkspaceView` inicial.
- Importador DOCX mínimo implementado con APIs JDK ZIP/XML.
- El importador extrae párrafos, estilos heading-like, imágenes detectadas y tablas simples.

## Limitaciones conscientes

- El DOCX importado vive en memoria; el payload `document/document.json` será la siguiente evolución.
- El importer DOCX es conservador y no reemplaza todavía a Apache POI.
- No copia todavía el DOCX fuente al árbol del proyecto; eso llegará con el workspace documental y assets físicos.
- El perfil de lectura todavía no es editable.

## Resultado

La app ya puede iniciar el flujo real: abrir Word/DOCX y ver bloques importados en un workspace claro.
