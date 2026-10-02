# Plan UX: Express con salida de audio y ciclo de ventanas

Estado: implementación incorporada el 2026-09-21. El diagnóstico de abajo describe el estado anterior al cambio.

## Resultado implementado

- Inicio ofrece «Documentos a audio o video Express». La configuración permite elegir Video con audio o Solo audio, y WAV/MP3/AAC para todo el lote. Solo audio oculta los ajustes visuales y conserva el análisis opcional de imágenes como contenido narrado.
- El perfil JSON conserva tipo/formato; los descriptores antiguos siguen siendo de video. Los audios se guardan en `audios-exportados`, con un destino único por documento; video mantiene `videos-renderizados`.
- La rama de audio reutiliza preparación y voz, compone el archivo con el exportador de audio existente y no inicia renderizado de video. MP3/AAC usan FFmpeg para comprimir; WAV no necesita el renderizador de video.
- La reutilización del audio exige una constancia de exportación terminada y un SHA-256 coincidente. Un archivo parcial sin constancia o modificado no se reutiliza.
- Express oculta la ventana principal. La pausa y el resultado final permanecen en la cola; Aceptar y volver a Inicio prepara Inicio antes de mostrar la ventana principal.
- Cancelar documento actual continúa con la cola. Cancelar toda la producción confirma una vez, cancela pendientes, solicita la detención activa y espera sus callbacks antes de cerrar. Las rutas de error de preparación de voz comunican el fallo a la cola, incluso antes de que exista un trabajo TTS.
- El resumen se muestra dentro de la cola y las operaciones se distribuyen en varias filas si hace falta. Se conservan reintentos individuales y el acceso a la carpeta de salida.

Validación automatizada: perfil y compatibilidad JSON, dos documentos por formato con destinos únicos, preservación de completados al cancelar, integridad de reutilización, composición WAV, selección de códec MP3/AAC mediante ejecutor simulado, visibilidad y retorno de ventanas JavaFX, pausa y espera de cancelación. No se ha ejecutado un lote completo con motores TTS reales ni certificado todas las escalas DPI de Windows.

Comando: `mvn -pl studio-desktop -am test -Dtest=DocumentVideoBatchWindowTest,DocumentVideoBatchProjectUseCaseTest,ExportPodcastWavUseCaseTest,DocumentExportContinuationTest,DocumentExportReadinessSnapshotTest,DocumentExportPassiveBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false`. Registro: `target/express-audio-validation.log`.

## Objetivo

Desde Inicio, preparar una carpeta de documentos y exportar un archivo final por documento, como video o solo audio. Durante la producción, la cola debe ser la única ventana de trabajo visible. Al finalizar y aceptar, o cancelar toda la producción y detenerla, volver a Inicio.

## Comportamiento corroborado en el código actual

- Inicio ofrece «Documentos a video Express» (`WelcomeWorkspaceView`).
- `DocumentVideoBatchWindow.showWindow` abre una ventana independiente y minimiza el Stage principal mediante `setIconified(true)`. No lo oculta ni impide restaurarlo desde Windows.
- Mientras la ejecución está activa se deshabilita «Ver en DocuPodcast». Cerrar la cola con X durante la ejecución muestra una indicación para pausar o cancelar.
- La devolución `finished` muestra un resumen con `showAndWait()` y después cierra la cola, incluso si el resultado es una pausa.
- `setOnHidden` restaura la ventana principal y luego ejecuta el retorno a Inicio. Ese orden permite que se vea el documento antes de cambiar de vista.
- `returnFromDocumentVideoBatch` puede pedir guardar/descartar cambios; si el usuario cancela ese paso, no llega a Inicio aunque la cola ya se cerró.
- «Cancelar documento actual» y «Cancelar toda la producción» son operaciones diferentes; la primera no implica salir del lote.
- La exportación del lote usa la ruta de video en segundo plano y sus callbacks, sin invocar el diálogo interactivo de exportación de cada archivo.

Esta corroboración es una inspección del código; no es una prueba visual de ventanas nativas en Windows.

## Ubicación de Solo audio

Cambiar el acceso de Inicio y el título a **«Documentos a audio o video Express»**. Subtítulo: «Procesa una carpeta de Word/PDF y exporta un archivo por documento».

En la configuración, mantener primero «Documentos y destino» y colocar inmediatamente después una sección **«Salida del lote»**, visible sin abrir un acordeón. Debe aparecer antes de cualquier ajuste exclusivo de video.

```text
Documentos y destino
  Carpeta de documentos …     Guardar en …

Salida del lote
  (●) Video con audio        ( ) Solo audio
  Se aplica a todos los documentos de esta cola.

  [Solo audio seleccionado]
  Formato de audio: [MP3 ▾]   WAV · MP3 · AAC
  Un archivo de audio por documento.

  [Crear cola de audio]
```

Usar radios excluyentes dentro de una fila adaptable, con etiqueta textual completa; no un checkbox aislado ni un ajuste escondido en «Apariencia del video». Conservar los componentes y estilos de la aplicación.

- **Video:** mostrar apariencia, duración de diapositivas y logo; conservar el comportamiento y valores existentes.
- **Solo audio:** mostrar formato; ocultar y retirar del espacio de diseño los ajustes exclusivamente visuales. Proponer MP3 como selección inicial; WAV y AAC disponibles según los exportadores existentes. No renombrar un archivo para simular una conversión.
- Mantener «Interpretar imágenes» como opción de contenido, separada de la duración visual: puede aportar narración incluso sin video. Respetar su estado actual, sin activarla automáticamente.
- Cambiar entre opciones antes de crear la cola conserva los valores introducidos. La selección se guarda en el descriptor y se bloquea una vez creada la cola para evitar mezclar resultados en una reanudación. Un lote de otro tipo se prepara como nueva cola.
- Resumen persistente: «12 documentos · Solo audio · MP3 · [destino]». El botón de ejecución dirá «Exportar 12 audios» o «Exportar 12 videos»; al reabrir una cola parcial, indicar los pendientes.

## Cola y salida a Inicio

| Estado | Ventana visible y acciones |
| --- | --- |
| Configuración | Solo Express; cerrar vuelve a Inicio. |
| Produciendo | Solo cola; progreso global, archivo actual y etapa. Pausar, cancelar actual y cancelar toda la producción se distinguen claramente. |
| Pausa solicitada | «Pausando…» hasta alcanzar un punto seguro; evitar clics repetidos. |
| Pausada | Mantener la cola abierta; ofrecer Continuar y Guardar cola y volver a Inicio. |
| Cancelación total solicitada | Confirmar una vez; mostrar «Deteniendo…» hasta que el trabajo y sus procesos estén realmente detenidos y el estado guardado. Después cerrar y volver a Inicio. Conservar archivos terminados válidos. |
| Completada | Mostrar el resumen en la propia cola: completados, fallidos y omitidos. Abrir carpeta de salida y Aceptar y volver a Inicio. No cerrar antes de que se lea el resultado. |
| Con errores | Mantener resultados y causas por documento; Reintentar fallidos o Aceptar y volver a Inicio. |

La X durante una ejecución debe seguir la cancelación total confirmada; rechazar la confirmación mantiene la producción. Cancelar solo el documento actual continúa con el resto. Abrir la carpeta no restaura el editor.

Ocultar realmente el Stage principal mientras Express está abierto, con la cola independiente y visible antes de ocultarlo. En la salida, resolver guardado, liberar el proyecto hijo y seleccionar Inicio **antes** de mostrar y enfocar el Stage principal. Evitar un cierre de la aplicación por quedarse transitoriamente sin ventanas. No usar eventos de ocultación como única señal de finalización: diferenciar cierre definitivo, transición y salida para inspección.

«Ver en DocuPodcast» solo estará disponible con el lote detenido. Debe ser una salida explícita de Express, con la cola guardada, para no dejar dos superficies de trabajo abiertas. El retorno normal solicitado sigue llevando a Inicio.

## Trabajo técnico previsto

1. Ampliar el perfil persistido con tipo de salida y formato de audio. Los descriptores anteriores sin estos campos seguirán representando video. Mantener compatibilidad de las rutas y archivos antiguos.
2. Reutilizar importación, preparación semántica y generación de voz. Después de verificar el audio, bifurcar: componer/exportar audio final o construir/renderizar video.
3. Para audio, exportar un archivo completo por documento usando el exportador existente, con orden, pausas y mezcla del proyecto. No entregar una colección de fragmentos TTS intermedios.
4. Generar destinos en `audios-exportados` y conservar `videos-renderizados` para video; nombres únicos y extensiones coherentes. Adaptar verificación, reanudación, progreso e informe a cada salida. Audio no debe exigir capacidades de renderizado visual; sí las herramientas necesarias para su formato.
5. Centralizar la transición entre cola e Inicio y hacerla idempotente. Redirigir notificaciones y errores del lote a la cola para que no restauren el editor.
6. No cambiar el backend de voz ni rediseñar el editor normal como parte de esta entrega.

## Aceptación y comprobaciones

- Dos documentos producen dos audios completos del formato elegido; no se generan MP4 ni se inicia renderizado visual en Solo audio.
- Una cola antigua conserva su salida de video. Reintentos y reanudaciones reutilizan únicamente resultados válidos del tipo y formato correctos.
- Toda la configuración es accesible por teclado, con foco visible y etiquetas accesibles. El cambio de salida no oculta el control que tiene el foco; el contenido oculto no participa en tabulación.
- La disposición se adapta mediante filas que se apilan y desplazamiento vertical; no ampliar la anchura mínima actual. Validar con escalado de Windows al 125 % y 150 %.
- Durante preparación, voz, exportación y errores, no aparece ni se restaura automáticamente el editor principal. Revisar también la barra de tareas y Alt+Tab en Windows.
- Aceptar al finalizar lleva a Inicio sin destello del último proyecto. Cancelación total espera la detención, conserva resultados válidos y vuelve a Inicio. Cancelar un documento mantiene la cola.
- Pausar permite continuar sin reabrir Express. Cerrar/reabrir una cola guardada recupera configuración, progreso y archivos.
- Si hay cambios ajenos al lote pendientes de guardar, resolverlos antes de entrar en Express, para no descubrirlos después de cerrar la cola.

La validación deberá combinar pruebas del perfil/descriptor y exportación con pruebas JavaFX del ciclo de ventanas y una comprobación visual en Windows. Este plan todavía no implica esas pruebas ni certifica conformidad de accesibilidad.
