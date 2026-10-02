# Auditoría de esperas y avisos de carga — 2026-09-24

## Cambios aplicados

- **Abrir proyecto, recientes e inicio automático:** lectura, validación, hidratación de artefactos y comprobación de integridad en una tarea de fondo. La sesión preparada permanece aislada hasta que se aplica en JavaFX. Capa clara con indicador indeterminado y «Cargando proyecto…»; controles temporalmente deshabilitados. Se retira también al fallar. La generación solicitada al inicio espera a que se abra el proyecto.
- **Cuadros del teatro:** «Cargando…» junto al título; un pulso de dibujo antes de construir los nodos. Abrir/cerrar un cuadro actualiza su tarjeta, sin reconstruir todos los cuadros hermanos. Las cargas pendientes comprueban que la tarjeta siga abierta y conectada.
- **Primera apertura de voces, configuración, frames y producción visual narrativa:** texto de carga antes de construir la vista. Las vistas ya almacenadas se abren directamente. Una navegación posterior invalida la apertura pendiente.

## Operaciones revisadas que ya tienen avisos

| Área | Operación y evidencia en código |
| --- | --- |
| Documento PDF | `PdfVisualDocumentView.startRenderTask`: render en Task, `slot.showLoading`, caché, descarte de resultados de otro documento y aviso de error. |
| Búsqueda PDF | `DocumentIndexPanel.runPdfSearch`: Task y «Buscando en las páginas preparadas…». |
| Preparación/OCR PDF | `PdfVisibleTextPreparationCoordinator`: estado y página en preparación. |
| Análisis contextual | `DocumentContextDetailsPanel`: mensajes por operación y Task. |
| Voces | `VoiceLibraryWorkspaceView.startGenerateVoiceTest`: Task, botón «Generando voz…» y desactivación temporal. |
| Modelos IA | `EngineAdministrationPane`: «Comprobando disponibilidad…», Task para disponibilidad y acciones. |
| Frames | `TheatreImageGenerationWorkspaceView`: Task para pruebas, generación e intermedios; mensajes y tiempo transcurrido. |

## Límite y seguimiento

Construir y adjuntar nodos JavaFX sigue perteneciendo al hilo gráfico. El texto de carga de cuadros y primeras vistas anuncia esa fase, pero no convierte su construcción en trabajo paralelo. Para escenas excepcionalmente grandes, el siguiente paso de rendimiento sería paginar/virtualizar sus nodos y separar el cálculo de los datos del dibujo. Esta auditoría es de rutas de código; no constituye una medición de latencia de cada motor o de todos los tamaños de proyecto.

No cambia la identidad ni el contenido de intervenciones, acotaciones, destinatarios o audios.

## Correcciones y comprobación posterior

- La desactivación del workspace combina ahora la carga del proyecto y el refresco teatral en el mismo binding. Se eliminó la escritura directa que provocaba `StackPane.disable: A bound value cannot be set`.
- Pantalla completa consulta primero la intervención activa para mantener el texto y la imagen en la misma selección.
- Auditoría automatizada de todos los SVG Lucide incluidos y de todos los recursos `AppIcon`: geometría disponible e imágenes decodificables. Las pruebas `SemanticActionIconsTest`, `SemanticActionButtonLayoutTest`, `ApprovedProductIconsTest` y `ProjectSessionCoordinatorTest` pasaron.
- Asociaciones comunes corregidas/ampliadas: cancelar/cerrar, reprocesar, refrescar, pausa, continuar, guardar notas, deshacer/rehacer, rotar y ampliar/reducir. La búsqueda genérica de «ir» ya no asigna un ojo a palabras no relacionadas.
- Los PNG se comparten por recurso y tamaño, y sus streams se cierran tras decodificarlos. Evita lecturas y decodificaciones repetidas al crear controles.
- Comprobación manual con la compilación reiniciada: apertura del proyecto de la obra con integridad OK (0 errores, 0 advertencias), iconos de transporte visibles en pantalla completa y texto de Eloy correspondiente a la selección. No se regeneraron audios ni se guardaron cambios de contenido.

La carga correcta del catálogo no demuestra por sí sola la causa de los iconos ausentes en la sesión anterior. Tampoco sustituye una revisión visual de cada diálogo en todos los tamaños.
