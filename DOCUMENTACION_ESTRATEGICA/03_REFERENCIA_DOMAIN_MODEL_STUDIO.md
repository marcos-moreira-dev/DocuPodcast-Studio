# Referencia principal: Domain Model Studio / UENS

Domain Model Studio/UENS se leyó estratégicamente como base para el scaffolding JavaFX de DocuPodcast Studio.

## Qué aporta

DMS aporta una carcasa de aplicación madura:

- bootstrap explícito;
- `ApplicationRuntime`;
- `ApplicationServicesFactory` e `InfrastructureServicesFactory`;
- shell con tabs multiproyecto;
- toolbar global y contextual;
- `WorkspaceViewRegistry` y ruteo de workspaces;
- SideDock modular;
- workspaces estructurados y visuales;
- CSS claro con tokens;
- pantalla de inicio;
- guía integrada tipo CHM;
- ayuda operativa en SideDock;
- persistencia JSON `.dms`;
- assets relativos;
- Markdown importable;
- recursos IA exportables;
- exportación por artefacto activo;
- tests fuente, guardarraíles y documentación viva.

## Qué no copiar

No copiar el dominio de diagramas:

- `DiagramTypeId` como eje;
- ER/UML/BPMN/C4;
- `diagram-*` como vocabulario central;
- lenguaje de notaciones;
- canvas conceptual para todo.

## Traducción a DocuPodcast

```text
DMS Shell                 → DocuPodcast Shell
DMS tabs                  → tabs de proyectos .docupodcast
DMS toolbar contextual    → toolbar por workspace: Documento, Guion, Storyboard, Audio, Voces
DMS SideDock              → módulos: Segmentos, Voces, Medios, Jobs, Logs, Ayuda
DMS StructuredWorkbench   → Documento, Guion, Audio, Voces
DMS DiagramWorkbench      → Storyboard vivo
DMS .dms                  → .docupodcast.json
DMS assets                → imágenes, muestras de voz, audio clips, manifests
DMS Markdown importable   → docupodcast-script-v1, storyboard-v1, voice-library-v1
```

## Tandas leídas

Se leyeron 15 tandas de DMS: bootstrap, shell, toolbar, SideDock, workspaces, diccionario documental, levantamiento lógico, canvas, persistencia, Markdown/IA, guía, CSS, exportación, tests y mapa final.

La síntesis corta: **DMS es la base visual/arquitectónica principal**.
