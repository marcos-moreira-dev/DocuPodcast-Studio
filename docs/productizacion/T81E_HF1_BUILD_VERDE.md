# T81E-HF1 — Build verde del rail derecho

## Resumen ejecutivo

Hotfix aplicada sobre T81E para resolver cuatro fallos de source tests/documentación sin cambiar el contrato visual.

La Tanda 81E sigue vigente: el rail derecho de Documento es una superficie visual de miniaturas y medios; el inspector izquierdo conserva las acciones de asignación.

## Contrato protegido

```text
Inspector izquierdo = operar fragmento seleccionado.
Workspace central = leer y escuchar.
Rail derecho = ver miniaturas, medios, storyboard y navegar al texto.
```

## Ajustes técnicos

- `DocumentMediaRailView` vuelve a contener los literales de contrato `inspector izquierdo`, `Capas activas` y `Medios asignados`.
- `assignedMediaCard(...)` se conserva como punto de compatibilidad y delega en `layerCard(...)`.
- `README.md` y `AI_HANDOFF.md` mencionan explícitamente `Tanda 81D-HF1`.

## No regresión

No se agregan botones de asignación al rail derecho. La derecha sigue sin `Elegir audio…`, `Extraer audio de video…`, `Asignar emoción` ni formularios de edición.

## Próximo paso

T81F debe trabajar toolbar con iconos y grupos.
