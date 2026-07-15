# T96 — Limpieza de placeholders y acciones visibles falsas

## Propósito

T96 limpia superficies visibles antes de rediseñar fuerte la GUI. El objetivo es que menú, toolbar y superficies operativas no muestren acciones que solo abren placeholders o prometen flujos todavía no implementados.

Regla de producto:

```text
Si aparece, funciona.
Si no funciona, se oculta hasta que exista.
```

## Cambios principales

- Se retiran del menú acciones lectoras que solo llamaban a `showPlaceholder(...)`:
  - Buscar en documento…
  - Ir a página / fragmento…
  - Mostrar / ocultar miniaturas
  - Mostrar / ocultar panel lateral
  - Modo lectura limpia
  - Ver información del documento fuente
  - Diagnóstico de importación…
- Se retira `Perfil de lectura` del toolbar contextual hasta que tenga flujo real y dueño de comando claro.
- `MainToolbarView` ya no cablea botones visibles hacia `showPlaceholder(...)`.
- Se limpia copy visible de configuración/cómputo para no reintroducir STT/Whisper ni audio a texto como promesa de producto.
- Se conserva `showPlaceholder(...)` solo como utilidad interna/legacy, sin invocadores visibles.

## Alcance conservador

T96 no rediseña todavía el menú, ribbon, sidebar ni rail derecho. Solo elimina relleno visible y acciones falsas. Las decisiones finas de GUI quedan para el contrato visual posterior.

## Criterios de aceptación

- Ningún `MenuItem` visible del shell llama a `viewModel.showPlaceholder(...)`.
- Ningún botón del toolbar llama a `showPlaceholder(...)`.
- Las utilidades lectoras no implementadas quedan ocultas hasta tener comando real.
- `Perfil de lectura` no aparece como acción visible de toolbar.
- La UI visible sigue enfocada en documento, lectura, voz/media y exportaciones reales.

## Pruebas nuevas

- `VisibleActionCleanupT96SourceTest`

## Siguiente paso recomendado

T97 — Inventario/congelación de componentes GUI transversales antes del rediseño fuerte.
