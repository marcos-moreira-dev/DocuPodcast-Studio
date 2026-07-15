# T111 — Iconografía/CSS final y guardado guiado desde playbar

## Objetivo
Cerrar el pulido visual pendiente sin volver a botones o estilos ad-hoc. La tanda mantiene el contrato de componentes transversales: las vistas componen y los componentes estilizados encapsulan la presentación reutilizable.

## Cambios principales
- `RibbonView` deja de declarar marcadores temporales tipo `DOC`, `PLAY`, `GEN`, `STOP`, `VOICE`, `PACK` y usa `RibbonIconCatalog`.
- `RibbonButton` sigue siendo el componente oficial del ribbon; no se crean botones directos en la vista.
- Se agrega `SourceVisualBlockView` como componente transversal para imágenes detectadas en la fuente documental.
- `DocumentWorkspaceView` deja de construir `ImageView` con estilo propio y delega el bloque visual al componente.
- La playbar flotante ahora guía el guardado del proyecto antes de ejecutar la acción primaria cuando el documento aún no tiene contenedor `.docupodcast`.
- El importador DOCX resuelve imágenes embebidas por relación `r:embed`/`document.xml.rels`, no solo por orden de `word/media`.
- Se agrega CSS modular `components/source-visual.css` y se pule el badge de icono del ribbon en `components/ribbon.css`.

## Reglas preservadas
- Imagen/tabla/LaTeX detectados en el documento son bloques fuente, no asignaciones automáticas de storyboard.
- La imagen embebida se muestra en la hoja cuando el DOCX la contiene.
- El usuario decide si asocia una imagen al storyboard.
- Si el usuario pulsa reproducir/preparar desde la playbar sin proyecto guardado, primero se explica la necesidad del proyecto y luego se abre el diálogo de guardado.
- No se reintroducen workspaces técnicos como Guion, Audio Jobs o STT/Whisper en la superficie visible.

## Validación focal
- Importación focal de `Instinto Creativo.docx`: 10 bloques, 1 imagen detectada, 1 imagen embebida con base64 preservado.
- `javac --release 21` focal de dominio documental + `DocxDocumentImporter`.
- Source checks de iconografía del ribbon, componente `SourceVisualBlockView`, CSS modular y guardado guiado desde playbar.
