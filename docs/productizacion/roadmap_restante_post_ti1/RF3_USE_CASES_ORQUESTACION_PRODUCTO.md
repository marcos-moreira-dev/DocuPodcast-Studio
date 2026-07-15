# RF3 — Use cases de orquestación de producto

Estado: implementada.

RF3 agrega `PrepareListeningSessionUseCase` para mover a application la decisión end-to-end de **Escuchar documento**. Presentation conserva handlers y estado observable, pero deja de reconstruir manualmente las precondiciones de documento/proyección/audio/proyecto.

Componentes agregados:

- `PrepareListeningSessionUseCase`.
- `PrepareListeningSessionRequest`.
- `ListeningSessionReadiness`.
- `ListeningSessionState`.

Integración:

- `DocumentApplicationServices.prepareListeningSession`.
- `ApplicationServicesFactory` cablea el use case.
- `DocumentNarrationCoordinator` lo consume.
- `DocuPodcastShellViewModel.refreshDocumentListenFlow()` delega la decisión al coordinador.

Sin cambio visual, sin CSS nuevo y sin reintroducir nombres técnicos de motores en la GUI.
