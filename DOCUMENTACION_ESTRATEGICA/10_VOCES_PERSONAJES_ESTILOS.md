# Voces, personajes y estilos

DocuPodcast debe contemplar tres familias de voces:

1. prediseñadas;
2. voz propia;
3. voces autorizadas.

## VoiceProfile

```text
VoiceProfile
  ├── id
  ├── name
  ├── type: PREDEFINED | OWN | AUTHORIZED | IMPORTED
  ├── engine
  ├── language
  ├── sampleAssetId
  ├── modelRef
  ├── capabilities
  └── consentInfo
```

## CharacterProfile

```text
CharacterProfile
  ├── id
  ├── name
  ├── defaultVoiceProfileId
  ├── defaultStyleId
  └── notes
```

## PerformanceStyle

```text
PerformanceStyle
  ├── id
  ├── name
  ├── intent
  ├── engineCapabilityRequired
  └── fallbackPolicy
```

Ejemplos:

- neutral;
- académico;
- dramático;
- triste;
- sorprendido;
- enojado;
- suave.

## Regla ética

No fomentar clonar voces sin permiso. La UI debe hablar de voz propia y voces autorizadas.

## Regla técnica

No mostrar estilos emocionales como garantía. Deben depender de capacidades reales del motor.

Si el motor no soporta estilo explícito:

- deshabilitar la acción;
- o permitir intención con advertencia;
- o usar muestras de referencia si el motor lo permite.
