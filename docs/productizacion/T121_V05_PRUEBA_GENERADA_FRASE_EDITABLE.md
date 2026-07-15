# T121-V05 — Prueba generada con frase editable

## Estado

Implementada sobre T121-HF1.

## Alcance

Esta tanda agrega la primera ruta funcional para diferenciar en la Vista Voces entre:

- reproducir o conservar una muestra original importada/grabada;
- generar una prueba nueva con una frase editable usando la voz seleccionada y un tono de referencia.

La implementación mantiene el lenguaje visible limpio: `Voz IA avanzada`, `Voz local simple`, `Modo de prueba`; no introduce `Coqui`/`XTTS` en la vista Voces.

## Cambios principales

- Nuevo `ResolveVoiceToneReferenceUseCase` para resolver tono solicitado, muestra exacta o fallback neutral.
- Nuevo `GenerateVoiceTestUseCase` para crear artefacto cacheable de prueba en `voices/generated-tests/<voice>/`.
- Nuevos contratos `VoiceGeneratedTestRequest`, `VoiceGeneratedTestResult` y `VoiceToneReferenceResolution`.
- `VoiceApplicationServices` expone resolución de tono y generación de prueba.
- `VoiceLibraryWorkspaceView` agrega bloque `Prueba generada` con `TextArea`, botón `Generar prueba con esta voz` y botón `Reproducir última prueba`.
- `DocuPodcastShellViewModel` expone `generateVoiceTest(...)`, `playLastGeneratedVoiceTest()`, estado de prueba generada y ruta de la última prueba.
- CSS dedicado: `voice-generated-test-text` y `voice-generated-test-status`.

## Reglas

- La prueba requiere que `Voz IA avanzada` esté lista/configurada.
- Si el tono solicitado no tiene muestra, se usa neutral y se avisa.
- Si no existe muestra neutral, la prueba se bloquea con mensaje humano.
- Si el motor activo está en `Modo de prueba` o no soporta muestras avanzadas, la prueba se bloquea y guía al usuario a Configuración.
- La prueba se guarda como artefacto temporal/auditable dentro de la carpeta del proyecto, no como modificación del documento fuente.

## Tests

- `ResolveVoiceToneReferenceUseCaseTest`
- `GenerateVoiceTestUseCaseTest`
- `VoiceGeneratedTestT121V05SourceTest`

## Correcciones incluidas

- Se actualizan tests heredados de T119/T121-HF1 que todavía esperaban nombres técnicos visibles (`XTTS / Coqui`, `Piper`) para alinearlos con `Voz IA avanzada` y `Voz local simple`.
- Se corrige la aserción de readiness de Voz IA avanzada para aceptar el mensaje femenino `lista`.
