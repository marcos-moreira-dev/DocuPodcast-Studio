# T121-V04 — Wizard de registro de voz avanzada con frase por tono

## Objetivo

Crear el scaffolding de aplicación para registrar una voz avanzada sin exponer jerga técnica en la UX. La interfaz visible debe hablar de **Voz IA avanzada**, no de Coqui/XTTS.

## Alcance implementado

- `VoiceToneRecordingPrompt`: representa la frase guía de un tono.
- `VoiceRegistrationWizardPlan`: representa el plan visible del wizard.
- `BuildVoiceRegistrationWizardPlanUseCase`: construye el wizard con muestra neutral, tonos recomendados y catálogo teatral extendido opcional.
- `VoiceToneRecordingPlan`: representa la grabación de una muestra concreta.
- `BuildVoiceToneRecordingPlanUseCase`: prepara frase, nombre de archivo seguro y etiquetas de grabación.
- `VoiceApplicationServices` expone los nuevos casos de uso.

## Reglas UX

- El motor visible se llama `Voz IA avanzada`.
- No se muestra `Coqui` ni `XTTS` en el plan visible.
- Cada tono tiene una frase guía por defecto.
- Al grabar, la app debe mostrar la frase del tono.
- El usuario puede cancelar, detener y guardar.
- Cancelar no reemplaza la muestra anterior.
- Guardar asocia la muestra al tono elegido.

## Frase por tono

Las frases se toman de `VoiceReferenceTone.suggestedRecordingPrompt()`. Esto cubre Neutral, Aburrida, Heroica, Insinuante no explícita y todo el catálogo básico/teatral extendido.

## Tests

- `BuildVoiceRegistrationWizardPlanUseCaseTest`
- `BuildVoiceToneRecordingPlanUseCaseTest`
- `VoiceRegistrationWizardT121V04SourceTest`

## Criterios de aceptación

- El wizard entrega plan con `Voz IA avanzada`.
- Neutral es obligatorio.
- El catálogo teatral puede incluirse u ocultarse.
- Cada tono tiene prompt.
- La grabación por tono tiene nombre seguro y contrato de cancelación.
