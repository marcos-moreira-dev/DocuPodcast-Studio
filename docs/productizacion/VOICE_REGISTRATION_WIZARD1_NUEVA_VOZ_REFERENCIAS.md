# VOICE-REGISTRATION-WIZARD1 — Nueva voz con muestras de referencia

## Propósito

Esta tanda incorpora la subvista **Nueva voz** dentro de Vista Voces. La vista no funciona como dashboard ni como reproductor de clips fijos: su objetivo operativo es registrar una voz humana con muestras de referencia para que **Voz IA avanzada** pueda sintetizar texto nuevo con esa voz y el tono seleccionado.

## Reglas de producto

- La grabación se realiza con Java Sound, no con Python.
- Las muestras de una voz, por ejemplo `María · Neutral` o `María · Enojada`, son referencias para generar texto nuevo con Coqui/XTTS.
- Las muestras no son clips fijos para repetir siempre igual.
- La muestra **Neutral** es obligatoria para terminar una voz avanzada.
- Las emociones adicionales son opcionales.
- Documento solo debe mostrar las emociones que la voz seleccionada realmente tenga registradas.
- Si María solo tiene Neutral, Feliz y Triste, el combo de Documento no debe mostrar Sorprendida.

## Cambios visibles

En **Gestionar voces**, el botón **Nueva voz** abre una subvista operativa con:

1. Nombre de la voz.
2. Selector simple de emoción/tono.
3. Frase guía para actuar la muestra.
4. Acciones de muestra:
   - Importar audio.
   - Grabar emoción.
   - Detener y asignar.
   - Reproducir grabación.
   - Eliminar grabación.
5. Guardar voz con compuerta Neutral.
6. Botón **Volver a gestionar voces**.

## Corrección incluida

La tanda corrige los source tests que todavía esperaban microcopy anterior como **Estado del motor de voz** y actualiza el contrato a los textos vigentes: **Motor activo y alcance**, subvista **Nueva voz** y acciones de grabación/asignación.

## Pendiente consciente

La biblioteca ya separa conceptualmente muestras de referencia y audio de Documento. La consolidación completa de almacenamiento app/runtime y sincronización transversal con Documento queda para **VOICE-LIBRARY-SYNC1**, porque ahí se cerrará el refresco de voces/tonos y la política de biblioteca común.
