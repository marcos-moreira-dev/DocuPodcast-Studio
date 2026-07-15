# Refactor cerebro — Narración y playback desde Documento (T63)

## Objetivo

Separar reglas de narración y playback del `DocuPodcastShellViewModel` sin rediseñar todavía la interfaz gráfica.

## Coordinadores agregados

| Coordinador | Responsabilidad |
|---|---|
| `DocumentNarrationCoordinator` | Documento narrable → proyección interna de narración; validación y mensaje de estado. |
| `PlaybackWorkflowCoordinator` | Reglas de buffer, cue inicial desde selección documental, pausa/continuación por chunks. |

## No objetivos

- No cambiar la cara final del Documento.
- No convertir Guion en paso obligatorio.
- No implementar MP4 final real.
- No cerrar aún configuración operativa de motores.

## Regla para futuras tandas

Cualquier flujo visible de Documento debe delegar sus decisiones de cerebro a coordinadores, use cases o políticas; no debe seguir aumentando el ViewModel central.
