# Plan: fotos de personajes y escenografía

Estado: implementado el modo `scenery`, con composición compartida de escenografía/personajes en panel, pantalla completa y vídeo. La selección se guarda por proyecto y viaja en el manifiesto V2. La lectura extensa se desplaza en pantalla completa y se pagina en vídeo. Compilación e instalación del reactor correctas; 32 pruebas seleccionadas pasaron, incluida la obra de 557 parlamentos. Registro: `target/scenery-final-build.log`.

Se verificaron por píxel transparencia y semitransparencia PNG, marcador negro de imagen ausente, cambio de escena sin arrastrar fondo, selección explícita de participantes, modo tras exportar/reimportar y conservación de duración del audio al componer vídeo sin mapa. Se inspeccionó el fotograma real generado en `C:/Users/MARCOS MOREIRA/Downloads/PATRIA ESCENOGRAFIA/carpeta-8219402265918337530/escenografia-preview.png`.

La composición conserva fondos opacos presentes en las fotos originales: no elimina el fondo de una imagen. No se ha certificado todavía toda la matriz de escalas de Windows ni la accesibilidad con lector de pantalla. La paginación de vídeo reparte la duración entre páginas; no es sincronización palabra por palabra.

## Hallazgos comprobados

El ejemplo «¿Cómo será la patria?» sí contiene escenografías en `theatre.stageBackdrops` y asignaciones de escena/intervención en `stageBackdropAssignments`. La gramática exportada diferencia `mapa_espacial` de `fondo_escenario` y de los cambios de `fondo` por intervención. No hace falta convertir la imagen cuadriculada en escenografía.

Los radios actuales de TheatreSpatialActionMapPanel sólo eligen el acompañamiento del mapa. TheatreFullscreenMapView añade además personajes presentes desde `characterLocations`, lo que explica que pueda mostrar más participantes de los deseados. El nuevo modo necesita una composición y una selección de participantes propias.

## Opción y flujo

Añadir al mismo grupo el radio «Fotos de personajes y escenografía». Mantener los tres modos existentes. Llamar al grupo «Vista» y cambiar el botón a «Ver en pantalla completa», porque el nuevo modo no muestra un mapa. El grupo se distribuye en varias filas si falta anchura, sin truncar etiquetas.

Seleccionar una intervención o reproducir actualiza juntos texto, hablante, destinatarios y fondo. La selección del modo se guarda por proyecto, se restaura al abrir y se comparte con pantalla completa y exportación de vídeo teatral. Los proyectos antiguos conservan su comportamiento.

## Composición visual

1. Cabecera compacta: escena y controles de reproducción, fuera de la imagen y del parlamento.
2. Centro: una gran imagen de escenografía, con proporción original y sin deformaciones. Ajuste inicial «contener»; bandas neutras cuando la relación de aspecto no coincide.
3. Sobre esa imagen: fila horizontal centrada de personajes, alineados por su base en el tercio inferior. No son tarjetas situadas por encima del escenario ni una columna lateral. Altura inicial máxima del personaje: 40 % de la imagen visible; anchura máxima individual: 24 %. Reducir proporcionalmente para que quepan sin solaparse; respetar pies y cabeza. Los valores se verificarán con imágenes reales antes de fijarlos.
4. Debajo: franja independiente «Intervención · Nombre», con texto claro sobre fondo oscuro uniforme. Texto alineado a la izquierda, tamaño cómodo (24–32 px en pantalla completa como referencia, adaptado a escala), interlineado generoso. Reservar altura; los retratos y controles nunca tapan el texto. Para parlamentos extensos, el panel permite desplazamiento y el vídeo pagina el texto por bloques legibles, sin puntos suspensivos que eliminen contenido.

Los nombres aparecen en etiquetas discretas bajo cada figura. El hablante se distingue con «Habla» y el destinatario con «Se dirige a»; no depender sólo del color. En panel estrecho se conserva la fila y se ajusta su escala; el texto permanece debajo y accesible con teclado. No introducir recorte ni salto de disposición durante una misma intervención.

## Participantes e imágenes

- Mostrar al hablante y los destinatarios declarados de la intervención, sin incorporar automáticamente todo `characterLocations`. Eliminar duplicados y conservar el orden declarado.
- No inferir referencias a personajes mediante el texto hablado. `interaccion` expresa de forma determinista a quién se dirige o se refiere. Si se necesita distinguir ambos conceptos, extender el contrato con un campo explícito y versionado.
- Coros: respetar miembros explícitos y reducir las figuras en la fila; no inventar miembros de «GRUPO», «PUEBLO» o «PÚBLICO». Esos colectivos se muestran como una etiqueta compacta cuando no tienen identidad visual propia.
- Resolver imagen explícita de la intervención para el hablante, luego imagen del personaje para esa escena, después imagen general del personaje. Los destinatarios usan su asignación de escena/general.
- Si falta imagen de un personaje concreto: recuadro negro de las mismas dimensiones reservadas, texto «Imagen sin asignar» y nombre. Si el archivo está roto: «Imagen no disponible», con diagnóstico fuera de la reproducción.
- Conservar el canal alfa del PNG. Dibujar la imagen directamente sobre la escenografía, sin relleno blanco/negro detrás, sin cuadrícula de transparencia y sin convertir antes a JPEG. Las zonas semitransparentes deben mezclarse correctamente. Un blanco opaco de origen permanece blanco; el programa no recorta fondos automáticamente.

## Fondos y contrato del proyecto

Reutilizar TheatreStageBackdropResolver. Antes de conectarlo al nuevo modo, probar y aclarar la precedencia en cambios de escena: fondo explícito de intervención, herencia válida dentro de escena y fondo de escena. Un borrado explícito se respeta. Actualmente el resolver busca la última asignación por secuencia sin limitarla a la escena, por lo que hay que verificar que un fondo anterior no eclipse el de la nueva escena.

Si falta escenografía, mostrar superficie neutra con «Escenografía sin asignar» y acceso de edición a la escena. No sustituirla silenciosamente por el mapa cuadriculado.

Revisar la carpeta oficial de Patria: existencia de archivos, fondos por escena, imágenes por personaje/escena y destinatarios colectivos. Mantener el repositorio humano como referencia externa. Cualquier corrección se hará en una copia oficial reproducible, con rutas relativas, sin introducir reglas o nombres de la obra en el producto. Extender el manifiesto con la preferencia de presentación documentada, sin meterla en el texto narrable.

## Implementación prevista

1. Definir el nuevo valor de modo en TheatreMapCompanionMode y normalización en TheatreStageGeometry; persistencia por proyecto y contrato de importación/exportación.
2. Extraer una proyección compartida de fondo, participantes e intervención para panel, pantalla completa y BuildTheatreSpatialVideoPlanUseCase. Evitar tres reglas distintas de selección de imágenes.
3. Crear el compositor de escenografía con superposición alfa y el bloque independiente de parlamento. Integrarlo en TheatreSpatialActionMapPanel y TheatreFullscreenMapView.
4. Aplicar la misma composición al vídeo teatral, manteniendo transparencia hasta la composición final sobre el fondo.
5. Validar/corregir el ejemplo oficial, guardar una nueva copia y comprobarla en la aplicación.

## Criterios de aceptación

- Escenografía grande central; hablante y destinatarios en horizontal sobre ella; texto completo y legible fuera de la imagen.
- Un PNG transparente y otro semitransparente dejan ver el fondo en panel, pantalla completa y un fotograma exportado.
- Imagen ausente, archivo roto, destinatario colectivo, autorreferencia y coro tienen comportamiento definido.
- Saltar directamente entre intervenciones produce el mismo fondo y participantes que reproducir secuencialmente; cambiar de escena y borrar fondo no arrastra un fondo incorrecto.
- Guardar/reabrir y exportar/reimportar preserva el modo y las asignaciones.
- Los modos existentes siguen mostrando el mapa como antes. Comprobar escala de Windows 100 %/125 %/150 %, ventana estrecha, texto largo, teclado y contraste de etiquetas/texto. No se considera accesibilidad verificada hasta probar la implementación.
