# Ejercicio técnico normal y Express

## Alcance

Revisión de las entradas del editor, herramientas, selección mixta, páginas, estado editable, crecimiento del lienzo, pantalla completa y exportación. No sustituye una prueba manual con tableta física.

## Componentes comunes verificados

- `DocumentWorkspaceView` (creación desde documento/PDF), `DocumentTechnicalProblemPanel` (edición) y `DocuPodcastShellView` (Express) abren `TechnicalProblemDialog` con `DrawingFeatureCatalog.DOCUMENT_PROBLEM`.
- Ambas modalidades pasan por el mismo inicializador y el mismo catálogo de herramientas. `TechnicalProblemEditorController` delega entrada, historial, zoom y ciclo de vida en `InkEditorSession`.
- El lienzo usa `InkCanvasSurface`, el contenedor `InkCanvasZoomPane` y la selección compartida `CanvasStrokeSelection`. Las imágenes participan por un adaptador opcional: no obliga a los otros consumidores del selector a incorporar objetos.
- Las páginas, plantillas, tinta e imágenes se guardan con `InkWorkspaceStateSerializer`. Se completó la restauración del título y se añadieron las notas a los metadatos. Los archivos anteriores sin esas claves conservan los valores del proyecto.
- Formularios, pestañas y botones reutilizan los controles estilizados existentes.
- El selector `StudioFontControls` comparte las fuentes instaladas y la vista nombre/muestra entre Video Express, exportaciones y el texto del lienzo.
- «Agregar texto» sustituye el alternador lienzo/texto. `CanvasTextObject` conserva contenido, familia, colores, efecto y giro junto a la representación visual; participa en el historial, selección mixta y estado de páginas. Doble clic abre el editor sobre el lienzo; Ctrl+Enter o perder el foco confirma, Escape cancela.

## Diferencias que deben mantenerse

El ejercicio normal recibe los enunciados, bloques y capturas del documento. Al guardar, el flujo documental conserva las fuentes y asocia la solución y su estado editable al proyecto. Express comienza sin esas fuentes y su resultado se maneja desde la pantalla principal. Estas diferencias de integración no cambian las herramientas del editor ni el formato del lienzo.

## Correcciones de esta revisión

- Escape recupera su salida nativa de pantalla completa. Escape y F11 también se capturan en la escena, aunque el foco estuviera en un control que se ocultó. Al entrar, el lienzo recibe el foco y se configura el aviso de salida.
- «Panear / agrandar lienzo» conserva espacio mientras se usa. Al volver a otra herramienta o soltar el paneo secundario, una espera de tres segundos retira las teselas sobrantes. Los gestos activos suspenden ese ajuste.
- Se preserva el contenido y un margen de 300 píxeles, además del tamaño inicial y del área visible. El lienzo trabaja en teselas de 1024 píxeles: el borde visible puede conservar más de 300 píxeles. La exportación técnica externa calcula sus dimensiones por contenido y margen, no por todas las teselas ampliadas.
- El ajuste elimina únicamente teselas exteriores, sin convertir tinta en imágenes ni reconstruir los trazos. Los objetos alejados participan en el cálculo del límite.

## Verificación automatizada

Pruebas de herramientas disponibles en ambas entradas, Escape/F11 despachados por la escena, intercambio de título/notas/plantilla mediante JSON, ajuste sin pérdida de tinta ni imágenes lejanas, tamaño de exportación, selección mixta, historial y captura de entrada. Las pruebas de selección también cubren consumidores sin imágenes y sus rellenos.

## Límites

Las primitivas nuevas conservan su geometría SVG, contorno, relleno y rotación en `CanvasShapeObject`, independiente de la imagen de visualización/exportación. El estado acompaña al objeto en el historial, copia, selección mixta y JSON. Las figuras antiguas guardadas únicamente como imagen no recuperan automáticamente esa geometría; la tinta mantiene puntos y presión. El tamaño inicial de página se conserva. La ventana del usuario que ya está abierta necesita cargar la versión nueva para recibir estas correcciones; la revisión no reemplaza el código de un proceso en ejecución.

## Relleno de figuras

Normal y Express comparten el grupo contextual Relleno: selector de color, Rellenar figura y Sin relleno. Aparece al seleccionar una primitiva cerrada; líneas, fotografías y texto no son rellenables. Reutiliza los controles estilizados y el icono paint-bucket existentes. El relleno de Teatro (`SketchBackdrop`) detecta regiones cerradas de tinta; no es equivalente al relleno geométrico de las primitivas y no se altera. El nuevo modelo geométrico reside en la composición transversal del lienzo.

## Opacidad y orden de objetos

Cada recurso de imagen incluye un deslizador estilizado de opacidad (0–100%) entre la vista previa y Transferir. Afecta a la vista previa y a las instancias asociadas en la página activa; las transferencias individuales y colectivas conservan el valor. Opacidad e identidad del recurso se guardan en el estado editable, copia e historial. El compositor transversal aplica el alfa del ImageView al exportar, sin modificar los píxeles originales.

El grupo Orden permite subir/bajar un nivel y enviar al frente/fondo imágenes, formas y texto, incluyendo selecciones múltiples; conserva el orden relativo de la selección. La lista persistida y el orden visual se actualizan juntos. La tinta mantiene su capa separada, por encima de los objetos: estas acciones no intercalan imágenes entre trazos.

## Auditoría de activación de herramientas

Los siete modos (Lápiz, Línea, Medir ángulo, Borrador, Panear, Seleccionar región y Agregar texto) son toggles exclusivos que admiten desactivación mediante un segundo clic. El estado NONE no dibuja, borra, inserta texto ni activa el paneo izquierdo; permite interacción con objetos y conserva el paneo secundario independiente. Se retiró la reselección forzada del grupo. Los controles de historial, transformación, orden, relleno e inserción de formas son acciones puntuales y no quedan activados como herramientas. Pantalla completa y los controles de plegado alternan sus vistas; Editar imágenes y formas conserva su estado de casilla independiente. Ambos editores usan esta misma implementación.

## Ribbon contextual (30 de septiembre)

Herramientas principales y modificador Editar objetos se agrupan arriba; propiedades de trazo, lienzo/vista y limpieza forman el segundo nivel. Transformaciones, orden, región, relleno y formato aparecen según selección. Mover lienzo sustituye el nombre largo, conservando el comportamiento. Las acciones de escala usan flechas de expansión/contracción; limpieza y eliminación llevan texto; restaurar recorte usa actualización y desplazar región usa movimiento general con tooltip del desplazamiento de 24 px. El catálogo conserva las 16 formas y los modos conservan su desactivación por segundo clic. No se modifica la separación entre tinta y objetos.
