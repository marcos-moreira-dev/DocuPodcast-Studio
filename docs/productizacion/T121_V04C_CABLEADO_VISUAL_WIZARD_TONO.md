# T121-V04C — Cableado visual mínimo del wizard por tono

## Objetivo

Conectar la Vista Voces con los planes de wizard ya existentes para que el usuario deje de ver una frase estática y empiece a trabajar por tono de referencia.

Esta tanda no implementa todavía la prueba generada con frase editable ni el rediseño visual final. Prepara la UI mínima para importar o grabar una muestra asociada al tono seleccionado.

## Cambios implementados

- `VoiceLibraryWorkspaceView` usa `VoiceRegistrationWizardPlan` para mostrar el contrato visible de **Voz IA avanzada**.
- La vista usa `VoiceToneRecordingPlan` para obtener frase guía, tono, etiquetas de grabación y contrato de cancelación.
- Se agrega selector de tono con neutral obligatoria y tonos recomendados.
- Se elimina la frase guía estática anterior.
- Importar muestra usa el tono seleccionado.
- Grabar muestra conserva el tono activo hasta detener y registrar.
- La vista muestra conteo/lista de muestras por tono registradas para la voz seleccionada.
- Se mantienen `ActionButtonFactory.primary/secondary` y clases CSS propias de Voces; no se agregan botoneras improvisadas ni `setStyle`.

## Corrección incluida

Se corrige el source test de T121-V04B para validar `referenceSampleSets` sin exigir comillas sin escapar dentro del código Java del writer.

## Contrato UX

- UX normal: **Voz IA avanzada**, **Voz local simple**, **Modo de prueba**.
- No se muestra `Coqui` ni `XTTS` en la Vista Voces.
- Neutral es obligatoria.
- Los tonos recomendados son muestras opcionales.
- Cancelar conserva la muestra anterior.
- Guardar/importar asocia la muestra al tono elegido.

## Tests agregados/actualizados

- `VoiceLibraryWizardWiringT121V04CSourceTest`
- `VoiceToneSamplesPersistenceT121V04BSourceTest` ajustado por literal escapado.

## Validación en entorno ChatGPT

- `javac --release 21` de `domain`, `application` e `infrastructure`.
- Compilación focal de source tests nuevos/modificados con stubs JUnit.
- Revisión estática del cableado de `VoiceLibraryWorkspaceView` y `DocuPodcastShellViewModel`.

Maven completo no se ejecutó porque `mvn` no está instalado en este entorno.
