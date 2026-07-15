# FIX 100848 + DOC-REFRESH-DECISIONS-HF1 + READINESS-DIALOGS-HF1

Esta tanda corrige el diagnóstico local `20260607-100848` y agrega avisos operativos para dos acciones donde el usuario necesita saber qué cambió:

- Refrescar la fuente documental puede dejar audio obsoleto y capas/visuales en revisión.
- Revisar readiness de exportación debe mostrar bloqueos en diálogo cuando existan faltantes.

Regla aplicada: si DocuPodcast cambia o bloquea una intención explícita del usuario, se comunica con message box o decisión visible; el status bar queda para estado secundario.
