# 22 — Riesgos técnicos y mitigaciones

## Riesgo: motor TTS difícil de empaquetar

Mitigación: `AudioGenerationGateway` con implementación mock primero. Worker real detrás de interfaz. No acoplar UI al motor.

## Riesgo: calidad de voz variable

Mitigación: perfiles de voz, prueba de voz, advertencias de capacidades, no prometer emociones.

## Riesgo: Word mal formateado

Mitigación: reglas configurables de título/subtítulo, diagnóstico del documento, edición del guion.

## Riesgo: jobs largos

Mitigación: segmentos, progreso, ETA, cancelación cooperativa, reanudación.

## Riesgo: assets rotos

Mitigación: catálogo con rutas relativas, checksum, validación de payload.

## Riesgo: storyboard se vuelva editor de video

Mitigación: MVP = imagen por segmento. Video simple futuro. No animación compleja.

## Riesgo: clases gigantes

Mitigación: factories por workspace, command coordinators, tests de tamaño y deuda conocida.

## Riesgo: chat/contexto se pierde

Mitigación: esta carpeta de documentación, `AI_HANDOFF.md`, `DOCUMENTACION/`, `docs/`, guardarraíles y plan por tandas.
