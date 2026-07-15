# Contrato de refresco de documento fuente — V1

## Decisión

Todos los documentos fuente siguen siendo **solo lectura** dentro de DocuPodcast Studio, pero la app debe contemplar una acción explícita de **Refrescar contenido**.

Esta acción existe porque el usuario puede abrir el Word, PDF, Markdown o TXT en otra aplicación, modificarlo y guardarlo. DocuPodcast debe poder detectar que el contenido externo cambió y ayudar a decidir qué artefactos derivados necesitan revisión.

## Flujo esperado

```text
Abrir documento fuente
→ crear Documento narrable del proyecto
→ generar/escuchar audio y capas opcionales
→ usuario edita el archivo fuente fuera de DocuPodcast
→ Refrescar contenido
→ comparar snapshot anterior contra fuente actual
→ mostrar impacto
→ actualizar Documento narrable si el usuario confirma
→ marcar audio/capas/storyboard según vigencia
```

## Qué debe comparar

La comparación mínima debe considerar:

- existencia del archivo fuente;
- fecha de modificación;
- tamaño;
- hash o huella del contenido cuando sea posible;
- bloques narrables derivados;
- relación entre bloques anteriores y bloques actuales.

## Resultado esperado

La acción debe producir un `SourceDocumentChangeReport` o equivalente con:

- sin cambios detectados;
- fuente no encontrada;
- bloques nuevos;
- bloques modificados;
- bloques eliminados;
- bloques movidos o dudosos;
- artefactos derivados afectados.

## Impacto sobre audio, capas y storyboard

Reglas V1:

- si el texto de un bloque no cambió, su audio y capas pueden conservarse;
- si el texto cambió, el audio asociado debe marcarse como **obsoleto** y requerir regeneración;
- si una capa sigue pudiendo reconciliarse con el texto, se conserva;
- si una capa ya no tiene texto asociado confiable, queda en revisión como posible capa huérfana;
- si una imagen de storyboard estaba asociada a texto eliminado, el vínculo queda en revisión;
- no se debe borrar automáticamente trabajo del usuario sin confirmación.

## Botón visible

El botón o acción visible debe llamarse **Refrescar contenido**.

No debe llamarse “Guardar Word”, “Actualizar fuente” ni “Escribir cambios”, porque eso sugeriría edición del archivo original.

## Frontera de arquitectura

Componentes propuestos para el cerebro:

- `SourceDocumentSnapshot`;
- `SourceDocumentChangeDetector`;
- `SourceDocumentChangeReport`;
- `RefreshSourceDocumentUseCase`;
- `DerivedArtifactStalenessPolicy`;
- `SourceDocumentRefreshCoordinator`.

El botón de UI debe ser solo una entrada. La decisión y el impacto deben vivir en aplicación/dominio/coordinadores, no en una vista JavaFX.

## Relación con Documento narrable raíz

Refrescar contenido no crea un segundo producto padre. Actualiza o compara el **Documento narrable** derivado del archivo fuente. La fuente sigue siendo el origen externo inmutable dentro de DocuPodcast. La operación no edita ni sobrescribe el documento fuente.
