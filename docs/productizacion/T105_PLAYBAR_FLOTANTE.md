# T105 — Playbar flotante

## Objetivo

Implementar el panel flotante principal para escuchar el documento.

## Ubicación

```text
20–30 px debajo del Ribbon, flotando sobre la hoja.
```

## Estilo

- Blanco semitransparente.
- Fondo difuminado/blur tipo glass.
- Bordes redondeados.
- Sombra sutil.
- No debe parecer un bloque pesado incrustado.

## Contenido

- Botón textual principal: Escuchar documento / Reproducir documento.
- Iconos para pausar, reanudar, detener.
- Reproducir oración seleccionada si aplica.
- Estado breve.
- Tooltips obligatorios.

## Automatización de pasos previos

Si falta proyecto:

```text
Explicar que se creará proyecto → Aceptar/Cancelar → elegir carpeta → crear contenedor → continuar.
```

Si falta audio:

```text
Preparar/generar lo necesario y continuar si es posible.
```

Si falta motor:

```text
Mostrar mensaje humano con botón a Configuración.
```

## Criterio de aceptación

- Es el dueño visual de escuchar/pausar/reanudar/detener.
- No compite con el Ribbon.
- Encadena pasos obvios.
