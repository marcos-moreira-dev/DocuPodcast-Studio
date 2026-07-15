# T106A — Sidebar contextual ajuste fino

Base: T106.

## Objetivo

Corregir fallos de tests fuente reportados por diagnostico local y aplicar feedback visual sobre el sidebar contextual del Documento antes de avanzar a T107.

## Cambios

- El contenido de los modulos del sidebar izquierdo usa toda la altura disponible; el scroll ya no queda encerrado en media columna con espacio vacio inferior.
- El rail derecho visual tambien deja que su scroll ocupe la altura del panel.
- Fragmento queda como modulo de lectura/seleccion: muestra texto elegido, ubicacion y `Reproducir desde aqui`.
- Fragmento elimina botones redundantes de Voz, Emocion e Imagen; esas acciones viven en sus modulos dedicados.
- Audio mantiene selector de origen.
- En `Voz IA`, la eleccion de voz y estilo pasa a ComboBox.
- Se elimina `Preparar audio` del modulo Audio para no confundir generacion TTS con configuracion de voz humana avanzada.
- Se elimina `Quitar emocion / estilo`; elegir estilo neutral cumple esa funcion sin boton extra.
- En `Audio del computador` se conservan `Elegir audio`, `Extraer audio de video` y `Quitar audio asignado`.
- Imagen conserva `Elegir imagen` y `Quitar imagen`; `Reemplazar imagen` desaparece porque volver a elegir ya reemplaza.
- Imagen agrega vista previa contextual de la imagen asociada a la seleccion.
- `Elegir imagen` importa la imagen al proyecto y la asocia al rango seleccionado.

## Alcance no incluido

- No implementa T107 rail derecho redimensionable.
- No implementa playback por unidad/oracion.
- No implementa clonacion de voz propia.
- No convierte el Documento en editor Word.
