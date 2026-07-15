# T98D — Contrato del sidebar izquierdo y rail derecho

## Sidebar izquierdo

El sidebar izquierdo es inspector y configurador contextual de la oración seleccionada.

No es menú general. No registra voces nuevas. No reemplaza el Ribbon.

Al seleccionar una oración debe mostrar:

```text
Fragmento seleccionado
  referencia de oración / fragmento
  texto breve
  estado de audio/voz/imagen

Audio / narración
  origen:
    Voz IA
    Audio del computador

  Si Voz IA:
    combobox de voces ya configuradas
    emoción / estilo
    velocidad de esta oración
    generar/regenerar audio de esta oración

  Si Audio del computador:
    elegir archivo de audio
    extraer audio desde video
    quitar audio asignado

Imagen
  elegir imagen
  quitar imagen
  abrir carpeta de la imagen

Capas
  ver capas aplicadas
  quitar capas del fragmento
```

### Voces

El combobox solo muestra voces ya registradas. Crear, grabar o importar voces nuevas pertenece al workspace `Voces`.

### Audio del computador

Se elige con FileChooser. Puede ser voz, música, pájaros, ruido o cualquier clip. La app no lo clasifica.

## Rail derecho

El rail derecho es visual y navegacional.

Muestra tarjetas de storyboard/imagen:

```text
[miniatura de imagen o vacío]
Fragmento / oración
microreferencia del texto
```

Si no hay imagen asignada, la tarjeta se muestra vacía, pero conserva la referencia de la oración.

Al hacer clic en una tarjeta:

- selecciona la oración asociada;
- desplaza el documento hasta ella;
- actualiza sidebar izquierdo;
- mantiene la posición visual estable.

## Posicionamiento estable de oración

La oración seleccionada o en reproducción debe quedar aproximadamente 50 px debajo del Ribbon. El scroll no debe saltar de forma errática.

## Mostrar / ocultar rail

Debe haber un botón flotante en el borde derecho, debajo del Ribbon, inspirado en la simplicidad de Blender.

Estados:

```text
Rail oculto:
  botón flotante visible para mostrarlo.

Rail visible:
  botón interno para minimizarlo.
  borde izquierdo arrastrable para redimensionar.
```

El mismo comando debe poder invocarse desde MenuBar, Ribbon y botón propio del rail.

## Redimensionamiento y reflow

Al mostrar, ocultar o cambiar ancho del rail, el workspace Documento debe reacomodar el texto como texto, no como canvas. La hoja debe adaptar líneas y ancho disponible de forma natural.

## Prohibiciones

- El rail no contiene formularios pesados.
- El rail no registra voces.
- El rail no abre carpetas de imagen; eso pertenece al sidebar izquierdo.
- El rail no debe parecer incrustado dentro de la hoja.
