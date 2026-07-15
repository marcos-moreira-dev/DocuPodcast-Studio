# Memoria — Tanda 4 implementada

Se cerró el Document Workspace modular. La vista Documento ya usa un SideDock reutilizable inspirado en Domain Model Studio. La decisión importante es que el documento importado se revisa estructuralmente antes de generar guion.

Módulos actuales: estructura, propiedades, acciones, perfil, diagnóstico y ayuda.

La acción `Ignorar en audio` introduce `DocumentBlockType.IGNORED`, que será clave para que el guion narrable no incluya bloques que el usuario descarte.
