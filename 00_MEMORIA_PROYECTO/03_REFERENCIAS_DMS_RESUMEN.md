# Referencia estratégica: Domain Model Studio / UENS

Domain Model Studio fue leído por tandas para extraer patrones reutilizables de scaffolding JavaFX.

## Qué aporta a DocuPodcast

DMS es la referencia principal para la carcasa de aplicación:

- `ApplicationBootstrap` y `ApplicationRuntime`.
- Shell principal con menú, toolbar, tabs y statusbar.
- Tabs reordenables multiproyecto.
- Toolbar global + toolbar contextual por contributors.
- SideDock modular con un único módulo activo.
- Workspaces estructurados y visuales.
- Pantalla de inicio clara.
- Guía integrada tipo CHM.
- Ayuda operativa por SideDock.
- CSS claro tokenizado y modular.
- Persistencia JSON versionada.
- Catálogo de assets con rutas relativas.
- Importación Markdown con frontmatter y dispatcher.
- Recursos IA exportables con índice.
- Exportación activa según workspace.
- Tests fuente y guardarraíles.

## Patrones que deben copiarse conceptualmente

- Entry point delgado.
- Composition root explícito.
- Servicios por familias.
- Workspace registry.
- Toolbar como acciones visibles filtradas por capacidades reales.
- SideDock para estructura, propiedades, validación y ayuda.
- Guion/documento/audio/voces como workspaces estructurados.
- Storyboard como workspace visual/canvas.
- Guía integrada y ayuda operativa separadas.
- Documentación viva y tests anti-promesa-falsa.

## Qué no debe copiarse

- `DiagramTypeId` como eje del sistema.
- Dominio ER/UML/BPMN/C4.
- Nombres `diagram-*` en CSS nuevo.
- Canvas para artefactos documentales.
- Servicios de derivación automática como promesa del producto.

## Traducción principal

| DMS | DocuPodcast |
|---|---|
| `DmsProject` | `DocuPodcastProject` |
| `.dms` | `.docupodcast.json` |
| `DiagramToolbar*` | `WorkspaceToolbar*` |
| `StructuredWorkbenchView` | `StructuredWorkspaceView` |
| `DiagramWorkbenchView` | `VisualWorkspaceView` |
| Data Dictionary | Document Reader / Script Editor |
| Logical Business | Narration Script |
| Wireframe/FreeGraph Canvas | Storyboard Canvas |
