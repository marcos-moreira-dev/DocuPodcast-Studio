# Hotfix T115 - PDF, Capturas, Lienzo y Exportacion

Fecha: 2026-06-29

## Implementado

- `Ir a pagina` en PDF deja de depender de anclas de bloques y salta directamente al visor PDF visual cuando el indice esta en modo `PDF_PAGES`.
- El rango valido de paginas PDF usa `indexedEntryCount` de la proyeccion `PDF_PAGES`, no solo las entradas visibles del arbol.
- Se elimino el item artificial `Paginas del PDF - Usa Ir a pagina...` del arbol del indice.
- Se agrego un micro modulo de marcadores en el SideDock izquierdo: nombre, pagina, agregar, ir y eliminar.
- La seleccion rectangular PDF calcula coordenadas desde `sceneX/sceneY` al frame real de la pagina renderizada, evitando fallos cuando el evento nace en el `ImageView`.
- El modal de problema tecnico tiene slider de zoom visual del lienzo.
- Al activar modo `panear`, se desactiva automaticamente `Interactuar con imagenes`.
- La exportacion PNG fuerza fondo opaco y compone sobre fondo antes de quemar el titulo, para evitar paneles transparentes.

## Fuera de alcance

- Persistencia formal de marcadores en `.docupodcast.json`.
- Galeria/detalle avanzado de problemas.
- OCR nuevo o cambios de temario PDF.
- Rediseño completo del modal.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentIndexPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/PdfVisualDocumentView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TechnicalProblemDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/StudyProblemCanvasSurface.java`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q test`

## Continuacion sugerida

1. Persistir marcadores de documento en JSON v3 de forma aditiva y compatible.
2. Probar manualmente captura rectangular PDF con zoom 78%, 100% y 156%.
3. Validar exportacion PNG externa con imagenes transferidas, titulo y fondo blanco.
4. Ajustar UX del modulo de marcadores si se decide hacerlo transversal persistente para todos los modos.
