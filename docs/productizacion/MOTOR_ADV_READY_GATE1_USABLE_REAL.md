# MOTOR-ADV-READY-GATE1 — Voz IA avanzada descargada vs usable para documentos

## Objetivo

Cerrar la confusión detectada en lectura masiva: que Voz IA avanzada aparezca como descargada/configurada no significa que ya pueda generar chunks largos de un documento.

La tanda agrega una compuerta explícita entre:

1. runtime/modelo/voz neutral presentes;
2. motor seleccionable;
3. prueba WAV real generada;
4. reproducción de prueba confirmada;
5. motor usable para generar documentos.

## Cambios productivos

### Nuevo gate de generación documental

Se agregan:

- `XttsDocumentGenerationReadinessReport`
- `InspectXttsDocumentGenerationReadinessUseCase`

El nuevo use case combina:

- `InspectXttsSetupReadinessUseCase`
- `InspectXttsSmokeTestUseCase`

Regla central:

> Voz IA avanzada no puede generar chunks de documento si no existe prueba WAV real generada por el runtime local.

La reproducción confirmada queda como verificación recomendada/fuerte antes de trabajos largos, pero el mínimo para no bloquear documento es `generatedWavProof()`.

### Preflight más honesto

`InspectAiEnginesPreflightUseCase` ya no marca como usable la Voz IA avanzada descargada/configurada cuando falta el WAV de prueba.

Se agrega el estado:

- `AiEngineReadinessStatus.NEEDS_VERIFICATION`

Ese estado no es `usable()` y aparece cuando el runtime/modelo parecen presentes, pero falta generar una prueba WAV real.

### Gateway bloquea generación si falta prueba real

`SettingsAwareAudioGenerationGateway` ahora revisa el gate antes de entregar el gateway real de Voz IA avanzada.

Si el usuario intenta generar audio documental sin prueba WAV real, el descriptor del motor queda no configurado y el submit/resume devuelve un error humano como:

> Voz IA avanzada está descargada/configurada, pero todavía no demostró que puede generar audio real. Pulsa Probar en Configuración y genera un WAV corto antes de usar documentos largos.

### Mensaje visible de generación

`DocuPodcastShellViewModel` deja de usar el texto genérico para motor no disponible y usa `audioEngineUnavailableMessage()`, de modo que Documento puede mostrar la causa real: falta prueba WAV de Voz IA avanzada.

## Qué no cambia

- No se toca GPU/CUDA. Eso queda para `MOTOR-GPU-SMOKE1`.
- No se cambia generación TTS por segmento cuando el motor sí está probado.
- No se toca la descarga del modelo.
- No se cambia playback ni exportación.
- No se elimina el modo CPU honesto agregado en `MOTOR-TTS-ADV-HF2`.

## Tests agregados/actualizados

Agregados:

- `InspectXttsDocumentGenerationReadinessUseCaseTest`
- `MotorAdvReadyGate1SourceTest`

Actualizado:

- `InspectAiEnginesPreflightUseCaseTest`

Casos cubiertos:

- modelo/runtime presentes sin WAV de prueba no pueden generar documentos;
- WAV generado permite generar, pero solicita confirmar reproducción;
- WAV generado + reproducción confirmada cierra el gate;
- preflight usa `NEEDS_VERIFICATION` cuando falta prueba WAV;
- full demo requiere prueba WAV + FFmpeg.

## Validación hecha en entorno ChatGPT

No se ejecutó Maven porque el entorno no tiene `mvn`.

Sí se validó:

- `javac --release 21` de núcleo `application/domain/infrastructure`: OK.
- Compilación focal de tests nuevos/modificados con stubs JUnit: OK.
- Ejecución reflexiva focal de 11 métodos de test: OK.

## Siguiente tanda

`MOTOR-GPU-SMOKE1`.

Motivo: una vez cerrado el contrato “descargado vs usable”, el siguiente riesgo es CPU/GPU real dentro del Python autocontenido.
