# T121-HF1 — Limpieza de lenguaje visible de motores y visuales

## Objetivo

Alinear las superficies normales de usuario con el contrato vigente de producto:

- `Coqui/XTTS` no debe aparecer como nombre principal de UX.
- `Piper` no debe aparecer como nombre principal de UX.
- `Storyboard` no debe presentarse como módulo visible del flujo normal.

La UX normal usa:

- `Voz IA avanzada`
- `Voz local simple`
- `Modo de prueba`
- `Visuales` / `Secuencia visual`

Los identificadores técnicos `xtts`, `piper`, clases Java y scripts técnicos pueden permanecer como nombres internos o diagnósticos avanzados.

## Cambios

- `WelcomeWorkspaceView` habla de `Voz IA avanzada`, `Voz local simple` y FFmpeg.
- `DocumentAudioNarrationPanel` evita `Narrador Piper` y evita recomendar `Coqui/XTTS` al usuario normal.
- `SettingsDialog`, `EngineSetupCatalog` y `ModelInstallAssistantCatalog` muestran asistentes con nombres amigables.
- `VoiceEngineCapabilityProfile` y `VoiceCapabilityPolicy` emiten mensajes amigables incluso si la configuración previa traía nombres técnicos.
- `VoiceEngineType.displayName()` devuelve nombres de UX seguros.
- La ayuda integrada reemplaza `Coqui/XTTS`, `Piper` y `Storyboard` por lenguaje de producto.
- El rail derecho de Documento muestra `Visuales`.
- Exportación diagnóstica/bundle usa `Visuales` y `resumen_visual.md` como salida visible nueva.

## Tests

Se agrega `VisibleEngineLanguageT121Hf1SourceTest` y se actualizan guardarraíles existentes para esperar nombres de UX.

## Límites

No se eliminan clases, paquetes ni nombres internos `Xtts`, `Piper` o `Storyboard`; son compatibilidad técnica y deuda legacy controlada. Esta hotfix no implementa la prueba generada de voz ni cambia capacidades reales de motores.
