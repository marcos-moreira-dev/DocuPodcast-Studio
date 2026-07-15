# Tanda 16 — Polish visual / UX inicial

## Estado
Implementada como tanda de refinamiento posterior al release candidate técnico.

## Objetivo
Corregir los problemas visuales detectados en pruebas manuales:

- la barra nativa de minimizar/maximizar/cerrar no era evidente en Windows;
- la ventana podía abrirse demasiado grande y quedar pegada a los bordes;
- la toolbar contextual mezclaba demasiadas acciones a la vez;
- la barra de estado tenía poca retroalimentación visual;
- la pantalla de inicio necesitaba una presencia visual más cálida y azulada.

## Cambios aplicados

### Barra visible de control de ventana
Se agregó una franja superior interna con:

- título actual del proyecto;
- botón Minimizar;
- botón Maximizar/restaurar;
- botón Cerrar.

La aplicación sigue solicitando `StageStyle.DECORATED`, pero esta barra garantiza controles visibles incluso si la decoración nativa no se muestra como se espera en el entorno del usuario.

### Tamaño seguro de ventana
`DocuPodcastStudioApp` ajusta el tamaño inicial contra `Screen.getPrimary().getVisualBounds()` y centra la ventana. Esto evita la sensación de ventana infinita o fuera de pantalla.

### Toolbar contextual por workspace
`MainToolbarView` ahora reconstruye la fila contextual según `WorkspaceKind`:

- Inicio: flujo rápido y ayuda;
- Documento: perfil, crear guion y ayuda Word;
- Guion: voces, voz IA, grabar voz, audio a texto y reproducción;
- Voces: importar voz, voz IA, grabar voz y guía;
- Storyboard: crear storyboard, importar/asociar imagen y reproducción;
- Audio: generar, cancelar, exportar podcast y reporte.

Esto reduce ruido visual y evita la fila de acciones infinita.

### Retroalimentación visual
La barra de estado ahora muestra un chip `Estado` antes del mensaje de estado.

### Pantalla de inicio
Se reforzó la estética clara con un fondo radial azul suave y tarjetas con sombra ligera.

## Tests agregados

- `WindowControlsAndStageFitSourceTest`
- `ToolbarContextualWorkspaceSourceTest`
- `VisualFeedbackCssSourceTest`

## Pendiente posterior

- iconografía real por acción;
- statusbar con indicadores de proyecto/audio/playback;
- acciones contextuales más estrictamente deshabilitadas según selección;
- smoke visual con capturas;
- revisión completa de textos visibles.
