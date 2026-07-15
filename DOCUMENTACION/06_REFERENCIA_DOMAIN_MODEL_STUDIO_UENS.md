# 06 — Referencia Domain Model Studio / UENS

Domain Model Studio es la referencia principal de scaffolding JavaFX. Se leyó en 15 tandas y se consideró mucho más útil que Fractal para la carcasa visual.

## Qué tomar

- `ApplicationRuntime`.
- `ApplicationBootstrap`.
- `InfrastructureServicesFactory`.
- `ApplicationServicesFactory`.
- Shell multiproyecto.
- Tabs reordenables.
- Toolbar global/contextual.
- SideDock modular.
- Workspaces estructurados.
- Canvas común para visuales.
- Pantalla de inicio clara.
- Guía integrada tipo CHM.
- Ayuda operativa en SideDock.
- CSS tokenizado y claro.
- Persistencia JSON versionada.
- Asset catalog con rutas relativas.
- Markdown importable con frontmatter.
- Recursos IA exportables.
- Exportación activa por workspace.
- Tests fuente y guardarraíles.

## Traducción conceptual

```text
DMS Workbench → DocuPodcast Workspace
DMS SideDock → DocuPodcast SideDock
DMS Toolbar Contributor → Workspace Toolbar Contributor
DMS .dms → .docupodcast.json
DMS Markdown IA → DocuPodcast Markdown IA
```

## Qué no tomar

- `DiagramTypeId` como eje.
- Nombres `diagram-*` en CSS.
- Dominio ER/UML/BPMN/C4.
- Derivación automática como promesa.
- Canvas para documentos o guion.

## Regla

Copiar la ingeniería, no el dominio.
