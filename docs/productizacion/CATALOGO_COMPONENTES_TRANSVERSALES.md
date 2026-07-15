# Catálogo de componentes transversales — DocuPodcast Studio

## Propósito

Este catálogo define el lenguaje transversal de interfaz de DocuPodcast Studio. No es una tanda para decorar pantallas ni para aplicar botones personalizados vista por vista. Su objetivo es fijar qué piezas de UI son reutilizables, qué rol cumple cada una y qué límites deben respetar los workspaces.

La implementación visual podrá cambiar cuando se estabilice mejor la cara final del producto. El contrato, en cambio, debe quedar claro desde ahora: una vista de producto no debe inventar controles repetibles si ya existe un componente, una acción o una superficie transversal con el mismo rol.

## Principio base

```text
Componente transversal = contrato semántico + API de uso + estilo común + regla de gobernanza.
```

No basta con crear una clase JavaFX bonita. Un componente transversal debe responder estas preguntas:

- ¿Qué intención de usuario representa?
- ¿En qué superficies puede aparecer?
- ¿Qué variantes son válidas?
- ¿Quién lo puede instanciar?
- ¿Qué estilos CSS usa?
- ¿Qué no debe hacer una vista al usarlo?

## Niveles del sistema de componentes

### 1. Primitivos visuales

Son piezas pequeñas que otras superficies pueden componer.

| Componente | Rol | Regla |
|---|---|---|
| `ActionButtonFactory` | Crear botones semánticos consistentes. | Las vistas no deben construir botoneras repetidas con `new Button(...)`. |
| `MetricBadge` | Mostrar métricas compactas. | No debe invadir el flujo de lectura normal. |
| `InfoBadge` | Mostrar estado o etiqueta informativa. | Debe usar lenguaje de usuario, no jerga interna. |
| `DiagnosticCard` | Mostrar diagnóstico o problema accionable. | Debe vivir en diagnóstico/avanzado salvo que bloquee el flujo. |
| `SectionHeader` | Encabezado de secciones. | Debe evitar títulos técnicos crípticos. |
| `EmptyStateView` | Estado vacío con guía de próxima acción. | Debe ofrecer una acción clara, no solo informar vacío. |

### 2. Superficies de acción

Agrupan acciones según intención y contexto.

| Componente | Rol | Uso esperado |
|---|---|---|
| `PrimaryActionStrip` | Acción principal de una pantalla. | Abrir Word, escuchar documento, exportar cuando corresponda. |
| `ActionBar` | Grupo de acciones secundarias o de workspace. | Guion, Audio, Storyboard, Voces, Diagnóstico. |
| `ActionGroup` futuro | Agrupar acciones por intención. | Producción, reproducción, exportación, configuración. |
| `TransportControls` | Reproducir, pausar, reanudar, detener. | Documento, Guion, Storyboard o Playback común. |
| `RailActionRow` | Acción pequeña asociada a una capa/asset. | Mini rail, capas narrativas, imágenes, voces. |

### 3. Superficies de contenido

Son contenedores que estructuran información.

| Superficie | Rol | Regla |
|---|---|---|
| Documento narrado | Lectura, selección y escucha. | Debe ser la pantalla simple principal. |
| Mini rail | Capas y assets relacionados con el texto. | Debe apoyar la lectura, no reemplazarla. |
| SideDock | Propiedades, diagnóstico, ayuda y acciones avanzadas. | Debe estar plegado o ser secundario en el flujo normal. |
| Guion | Edición avanzada del guion narrable. | No debe ser obligatorio para escuchar un Word simple. |
| Audio/Jobs | Observabilidad y recuperación. | Debe hablar de estado de usuario, no de infraestructura. |
| Configuración | Bodega técnica guiada. | Debe ocultar complejidad hasta que el usuario la necesite. |

### 4. Servicios de UI transversales

No son controles visibles, pero hacen que el front-end sea consistente.

| Servicio | Rol |
|---|---|
| Catálogo de acciones | Define acciones por intención, no por botón. |
| Política de capacidades | Decide qué acciones se habilitan según workspace/estado. |
| Presenters de notificación | Muestran errores, advertencias y éxito con lenguaje de usuario. |
| Factories de diálogo | Centralizan diálogos de archivo, configuración y confirmación. |
| Renderizadores de guía | Evitan mostrar Markdown crudo como si fuera consola técnica. |

## Variantes semánticas permitidas

Las variantes deben responder a intención, no a capricho visual.

| Variante | Significado |
|---|---|
| Primaria | Acción principal del momento. |
| Secundaria | Acción útil, pero no central. |
| Destructiva | Elimina, cancela o sobrescribe. |
| Transporte | Reproducción o control temporal. |
| Diagnóstico | Ayuda a reparar o entender un problema. |
| Avanzada | Configuración, exportación técnica o mantenimiento. |

## Regla anti-botonera

Una vista de workspace no debe construir una botonera propia si la acción puede expresarse como:

```text
Acción semántica → ActionButtonFactory/ActionBar/TransportControls/RailActionRow
```

Excepciones permitidas:

- componentes transversales que internamente crean botones;
- controles JavaFX nativos no repetibles y específicos;
- prototipos documentados como deuda temporal;
- diálogos pequeños cuando aún no exista factory y se registre deuda.

## Qué no cubre esta tanda

Esta tanda no pretende aplicar rediseño visual completo. Tampoco decide colores finales, layout definitivo del Documento ni estética final de Storyboard. Solo fija el catálogo y las reglas para que las futuras tandas no improvisen.

## Próxima evolución

1. Definir un catálogo de acciones semánticas.
2. Auditar qué vistas consumen componentes transversales y cuáles solo deberían declarar intenciones.
3. Reducir acoplamiento entre UI y `DocuPodcastShellViewModel`.
4. Aplicar rediseño visible cuando el cerebro esté ordenado.
