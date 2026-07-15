# VOZ-UX4R-3 — Gestionar voces real

## Objetivo

La vista Voces deja preparado el módulo **Gestionar voces** como una microaplicación administrativa sobria para crear, editar, eliminar, importar, grabar, reemplazar y exportar muestras por emoción.

## Reglas cerradas

- Las filas deben ser sobrias: nombre, estado, emociones registradas y acciones justas.
- Una voz es una entidad única: `Pepito`; sus variantes son emociones/muestras, no voces separadas.
- Neutral es necesaria para que una voz aparezca como usable en Documento.
- La lista de emociones puede crecer: Neutral, Feliz, Triste, Enojada, Misteriosa, Suave, Enérgica, Tensa, Melancólica, Heroica, Dramática, etc.
- En Gestionar voces se puede seleccionar una voz existente o crear una nueva escribiendo su nombre.
- Al seleccionar una emoción, importar o grabar reemplaza la muestra de esa emoción.
- Eliminar una voz muestra confirmación y avisa que se eliminarán sus archivos de audio/muestras asociadas.
- Exportar muestras copia las muestras de la voz seleccionada a una carpeta elegida por el usuario.
- Voz local simple mantiene una experiencia mínima; las emociones y muestras por referencia pertenecen a Voz IA avanzada.
- Documento, en una tanda posterior, solo debe mostrar voces con Neutral y emociones realmente registradas para esa voz.

## Alcance implementado

- `Gestionar voces` muestra selector de voz registrada y campo de nombre.
- Acciones: Nueva voz, Guardar voz, Eliminar voz, Exportar muestras.
- Importar/grabar muestra opera sobre la voz seleccionada, no sobre un placeholder fijo.
- Stop de grabación registra la muestra sobre la voz/emoción activa.
- Se agrega coordinación de creación/eliminación en `VoiceProfileAdministrationCoordinator`.
- Se agrega `VoiceLibrary.withoutVoice(...)`.

## Pendiente inmediato

- VOZ-TTS5 debe conectar Documento con voces/emociones registradas.
- MOTOR-SMOKE4R / COQUI-DL1 debe resolver descarga/verificación real de Voz IA avanzada.
