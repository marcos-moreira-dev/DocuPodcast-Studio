# T119B — UX por capacidades del motor de voz

## Objetivo

Hacer que Documento, Voces y Configuración respondan al motor activo. La interfaz debe ser **capability-driven**: no se muestran opciones imposibles para el motor seleccionado.

## Regla de producto

- Coqui/XTTS es el motor principal de calidad alta.
- Piper es un modo intermedio/liviano.
- Mock es modo de prueba.
- La UI no debe prometer emociones, clonación o voces importadas por muestra cuando Piper está activo.

## Coqui/XTTS

Cuando Coqui/XTTS está activo, la UI puede mostrar:

- voz neutral;
- voces importadas por muestra;
- referencia de voz autorizada;
- selector CPU/GPU configurado;
- estilos/emociones solo como capacidad del motor activo;
- clonación/voz de referencia cuando exista muestra válida.

El sidebar de Documento puede mostrar voz importada y estilo/emoción si `VoiceEngineCapabilityProfile` lo permite.

## Piper

Cuando Piper está activo, la UI debe mostrar un modo más simple:

- narrador local intermedio;
- modelo Piper `.onnx` + `.onnx.json`;
- sin emociones;
- sin estilos expresivos;
- sin clonación por muestra humana;
- sin selector complejo de voces tipo Coqui/XTTS.

Texto UX base:

> Piper está activo. Este modo ofrece lectura local intermedia. Las voces personalizadas por muestra, emociones y estilos expresivos requieren Coqui/XTTS.

## Mock

Cuando Mock está activo:

- debe decir que es modo de prueba;
- no debe prometer voz real;
- no debe mostrar emociones ni opciones expresivas como si fueran funcionales.

## Implementación

Se agrega `VoiceEngineCapabilityProfile` como contrato central y se amplía `VoiceCapabilityPolicy` para responder:

- `supportsCustomVoiceSample()`;
- `supportsPiperModelVoice()`;
- `supportsEmotion()`;
- `supportsExpressiveStyle()`;
- `supportsVoiceCloning()`;
- `supportsMultipleImportedVoices()`;
- `canSynthesizeNow()`;
- `blockedReason()`.

`DocumentAudioNarrationPanel` consume ese perfil para mostrar u ocultar controles.

## Criterio de aceptación

- Piper no muestra emociones ni voces por muestra humana.
- Coqui/XTTS puede mostrar controles avanzados si está activo.
- Mock no promete voz real.
- La regla queda protegida por tests fuente y tests de política.
