# Tanda 55 — Motor de voz usable

## Objetivo

Convertir la configuración de voz en una experiencia entendible para usuarios no técnicos: motor potente primero, respaldo liviano y controles de voz claros sin exponer comandos crudos en la pantalla principal.

## Decisiones

- Motor potente prioritario: **XTTS / Coqui**.
- Motor liviano de respaldo: **Piper**.
- Motor mock queda como diagnóstico y pruebas de flujo, no como experiencia final.
- La pantalla Documento no debe hablar de XTTS, Piper, comandos, modelos ni CUDA.
- La configuración sí puede mostrar la “bodega” técnica con lenguaje guiado.
- La emoción operativa sigue siendo intención narrativa; solo se promete efecto audible si el motor activo lo soporta.

## Implementado

- `VoiceEngineControl`: controles configurables por motor.
- `VoiceEngineOption`: opción guiada de motor.
- `VoiceSynthesisSettings`: defaults seguros de velocidad, volumen y pausas.
- `VoiceEngineUsabilityPolicy`: catálogo de XTTS, Piper y Mock.
- Configuración → TTS / voz muestra velocidad, volumen, pausas, prueba de voz, muestras por intención y línea de comandos oculta para usuario normal.

## Tests

- `VoiceEngineUsabilityPolicyTest`
- `VoiceSynthesisSettingsTest`
- `VoiceEngineSettingsSourceTest`

## No implementado todavía

- Descargar modelos reales.
- Probar voz real desde UI.
- Conectar XTTS/Piper a generación real.
- Persistir preferencias globales de motor.
- Empaquetar modelos pesados.
