# Tanda 4 avanzada — Document Workspace con SideDock inicial

Esta tanda avanza la UI documental inspirada en Domain Model Studio/UENS.

## Implementado

El `DocumentWorkspaceView` ahora tiene un SideDock inicial con cuatro módulos:

- **Estructura documental**: lista los bloques extraídos desde el DOCX.
- **Propiedades**: muestra métricas del documento y del bloque seleccionado.
- **Diagnóstico**: muestra advertencias y mensajes del importador.
- **Ayuda operativa**: explica el flujo Word-first y los límites actuales.

## Decisión de arquitectura

El documento importado se mantiene como workspace estructurado, no como canvas. El canvas queda reservado para Storyboard vivo.

## Pendiente para cerrar completamente Tanda 4

- Extraer componentes `DocumentStructurePanel`, `DocumentPropertiesPanel`, `DocumentDiagnosticsPanel` y `DocumentHelpPanel` para evitar que `DocumentWorkspaceView` crezca demasiado.
- Sincronizar selección con scroll real al bloque central.
- Añadir filtros por tipo de bloque.
- Añadir contador visual y estilos finales del SideDock.
- Crear guardarraíles de no doble scroll y no canvas para Documento.
