# DOCUMENT-SIDEBAR-VOICE-UX1 — Voz generada vs audio del computador en Documento

## Propósito

Esta tanda pule el módulo **Audio** del sidebar izquierdo de Documento para que el usuario entienda dos caminos distintos y operativos:

1. **Voz generada**: la app genera audio nuevo para el fragmento usando el motor de voz activo y, cuando corresponde, una voz/tono registrados en la biblioteca de voces.
2. **Audio del computador**: el usuario asigna un archivo de audio propio o extrae audio de un video para ese fragmento.

Estos caminos no deben mezclarse conceptualmente. Una muestra como `María · Neutral` o `María · Enojada` es una referencia para sintetizar texto nuevo; **no son clips fijos** que se reproducen siempre igual.

## Reglas de UX fijadas

- El selector principal conserva `Audio del computador` como camino independiente.
- El bloque de voz habla de **Voz generada** y no fuerza al usuario a entender detalles técnicos del motor.
- El campo de voz se etiqueta como `Voz`.
- El campo de emoción/estilo se etiqueta como `Tono`.
- Los ComboBox de tono muestran solo nombres simples: `Neutral`, `Feliz`, `Triste`, `Enojada`, etc.
- No deben aparecer prefijos como `Tonos recomendados · Feliz`, `Catálogo teatral extendido · Dramática` ni códigos internos.
- Cuando se elige una voz, Documento **solo muestra tonos registrados** para esa voz.
- Si `María` tiene `Neutral`, `Feliz` y `Triste`, el ComboBox solo muestra esos tonos; `Sorprendida` no aparece hasta que María tenga una muestra de `Sorprendida`.
- La Voz local simple puede recibir el dispositivo seleccionado, pero no muestra tonos por muestra humana; ese flujo pertenece a Voz IA avanzada.

## Cambios técnicos

- `DocumentAudioNarrationPanel` usa `VoiceToneLabelPolicy.comboLabel(...)` para el ComboBox de tonos.
- El label visible cambia de `Tono de referencia` a `Tono`.
- El panel diferencia `Voz generada` de `Audio del computador` con microcopy breve.
- El botón de limpieza pasa de `Quitar voz/audio asignado` a `Quitar asignación`.
- Se mantiene la lógica existente `registeredDocumentTones(...)` basada en `sampleSet.registeredTones()`.
- Se conserva la escucha de `activeVoiceLibraryProperty()` para refrescar Documento cuando la biblioteca de voces cambia.

## Criterio de salida

En Documento, para un fragmento seleccionado:

- el usuario puede elegir voz generada o audio del computador;
- si elige voz generada, selecciona una voz y, si aplica, un tono simple;
- el tono solo aparece si esa voz tiene muestra registrada;
- el usuario no ve etiquetas largas ni técnicas en ComboBox.
