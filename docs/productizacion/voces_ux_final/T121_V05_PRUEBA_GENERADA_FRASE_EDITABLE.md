# T121-V05 — Prueba generada con frase editable

## Objetivo

Permitir que el usuario pruebe cómo suena una voz avanzada generando una frase nueva con una muestra de referencia seleccionada.

## Diferencia clave

### Reproducir muestra

Reproduce el audio original importado o grabado.

### Generar prueba con esta voz

Genera una frase nueva usando el motor de voz y la muestra seleccionada como referencia.

## UI

Bloque:

```text
Prueba generada

Texto de prueba:
[Esta es una prueba de lectura con la voz seleccionada...]

Tono de referencia:
[Neutral ▼]

[Generar prueba con esta voz]
[Reproducir última prueba]
```

## Placeholder

El TextBox debe tener una frase por defecto editable. Ejemplo:

```text
Esta es una prueba de lectura con la voz seleccionada.
```

Si el usuario escribe otra frase, se genera con ese texto.

## Reglas

- Requiere voz avanzada lista.
- Requiere muestra neutral o tono seleccionado existente.
- Si el tono seleccionado no existe, usar neutral y avisar.
- Si el motor no está preparado, mostrar mensaje y link/botón a configuración.
- La prueba generada debe guardarse como cache temporal o artefacto de prueba.

## Artefactos

Ruta sugerida:

```text
<AppData>/DocuPodcast Studio/voice-tests/
```

o dentro del proyecto si la voz es de proyecto.

## Use cases

- `GenerateVoiceTestUseCase`
- `PlayGeneratedVoiceTestUseCase`
- `ResolveVoiceToneReferenceUseCase`

## Tests recomendados

- `GenerateVoiceTestUseCaseTest`
- `VoiceTestUsesEditableTextSourceTest`
- `VoiceTestFallsBackToNeutralToneTest`
- `VoiceTestRequiresAdvancedVoiceEngineSourceTest`

## Criterios de aceptación

- El usuario puede escribir una frase y generar prueba.
- Reproducir muestra y generar prueba son acciones separadas.
- Si falta tono, se usa neutral con aviso.
