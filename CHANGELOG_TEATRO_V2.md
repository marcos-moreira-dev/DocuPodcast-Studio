# Changelog Teatro v2

- Añadido formato oficial schema 2 y grammar `theatre-v2`.
- Unificado ingreso por carpeta y ZIP seguro.
- Añadidos IDs estables, aliases y materialización completa desde gramática.
- Añadido estado tipado de personaje/objeto, eventos, herencia y reset.
- Añadido catálogo único de nueve zonas y snapshot canónico.
- Añadidos hash/tamaño obligatorios para assets declarados y semántica limpia para opcionales ausentes.
- Añadida exportación simétrica y persistencia JSON del estado.
- Corregido `GrammarWorkflowCoordinator`: ahora usa `TheatreImportUseCase` en vez de poblar solo perfiles/actos/escenas.

