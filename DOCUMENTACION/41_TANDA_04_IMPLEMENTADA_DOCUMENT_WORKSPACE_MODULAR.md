# Tanda 4 implementada — Document Workspace modular

Esta tanda cerró el workspace Documento como una base reutilizable inspirada en el SideDock de Domain Model Studio.

## Implementado

- `presentation.sidedock` con `WorkspaceSideDock`, `SideDockModule`, `SideDockModuleRegistry`, `StaticSideDockModule`, `SideDockStatePolicy` y `SideDockContext`.
- `DocumentWorkspaceView` ahora usa SideDock modular, no `TabPane` local.
- Módulos del Documento:
  - Estructura documental.
  - Propiedades.
  - Acciones.
  - Perfil de lectura.
  - Diagnóstico de importación.
  - Ayuda operativa.
- Filtro de estructura por todos, estructura, narrables, imágenes, tablas, ignorados y advertencias.
- Acciones manuales de bloque:
  - marcar como título principal;
  - marcar como título;
  - marcar como subtítulo;
  - marcar como párrafo;
  - marcar como lista;
  - ignorar en audio.

## Decisión

El Documento queda preparado para la fase `Word → revisión estructural → perfil de lectura → guion narrable`.

El SideDock se mantuvo genérico para que después se use en Guion, Audio, Voces y Storyboard.
