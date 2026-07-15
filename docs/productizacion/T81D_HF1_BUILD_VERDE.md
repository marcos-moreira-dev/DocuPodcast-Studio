# Tanda 81D-HF1 — Hotfix build verde del sidebar contextual

## Motivo

La Tanda 81D implementó correctamente el inspector contextual izquierdo del workspace Documento, pero la ejecución local completa de Maven reportó seis fallos de source tests/documentación. El log local mostró `386 tests, 6 failures, 0 errors`. Los fallos estaban concentrados en guardarraíles que seguían esperando contratos antiguos de UI/documentación, no en errores de compilación del producto.

## Fallos corregidos

### 1. `DocumentWorkspaceSourceTest`

El test exigía que `DocumentWorkspaceView` siguiera exponiendo módulos antiguos de estructura, diagnóstico y acciones (`DocumentStructurePanel`, `DocumentDiagnosticsPanel`, `DocumentActionsPanel`). Ese contrato ya no corresponde a T81D.

T81D define como superficie principal:

- barra flotante de lectura global;
- inspector contextual izquierdo;
- módulos `Detalles`, `Audio / Narración`, `Imagen`;
- rail derecho visual/navegacional;
- documento central limpio.

El test fue actualizado para validar `FloatingReadingControlBar`, `WorkspaceSideDock`, `DocumentContextDetailsPanel`, `DocumentAudioNarrationPanel`, `DocumentImageContextPanel`, `DocumentMediaRailView` y `selectedBlockId`.

### 2. `DesignScaffoldingPrinciplesSourceTest`

El test seguía esperando que el README raíz mencionara explícitamente `Tanda 59`. Desde T80/T81 el README raíz ya refleja el estado vigente del producto, por lo que el guardarraíl ahora valida `Tanda 81D` y mantiene intacta la verificación de principios históricos de diseño/scaffolding.

### 3. `NarratedDocumentRootContractSourceTest`

El test requería la frase `Documento narrable como raíz V1` en `AI_HANDOFF.md`. La frase se reincorporó como contrato explícito vigente y se aclaró que aplica a Word/PDF/TXT/Markdown.

### 4. `OfficeLikeUiReferenceSourceTest`

`VALIDATION.md` no conservaba la frase sobre inspiración Word/WPS. Se agregó el criterio: la inspiración Word/WPS no equivale a editor ofimático completo. Esto preserva la decisión de producto: página limpia y acciones sobrias, sin convertir DocuPodcast en Word.

### 5. `ReleaseCandidateDocumentationSourceTest`

El test exigía una redacción antigua centrada en `Tanda 15`. Se actualizó para validar la ruta vigente: T81D, T81E, Documento narrable y formatos fuente. La documentación histórica de release candidate sigue existiendo, pero no debe forzar que el handoff actual parezca de una tanda antigua.

### 6. `SmokeExploratorioMinimoSourceTest`

El test histórico de smoke esperaba referencias a T58C en documentación raíz. Se añadió una sección breve de referencias históricas de validación en README y AI_HANDOFF, dejando claro que T58C es protocolo histórico, no tanda vigente.

## Decisiones preservadas

- No se revierte el inspector contextual.
- No se reintroducen `DocumentStructurePanel`, `DocumentDiagnosticsPanel` ni `DocumentActionsPanel` como superficie principal del lector.
- El Documento central sigue limpio de metadatos técnicos.
- El rail derecho sigue siendo visual/navegacional.
- La configuración sigue accesible solo desde menú `Configuración`.
- El cerebro V1 no se toca.

## Validación realizada en entorno ChatGPT

No se pudo ejecutar Maven completo porque este entorno no tiene `mvn`. Se validaron con stubs JUnit los seis tests/familias fallidas reportadas por el log local:

- `DocumentWorkspaceSourceTest`
- `DesignScaffoldingPrinciplesSourceTest`
- `NarratedDocumentRootContractSourceTest`
- `OfficeLikeUiReferenceSourceTest`
- `ReleaseCandidateDocumentationSourceTest`
- `SmokeExploratorioMinimoSourceTest`

Resultado local de la validación aislada: 8 métodos de test ejecutados correctamente.

## Próximo paso

Reejecutar `scripts\02-ejecutar-tests.bat` localmente. Si queda verde, continuar con T81E — sidebar derecho de miniaturas / medios.
