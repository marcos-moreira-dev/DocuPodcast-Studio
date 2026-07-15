# Estudio documental: problemas tecnicos

## Alcance inicial

La modalidad Estudio documental ahora permite preparar problemas tecnicos desde bloques del documento fuente. El documento original sigue siendo solo lectura.

## Flujo de usuario

- El comando `PREPARE_TECHNICAL_PROBLEM` aparece en la pestana Estudio, grupo Problemas.
- Desde Tanda 93, la pestana Estudio queda reducida a opciones de problema tecnico; lectura, voces y documento se mantienen en sus superficies generales.
- Desde Tanda 97, al pulsar `Problema tecnico` en la cinta solo se abre/maximiza el SideDock derecho. La seleccion de fragmentos se activa desde el propio panel.
- El boton del panel para iniciar seleccion se llama `Seleccionar fragmentos para asociar a problema`.
- Al iniciar seleccion desde el panel, el lector muestra checkboxes junto a los bloques visibles.
- Desde Tanda 103, para PDF el panel usa seleccion de capturas del PDF: el usuario arrastra rectangulos sobre paginas renderizadas y cada captura se convierte a `sourcePage` + `bbox` con un PNG temporal fiel.
- En terminos de producto, el gesto es arrastrar rectangulos sobre la pagina renderizada para preservar formulas, tablas o enunciados como imagen fiel.
- Desde Tanda 104, al generar un problema desde PDF se usan esas capturas rectangulares como fuentes. Los checkboxes por bloque quedan reservados para DOCX/Markdown/TXT.
- Desde Tanda 113, el texto visible del panel evita jerga tecnica: habla de capturas PDF seleccionadas, no de `bbox`, OCR ni chunks.
- Desde Tanda 115, el flujo PDF mantiene seleccion por capturas visuales y corrige la conversion desde la pagina visible al `sourcePage` + `bbox` real, incluso con zoom o ajuste a ancho.
- Desde Tanda 115B, la captura PDF usa un overlay propio sobre la pagina renderizada: el rectangulo se dibuja sobre la imagen visible, se materializa como PNG y actualiza el contador de capturas.
- Desde Hotfix T116B, la captura PDF agrega fallback de hit-test contra el frame de pagina renderizada cuando el overlay no tiene bounds validos; si el cursor de captura aparece, el drag debe dibujar rectangulo y crear captura.
- Desde Tanda 116C, la captura PDF usa filtros de evento sobre el overlay y el frame de pagina para interceptar el drag antes de que el scroll o la imagen lo consuman. La conversion usa el rectangulo visible real de la pagina renderizada.
- El SideDock derecho muestra el modulo Problema con contador, vista previa, limpiar seleccion y generar problema. El SideDock izquierdo queda para indice, fragmento y audio/reproduccion del documento.
- Desde Tanda 92, el mismo SideDock tambien lista problemas guardados del proyecto.
- Desde Tanda 96, el SideDock derecho usa scroll vertical propio para que filtros, lista, preview y acciones no queden tapados por la barra inferior.
- Desde Tanda 97, las acciones del SideDock derecho se apilan verticalmente para evitar botones comprimidos o texto vertical.
- Cada problema guardado puede reabrirse para editar solucion textual, notas y opcionalmente reemplazar/crear PNG de lienzo.
- El enunciado, fuentes, crops, `sourcePage`, `bbox` y titulo quedan solo lectura al reabrir en v1.
- El panel permite exportar la solucion PNG cuando exista `STUDY_SOLUTION_IMAGE` y exportar TXT cuando exista solucion textual.
- El panel permite eliminar un problema guardado; se quitan sus assets de estudio y se borran sus archivos generados si siguen dentro de la carpeta del proyecto.
- Desde Tanda 94, el listado de problemas guardados permite buscar por titulo, enunciado, fuente, solucion y notas.
- El listado filtra por capitulo/rango derivado del indice, pagina inicial/final, bloque fuente y estado: todos, sin solucion, con texto, con lienzo o con crops.
- Cada problema guardado permite escoger una fuente capturada y usar `Ir a fuente` para seleccionar el bloque original en el lector.
- El modal de problema muestra el enunciado a la izquierda con texto extraido y crops fuente cuando el PDF tiene `bbox`.
- Cuando el problema viene de capturas rectangulares PDF, el enunciado puede componerse solo por imagenes/crops; `selectedText` puede quedar vacio o best-effort.
- Desde Tanda 105, el enunciado incluye acciones para transferir una captura/imagen individual o transferir todas las fuentes visuales al lienzo.
- Desde Tanda 116, el enunciado permite `Cargar imagen externa`. La imagen se copia al proyecto al guardar y queda como fuente visual del problema, reutilizando el campo de asset de crop para no cambiar la forma del JSON.
- Desde Tanda 119, `Cargar imagen externa` acepta seleccion multiple (`png`, `jpg`, `jpeg`, `webp`, `bmp`) y agrega cada imagen como fuente visual transferible al lienzo.
- Desde Hotfix T116B, las acciones del enunciado viven en una barra superior (`Cargar imagen externa`, `Transferir todas al lienzo`, `Ocultar enunciado`) y al ocultarlo queda un boton `Mostrar enunciado` visible desde el lado del lienzo.
- Desde Tanda 116C, esa barra queda alineada a la izquierda con margen interno amplio para que los botones no se peguen al borde ni se compriman.
- Desde Tanda 105, el modo de pantalla completa del modal prioriza el lienzo, oculta enunciado/notas/titulo interno y `Escape` restaura la vista anterior.
- Desde Tanda 114, el encabezado interno del problema se etiqueta como `Titulo del problema`. Ese titulo se muestra como banda superior dentro del lienzo mientras el usuario dibuja y se quema dentro del PNG exportado para evitar escribir encima por accidente.
- Desde Tanda 95, el enunciado se muestra como un panel continuo y no repite codigos de bloque/pagina por cada fragmento.
- Si un fragmento tiene crop o imagen fuente embebida, esa imagen aparece en el enunciado y puede transferirse al lienzo con `Transferir imagen al lienzo`.
- En el flujo PDF por regiones, cada captura se muestra como imagen fuente en el modal y puede transferirse individualmente al lienzo.
- La derecha permite resolver con texto o lienzo mediante el toggle `Lienzo/texto`.
- El lienzo tiene toggle `Panear/dibujar`: en `dibujar` la tableta/mouse traza; en `panear` el scroll mueve el lienzo sin dibujar.
- En modo `dibujar`, los scrollbars del lienzo quedan deshabilitados para que la tableta no panee accidentalmente.
- En modo `panear`, los scrollbars se habilitan para moverse por el lienzo.
- Desde Tanda 115B, activar `Panear` apaga y deshabilita temporalmente `Interactuar con imagenes`; al volver a `Dibujar`, la interaccion puede activarse otra vez.
- El lienzo crece automaticamente hacia abajo y hacia la derecha al acercarse a los bordes o llegar al final del scroll, con limites tecnicos para no saturar JavaFX.
- Desde Tanda 97, el lienzo crece agregando tiles acotados en filas/columnas; ya no se redimensiona un unico `Canvas` gigante.
- Desde Tanda 106, esa superficie por tiles vive como componente separado del dialogo para evitar que el modal concentre la composicion, exportacion y crecimiento del lienzo.
- Desde Tanda 116C, el crecimiento por scroll agrega 25% de espacio en la dimension requerida, con guard reentrante y debounce para evitar congelamientos por crecimiento repetido.
- Desde Tanda 98, el lienzo separa fondo, imagenes transferidas y tinta en capas. La tinta se compone por encima de las imagenes, por lo que se puede escribir sobre ellas cuando `Interactuar con imagenes` esta apagado.
- El lienzo soporta color de lapiz, color de fondo, grosor, borrador, deshacer, rehacer, limpiar, scroll en modo paneo y exportacion PNG completa.
- Desde Hotfix T116B, el borrador borra solo la capa de tinta con alpha real; no pinta con color de fondo, no mancha imagenes y undo/redo no mezcla fondo ni genera bordes oscuros.
- Desde Tanda 114, el grosor del lapiz se controla con slider y una vista previa circular que refleja el tamano activo del trazo.
- Las imagenes transferidas al lienzo son objetos seleccionables solo si esta activo `Interactuar con imagenes`; con el checkbox apagado se puede escribir encima de ellas.
- Con `Interactuar con imagenes` activo, una imagen del lienzo puede moverse, reducirse, ampliarse, eliminarse o redimensionarse con manijas diagonales manteniendo proporcion.
- Desde Tanda 118, una imagen seleccionada puede recortarse usando una region marcada en el lienzo. El recorte genera una version derivada dentro del problema y conserva respaldo para `Restaurar recorte`.
- Desde Tanda 117, el lienzo agrega modo `Seleccionar region` para marcar un rectangulo interno. La seleccion permite copiar, pegar como imagen editable, mover como objeto compuesto o eliminar tinta e imagenes contenidas.
- El modal es redimensionable/maximizable y usa controles transversales con tooltips.
- Desde Tanda 96, los controles del lienzo son responsivos y se envuelven en varias filas cuando no hay ancho suficiente.
- El boton `Pantalla completa` entra en modo foco: oculta titulo, enunciado, notas y herramientas secundarias para dar prioridad al area de solucion; `Escape` restaura la vista anterior.
- Al crear un problema, `Guardar` persiste en el proyecto y `Guardar + exportar PNG...` abre un selector de archivo externo. Si se cancela el selector, el modal permanece abierto.
- El PNG externo se escribe despues de guardar el problema en el proyecto y compone fondo, imagenes transferidas y tinta.
- Desde Tanda 107, el PNG externo usa exportacion premium separada del asset interno: compone fondo opaco, imagenes y tinta, recorta al area util con margen, intenta 4x para detalles finos y baja a 2x/1x con aviso si excede el limite seguro.
- Desde Tanda 114, la exportacion usa una composicion unica para asset interno y salida externa: fondo opaco, titulo quemado en el lienzo, imagenes transferidas y tinta, sin aura ni manijas de seleccion.
- Desde Tanda 115B, la composicion deja de depender de snapshots de nodos grandes de JavaFX: usa las capas internas de fondo, imagenes y tinta para evitar exportaciones transparentes o congelamientos por texturas enormes.
- Desde Tanda 118, la exportacion de tinta usa el modelo de trazos cuando el lienzo no ha sido rehidratado desde una solucion raster antigua. Esto permite replay HD con antialiasing y reduce trazos pixelados en salidas 2x/4x.
- Desde Tanda 122, la tinta se captura como trazos crudos editables (`inkStrokes` v2) con puntos, tiempo, color, grosor y modo. El arrastre solo pinta preview incremental en una capa live; el sidecar, la composicion final y la exportacion se calculan al guardar/exportar.
- Desde Tanda 122, el preview de escritura usa remuestreo maximo de 2.5 px y curvas cuadraticas con extremos redondeados para reducir trazos rectos cuando JavaFX compacta eventos de mouse/tableta.
- Desde Tanda 125, el flujo caliente de tinta vive en `presentation.ink.InkRealtimeStrokeEngine`: el handler JavaFX agrega puntos crudos a un buffer y un `AnimationTimer` drena por presupuesto de frame. Durante `drag` no se guarda sidecar, no se exporta, no se recalculan bounds globales, no se reconstruyen tiles completos y no se dispara render PDF.
- Desde Tanda 126, `TechnicalProblemDialog` delega la captura y el render live a la infraestructura transversal `presentation.ink`. El modal conserva botones, guardado y workflow de problemas, pero el motor queda preparado para ser reutilizado despues por bocetos de teatro sin depender de estudio documental.
- Desde Tanda 131, el modo `Dibujar` tiene prioridad sobre imagenes y crops colocados: se puede escribir encima de una imagen aunque este seleccionada. `Interactuar con imagenes` queda como edicion explicita; mover o redimensionar usa manijas y no roba la tinta normal.
- Desde Tanda 132, el pincel unico usa punta redonda, suavizado live y conserva presion por punto. La presion solo modula grosor cuando el proveedor activo reporta entrada nativa real; con `JavaFX mouse` el grosor se mantiene estable.
- Desde Tanda 116, el SideDock Problema permite `Renderizar y exportar todos...`: el usuario elige una carpeta, se copian los PNG finales disponibles y se genera un `manifest.txt` con exportados, omitidos y errores.
- Desde Tanda 117, el SideDock Problema permite `Exportar ejercicios en PDF...`: el usuario elige un archivo `.pdf`, cada problema con PNG final se convierte en una pagina con fondo blanco y se genera un manifiesto junto al PDF.
- Desde Tanda 119, el SideDock Problema permite crear una fuente PDF desde una carpeta de imagenes. La app ordena las imagenes por nombre natural, crea un PDF con una imagen por pagina dentro del proyecto y lo abre como fuente documental para reutilizar visor PDF y capturas.
- Desde Tanda 114, al reabrir un problema antiguo con `STUDY_SOLUTION_IMAGE`, la solucion compuesta se carga como capa base para poder seguir dibujando encima. Las imagenes transferidas de problemas nuevos aun se persisten como composicion final, no como objetos editables independientes.

## Navegacion PDF de estudio

- Desde Tanda 113, el porcentaje `Documento N%` en PDFs se calcula por posicion visual de scroll/pagina dominante, no por chunks de texto.
- Desde Tanda 115, cuando el indice cae a `Paginas del PDF`, el panel evita listar cientos de paginas como arbol principal y ofrece `Ir a pagina` con validacion numerica.
- Desde Tanda 115, la pestana Estudio incluye `Desplazar playbar` como configuracion rapida para alternar entre playbar flotante sobre el documento y playbar acoplado al rail izquierdo. El acople automatico al abrir el SideDock derecho se mantiene.
- Los marcadores persistentes de pagina por proyecto quedan pendientes para una tanda posterior: nombre + pagina actual, lista corta, ir y eliminar.

## Imagenes fuente transversales

- `SourceVisualBlockView` es el componente transversal para imagenes detectadas en fuentes documentales.
- Las imagenes embebidas se ajustan al ancho util del contenedor donde se muestran, con proporcion preservada y sin ancho/alto fijo que empuje la pagina.
- Esta regla aplica a cualquier superficie que reutilice el componente, no solo al lector documental.

## Persistencia

El formato del proyecto sube a `formatVersion = 3`.

La nueva seccion `study` contiene:

- `technicalProblems`;
- fuentes seleccionadas por bloque/rango;
- texto capturado del enunciado;
- referencias a crops fuente cuando existan;
- solucion textual;
- asset de imagen de solucion cuando se exporta el lienzo;
- fechas de creacion y actualizacion.

Los assets semanticos nuevos son:

- `STUDY_SOURCE_CROP`;
- `STUDY_PROBLEM_IMAGE`;
- `STUDY_SOLUTION_IMAGE`.

La gestion de problemas guardados no agrega formato nuevo. Usa la lista `study.technicalProblems`, el campo `notes`, `solutionText`, `solutionImageAssetId` y el catalogo de assets existente.

Tandas 100, 103 y 104 confirman que las capturas rectangulares PDF reutilizan el mismo contrato v3:

- asset `STUDY_SOURCE_CROP`;
- `StudySourceReference.sourcePage`;
- `StudySourceReference.bbox`;
- `StudySourceReference.sourceCropAssetId`;
- `StudySourceReference.selectedText` opcional.

## Limites vigentes

- La entrada de lapiz es honesta: si la UI muestra `Entrada: JavaFX mouse`, el lienzo escribe con grosor estable y `pressure=1.0`; la presion real solo afecta el pincel cuando `Windows Pointer` o `Wintab` entregan paquetes nativos reales.
- OCR local ya existe como backend interno por pagina para detectar texto PDF y alimentar capas futuras de lectura/voz, busqueda e indice. No forma parte del gesto de creacion de problemas tecnicos en esta tanda.
- LaTeX no se reconstruye como formula editable; la ruta preferida para formulas complejas es conservar recortes fieles del PDF.
- Desde Tanda 99, los crops por bbox se renderizan primero con el motor embebido PDFBox; `pdftoppm` queda como fallback si PDFBox falla.
- Desde Tanda 100, los crops tambien pueden venir de una seleccion rectangular sobre viewport PDF renderizado, convertida a puntos PDF antes de llamar al motor.
- Desde Tanda 103, esa seleccion rectangular ya esta activa en el visor PDF visual central.
- Desde Tanda 104, los problemas PDF se guardan desde esas capturas como `StudySourceReference.visualRegion(...)`.
- Los crops por bbox requieren que `pdftotext -bbox-layout` u otra fuente entregue coordenadas `sourcePage` + `bbox`.
- Los crops siguen sin ser obligatorios para abrir ni resolver un problema: si fallan, el flujo continua con texto.
- Tanda 94 no cambia las fuentes guardadas ni edita enunciados; busqueda, filtros y navegacion son proyecciones sobre los datos existentes.
- Tanda 95 no cambia persistencia: las imagenes transferidas se fusionan en el PNG final de solucion, no se guardan como objetos editables independientes en JSON.
- Tanda 96 tampoco cambia persistencia: la ruta externa de `Guardar + exportar PNG...` es una accion de UI, no un campo del `.docupodcast.json`.
- Tanda 97 mantiene el mismo contrato: el lienzo por tiles se compone en un unico PNG de solucion al guardar/exportar.
- Tanda 98 no cambia persistencia: las imagenes del lienzo siguen fusionandose en el PNG final; las manijas y seleccion no se guardan como objetos JSON.
- Tanda 107 no cambia persistencia: la exportacion externa premium es una salida elegida por `FileChooser`; el proyecto conserva su asset `STUDY_SOLUTION_IMAGE` interno.
- Tandas 118 y 119 no cambian la forma del JSON: los recortes de imagen viven como derivados internos del problema, las imagenes externas reutilizan assets de estudio y la fuente PDF creada desde carpeta se trata como fuente documental generada dentro del proyecto.
