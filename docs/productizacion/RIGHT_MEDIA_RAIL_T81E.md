# Right Media Rail — Contrato T81E

## Contrato de superficie

El rail derecho de Documento es una superficie de navegación visual. No es una zona de configuración ni un formulario de asignación. Su responsabilidad es mostrar una lista compacta de medios y permitir volver al fragmento relacionado.

## Layout permitido

Cada ítem del rail debe tener:

```text
miniatura / placeholder
fragmento relacionado
breve descripción
```

No debe tener formularios largos, botones de asignación ni controles técnicos.

## Componente obligatorio

Para tarjetas visuales se debe usar:

```text
MediaThumbnailCard
```

Los workspaces no deben recrear manualmente la estructura `thumbnail + labels + handler`.

## Relación con los sidebars

- Sidebar izquierdo: acciones contextuales por oración o fragmento.
- Sidebar derecho: miniaturas/medios y navegación.
- Workspace central: hoja + lectura global.

## Acciones explícitamente prohibidas en el rail derecho normal

```text
Asignar voz IA
Elegir audio
Extraer audio de video
Asignar emoción
Quitar audio asignado
Quitar imagen
```

Estas acciones pertenecen al inspector contextual izquierdo.

## Estado del refinamiento de lectura global

T81E también deja la barra flotante más compacta: la explicación de `Escuchar documento` queda en tooltip mediante `PrimaryActionStrip(..., false)`. Esto no reemplaza T81F; solo corrige saturación visual mientras se llega a la tanda de toolbar.
