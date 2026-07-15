# Tanda 30 — Biblioteca de voces madura

## Objetivo

Convertir el workspace de Voces en una superficie honesta de producto: asignar una voz a un segmento no implica prometer síntesis real si el motor actual no lo soporta.

## Cambios principales

- Se agrega `VoiceCapabilityPolicy` como política de aplicación.
- Se agregan `VoiceProfileCapability`, `PerformanceStyleCapability` y `VoiceLibraryCapabilityReport`.
- `VoiceApplicationServices` expone la política de capacidades.
- `DocuPodcastShellViewModel` expone `voiceCapabilityReport()` y `voiceCapabilityLabels()`.
- `VoiceLibraryWorkspaceView` muestra readiness del motor, chips de estado por voz/estilo y textos operativos honestos.
- Se agrega `voice-library.css` e import en `docupodcast-light.css`.

## Regla de producto

Una voz puede estar:

```text
asignable al guion
sintetizable ahora
lista solo como referencia humana
bloqueada por falta de muestra
bloqueada por motor no configurado
reservada para motor avanzado
```

Los estilos expresivos se conservan como intención cuando el motor actual no los honra realmente.

## Validación agregada

- `VoiceCapabilityPolicyTest`
- `VoiceLibraryCapabilityUiSourceTest`
- `VoiceLibraryWorkspaceSourceTest` actualizado
