# Contrato de documentos fuente solo lectura — V1

## Decisión

Para la versión actual de DocuPodcast Studio, todo documento fuente abierto por el usuario debe tratarse como **solo lectura**.

Formatos cubiertos por este contrato:

- Word/DOCX;
- PDF;
- Markdown/MD;
- TXT.

Esta regla aplica incluso cuando algunos formatos todavía no estén implementados como entrada principal. Si se agregan PDF, Markdown o TXT como documentos fuente, deben entrar bajo el mismo contrato: **lectura, análisis, narración y capas del proyecto, pero no edición del archivo original**.

## Regla central

```text
Documento fuente = evidencia/origen inmutable.
Proyecto DocuPodcast = lugar donde viven Documento narrable, proyecciones de narración, capas, audio, storyboard, transcripciones y metadatos.
```

DocuPodcast puede derivar artefactos editables dentro del proyecto, pero no debe modificar ni guardar cambios sobre el archivo fuente.

## Qué significa “solo lectura”

El usuario puede:

- abrir un documento;
- verlo como página de lectura;
- escucharlo;
- seleccionar bloque, oración o rango;
- asignar voz, audio, emoción, imagen, ambiente o nota como capa;
- crear proyección de narración derivada para TTS/audio o compatibilidad avanzada;
- generar audio, storyboard, video o exportaciones;
- guardar todo dentro del proyecto `.docupodcast.json` y sus carpetas asociadas.

El usuario no debe poder, en esta versión:

- editar el contenido del Word/PDF/Markdown/TXT fuente desde DocuPodcast;
- guardar sobre el archivo fuente;
- insertar acotaciones dentro del documento original;
- convertir el visor de documento en procesador de texto completo;
- confundir capas narrativas con cambios escritos en el archivo fuente.

## Diferencia entre fuente y artefacto del proyecto

| Elemento | Estado en V1 | Editable desde DocuPodcast |
|---|---|---|
| Word/DOCX fuente | Solo lectura. | No. |
| PDF con texto u OCR local | Solo lectura desde T70; si no hay texto nativo suficiente, OCR local puede crear bloques narrables y, si falla, queda fallback visual. | No. |
| Markdown/TXT fuente | Solo lectura cuando se abra como documento. | No. |
| Guion narrable derivado | Artefacto del proyecto. | Sí, en vista avanzada. |
| Capas narrativas | Artefacto del proyecto. | Sí. |
| Storyboard | Artefacto del proyecto. | Sí. |
| Audio/jobs/manifests | Artefactos del proyecto. | Operables, no edición de fuente. |
| Exportaciones | Salidas generadas. | Se regeneran, no modifican fuente. |

## Markdown como caso especial

Markdown puede cumplir dos papeles distintos:

1. **Documento fuente leído:** se abre como contenido solo lectura.
2. **Proyección de narración importable `docupodcast-script-v1`:** se importa como artefacto avanzado del proyecto mediante una acción explícita.

La interfaz debe distinguir ambos casos. Abrir Markdown como documento no equivale a editar el archivo Markdown original. Importar Markdown `docupodcast-script-v1` crea/actualiza una proyección de narración del proyecto, no el archivo fuente.

## Implicación para el cerebro de la app

Los subsistemas de documento, importación, persistencia y selección deben respetar esta frontera:

```text
SourceDocumentReader / Importer → ReadableDocument → Project Artifacts
```

No debe existir un flujo inverso automático:

```text
Project Artifacts → sobrescribir documento fuente
```

Si en el futuro se ofrece exportar un Word/PDF/Markdown/TXT nuevo, debe ser una exportación explícita con ruta elegida por el usuario, no una edición del archivo original.

## Implicación para la UI

La pantalla Documento puede inspirarse en programas de ofimática por su legibilidad, estructura de página y agrupación de acciones, pero no debe presentarse como editor Word completo en esta versión.

Lenguaje recomendado:

- “Abrir documento”;
- “Leer documento”;
- “Escuchar documento”;
- “Asignar capa al proyecto”;
- “Crear proyección de narración”;
- “Exportar”.

Lenguaje a evitar:

- “Editar Word”;
- “Guardar cambios en el documento original”;
- “Modificar PDF”;
- “Reescribir fuente”;
- “Insertar acotación en Word”.


## Refrescar contenido sin editar la fuente

El contrato de solo lectura no significa que DocuPodcast ignore cambios externos.

Debe existir una acción visible de **Refrescar contenido** para volver a leer el archivo fuente desde disco cuando el usuario lo haya editado en Word, WPS Office, un editor Markdown, un visor/herramienta PDF o cualquier aplicación externa compatible.

La acción **Refrescar contenido** debe cumplir estas reglas:

- compara el estado actual del archivo fuente con el snapshot importado en el proyecto;
- no sobrescribe el Word/PDF/Markdown/TXT fuente;
- no aplica cambios automáticamente sobre audio, capas o storyboard sin mostrar impacto;
- informa si el contenido externo cambió, no cambió o ya no está disponible;
- permite decidir si se actualiza el Documento narrable del proyecto;
- marca como audio obsoleto los fragmentos cuyo texto cambió;
- conserva capas narrativas, voces e imágenes cuando el texto asociado puede reconciliarse;
- deja capas huérfanas o vínculos dudosos en revisión cuando el texto asociado fue eliminado o cambió demasiado;
- nunca transforma una actualización externa en edición inversa del documento original.

En términos de cerebro, refrescar fuente es una operación de lectura y comparación:

```text
SourceDocumentSnapshot anterior + archivo fuente actual
→ SourceDocumentChangeReport
→ decisión del usuario
→ actualización del Documento narrable y marcado de artefactos derivados
```

El botón recomendado para V1 es **Refrescar contenido**. Puede vivir cerca de Abrir documento o dentro de Documento/Avanzado, pero debe expresarse como actualización de lectura, no como guardado sobre la fuente.

## Guardarraíl de producto

Cualquier tanda que agregue soporte de lectura para PDF, Markdown o TXT debe declarar explícitamente:

```text
El formato se abre en modo solo lectura y no se modifica el archivo fuente.
```

Cualquier cambio que intente editar el archivo fuente requiere una decisión de producto posterior y no pertenece al alcance actual.
