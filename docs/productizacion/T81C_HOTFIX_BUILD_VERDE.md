# T81C-HF1 — Contrato actualizado de barra flotante y guardarraíles

## Problema de fondo

La Tanda 81C cambió la ubicación de la acción primaria del documento. Antes, el workspace Documento exponía directamente una tira de acción principal. Después de T81C, la operación global de lectura vive en `FloatingReadingControlBar`, una superficie transversal situada encima de la hoja.

Algunos tests fuente seguían buscando las piezas internas en `DocumentWorkspaceView`, por lo que el build local falló aun cuando la intención arquitectónica era correcta.

## Contrato nuevo

- `DocumentWorkspaceView` monta la barra con `documentSurface.setTop(floatingReadingControl())`.
- `DocumentWorkspaceView` no debe componer manualmente botones repetibles.
- `FloatingReadingControlBar` compone `PrimaryActionStrip`, `TransportControls` y `ActionButtonFactory.secondary`.
- El refresco del documento fuente sigue disponible como acción secundaria de la barra, pero se valida en el componente transversal.
- La documentación raíz debe conservar contexto histórico suficiente para las pruebas de producto.

## Implicación para T81D

El sidebar izquierdo contextual deberá seguir la misma regla: el workspace puede montar el inspector, pero las secciones `Detalles`, `Audio / Narración` e `Imagen` deben estar encapsuladas en componentes reutilizables y estilizados. No se debe regresar a una vista saturada con controles manuales.
