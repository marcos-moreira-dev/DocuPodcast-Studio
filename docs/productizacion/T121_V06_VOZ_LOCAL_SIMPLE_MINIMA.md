# T121-V06 — Voz local simple mínima

## Objetivo

Cerrar la ruta de **Voz local simple** como modo liviano/intermedio sin mostrar nombres técnicos de motor en la interfaz gráfica. Este modo no compite con Voz IA avanzada ni promete clonación, emociones, tonos teatrales o voces por muestra humana.

## Implementación

- `VoiceLibraryWorkspaceView` detecta el perfil activo del motor y, cuando está en modo local simple, muestra una superficie mínima.
- En modo local simple la vista Voces oculta:
  - wizard de voz avanzada;
  - tonos de referencia;
  - grabación/importación de muestras humanas;
  - catálogo teatral;
  - clonación y estilos expresivos.
- Se agrega sección **Voz local simple** con:
  - estado simple del modelo;
  - frase editable de prueba;
  - acción `Probar lectura simple`;
  - acción `Reproducir última prueba`.
- `GenerateVoiceTestUseCase` soporta prueba simple cuando el perfil activo es Voz local simple, generando artefactos auditables bajo `voices/generated-tests/local-simple/` sin exigir muestra humana.
- `SettingsDialog` mantiene valores internos de motor, pero usa `TtsEngineModes.safeLabel(...)` y `StringConverter` para mostrar nombres amigables.
- Se agrega `VoiceSampleWorkflowCoordinator` para reducir deuda en `DocuPodcastShellViewModel` y corregir el guardarraíl de tamaño del ViewModel.
- `InfrastructureServicesFactory` devuelve nombres visibles seguros: **Voz IA avanzada** y **Voz local simple**.

## Regla UX reforzada

En la interfaz gráfica no deben aparecer nombres técnicos como nombres visibles del motor. La UX normal debe usar:

- **Voz IA avanzada**
- **Voz local simple**
- **Modo de prueba**

Los nombres técnicos quedan reservados para código, scripts, rutas internas o diagnóstico técnico avanzado cuando sea inevitable.

## Tests

- `VoiceLocalSimpleT121V06SourceTest`
- Corrección de `VoiceGeneratedTestT121V05SourceTest`
- Corrección indirecta de `ShellViewModelBrainDebtSourceTest` mediante extracción parcial a `VoiceSampleWorkflowCoordinator`

## Validación ChatGPT

- `javac --release 21` de domain + application + infrastructure: OK.
- `javac --release 21` focal de `ProjectSession` + `VoiceSampleWorkflowCoordinator`: OK.
- Compilación focal de tests de V05/V06 y guardarraíl de deuda con stubs JUnit: OK.
- Smoke Java manual de prueba simple: OK.
- Sanity check de strings visibles en `presentation/*.java`: sin literales visibles con nombres técnicos de motor.

Maven completo no se ejecutó en este entorno porque `mvn` no está instalado.
