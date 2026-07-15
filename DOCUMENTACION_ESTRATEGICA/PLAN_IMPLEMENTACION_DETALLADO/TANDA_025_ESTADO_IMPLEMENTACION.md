# Tanda 2.5 — Estado de implementación

## Estado

Implementada.

## Alcance entregado

- Session UI mínima.
- Nuevo/Abrir/Guardar/Guardar como/Cerrar proyecto.
- Dirty state y confirmación de cierre.
- Importación DOCX mínima.
- Document workspace inicial.
- Tests de sesión e importer DOCX.

## Pendientes derivados

1. Persistir documento importado como `document/document.json`.
2. Registrar el DOCX como `SOURCE_DOCUMENT` asset relativo.
3. Crear SideDock de estructura documental.
4. Crear perfil de lectura configurable.
5. Reemplazar o ampliar importer con Apache POI si el extractor JDK queda corto.
