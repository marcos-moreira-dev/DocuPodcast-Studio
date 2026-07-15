# Tanda 81F — Toolbar con iconos, grupos y acciones principales

## Propósito

T81F continúa la etapa frontal posterior a la congelación del cerebro V1. La tanda no modifica el dominio ni los casos de uso de audio, documento, storyboard, media, exportación o configuración. Su foco es la **barra superior de acciones**: pasar de una fila de botones textuales acumulados a una superficie más típica de aplicación de escritorio, con iconos, grupos claros y acciones realmente frecuentes.

La decisión de producto se mantiene:

```text
Documento central = leer y escuchar.
Inspector izquierdo = operar el fragmento seleccionado.
Rail derecho = ver medios y navegar visualmente.
Menu bar = acciones generales y configuración.
Toolbar = accesos frecuentes globales, no cabina técnica.
```

## Problema que corrige

Antes de esta tanda, `MainToolbarView` todavía exponía como botones principales superficies técnicas o secundarias:

```text
Narración avanzada
Voces
Audio
Storyboard
```

Eso hacía que el producto pareciera una suite de producción o una cabina de avión, incluso después de haber definido que el usuario normal debe operar desde Documento. Además, la toolbar era primordialmente textual; no daba la sensación de grupos visuales típicos como en WPS Office, Xournal++ u otras aplicaciones de escritorio donde los botones tienen icono, texto corto y espacio respirado.

## Decisión UX

La toolbar principal queda reducida a acciones globales de alta frecuencia:

```text
Documento: Inicio, Abrir documento
Lectura: Escuchar documento / acción primaria dinámica
Salida: Exportar
Vista: Pantalla completa
```

No se eliminan capacidades del programa. Las vistas avanzadas y los casos de uso siguen existiendo, pero dejan de competir visualmente con la lectura del documento.

## Nuevo componente transversal

Se agrega:

```text
presentation.components.ToolbarActionButton
```

Este componente renderiza una acción de toolbar con:

- icono;
- texto corto;
- tooltip;
- zona clickeable generosa;
- estilo centralizado;
- variante primaria para acciones principales.

El objetivo es impedir que cada workspace o barra vuelva a construir botones JavaFX sueltos. La toolbar ahora delega la presentación del botón al componente transversal y conserva solo la lógica de ruteo, enablement y agrupación.

## Cambios en `MainToolbarView`

`MainToolbarView` conserva dos filas:

1. **Fila global**: accesos principales, con iconos y grupos.
2. **Fila contextual**: acciones propias del workspace activo, alimentadas por `WorkspaceToolbarActionProvider`.

La fila global ya no contiene botones directos a:

```text
Narración avanzada
Voces
Audio
Storyboard
```

Estas superficies quedan para `Herramientas`/modo avanzado o para la futura tanda T81G, donde se decidirá qué vistas se degradan, ocultan o retiran de navegación principal.

## Iconos y grupos

La toolbar principal usa grupos explícitos:

```text
Documento
Lectura
Salida
Vista
```

Y acciones con iconos:

```text
⌂ Inicio
📄 Abrir documento
▶ Escuchar documento / Preparar y escuchar / Reanudar
📦 Exportar
⛶ Pantalla completa
```

La acción de lectura sigue vinculada a `documentPrimaryActionLabelProperty()`, por lo que mantiene el comportamiento inteligente del flujo Documento.

## Pantalla completa

La toolbar ahora expone también pantalla completa mediante:

```text
shellView::handleToggleFullScreen
```

Esto complementa `Ver → Pantalla completa`, sin duplicar configuración técnica ni añadir una vista nueva.

## CSS

`toolbar.css` queda actualizado con clases para la nueva superficie:

```text
.ui-toolbar-action
.ui-toolbar-action-primary
.ui-toolbar-action-icon
.ui-toolbar-action-text
```

El estilo mantiene el lenguaje visual existente: plano, moderno, sin gradientes XP, sin botones chillones y con mayor espaciado.

## Reglas de no regresión

A partir de T81F:

- La toolbar global no debe volver a mostrar `Narración avanzada`, `Voces`, `Audio` ni `Storyboard` como botones permanentes.
- Las acciones frecuentes por oración siguen en el inspector izquierdo.
- La configuración sigue exclusivamente en el menú `Configuración` y su diálogo.
- La barra flotante de lectura sigue siendo el control operativo dentro del workspace Documento.
- El rail derecho sigue siendo visual/navegacional.
- Nuevos botones de toolbar deben pasar por `ToolbarActionButton` o un componente transversal equivalente.

## Tests agregados

Se agrega:

```text
MainToolbarIconGroupsSourceTest
```

El test valida que:

- `MainToolbarView` use `ToolbarActionButton`;
- existan los grupos Documento, Lectura, Salida y Vista;
- existan iconos relevantes;
- pantalla completa esté expuesta desde la toolbar;
- la toolbar global no vuelva a introducir vistas técnicas como botones principales.

## Validación en entorno ChatGPT

No se ejecutó Maven completo por ausencia de `mvn` en el entorno. Se validó mediante tests fuente focales con stubs JUnit e integridad ZIP.

## Próximo paso

La siguiente tanda lógica es:

```text
T81G — Retiro o degradación de vistas secundarias redundantes
```

Esa tanda debe revisar rutas/workspaces visibles y decidir qué superficies siguen accesibles como herramientas avanzadas, cuáles se ocultan y cuáles quedan absorbidas por Documento + inspector + configuración.
