# T100 — MenuBar final

## Objetivo

Cerrar la barra de menú como superficie estable de comandos antes de construir Ribbon, StatusBar/zoom y playbar final. Esta tanda no rediseña el Ribbon ni cambia el cerebro; alinea el MenuBar con el contrato de producto vigente y agrega solo comandos reales.

## Alcance implementado

- `AppCommandId` agrega comandos explícitos para:
  - `OPEN_SOURCE_DOCUMENT_LOCATION`
  - `OPEN_EXPORTS_FOLDER`
  - `TOGGLE_RIGHT_RAIL`
- `AppCommandRegistry` registra esos comandos como acciones visibles e implementadas.
- `DocuPodcastShellView` registra handlers reales para los nuevos comandos.
- Menú **Fuente documental** queda con:
  - abrir fuente documental;
  - refrescar desde fuente documental;
  - abrir ubicación de la fuente;
  - preparar audio.
- Menú **Ver** queda con:
  - pantalla completa;
  - mostrar/ocultar rail derecho.
- Menú **Exportar** queda con:
  - exportar audio;
  - exportar paquete;
  - exportar paquete de video simple;
  - ver estado de exportación;
  - abrir carpeta de exportaciones.
- El rail derecho de Documento queda conectado a estado real de presentación mediante `documentRightRailVisibleProperty()`.

## Decisiones de producto

- La carpeta de exportaciones se crea bajo `exports/` dentro del proyecto guardado cuando el usuario la abre desde el menú.
- La ubicación de fuente documental abre la carpeta de la fuente activa, que actualmente puede ser la copia materializada dentro de `source/` del proyecto.
- `Configuración` permanece como menú superior con entrada `Configuración`; no se convierte todavía en control custom directo porque T100 conserva `MenuBar` JavaFX estándar.
- No se implementa Ribbon ni zoom en esta tanda.

## Archivos principales

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandId.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/MenuBarFinalT100SourceTest.java`

## Validación

Validación focal en entorno ChatGPT:

- Revisión fuente de comandos nuevos y handlers.
- Compilación focal de clases de comando.
- Compilación focal y ejecución reflexiva de tests fuente relacionados con T100.
- ZIP íntegro.

Maven completo debe validarse localmente con:

```bat
scripts\99-diagnostico-completo.bat
```

## Próxima tanda recomendada

T101 — Ribbon base real.
