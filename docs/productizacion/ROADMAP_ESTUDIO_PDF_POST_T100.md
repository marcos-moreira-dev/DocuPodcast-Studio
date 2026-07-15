# Roadmap Estudio PDF Post T100

Este documento es la guia viva vigente para continuar el trabajo de estudio documental con PDF visual despues de la Tanda 100. Reemplaza como punto de partida a notas dispersas de continuidad, sin borrar el valor historico de memorias anteriores.

## Guardarrailes

- No eliminar ni mover documentacion de Domain Model Studio/UENS. En particular se conservan `docs/00_ONBOARDING_PROYECTO.md`, `docs/03_REFERENCIAS_DMS_FRACTAL.md` y `00_MEMORIA_PROYECTO/03_REFERENCIAS_DMS_RESUMEN.md`.
- No cambiar `.docupodcast.json` v3 para estas tandas salvo decision explicita posterior.
- OCR, busqueda visual y seleccion rectangular empiezan solo cuando sus tandas lo indiquen.
- Los documentos antiguos de `docs/productizacion` con nombres `T100`, `T101`, `T102`, etc. pertenecen a una numeracion historica de otra linea de trabajo y no deben usarse como guia actual de estudio PDF. Se mantienen como referencia historica/legacy.
- La fuente oficial de continuidad para esta linea es este archivo y las memorias nuevas en `00_MEMORIA_PROYECTO`.

## Orden De Tandas Pendientes

### T102 - Visor PDF Visual Central
Implementado. Activar el visor PDF visual central cuando la fuente documental sea PDF. DOCX, Markdown y TXT conservan el lector por bloques.

### T103 - Seleccion Rectangular Sobre PDF
Permitir arrastrar rectangulos sobre paginas renderizadas y convertir viewport a coordenadas PDF en puntos.

### T104 - Problemas Tecnicos PDF Por Capturas
Crear problemas tecnicos desde capturas rectangulares PDF, guardando `STUDY_SOURCE_CROP`, `sourcePage`, `bbox` y texto best-effort opcional.

### T105 - Modal De Problema Tecnico Definitivo
Implementado. Cerrar el modal de problema tecnico con enunciado continuo, transferencia individual o masiva de capturas al lienzo, pantalla completa con Escape, split ajustable y controles responsivos.

### T106 - Lienzo Seguro De Alta Fidelidad
Implementado. Resolver la superficie de lienzo como componente separado con tiles acotados, capa de fondo, capa de imagenes, capa de tinta, escritura sobre imagenes, seleccion con manijas, resize proporcional, mover y eliminar.

### T107 - Exportacion PNG Premium
Implementado. Exportar PNG compuesto con calidad x4 opcional para trazos finos, fondo correcto, imagenes transferidas, recorte seguro del area util y degradacion controlada de escala cuando el tamano excede el limite seguro.

### T108 - Capa Textual PDF Nativa
Implementado. Crear `PdfTextLayer` desde metadatos nativos `bbox-layout`, con lineas, tokens aproximados, coordenadas PDF, origen `NATIVE_BBOX`, confianza y paginas `UNAVAILABLE` cuando no haya texto nativo.

### T109 - Lectura/Resaltado Sobre PDF Visual
Implementado. Reproducir texto PDF usando la capa textual nativa disponible y resaltar visualmente el bloque/linea activa sobre la pagina renderizada.

### T110 - OCR Local Opcional
Implementado como base interna. Agregar OCR local por pagina para cualquier PDF cuando se solicite o cuando la capa nativa sea insuficiente, con cache, diagnostico, idioma `spa+eng` y confianza por palabra/linea. Su objetivo es alimentar la capa textual usada por lectura, voz, busqueda e indice futuro; no reconstruye LaTeX ni edita el PDF.

### T111 - Temario/Indice PDF Robusto
Implementado. Perfeccionar temario PDF con prioridad de bookmarks/Contents existentes y enriquecimiento controlado con capa textual nativa/OCR en paginas iniciales cuando el indice base es debil. Rechaza inferencias pobres, bibliograficas o poco distribuidas y conserva fallback por paginas renderizadas.

### T112 - Busqueda PDF Visual
Implementado. Buscar en capa textual PDF nativa y, de forma explicita, con OCR local para paginas sin texto. Los resultados aparecen por pagina en el panel Indice y pueden saltar/resaltar la region visual sobre el visor PDF.

### T113 - SideDock Estudio Adaptado A Paginas/Regiones PDF
Implementado como flujo PDF visual mas limpio. El visor PDF ahora ajusta zoom visual y DPI de render para reducir borrosidad, limita la cola de renderizado, actualiza `Documento N%` segun el scroll visual, evita jerga OCR en la busqueda principal y habla de capturas PDF para problemas tecnicos.

### T114 - Modal, Lienzo Y Exportacion Fiel
Implementado. Corregir el modal de problema tecnico con slider de grosor, preview circular, titulo visible dentro del lienzo y quemado en PNG, exportacion con imagenes/tinta/fondo y reapertura de soluciones antiguas como capa base dibujable.

### T115 - PDF Seleccion, Navegacion Y Playbar
Implementado con hotfix T115B. Corregir seleccion rectangular PDF con overlay sobre la pagina renderizada, reemplazar la lista interminable de paginas por `Ir a pagina`, actualizar porcentaje por scroll visual y agregar `Desplazar playbar` en Estudio. El playbar puede mostrarse completo en el rail izquierdo aun sin SideDock derecho abierto. Pendiente: marcadores persistentes de pagina por proyecto.

### T116 - Imagenes Externas Y Exportacion Masiva De Problemas
Implementado. El modal permite cargar imagenes externas como fuentes visuales del problema, copiarlas al proyecto al guardar y transferirlas al lienzo. El SideDock Problema permite renderizar/exportar todos los PNG finales disponibles a una carpeta elegida por el usuario con `manifest.txt`.

### Hotfix T116B - Captura PDF, Borrador Real Y UX Del Modal
Implementado. La captura PDF suma fallback de hit-test contra el frame de pagina cuando el overlay no recibe bounds validos. El borrador limpia solo la capa de tinta con alpha real y no mancha imagenes. El enunciado tiene barra superior de acciones y siempre deja visible `Mostrar enunciado` al colapsarse.

### T117 - Politica Final Por Modo De Proyecto
Formalizar que estudio documental usa PDF visual como primera clase, mientras teatro y narrativa recomiendan DOCX/Markdown para guion editable.

### T118 - ComfyUI Alto Consumo Real
Implementar perfil alto consumo opt-in para ComfyUI con diagnostico de VRAM/RAM y flags detectados por probe del runtime.

### T119 - Configuracion Clara De Modelos Pesados
Separar claramente descargar modelo, importar modelo local, importar workflow e importar runtime; mostrar estados accionables para modelos gated.

### T120 - Pruebas Con PDFs Reales Grandes
Probar con libros PDF grandes, escaneados, protegidos, matematicos y con imagenes pesadas. Documentar memoria, tiempos, fallos y limites.

### T121 - Documentacion Final Y Guia Operativa
Cerrar guia de uso para estudio documental con PDF, problemas tecnicos, OCR opcional, indice, exportacion PNG y ComfyUI pesado.

## Documentos Historicos Con Numeracion Conflictiva

Los archivos antiguos en `docs/productizacion` cuyo nombre empieza por `T100`, `T101`, `T102` y demas numeracion alta fueron parte de una linea previa de UI/productizacion. No se eliminan en esta tanda; quedan como legado historico. Si una tanda posterior decide archivar fisicamente, debera validar referencias y no tocar DMS/UENS.

## Proxima Tarea Implementable

T117A: implementar marcadores PDF por proyecto para completar la parte pendiente de navegacion rapida: crear marcador desde pagina actual, nombrarlo, listarlo, saltar y eliminarlo sin romper proyectos existentes. Luego continuar con la politica final por modo de proyecto.

## Continuidad De Tinta Tecnica Post T122

La continuidad vigente para corregir la escritura lenta/angular del lienzo tecnico queda registrada en `docs/productizacion/PLAN_T123B_T124_ARQUITECTURA_ENTRADA_STYLUS.md`.

- T123B implementado: separar entrada de tinta con `InkInputProvider`, mantener JavaFX MouseEvent como fallback y agregar diagnostico real de samples/latencia/distancia.
- T124 implementado: crear motor transversal `application.ink`, frontera reusable `presentation.ink`, sidecar generico v3 y proveedor stylus opcional detras de factory + fallback. La base queda lista para que teatro use el mismo estado de tinta en futuros bocetos de frames.

## Continuidad De Tinta Post T124

- T125 implementado: el hot path de tinta del problema tecnico usa `presentation.ink.InkRealtimeStrokeEngine`. El `drag` solo encola puntos crudos con tiempo/color/grosor/presion/modo; un `AnimationTimer` drena con presupuesto de 4 ms por frame, pinta preview cuadratico por midpoint y confirma trazos vectoriales al terminar. La tinta ya no llama a exportacion, sidecar, snapshots, bounds globales ni render PDF durante el gesto.
- T126 base implementada: `TechnicalProblemDialog` delega captura/render live a `presentation.ink` y queda mas cerca de orquestador UI. Siguen pendientes extracciones mayores de imagenes, seleccion, exportacion y serializacion desde `StudyProblemCanvasSurface` hacia controladores reutilizables, pero el flujo pesado de input/render ya no vive inline en el modal.
- T127-T130 implementados: se corrigio el diagnostico de proveedor, se agrego base Windows Pointer/JNA y fallback honesto a JavaFX mouse. La app solo declara entrada nativa cuando llegan paquetes reales.
- T131 implementado: en modo `Dibujar`, la capa de tinta tiene prioridad sobre imagenes/crops colocados; se puede escribir encima de una imagen seleccionada y la edicion de imagenes queda explicita.
- T132 implementado: el pincel unico usa presion por punto solo con proveedor nativo real; JavaFX mouse conserva grosor constante. La presion queda persistida en sidecar de tinta sin cambiar `.docupodcast.json`.
- Proxima tarea implementable: validar con tableta fisica si Windows Pointer entrega paquetes reales. Si la UI sigue mostrando `Entrada: JavaFX mouse`, implementar Wintab funcional antes de invertir mas en suavizado.
