# Centro de exportaciones: scroll y preview documental T95

## Decision

El centro de exportaciones debe mantener visible toda la informacion accionable de la salida seleccionada, incluso en pantallas medianas. Desde Tanda 95, la columna derecha usa scroll vertical y `fitToWidth=true`.

## Preview documental

La preview de `Video de estudio documental` es una vista hipotetica de frame. Aplica en vivo el color de fondo, color de texto, fuente y tamano configurados, pero no genera assets ni audio.

El texto se renderiza directamente sobre el fondo elegido. No debe aparecer una caja interna translucida que pueda confundirse con color de fondo, color de texto o una capa exportable.

## Fuera de alcance

- No se reintroduce generacion de video en esta linea de estudio.
- No se generan assets reales desde la preview.
- No se cambia el contrato de readiness/exportacion.
