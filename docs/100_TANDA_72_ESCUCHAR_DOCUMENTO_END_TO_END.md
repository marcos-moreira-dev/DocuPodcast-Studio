# Tanda 72 — Escuchar documento end-to-end

## Objetivo

Cerrar en el cerebro el flujo principal del producto: el usuario abre un documento fuente solo lectura y pulsa **Escuchar documento** sin tener que entender guion, jobs, manifest, buffer o render.

## Decisión de producto

El flujo V1 queda gobernado por esta cadena:

```text
Documento fuente solo lectura → Documento narrable → narración interna → audio/buffer → playback
```

La narración interna sigue existiendo como proyección técnica para TTS, playback, Markdown compatible y diagnóstico, pero no debe convertirse en el objeto padre visible para el usuario normal.

## Cambios técnicos

Se agregan contratos de aplicación:

- `DocumentListenPhase`
- `DocumentListenPlan`
- `PrepareDocumentListeningUseCase`

`DocumentApplicationServices` expone `prepareDocumentListening()` y `DocumentNarrationCoordinator` lo consume mediante `listeningPlan(...)`.

`DocuPodcastShellViewModel.listenToDocument()` empieza a utilizar el plan del cerebro para decidir si debe:

1. pedir abrir documento;
2. informar que no hay texto narrable;
3. preparar narración interna;
4. reproducir audio existente;
5. esperar buffer;
6. pedir guardar proyecto;
7. generar audio.

## Hotfix incluido

T71 falló localmente por `BrainRefreshExecutableSourceTest`: el source test todavía esperaba `ImportDocumentUseCase` dentro de `RefreshSourceDocumentUseCase`, aunque T71 movió correctamente el refresco hacia `DocumentSourceImportService`. El test ahora verifica el contrato vigente: refresco por `DocumentSourceImportService`, estado `UNSUPPORTED` para fuentes que no cumplen el contrato V1 y regla de no editar/sobrescribir la fuente.

## Criterio de salida

- El flujo principal se puede razonar desde un plan único de aplicación.
- La UI conserva Documento como superficie principal.
- El usuario no necesita entrar a Narración avanzada para escuchar.
- El cerebro distingue claramente entre preparar narración, reutilizar audio, esperar buffer y generar audio.
