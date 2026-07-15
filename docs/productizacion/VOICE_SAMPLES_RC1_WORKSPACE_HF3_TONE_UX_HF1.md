# VOICE-SAMPLES-RC1 + VOICE-WORKSPACE-HF3 + VOICE-TONE-UX-HF1

## Objetivo

Cerrar el flujo operativo de voces sin depender de un proyecto guardado y sin convertir la vista en un dashboard decorativo.

## Cambios

- Las muestras de voz pueden importarse y grabarse usando la biblioteca de voces de la app/runtime cuando no hay proyecto abierto.
- `LocalVoiceSampleFileRepository` expone una ruta gestionada para temporales en `voice-library/tmp-recordings` y conserva las muestras finales en `voice-library/samples`.
- `ImportVoiceSampleUseCase` usa `effectiveProjectFile(...)` para soportar biblioteca de app sin exigir `.docupodcast` guardado.
- `VoiceSampleWorkflowCoordinator` inicia grabaciones en la carpeta temporal de la biblioteca de voces, no en la carpeta de un proyecto.
- Vista Voces cambia textos ambiguos: `Crear voz`, `Grabar muestra`, `Detener y guardar`, `Cancelar grabación`, `Reproducir muestra`, `Eliminar muestra`.
- Se agrega cancelación de grabación con `CancelAudioRecordingUseCase` y `AudioRecordingGateway.cancelRecording()`.
- Los controles de muestra quedan agrupados por intención operativa para evitar una botonera horizontal excesiva.
- Documento mantiene el filtro de tonos por voz: solo aparecen tonos con muestra real y Neutral registrada.

## Criterio UX

Cada región visible debe responder a una acción, estado o decisión del usuario. La vista Voces evita tarjetas decorativas: crear voz, grabar/importar muestra, cancelar, reproducir, eliminar y terminar la voz son acciones concretas.

## Validación focal en entorno ChatGPT

- `javac --release 21` focal de aplicación/infraestructura de grabación y muestras.
- Compilación y ejecución reflexiva con stubs JUnit de 11 source tests relacionados.
- Maven completo no se ejecutó por falta de `mvn` en el entorno.
