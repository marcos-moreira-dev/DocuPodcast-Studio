# Fondos personalizados en lotes Express

La configuración común conserva resolución, tipografía, efectos y ajuste de imagen. El punto 2 indica que franjas, recorte/zoom y fondo difuminado se aplican a imágenes primarias, secundarias y globales. Los fondos y las imágenes de contenido emplean DocumentBackgroundImagePainter; el logo/mascota conserva su tratamiento como superposición.

DocumentVideoBatchProfile.documentBackgrounds asocia rutas relativas de documento a DocumentBackgroundOverride(imagePath, visibility). visibility null hereda el valor común en el momento de renderizar. Sin entrada se hereda todo el fondo común. No se copian tipografía, resolución ni ajuste por documento.

Las filas virtualizadas muestran estado, selección/cambio, visibilidad opcional y quitar personalización. Seleccionar la fila cambia la muestra del fondo. Solo audio conserva las opciones y deshabilita los controles individuales. Cambiar la carpeta fuente limpia las asociaciones de la carpeta anterior; cargar un borrador restaura las suyas después de establecer la carpeta.

Los borradores y proyectos guardan el mapa; proyectos antiguos sin mapa usan configuración común. Crear un lote de video copia los fondos personalizados a assets/background con nombres únicos. Un archivo faltante muestra aviso en la fila y detiene creación/exportación con un mensaje, sin sustituir el fondo silenciosamente.

Pruebas: herencia, porcentaje explícito, cambio del valor común, persistencia del borrador, copia al proyecto, filas con archivo ausente, quitar personalización, conservación en audio y los tres modos del pintor de imágenes.
