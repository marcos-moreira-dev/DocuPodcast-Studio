# Tanda 81E-HF1 — Hotfix build verde del rail derecho de miniaturas

## Propósito

Esta hotfix corrige los fallos detectados localmente después de la Tanda 81E. La Tanda 81E había convertido el sidebar derecho del workspace Documento en un rail visual y navegacional de miniaturas/medios. La implementación compilaba, pero cuatro tests fuente quedaron desalineados o sin los literales históricos que protegen contratos de producto ya aceptados.

El objetivo de esta hotfix es **dejar verde T81E sin revertir el rediseño**.

## Fallos reportados

El log local reportó cuatro fallos:

```text
DocumentLayerAssignmentRailSourceTest.documentWorkspaceMovesFrequentLayerActionsToContextInspector
DocumentLayerAssignmentWorkflowSourceTest.contextualInspectorPersistsAssignmentsAndSupportsRemoveOrSelect
DesignScaffoldingPrinciplesSourceTest.designPrinciplesDocumentDefinesProductCompassAndScaffoldingRules
ReleaseCandidateDocumentationSourceTest.handoffMentionsCurrentFrontendPolishAndRemainingReleaseCandidatePath
```

Los fallos se explican así:

1. El rail derecho ya no contenía literalmente la frase `inspector izquierdo`, aunque el contrato seguía siendo cierto: las acciones frecuentes viven en el inspector izquierdo y el rail derecho es visual.
2. El rail derecho cambió el título visible de la sección de capas activas hacia una nomenclatura más visual, pero tests históricos seguían esperando `Capas activas` y `Medios asignados`.
3. La refactorización de T81E dejó el helper de tarjetas con nombre nuevo, mientras tests fuente todavía protegían el nombre de flujo `assignedMediaCard`; al mismo tiempo otro contrato nuevo esperaba `layerCard`.
4. `README.md` y `AI_HANDOFF.md` mencionaban `T81D-HF1`, pero no contenían el literal exacto `Tanda 81D`, requerido por tests de continuidad documental.

## Cambios aplicados

### 1. Rail derecho: contrato visual y contrato histórico alineados

`DocumentMediaRailView` ahora conserva explícitamente los dos conceptos:

```text
Capas activas
Medios asignados
```

La UI mantiene la intención de T81E: el rail derecho muestra medios, capas, mini storyboard e imágenes disponibles como navegación visual. Las acciones de asignación siguen fuera de este rail.

### 2. Mención explícita al inspector izquierdo

Se actualizó el comentario de contrato de `DocumentMediaRailView` para dejar literal y conceptualmente claro que:

```text
Assignment controls live in the left contextual inspector / inspector izquierdo.
```

Esto no introduce interfaz nueva; solo protege el contrato fuente.

### 3. Compatibilidad de nombres internos para tarjetas de capa

Se mantiene un método `assignedMediaCard(...)` que delega en `layerCard(...)`. Esto preserva simultáneamente:

- el contrato histórico de `assignedMediaCard`;
- el contrato nuevo de `layerCard` como tarjeta de capa activa;
- el uso de `MediaThumbnailCard` como componente GUI transversal.

### 4. Documentación raíz

`README.md` y `AI_HANDOFF.md` ahora mencionan explícitamente `Tanda 81D-HF1`. Esto resuelve tests de continuidad que verifican que la documentación siga trazando la ruta desde el inspector contextual de T81D hacia el rail derecho de T81E.

## Decisiones que se preservan

Esta hotfix **no revierte** T81E:

- El sidebar derecho sigue siendo visual/navegacional.
- No vuelven botones de asignación al rail derecho.
- Las acciones por fragmento siguen en el inspector izquierdo.
- `MediaThumbnailCard` sigue siendo el componente transversal para miniaturas.
- Documento central sigue siendo la superficie principal.
- Configuración sigue accesible desde el menú, no desde la bienvenida.

## Validación local en entorno ChatGPT

No se ejecutó Maven completo porque este entorno no tiene `mvn`.

Sí se validaron con stubs JUnit los tests afectados y pruebas relacionadas:

```text
DocumentLayerAssignmentRailSourceTest
DocumentLayerAssignmentWorkflowSourceTest
DesignScaffoldingPrinciplesSourceTest
ReleaseCandidateDocumentationSourceTest
RightMediaRailVisualNavigationSourceTest
DocumentMiniStoryboardRailSourceTest
MiniMediaRailRealAssignmentsSourceTest
GuiComponentReuseSourceTest
SharedActionComponentsSourceTest
```

Resultado local: todos los métodos focales ejecutados correctamente.

## Próximo paso

Continuar con:

```text
T81F — Toolbar con iconos y grupos
```

La próxima tanda debe compactar la toolbar superior, retirar accesos técnicos visibles y dejar una barra de acciones globales con iconos, agrupación clara y espacio visual generoso.
