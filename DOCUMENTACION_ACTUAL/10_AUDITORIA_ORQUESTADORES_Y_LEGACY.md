# Auditoría de orquestadores y legacy documental

Este documento gobierna la siguiente fase de refactor consciente. Su objetivo es reducir orquestadores grandes sin cambiar comportamiento visible y bajar ruido documental sin borrar trazabilidad.

## Orquestadores grandes restantes

Medición local del 2026-06-08:

- `DocuPodcastShellViewModel` ronda 2600 líneas. Sigue siendo el mayor coordinador de estado de Documento, playback, selección, audio, visuales y proyecto. Ya delega transporte de playback; no debe crecer. Próxima extracción: comandos de Documento que mezclan selección, asignación de audio/imagen y refresco de estado.
- `SettingsDialog` ronda 890 líneas tras RF11. Ya delega progreso visual, mapeo de formulario, arranque de workers largos al runner común y operaciones de Voz IA avanzada/CUDA, Voz local simple/Piper, Video local/FFmpeg y configuración inicial a coordinadores de dominio. No debe crecer; futuras mejoras de Configuración deben agregarse como coordinadores o componentes pequeños.
- `VoiceLibraryWorkspaceView` queda por debajo de 1000 líneas tras RF13. Ya delega acciones operativas de muestras en `VoiceSampleActions`, motor/dispositivo en `VoiceEngineSettingsControls` y primitivas de layout en `VoiceWorkspaceLayout`. Próxima extracción: render de detalle de voz o presentación de catálogo/tonos.
- `LocalTtsProcessAudioGenerationGateway` ronda 1000 líneas. Es infraestructura específica de TTS. Próxima extracción: planificación de comandos, ejecución de proceso, validación WAV y reporte diagnóstico.
- `DocuPodcastShellView` ronda 1000 líneas. Ya delega exportación de video y workers comunes; próximas extracciones: importación documental, menús/comandos auxiliares y soporte avanzado.

## Orden recomendado

1. `VoiceLibraryWorkspaceView`: continuar separando la microaplicación `Voces` por objetivo operativo; después de RF13 quedan render de detalle de voz y catálogo/tonos.
2. `LocalTtsProcessAudioGenerationGateway`: dividir el gateway por responsabilidades internas, sin cambiar contrato público ni diagnóstico.
3. `DocuPodcastShellViewModel`: extraer flujos de Documento que todavía mezclan selección, asignación de capas, manifest y mensajes.
4. `DocuPodcastShellView`: mantenerlo como cableado de ventanas/comandos; no debe volver a absorber diálogos grandes ni workers.
5. `SettingsDialog`: mantenerlo estable; solo aceptar nuevas funciones mediante coordinadores/componentes pequeños.

## Regla de extracción

- Extraer solo cuando el código sirva a un caso de uso real repetido o reduzca una clase grande ya medida.
- Mantener los límites: presentation orquesta interacción; application coordina casos de uso; infrastructure ejecuta detalles externos.
- Cada extracción debe preservar schema, archivos `.docupodcast.json`, textos visibles y comportamiento de pruebas.
- Cada paso grande se registra en `09_BITACORA_REFACTOR_PRESENTACION.md` y se resume en `01_REGISTRO_TANDAS_RECIENTES.md`.

## Legacy documental

`DOCUMENTACION_ACTUAL/` es la única fuente de verdad operativa.

Carpetas históricas:

- `docs/`
- `DOCUMENTACION/`
- `DOCUMENTACION_ESTRATEGICA/`
- `00_MEMORIA_PROYECTO/`

No borrar documentación histórica en esta fase. Se conserva por compatibilidad de pruebas, trazabilidad y memoria del proyecto. La limpieza consiste en:

- no crear nuevas decisiones vigentes fuera de `DOCUMENTACION_ACTUAL/`;
- no reactivar roadmaps históricos si contradicen la documentación vigente;
- mantener avisos de archivo histórico en las carpetas antiguas;
- resumir solo lo necesario en documentación vigente antes de refactorizar.

## Avance RF6

`EmbeddedDependencySetupAssistant` ya empezó a usar `FxBackgroundTaskRunner` para los workers de preparación recomendada y video local. `SettingsDialog` conserva workers propios como deuda explícita para una tanda posterior, porque sus operaciones tienen confirmaciones y mensajes específicos que conviene migrar por grupos.

## Avance RF7

`SettingsDialog` ya no crea workers manuales. Sus operaciones largas usan `FxBackgroundTaskRunner` con los mismos cuerpos de operación, confirmaciones y mensajes visibles. La deuda siguiente no es el arranque de hilos, sino separar la orquestación por dominio: Voz IA avanzada/CUDA, Voz local simple/Piper, Video local/FFmpeg y configuración inicial.

## Avance RF8

`AdvancedVoiceSettingsOperations` concentra la orquestación de Voz IA avanzada y CUDA: preparación CUDA, smoke GPU, preparación/descarga/importación del modelo, smoke WAV, reproducción de prueba y selección del motor. `SettingsDialog` conserva la tarjeta visual y métodos delegadores históricos, pero ya no importa los casos de uso XTTS/CUDA. La deuda inmediata queda en Piper, FFmpeg y configuración inicial.

## Avance RF9

`PiperSettingsOperations` concentra la orquestación de Voz local simple/Piper: verificación, preparación automática, importación de carpeta de voz, selección del motor y resumen de requisitos pendientes. `SettingsDialog` conserva la tarjeta visual, URLs editables y métodos delegadores históricos, pero ya no importa los casos de uso de importación/selección de Piper. La deuda inmediata queda en FFmpeg/video local y configuración inicial.

## Avance RF10

`VideoLocalSettingsOperations` concentra la orquestación de Video local/FFmpeg: verificación de runtime, preparación automática, importación de carpeta, persistencia de ruta embebida y estado de progreso. `SettingsDialog` conserva la tarjeta visual, URL editable y métodos delegadores históricos, pero ya no importa los casos de uso FFmpeg ni reportes de runtime. La deuda inmediata de Configuración queda en la configuración inicial.

## Avance RF11

`InitialSetupSettingsOperations` concentra la orquestación de primer uso: inspección de Voz local simple, confirmación, descarga, selección del motor y mensajes de cierre. `SettingsDialog` conserva el punto de entrada `showFirstUseSetup` y métodos delegadores históricos, pero ya no importa la descarga de Piper ni su reporte para configuración inicial. Configuración queda estabilizada por coordinadores de dominio; la siguiente prioridad pasa a `VoiceLibraryWorkspaceView`.

## Avance RF12

`VoiceSampleActions` concentra las acciones operativas de muestras dentro de `Voces`: importar audio, grabar, detener, cancelar, reproducir, exportar y eliminar muestras por tono. `VoiceEngineSettingsControls` concentra selector de motor, selector de dispositivo, inspección de entorno y guardado de settings compartidos. `VoiceLibraryWorkspaceView` conserva navegación interna, selección, render de módulos y actualización de estado, pero deja de abrir `FileChooser` para muestras y deja de persistir directamente motor/dispositivo. La siguiente deuda de `Voces` queda en catálogo/tonos o gestión de voz.

## Avance RF13

`VoiceWorkspaceLayout` concentra las primitivas de presentación de `Voces`: raíz de módulo, secciones, maestro-detalle y pasos del asistente. `Inicio`, `Configurar motor` y `Gestionar voces` dejan de presentarse como columnas largas y pasan a selección izquierda + trabajo/detalle derecho. `Nueva voz` sigue dentro de `Gestionar voces`, ahora como subflujo guiado con `Identidad`, `Neutral` y `Tonos opcionales`. La deuda restante de `Voces` ya no es flujo visual básico, sino extraer el render de detalle de voz y/o catálogo de tonos para reducir más la clase.
