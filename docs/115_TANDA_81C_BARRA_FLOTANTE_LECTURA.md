# Tanda 81C — Barra flotante de lectura global

## Propósito

T81C continúa el rediseño frontal iniciado en T81A y T81B. La tanda no cambia el cerebro V1 ni agrega una capacidad nueva de audio; reorganiza la superficie del workspace Documento para que el control de lectura global esté cerca de la hoja, visible y comprensible para una persona que solo quiere abrir un documento y escucharlo.

La decisión de producto es separar tres niveles de interacción:

1. **Toolbar superior**: acciones generales de la aplicación. Todavía será simplificada en T81F.
2. **Barra flotante de lectura**: control operativo global de lectura del documento/proyecto abierto.
3. **Sidebar contextual futuro**: acciones sobre una oración o fragmento seleccionado, como asignar audio, imagen o emoción.

Antes de esta tanda, el control primario de lectura vivía dentro de la propia página renderizada. Visualmente quedaba mezclado con el contenido del documento y competía con notas de estado, contratos de fuente solo lectura y bloques narrables. T81C mueve ese control a una franja superior del workspace Documento, debajo de la toolbar general y encima de la hoja.

## Cambio central

`DocumentWorkspaceView` ahora instala una barra flotante mediante:

```java

documentSurface.setTop(floatingReadingControl());
```

La barra se compone con `FloatingReadingControlBar`, un componente transversal nuevo ubicado en:

```text
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java
```

Este componente no crea una botonera ad hoc dentro del workspace. Internamente reutiliza:

- `PrimaryActionStrip`
- `TransportControls`
- `ActionButtonFactory.secondary(...)`
- constantes de `AppStyles`

La regla de la fase T81 se mantiene: **no hardcodear controles JavaFX repetidos dentro de workspaces cuando ya existen componentes GUI estilizados/transversales**.

## Estados de lectura contemplados

La barra usa las propiedades ya existentes del `DocuPodcastShellViewModel`:

- `documentPrimaryActionLabelProperty()`
- `documentPrimaryActionHintProperty()`
- `runDocumentPrimaryAction()`
- `pausePlayback()`
- `resumePlayback()`
- `stopPlayback()`

Por tanto, no duplica reglas de negocio ni decide por su cuenta si hay que preparar guion, generar audio o reproducir manifest. El cerebro sigue concentrado en el ViewModel/coordinadores/casos de uso existentes.

Los textos esperados siguen la intención ya existente:

- **Escuchar documento** cuando no hay selección o debe iniciar el flujo general.
- **Reproducir selección** cuando hay una oración/rango seleccionado.
- **Reproducir desde aquí** cuando hay un bloque o segmento anclado.
- **Pausar**, **Reanudar** y **Detener** como controles de transporte.

La barra no menciona `job`, `manifest`, `chunk`, `gateway`, `buffer interno` ni conceptos técnicos equivalentes. Es una superficie humana para lectura.

## Por qué no se cambió el cerebro

T80 congeló el cerebro V1. T81C no debe volver a agregar lógica funcional profunda. El flujo real ya existe:

1. Documento fuente importado.
2. Documento narrable en workspace.
3. Proyección interna de narración cuando hace falta.
4. Job de audio o manifest de playback.
5. Reproducción sincronizada y seguimiento activo.

T81C solo hace que ese flujo sea más evidente desde la interfaz.

## Relación con T81B

T81B limpió la hoja principal eliminando metadatos visibles como:

- `styleName`
- `styleId`
- `readingProfile`
- `classificationSource`
- `FirstParagraph`

T81C complementa esa limpieza sacando también el control principal de lectura fuera del cuerpo de la hoja. La hoja queda más parecida a un documento de estudio; la acción de lectura queda disponible arriba, como una barra operativa del workspace.

## Relación con T81D y T81E

T81C no implementa todavía el sidebar contextual ni el rail derecho final. Es una tanda previa para que el control global no se mezcle con las acciones por fragmento.

La separación esperada queda así:

```text
Barra flotante: lectura global del documento.
Sidebar izquierdo futuro: detalles, audio/narración e imagen del fragmento seleccionado.
Sidebar derecho futuro: miniaturas/medios y navegación visual al texto relacionado.
```

## Refrescar contenido

La acción `Refrescar contenido` se conserva como acción secundaria dentro de la barra. El criterio es que refrescar el documento fuente sigue siendo una acción propia del workspace Documento, pero no debe verse como parte de la narración avanzada ni como diagnóstico técnico.

En el menú, el equivalente semántico es `Documento → Refrescar documento fuente`. En el workspace, `Refrescar contenido` sigue siendo un acceso práctico para el usuario que editó el DOCX/PDF/TXT/Markdown fuera de DocuPodcast.

## Estilo CSS

Se agregan estilos en:

```text
src/main/resources/css/components/actions.css
```

Clases principales:

```text
.ui-floating-reading-control
.ui-floating-reading-transport
```

También se conserva `document-operation-strip` para compatibilidad visual y guardarraíles previos, pero la intención semántica nueva es `ui-floating-reading-control`.

## Guardarraíles

Se agrega:

```text
FloatingReadingControlSourceTest
```

Este test verifica que:

- `DocumentWorkspaceView` coloque la barra sobre el documento con `documentSurface.setTop(...)`.
- La barra use `FloatingReadingControlBar`.
- La barra componga `PrimaryActionStrip`.
- La barra componga `TransportControls`.
- La acción secundaria use `ActionButtonFactory.secondary(...)`.
- El CSS declare `ui-floating-reading-control`.

Además, se corrige `VoiceSampleImportUiSourceTest` para que acepte el nuevo contrato de navegación: la importación de muestra de voz sigue existiendo como capacidad interna y en la vista avanzada de voces, pero no debe exigir que el menú principal vuelva a tener `Voz` como superficie visible.

## No objetivos

T81C no intenta:

- rediseñar completamente la toolbar superior;
- retirar vistas secundarias;
- crear el inspector izquierdo por fragmento;
- crear el rail derecho final de miniaturas;
- descargar o instalar modelos TTS/STT;
- cambiar reglas de playback;
- modificar audio jobs;
- tocar persistencia del cerebro.

Estos puntos quedan para T81D, T81E, T81F, T81G y T82 según roadmap.

## Validación esperada

En entorno local con Maven/Toolchain:

```bat
scripts\02-ejecutar-tests.bat
```

En entorno ChatGPT, la validación se limita a checks fuente, compilación parcial sin Maven completo e integridad de ZIP.
