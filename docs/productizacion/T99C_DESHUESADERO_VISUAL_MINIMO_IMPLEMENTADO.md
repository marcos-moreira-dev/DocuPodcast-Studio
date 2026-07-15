# T99C — Deshuesadero visual mínimo implementado

## Objetivo

T99C limpia la basura visual heredada antes de construir MenuBar final, Ribbon, StatusBar con zoom, Documento limpio, Playbar flotante final, Sidebar y Rail definitivos.

Esta tanda no cambia el cerebro, no agrega nuevas capacidades funcionales y no implementa todavía el Ribbon real. Su propósito es dejar una base visual menos contaminada.

## Cambios productivos

- `DocumentWorkspaceView` deja de mostrar tabs truncados `Det`, `Aud` e `Img`.
- El SideDock del Documento usa rótulos legibles: `Info`, `Audio` e `Imagen`.
- La hoja del Documento ya no muestra textos permanentes de onboarding como `Pantalla operativa simple` ni explicaciones largas en cada documento cargado.
- La hoja conserva solo cabecera mínima: título, contrato de fuente solo lectura y estado operativo de escucha.
- Los bloques del documento ya no muestran etiquetas visibles `Párrafo`, `Título`, `Subtítulo` ni la clase `document-block-kind`.
- El tipo de bloque queda disponible como tooltip contextual, no como texto fijo en la hoja.
- La toolbar sustituye emojis de prototipo por marcadores textuales sobrios (`IN`, `DOC`, `PLAY`, `OUT`, `VIEW`, `VOICE`, `IMG`, etc.).
- Se corrigió una duplicación accidental de `selectSentence(span)` en el handler de oración.
- Se limpió CSS legacy asociado a `document-block-kind`.

## Guardarraíles añadidos o actualizados

- `VisualCleanupT99CSourceTest` impide reintroducir `Det/Aud/Img`, etiquetas de tipo de bloque en la hoja y emojis de prototipo en `MainToolbarView`.
- `ReaderUiRedesignSourceTest` ahora exige que el workspace Documento no recupere textos permanentes de cabina técnica ni `document-block-kind`.
- `MainToolbarIconGroupsSourceTest` ahora verifica marcadores sobrios en lugar de iconografía emoji.

## No incluido

T99C no implementa todavía:

- Ribbon real por pestañas.
- `ReadingZoomControl` en StatusBar.
- Playbar flotante final 20-30 px debajo del Ribbon.
- Rail derecho con botón flotante externo y resize.
- Sidebar izquierdo final con `SidebarIconTab` completo.
- Overlay de procesos largos.
- Playback por oración/unidad.
- RuntimePathResolver ni preflight real de arranque.

## Validación en entorno ChatGPT

- Verificación fuente: no quedan `"Det"`, `"Aud"`, `"Img"`, `Pantalla operativa simple`, `document-block-kind` ni emojis de prototipo en `src/main/java` y `src/main/resources`.
- Compilación focal con `javac --release 21` de los tests fuente modificados usando stubs mínimos de JUnit.
- Ejecución reflexiva de 4 métodos `@Test` focales.
- ZIP íntegro.

No se ejecutó Maven completo porque `mvn` no está instalado en este entorno.

## Prueba local recomendada

```bat
scripts\99-diagnostico-completo.bat
```

Si queda verde, continuar con T100 — MenuBar final.
