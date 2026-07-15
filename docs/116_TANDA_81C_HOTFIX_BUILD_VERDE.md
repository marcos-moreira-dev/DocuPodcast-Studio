# Tanda 81C-HF1 — Hotfix build verde de barra flotante de lectura

## Motivo

La Tanda 81C introdujo correctamente `FloatingReadingControlBar`, una barra flotante de lectura global colocada encima de la hoja del documento. Sin embargo, el smoke local con Maven detectó regresiones en pruebas fuente antiguas que todavía esperaban el contrato anterior de `DocumentWorkspaceView`: la acción principal de lectura vivía como `PrimaryActionStrip` directamente dentro del workspace y el botón `Refrescar contenido` se validaba buscando `ActionButtonFactory.secondary` en la vista.

El nuevo diseño es más correcto: el workspace debe montar el componente transversal y el componente debe componer internamente `PrimaryActionStrip`, `TransportControls` y `ActionButtonFactory.secondary`. Por tanto, el hotfix no revierte la arquitectura de T81C; actualiza los guardarraíles para comprobar el contrato nuevo.

## Fallos locales reportados

El log local reportó 386 tests ejecutados y 9 fallos. Los fallos eran de pruebas fuente, no de compilación productiva. Los casos afectados fueron:

- `GuiComponentReuseSourceTest.mainSurfacesUseSharedGuiComponentsInsteadOfAdHocJavaFxRows`
- `DocumentComfortReadingSourceTest.documentReaderUsesComfortableTextAndKeepsTechnicalSettingsOutside`
- `DocumentPrimaryOperationSourceTest.documentWorkspaceExposesSingleSmartPrimaryAction`
- `BrainCoordinatorRefactorSourceTest.documentWorkspaceExposesRefreshThroughSharedActionFactory`
- `DesignScaffoldingPrinciplesSourceTest.designPrinciplesDocumentDefinesProductCompassAndScaffoldingRules`
- `NarratedDocumentRootContractSourceTest.narratedDocumentIsDeclaredAsV1RootAndScriptIsInternalProjection`
- `OfficeLikeUiReferenceSourceTest.officeLikeReferenceIsInspirationNotFullWordProcessorScope`
- `ReleaseCandidateDocumentationSourceTest.handoffMentionsTanda15AndRemainingPolish`
- `SmokeExploratorioMinimoSourceTest.rootDocumentationKeepsT58CProtocolAndDoesNotAdvertiseOld55BAsCurrent`

## Correcciones aplicadas

### 1. Guardarraíl de componentes transversales

`GuiComponentReuseSourceTest` ahora reconoce que `DocumentWorkspaceView` usa `FloatingReadingControlBar`, y que dicho componente es quien contiene `PrimaryActionStrip`, `TransportControls` y `ActionButtonFactory.secondary`.

Esto preserva la regla del usuario: no crear controles JavaFX sueltos en workspaces. El workspace sólo instala una superficie transversal.

### 2. Guardarraíles de Documento limpio y acción primaria

`DocumentComfortReadingSourceTest` y `DocumentPrimaryOperationSourceTest` fueron actualizados al contrato T81C:

- la barra global de lectura vive arriba de la hoja;
- el workspace contiene `FloatingReadingControlBar`;
- la barra conserva la acción primaria inteligente mediante `documentPrimaryActionLabelProperty` y `runDocumentPrimaryAction`;
- `document-operation-strip` sigue existiendo como clase visual, pero ahora pertenece al componente transversal.

### 3. Guardarraíl de refresco del documento fuente

`BrainCoordinatorRefactorSourceTest` ya no exige `ActionButtonFactory.secondary` dentro de `DocumentWorkspaceView`; lo exige dentro de `FloatingReadingControlBar`, que es el lugar correcto desde T81C.

### 4. Documentación raíz restaurada con contexto histórico

`README.md`, `AI_HANDOFF.md` y `VALIDATION.md` se ampliaron para conservar marcadores históricos usados por pruebas de no regresión:

- Tanda 15 como antecedente de release candidate y polish visual;
- Tanda 58C como smoke exploratorio mínimo histórico;
- Tanda 59 como brújula de diseño/scaffolding;
- Documento narrable como raíz V1;
- aclaración de que la inspiración Word/WPS no convierte el producto en editor ofimático completo.

## Decisión de arquitectura preservada

No se volvió a meter `PrimaryActionStrip` directamente en `DocumentWorkspaceView`. La solución correcta es mantener el componente transversal `FloatingReadingControlBar`. Esta decisión prepara T81D, donde el sidebar izquierdo contextual deberá seguir el mismo principio: componente reutilizable/estilizado antes que composición manual de controles JavaFX.

## Validación local esperada

Después del hotfix, se debe ejecutar:

```bat
scripts\02-ejecutar-tests.bat
```

El objetivo es que los 9 fallos del log desaparezcan sin revertir el rediseño de T81C.
