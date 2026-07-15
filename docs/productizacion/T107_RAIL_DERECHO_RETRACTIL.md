# T107 — Rail derecho retráctil

## Objetivo

Implementar el rail derecho como navegación visual de storyboard/imágenes.

## Función

El rail solo muestra y navega. No configura.

## Tarjetas

Cada tarjeta muestra:

```text
miniatura de imagen o estado vacío
microreferencia de oración/fragmento
texto breve opcional
```

Al hacer clic:

```text
selecciona la oración
hace scroll estable
actualiza sidebar izquierdo
```

## Mostrar/ocultar

Debe haber:

```text
botón flotante para mostrar rail
botón interno para minimizar
comando MenuBar/Ribbon que usa el mismo AppCommandId
```

## Redimensionar

El usuario puede arrastrar el borde izquierdo del rail para cambiar su ancho.

## Reflow

El workspace Documento reacomoda texto como documento al cambiar el ancho del rail.

## Criterio de aceptación

- El rail se siente separado de la hoja.
- No parece incrustado.
- Es simple de mostrar/ocultar.
- Es redimensionable.
