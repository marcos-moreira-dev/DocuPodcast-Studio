# Tanda 3 cerrada y Tanda 4 avanzada — DOCX robusto + Document Workspace

Esta entrega cierra la primera versión sólida del flujo **Word/DOCX → documento normalizado** y avanza el workspace Documento hacia la estructura planteada en DMS: centro documental amplio + panel lateral contextual.

## Implementado en Tanda 3

- Importador DOCX basado en ZIP/XML del JDK.
- Lectura en orden del cuerpo `w:body`, respetando la secuencia de párrafos y tablas.
- Lectura opcional de `word/styles.xml` para traducir `styleId` a nombre visible de estilo.
- Lectura opcional de `docProps/core.xml` para usar el título interno del Word cuando exista.
- Detección inicial de:
  - título;
  - título principal / heading;
  - subtítulo;
  - párrafo;
  - elemento de lista;
  - tabla como aviso narrable;
  - imagen como aviso narrable.
- Extracción de descripción de imagen mediante `wp:docPr` cuando el DOCX la provee.
- Diagnóstico de importación mediante `DocumentImportReport` + `DocumentImportIssue`.
- Advertencias/notas para:
  - imagen sin descripción;
  - tabla convertida a aviso;
  - documento sin títulos/subtítulos detectables;
  - párrafos muy largos;
  - elementos de lista detectados.
- Metadatos por bloque: `styleId`, `styleName`, `list`, `rows`, `cells`, `description`, etc.

## Implementado en Tanda 4 avanzada

- `DocumentWorkspaceView` con `SplitPane`.
- Centro documental con tarjetas por bloque.
- Panel lateral inicial tipo SideDock con pestañas:
  - `Estructura`;
  - `Propiedades`;
  - `Diagnóstico`;
  - `Ayuda`.
- Selección de bloque desde estructura o desde la tarjeta central.
- Resaltado del bloque seleccionado.
- Scroll aproximado hacia el bloque seleccionado.
- Panel de propiedades del documento y bloque seleccionado.
- Panel de diagnóstico con métricas y issues de importación.
- Ayuda operativa Word-first.
- CSS inicial para estructura, métricas, issues, selección y estados visuales.

## Persistencia documental

Al guardar un proyecto con DOCX importado se materializa:

```text
source/<archivo>.docx
document/document.json
```

El `document/document.json` incluye:

```text
título
formato
sourcePath
conteos
importReport
blocks[]
metadata por bloque
```

## Decisión técnica

Todavía no se introdujo Apache POI. El adaptador actual funciona con APIs del JDK para mantener la tanda autocontenida. Si los Word reales requieren mayor fidelidad, se puede reemplazar `DocxDocumentImporter` por un adaptador POI sin romper el puerto `DocumentImporter`.

## Validación realizada aquí

- Compilación parcial con `javac --release 21` de `domain`, `application` e `infrastructure`.
- Smoke manual de importación DOCX mínimo con estilos, lista, imagen y tabla.

No se ejecutó Maven completo en este entorno porque no está disponible aquí. La validación completa sigue siendo local con Maven Toolchain y Eclipse Temurin 21.

## Pendiente para cerrar completamente Tanda 4

- Extraer un SideDock genérico reutilizable.
- Convertir estructura plana en árbol jerárquico por títulos/subtítulos.
- Filtros por tipo de bloque.
- Búsqueda textual dentro del documento.
- Acciones directas sobre bloque: marcar como título, subtítulo, párrafo, lista o ignorado.
- Persistir scroll/selección en `viewState`.

## Tanda siguiente recomendada

**Tanda 4.5 — Document Workspace completo + SideDock genérico.**

Después debe seguir **Tanda 5 — Reading Profile**, porque la detección inicial de títulos/subtítulos debe poder corregirse desde la UI antes de crear el guion narrable.
