# RF16-01: Sidebar teatral compacto y redimensionable

## Objetivo

Corregir la sensación apretada del módulo `Fragmentos visuales` del sidebar teatral y permitir que el panel derecho se pliegue más sin perder el carril de fragmentos.

## Archivos tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/WorkspaceSideDock.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/main/resources/css/compat-legacy.css`
- Tests fuente de RF16 y del componente `CollapsibleModuleSplitPane`.

## Decisión arquitectónica

- El sidebar teatral distingue tres estados de ancho: colapsado, compacto y expandido.
- El split interno de `Fragmentos visuales` puede ocultar la zona de asignación y reducir el ancho del dock completo, no solo esconder media columna.
- `WorkspaceSideDock` expone el módulo activo para que el dock derecho pueda resetear compactación cuando cambie de módulo.
- El dock teatral queda redimensionable desde el split principal, con mínimos para evitar que el contenido se rompa.

## Comportamiento preservado

- No cambia el contenido de `Fragmentos visuales`.
- El rail derecho conserva módulos teatrales.
- Documento simple sigue sin sidebar derecho; el cambio aplica a `Teatro > Guión`.

## Pendiente siguiente

- Diseñar la regla futura “promover a workspace”: si el usuario expande mucho el sidebar teatral, el módulo activo podría abrirse como workspace central dedicado. Esta regla queda planificada, no implementada en este ajuste.
