# Matriz de dependencias y gates

Este documento evita reordenar tandas por intuición. Si se rompe el orden, debe existir una razón técnica nueva y documentada.

## Dependencias principales actualizadas

| Tanda | Depende de | Motivo |
|---|---|---|
| MOTOR-GPU-SMOKE1 | MOTOR-ADV-READY-GATE1 | Primero se define qué significa motor usable; luego GPU. |
| DOC-INDEX-PLAYBACK-HF1 | Ninguna, pero conviene después de motores | Afecta playback/generación desde pivote. |
| PLAYBACK-SPEED-HF9 | DOC-INDEX-PLAYBACK-HF1 recomendado | Ambos modifican continuidad de lectura. |
| VOICE-UX-POLISH1A | Gate A cumplido | Limpia la vista actual antes de agregar más flujo. |
| VOICE-REGISTRATION-WIZARD1 | VOICE-UX-POLISH1A | No construir grabación sobre una vista con elementos confusos. |
| VOICE-LIBRARY-SYNC1 | VOICE-REGISTRATION-WIZARD1 | Documento solo puede sincronizar voces/muestras cuando ya existe flujo de registro. |
| DOCUMENT-SIDEBAR-VOICE-UX1 | VOICE-LIBRARY-SYNC1 | El sidebar debe pulirse usando la biblioteca real, no una suposición. |
| FIRST-USE-ONBOARDING1 | MOTOR-ADV + VOICE polish/sync | El onboarding debe mostrar caminos reales. |
| EXPORT-READINESS-UX1 | AUDIO/VIDEO readiness existente | Debe coordinar audio/video/exportación. |
| SETTINGS-RUNTIME-UX1 | MOTOR-ADV + GPU | Settings debe reflejar estados reales. |
| RF-TX2A/B | UX/motor/voice labels definidos | Refactor sin cambiar semántica. |
| RF-TX2C/D | Paths/mensajes definidos | Procesos y descargas usan contratos previos. |
| AUDIO-EXPORT-FINAL-HF1 | EXPORT-READINESS-UX1 | El estado de exportación debe guiar la acción. |
| VIDEO-RUNTIME-SMOKE1 | FFmpeg preparado + readiness | Smoke real requiere Video local. |
| PERSISTENCE-RC1 | Audio/video/voz funcionales | Roundtrip debe incluir voces, muestras, audio y video reales. |
| TEST-CLEAN1 | Bloques funcionales cerrados | No limpiar tests mientras contratos siguen cambiando. |
| DOCS-CLEAN1 | TEST-CLEAN1 | Muchos tests apuntan a Markdown. |
| PF9 | Superficies finales definidas | Anti-placeholder debe revisar UI final. |
| DIAGNOSTIC-SCRIPTS-RC1 | Smokes reales definidos | Diagnóstico debe saber qué exigir. |
| PACKAGING-MEMORY-RC1 | RC scripts casi cerrados | Empaquetado debe reflejar runtime final. |
| RC-GATE1 | Todo lo anterior | Cierre. |

## Gates por fase

### Gate A — motores y reproducción confiables

Debe cumplirse tras tanda 4:

- Voz IA avanzada no se usa sin prueba real.
- GPU no se promete si Python no confirma CUDA.
- Índice seleccionado reproduce/genera desde el pivote.
- 1.5x/1.75x no dejan silencio artificial.

### Gate B — Vista Voces operativa y biblioteca real

Debe cumplirse tras tanda 8:

- Vista Voces explica su propósito: administrar voces y muestras.
- No hay dashboard decorativo ni acciones muertas.
- ComboBox de emociones muestra solo nombres simples.
- Nueva voz se crea desde subvista propia.
- Grabación se plantea con Java, no Python.
- Las muestras se guardan en biblioteca de voces de app/runtime, no en el proyecto.
- Documento ve voces nuevas y solo tonos registrados para la voz seleccionada.
- Documento distingue voz generada por IA vs audio local del usuario.

### Gate C — UX principal limpia

Debe cumplirse tras tanda 11:

- Primer uso permite escuchar rápido.
- Exportación dice qué falta antes de fallar.
- Configuración distingue preparar/probar/usar.
- GPU aparece como candidata o usable con evidencia, no como promesa.

### Gate D — refactor transversal mínimo

Debe cumplirse tras tanda 15:

- Rutas runtime centralizadas.
- Labels de motores centralizados.
- Mensajes humanos centralizados.
- Procesos externos con contrato común.
- Descargas con contrato común.

### Gate E — salida audio/video/persistencia

Debe cumplirse tras tanda 18:

- Audio final no congela UI.
- MP4 final tiene smoke real.
- Guardar/reabrir conserva documento, voces, tonos, muestras, imágenes, audio, jobs y exportaciones.

### Gate F — RC personal

Debe cumplirse tras tanda 25:

- Tests limpios.
- Docs vigentes.
- Sin placeholders visibles.
- Diagnóstico/RC distinguen motores reales omitidos vs aprobados.
- Packaging con heap explícito.
- Legal/terceros definido si hay distribución pública.

## Reglas de no adelantar

- No implementar `VOICE-REGISTRATION-WIZARD1` antes de `VOICE-UX-POLISH1A`.
- No implementar `DOCUMENT-SIDEBAR-VOICE-UX1` antes de `VOICE-LIBRARY-SYNC1`.
- No limpiar documentación antes de limpiar tests que apuntan a Markdown.
- No adelantar RC si motores reales fueron omitidos sin declararlo.
- No prometer GPU si el Python autocontenido no pasó smoke CUDA.
