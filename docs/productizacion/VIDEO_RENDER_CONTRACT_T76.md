# Video render contract — T76

## Contrato central

T76 establece `docupodcast-simple-video-render-v1` como contrato de cerebro para el render de video simple.

La app debe poder construir un paquete donde cada entrada representa un fragmento hablado del Documento narrable. Si el fragmento tiene imagen y audio, se puede generar un clip. Si todos los clips están listos y FFmpeg está disponible, el paquete puede pasar a MP4 final. Si algo falta, el paquete queda auditable y no se destruye.

## Artefactos de cerebro

- `SimpleVideoPlan`: lista de frames lógicos.
- `SimpleVideoFrame`: frame asociado a segmento, imagen y audio.
- `VideoRenderCommandPlan`: contrato de render con warnings y comandos.
- `BuildVideoRenderCommandPlanUseCase`: genera el plan de comandos sin ejecutar FFmpeg.
- `ExportSimpleVideoPackageUseCase`: materializa el paquete.

## Reglas de bloqueo operativo

Mientras el render se ejecuta, el frontend futuro debe bloquear:

- lectura normal;
- edición/asignación de capas;
- generación de otro video;
- operaciones que compitan por audio/video.

Esto no significa congelar toda la app sin explicación; debe mostrarse un estado de progreso claro.

## Cancelación segura

Cancelar o detener FFmpeg debe conservar:

- `RENDER_MANIFEST.json`;
- `render-commands.txt`;
- `frames.csv`;
- plan de video;
- assets originales.

La cancelación no debe borrar audio, imágenes ni capas.

## MP4 final

El contrato diferencia entre tres estados:

1. `MP4_RENDER_READY`: se puede producir MP4 si se ejecutan comandos FFmpeg.
2. `PACKAGE_READY_FFMPEG_REQUIRED`: falta FFmpeg, pero el paquete está completo.
3. `PACKAGE_NEEDS_REVIEW`: faltan imágenes, audio o frames.

## Relación con el frontend

La interfaz todavía no está cerrada. T76 no intenta convertir Documento en editor de video. El frontend debe seguir la idea: usuario lee/escucha, asocia imágenes y luego exporta video simple.
