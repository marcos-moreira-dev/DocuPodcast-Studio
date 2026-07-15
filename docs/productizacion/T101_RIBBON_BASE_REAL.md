# T101 - Ribbon base real

## Base

T101 se aplica sobre T100 verde. La tanda reemplaza la toolbar superior transicional como superficie principal por un `RibbonView` real con pestanas, grupos y botones de ribbon. No implementa todavia el zoom de lectura, la playbar flotante final, el rail redimensionable ni iconografia definitiva.

## Objetivo

Construir una superficie superior alineada con el contrato GUI:

- MenuBar = comandos estructurales y transversales.
- Ribbon = herramientas agrupadas por modo/pestana.
- Playbar = dueno visual principal de escuchar/pausar/reanudar/detener en una tanda posterior.
- Workspace/sidebars/rail = acciones constantes del flujo de lectura.

## Cambios productivos

### Nuevo `RibbonView`

Se agrega `presentation.ribbon.RibbonView`, montado por `DocuPodcastShellView` en lugar de `MainToolbarView`:

```java
new RibbonView(viewModel, this::dispatchCommand)
```

El ribbon usa:

- `AppCommandRegistry.official()` como fuente de verdad.
- `AppCommandId` para despachar acciones.
- `RibbonButton` y `RibbonGroup` como componentes transversales.
- `ToggleGroup` para pestanas.
- `ScrollPane` horizontal sin scrollbar visible para contenido de comandos.

### Pestanas iniciales

T101 define una base de pestanas:

- Inicio
- Lectura
- Storyboard
- Vista
- Exportar

La seleccion de workspace puede enfocar la pestana inicial esperada:

- Inicio -> Inicio
- Documento -> Lectura
- Voces -> Lectura

### Comandos incluidos

El ribbon expone comandos reales del catalogo:

- Abrir fuente documental
- Escuchar documento
- Guardar proyecto
- Preparar audio
- Abrir voces / importar muestra
- Generar/cancelar audio
- Mostrar rail / crear storyboard simple
- Exportar WAV / paquete / video simple / carpeta de exportaciones
- Pantalla completa / configuracion / guia

No expone comandos heredados de workspace:

- `OPEN_AUDIO_JOBS`
- `OPEN_STORYBOARD`
- `SCRIPT_EDITOR`
- `AUDIO_JOBS`
- `STORYBOARD`

## Cambios tecnicos

- `DocuPodcastShellView` importa `presentation.ribbon.RibbonView`.
- `module-info.java` exporta `presentation.ribbon`.
- `RibbonButton` ahora acepta texto dinamico mediante `ObservableValue<String>`, usado para la accion inteligente de documento.
- `AppStyles` agrega clases para superficie ribbon, tab strip, tab, scroll y contenido.
- `components/ribbon.css` define la base visual del ribbon.
- `docupodcast-light.css` importa `components/ribbon.css`.
- `GuiComponentCatalog` registra `RibbonView` como componente ready.
- `AppCommandRegistry` permite `TOGGLE_RIGHT_RAIL` en `RIBBON`.

## Limites deliberados

T101 no intenta cerrar:

- Ribbon final de iconografia.
- StatusBar + `ReadingZoomControl`.
- Playbar flotante final.
- Sidebar izquierdo final.
- Rail derecho redimensionable.
- Overlay de procesos largos.

## Guardarrailes

Se agrega `RibbonBaseT101SourceTest` para verificar:

- El shell usa `RibbonView`, no `MainToolbarView`, como superficie superior principal.
- El ribbon usa `RibbonButton`, `RibbonGroup` y `AppCommandRegistry`.
- Las pestanas de producto existen.
- No se exponen workspaces heredados.
- El ribbon no reintroduce Whisper/STT.
- La playbar sigue siendo el dueno conceptual futuro de la escucha.

Tambien se actualizan tests fuente relacionados con Configuracion, Exportacion, Documento y catalogo de componentes.

## Validacion recomendada

En Windows:

```bat
scripts\99-diagnostico-completo.bat
```

Resultado esperado:

- Maven compile OK.
- Maven tests OK.
- Smoke automatico cerebro OK.
- Preflight arranque motores OK.
- Piper/FFmpeg locales OK.

## Siguiente tanda

T102 - StatusBar + ReadingZoomControl.
