# RF3 — Use cases de orquestación de producto

Estado: implementada sobre RF2.

RF3 mueve la decisión end-to-end de **Escuchar documento** hacia application sin cambio visual. La interfaz sigue en Documento, con la misma playbar, sidebars y rail; la diferencia es que la presentación deja de reconstruir manualmente la matriz de precondiciones.

## Cambio principal

Se agregan use cases/records de aplicación:

```text
application.document.PrepareListeningSessionUseCase
application.document.PrepareListeningSessionRequest
application.document.ListeningSessionReadiness
application.document.ListeningSessionState
```

`PrepareListeningSessionUseCase` reutiliza `PrepareDocumentListeningUseCase` y devuelve una decisión única:

- plan ejecutable (`DocumentListenPlan`);
- estado de recorrido de escucha (`ListeningSessionState`).

## Integración

`DocumentApplicationServices` expone `prepareListeningSession`.

`ApplicationServicesFactory` lo cablea junto a la familia Documento.

`DocumentNarrationCoordinator` delega la decisión a application mediante `prepareListeningSession()`.

`DocuPodcastShellViewModel.refreshDocumentListenFlow()` consume `documentNarration.listeningSession(...)` y deja de reconstruir el estado con booleanos propios.

## Corrección incluida

Se corrige `LongProcessOverlayT109SourceTest`: desde RF2, `previewForRange` pertenece a `DocumentSelectionCoordinator`, no al ViewModel.

## Decisiones

- No cambia UX.
- No cambia CSS.
- No cambia formato de proyecto.
- No toca componentes transversales.
- No reintroduce nombres técnicos de motores en la GUI.
