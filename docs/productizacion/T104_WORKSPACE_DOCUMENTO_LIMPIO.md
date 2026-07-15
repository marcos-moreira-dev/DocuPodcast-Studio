# T104 — Workspace Documento limpio

## Objetivo

Convertir Documento en una hoja de lectura cómoda, no en una cabina técnica.

## Contenido

- Hoja central limpia.
- Playbar flotante arriba de la hoja.
- Sidebar izquierdo contextual.
- Rail derecho retráctil.
- StatusBar con ReadingZoomControl.

## Limpieza

- Quitar etiquetas `Párrafo`, `Título`, `Subtítulo` como marcas permanentes.
- Usar indicadores mínimos junto a oración/bloque.
- Quitar textos de relleno.
- Evitar jerga técnica.

## Scroll estable

La oración seleccionada o en reproducción debe quedar aproximadamente 50 px debajo del Ribbon.

```text
Seleccionar tarjeta del rail → seleccionar oración → scroll estable → sidebar actualizado.
```

## Reflow

Al mostrar/ocultar o redimensionar el rail derecho, el texto debe reacomodarse como documento.

## Criterio de aceptación

- El usuario puede estudiar leyendo y escuchando sin saturación.
- El texto no salta bruscamente.
- El workspace se adapta al rail.

## Implementación T104

La implementación efectiva se documenta en `docs/productizacion/T104_DOCUMENTO_LIMPIO_LECTOR_WORD.md`.

Cambios aplicados:

- Hoja centrada mediante `StackPane pageHost`.
- Ancho visual máximo de página.
- Header mínimo con chips de fuente y refresco.
- Scroll estable por bounds (`localToScene`) con offset de lectura.
- Ribbon ajustado tras captura local: más ancho y más alto que T103A para evitar puntos suspensivos y preparar iconografía futura.
