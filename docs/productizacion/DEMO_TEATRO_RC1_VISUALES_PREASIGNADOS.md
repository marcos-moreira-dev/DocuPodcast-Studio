# DEMO-TEATRO-RC1 — Demo teatral con visuales preasignados

El demo `El vuelo del Tornillo Dorado` mantiene el Word como fuente limpia y copia sus visuales dentro del proyecto. A partir de esta tanda, los visuales se asocian automaticamente solo al cuerpo de la obra: `imagen_01_presentacion_personajes.png` para Personajes y `fragmento_01` a `fragmento_24` para las lineas de Escena 1 a cierre. Titulo, subtitulo y notas no reciben visuales preasignados.

Regla de producto: las imagenes del demo no se incrustan en el Word; se importan como assets del proyecto y se anclan a fragmentos para probar rail visual, playback y exportacion MP4.

Si alguna imagen no puede asociarse por falta de fragmento preparado, la app conserva el proyecto usable y muestra una advertencia operativa para que el usuario complete la asignacion desde el panel Imagen.
