# FIX 100112 + DOCUMENT-CAPABILITIES-RF1

## Corrección del diagnóstico 20260607-100112

El diagnóstico fallaba en `mvn compile`, `mvn test` y `smoke automatico cerebro` por un único error de compilación:

```text
cannot find symbol: variable exampleVisualBindingWorkflow
```

La causa era que `DocuPodcastShellViewModel.importAndBindExampleVisuals(...)` usaba el workflow creado para el demo teatral, pero el campo no había quedado declarado en el ViewModel.

Corrección aplicada:

```java
private final ExampleVisualBindingWorkflow exampleVisualBindingWorkflow = new ExampleVisualBindingWorkflow();
```

El guardarraíl de deuda del ViewModel se conserva: `DocuPodcastShellViewModel` queda por debajo del límite transitorio de 2700 líneas.

## DOCUMENT-CAPABILITIES-RF1

Se agrega un contrato de capacidades por formato documental para evitar que la experiencia unificada de Documento haga promesas idénticas para Word, PDF, Markdown y TXT.

Nuevos archivos:

- `DocumentSourceCapability`
- `DocumentSourceCapabilities`
- `InspectDocumentSourceCapabilitiesUseCase`

Criterio:

- Word/DOCX es la ruta rica: texto, jerarquía, estilos e imágenes fuente.
- Markdown conserva estructura por encabezados y texto narrable.
- PDF V1 requiere texto nativo; no promete OCR ni fidelidad visual completa.
- TXT es lectura lineal sin jerarquía ni visuales fuente.

La UI debe usar estas capacidades para habilitar o explicar funciones sin fingir paridad total entre formatos.
