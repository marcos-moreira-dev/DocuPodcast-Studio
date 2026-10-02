# Auditoría: imágenes representativas en video documental

Fecha: 2026-09-30. Alcance: inspección estática de código; no se ejecutaron motores IA ni exportaciones reales.

## Implementación posterior a la auditoría (2026-09-30)

La inspección original que sigue se conserva como diagnóstico previo. Ya se implementaron:

- Opción persistente apagada por defecto, en Contenido del video → Ajustes del video → Ilustraciones con IA, con `StudioFormControls`. Usa la configuración global de motores, sin otra selección duplicada.
- `PrepareDocumentIllustrationsUseCase` en aplicación y preparación común antes de componer el video documental. No importa clases teatrales. El flujo de render completo sigue resolviendo el audio primero.
- Operaciones neutrales de planificación del prompt y revisión visual, soportadas por el adaptador de la IA general. Contexto acotado de hasta tres intervenciones anteriores y tres posteriores; estilo común de dibujo simbólico sencillo.
- Solicitud de 512 × 512, archivo final máximo 600 px por lado, sin superresolución; un solo reintento correctivo, cancelación y caché por contenido/contexto/estilo/motor/preset. No se ilustran títulos ni subtítulos ni se sustituyen imágenes explícitas o capturas originales.
- Imágenes aceptadas y manifiestos en `media/images/document-illustrations`, conservados al exportar el proyecto. Avisos de fallos parciales en el resultado del video y `last-report.txt`.
- Composición con regiones independientes de texto e imagen; la imagen se reduce cuando hace falta. Usa las unidades de narración existentes, sin inventar otra duración del audio.
- GPU preferida al generar imágenes, respetando una preferencia explícita. El contrato de motores admite demanda y liberación de recursos. ComfyUI mantiene admisión conservadora exclusiva: no declara consumos ficticios ni promete convivencia física comprobada. Solicita descarga antes de la revisión por la IA general; el acuse HTTP de `/free` no se interpreta como prueba de memoria ya liberada.
- PDF de ejercicios recorre todas las páginas del cuaderno guardado en orden, usando el renderizador de tinta compartido. Los proyectos antiguos sin estado editable conservan la exportación de su PNG final.
- El motor común de tinta usa curvas entre muestras en lugar de unir toda escritura con segmentos rectos, tanto en la vista en vivo como al reconstruir tinta guardada. Prueba comparativa Normal/Express de puntos y píxeles.

Validación automatizada: compatibilidad JSON, preparación desactivada, generación y revisión con motores de prueba, caché, cancelación, límite de reintentos, conservación de visuales explícitos, composición, páginas del cuaderno y edición compartida. No se ha certificado generación real ni convivencia de modelos en el hardware del usuario. Las pruebas HTTP de adaptadores encontraron `Unable to establish loopback connection` de Java en este entorno; esto no demuestra un fallo del servidor ComfyUI.

Pendiente de certificación de runtime: medir residencia real de ComfyUI/Ollama y confirmar descarga efectiva para habilitar convivencia adaptativa por capacidad. La admisión conservadora es la política actual, no una certificación de offload combinado.

## Conclusión de la inspección inicial

La infraestructura de generación es reutilizable, pero el flujo de producto no está completamente desacoplado de Teatro. No existe todavía la opción documental persistente de generar automáticamente una ilustración sencilla por diapositiva, desactivada por defecto. No basta con añadir un checkbox.

## Hallazgos

1. **PDF de ejercicios incompleto para cuadernos multipágina.** `presentation/shell/workflow/StudyProblemWorkflow.exportAllSolutionImagesAsPdf` añade exactamente un `StudyProblemPdfPage` por `TechnicalProblem.solutionImageAssetId`. `TechnicalProblemDialog.exportCanvas` exporta la superficie actual; las otras hojas se conservan separadamente en los metadatos `notebook.page.*`. La exportación PDF no recorre esos estados. Exportar todos los ejercicios no equivale a exportar todas sus hojas.
2. **Contrato de motor adecuado.** `studio-media-api/.../ImageGenerationEngine` recibe `ImageGenerationRequest` y `ExecutionContext`, sin depender de Teatro. `application/media/MediaCapabilityService` centraliza generación y adquisición de recursos mediante el scheduler. `application/visual/VisualImageWorkflowResolver` y `LocalVisualImageEngineManager` son reutilizables. Conviene conservar esta frontera.
3. **Acoplamiento en presentación.** `DocumentMediaRailView.handleEmptyVariantAction` abre `showTheatreImageGenerationWorkspace` al solicitar una variante generada. Usa `TheatreVisualVariant`, incluso para contenido documental. Un motor compartido no requiere compartir la pantalla teatral ni sus conceptos de escenografía.
4. **Flujo narrativo existente, pero inadecuado como implementación directa.** `presentation/shell/workflow/NarrativeImageGenerationWorkflow` genera por segmento, importa un asset y usa el motor seleccionado en ajustes operativos. Construye un prompt cinematográfico, formato 16:9 y pasa por superresolución según configuración. Usa `CancellationToken.NONE`; además modifica `ProjectSession` desde el workflow de presentación. No resuelve una cola documental cancelable de ilustraciones simples por diapositiva. La política visual debe ser específica del caso documental y la orquestación debe vivir en aplicación.
5. **Falta configuración documental.** `DocumentStudyVideoConfiguration` no contiene activación de ilustraciones IA automáticas ni selección de motor para esta función. Los paneles documentales inspeccionados tampoco exponen ese contrato. La selección global existente no sustituye una opción explícita en esta GUI.
6. **Falta composición texto + ilustración representativa.** `DocumentStudySlideCompositor.composeParagraph` ofrece imagen principal en modo `illustrationOnly`, o texto con mascota en el otro modo. Una mascota no representa semánticamente cada intervención. Hace falta un espacio de ilustración acompañante, con reglas de distribución que no tapen texto ni alteren capturas o fórmulas de origen.
7. **PDF sí tiene una ruta de video.** `BuildDocumentStudyVideoPlanUseCase` trabaja con proyección documental y materializadores PDF/Word; contempla capturas ROI de PDF y bindings de narración. `ExportDocumentStudyVideoUseCase` usa el renderizador compartido sin overlays de Teatro. Esto demuestra soporte arquitectónico, no garantiza que el PDF concreto abierto tenga ya lectura, narración y assets preparados.

## Diseño recomendado

- GUI en Ajustes del video: checkbox **«Incluir una imagen representativa con IA en cada diapositiva»**, apagado por defecto. Debajo, combobox estilizado **«Motor de imágenes»**, habilitado al activar la función, con los motores de generación disponibles y su estado. Separar claramente motor y preset/modelo.
- Configuración persistente del proyecto: activación, motor elegido y perfil de ilustración sencilla. Proyectos antiguos: desactivado. No iniciar generación por abrir el panel o marcar el checkbox; ejecutarla en una fase explícita de preparación del video con progreso y cancelación.
- Nuevo caso de uso documental que consume las diapositivas narradas resueltas y llama a `MediaCapabilityService`. No depender de personajes, escenografía, variantes ni controladores teatrales.
- Prompt breve, por ejemplo: «Ilustración educativa sencilla tipo icono sobre [concepto]. Un solo motivo, pocos detalles, sin letras ni números». Usar el texto de la intervención correspondiente, acotado; evitar prompts cinematográficos. No usar imágenes generadas como sustitutos de fórmulas o diagramas técnicos exactos.
- Una imagen por diapositiva narrada pertinente, no por fotograma. Excluir portada/cierre sin contenido semántico y preservar imágenes o capturas explícitas salvo elección del usuario. Mantener el texto como contenido principal.
- Presupuesto de resolución moderado compatible con el motor; sin upscale automático para este perfil. El coste depende del modelo y sus parámetros, no solo de que el prompt pida un icono.
- Caché por texto, motor/modelo, versión del prompt, dimensiones y semilla; almacenar assets y vínculo estable a diapositiva/segmento. Reutilizar resultados válidos, invalidar solo los afectados y permitir reintentos. No volver a generar al reproducir o exportar repetidamente.
- Generación fuera del hilo JavaFX, con cancelación real y scheduler compartido. Si falla una imagen, informar por diapositiva y permitir continuar sin ella; nunca reemplazar contenido de origen silenciosamente.

## Verificación necesaria antes de darlo por terminado

- Desactivado: cero llamadas IA y render idéntico al actual.
- Activado: misma ruta para PDF y Word, correspondencia correcta texto/imagen, motor seleccionado respetado y persistido.
- Repetir exportación reutiliza assets; cambiar una intervención invalida solo su ilustración.
- Cancelación, motor no disponible y fallo parcial no bloquean la GUI ni pierden trabajo.
- Composición legible en todas las resoluciones y preservación de capturas técnicas.
- PDF de ejercicios: recorrer todas las hojas guardadas en su orden, no solo el PNG activo; prueba con dos ejercicios de varias hojas.

No se modificó comportamiento de producto durante esta auditoría.

## Ampliación: coordinación de GPU, RAM y modelos residentes

Inspección adicional del mismo día:

- `ApplicationBootstrap` instala `PriorityResourceScheduler.incrementalReaderDefaults()`. Hay prioridades, admisión atómica, presupuestos físicos y contabilidad de modelos residentes en el scheduler. Las capacidades heredadas predeterminadas incluyen una plaza GPU y una plaza MODEL_MEMORY; no equivalen a una política automática de convivencia de dos modelos según su tamaño.
- Voz declara `engine.resourceDemand(preference)` en `MediaCapabilityService.synthesize`. Imágenes, en cambio, solicita todavía `ComputeResourceDemand.of(MODEL_MEMORY, GPU)`: reservas genéricas, estimaciones de RAM/VRAM de cero y sin identidad de modelo residente. Por tanto, no está completa la integración de imágenes con la contabilidad física disponible.
- `ComputePreference` contempla PREFER_GPU y permiso de memoria host. El soporte concreto de reparto GPU/RAM corresponde al adaptador/runtime; reservar recursos no mueve ni descarga pesos.
- Ollama tiene seguimiento de residencia y descarga en `ManagedOllamaProcess` y `OllamaModelLifecycle`. ComfyUI tiene perfiles de memoria, opciones lowvram y reintento ante falta de memoria. Esto no demuestra una coordinación conjunta de residencia Ollama/ComfyUI.

Requisitos previos a la función documental:

1. Añadir demanda física y preferencia GPU a la interfaz/adaptador de imágenes, con identidad de modelo y estimaciones conservadoras de pesos, memoria temporal y RAM de apoyo.
2. Usar el mismo scheduler para preparar prompts, generar imágenes y verificarlas. Mantener separados permisos de ejecutar y residencia real del modelo después de ejecutar.
3. Permitir convivencia solo con capacidad comprobada y margen para sistema/otras tareas. Ante información desconocida, usar alternancia conservadora, no interpretar cero como consumo real nulo.
4. Si no caben ambos modelos, procesar por lotes acotados: preparar prompts, liberar memoria del modelo general cuando sea seguro, generar imágenes y volver al modelo general para revisión. No descargar un modelo que otra tarea esté utilizando; confirmar liberación en el runtime antes de conceder su memoria a otro.
5. Delegar offload GPU/RAM únicamente a runtimes que lo soporten y comunicar dispositivo/modo efectivo. No degradar silenciosamente a CPU ni asumir que un tamaño de imagen pequeño implica un modelo ligero.
6. Probar convivencia, alternancia, memoria insuficiente, cancelación y competencia con audio usando demandas simuladas; validar después en hardware real sin interrumpir trabajos del usuario.

No se midió el hardware ni se certificó convivencia real de modelos durante esta inspección.
