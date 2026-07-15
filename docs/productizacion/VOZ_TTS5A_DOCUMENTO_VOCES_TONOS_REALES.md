# VOZ-TTS5A — Documento muestra voces y tonos reales

## Motivo

Después de cerrar la navegación sobria de Vista Voces, el siguiente riesgo funcional estaba en Documento: el panel de Audio podía mostrar el catálogo completo de tonos aunque la voz seleccionada no tuviera esas muestras registradas. Eso generaba una promesa falsa: el usuario podía elegir una emoción que el motor avanzado no tenía como referencia real.

## Regla de producto cerrada

Documento no es un editor de promesas. En la vista Documento solo deben aparecer voces y tonos que el motor activo pueda usar con honestidad.

- Con **Voz IA avanzada**, una voz solo aparece si tiene **muestra Neutral** registrada en la biblioteca de voces.
- Al elegir una voz avanzada, el ComboBox de tono solo muestra los **tonos registrados** para esa voz.
- La muestra Neutral queda primero cuando está disponible.
- Si una voz no tiene Neutral, Documento no la ofrece para asignación TTS avanzada.
- Con **Voz local simple**, Documento no muestra emociones, tonos expresivos ni voces por muestra humana.
- El **Modo de prueba** sigue siendo diagnóstico; no promete voz real.

## Cambios técnicos

Archivo principal:

```text
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java
```

Cambios:

- Se elimina el uso de `VoiceReferenceTone.values()` en Documento.
- Se agrega `usableInDocumentForEngine(...)` para filtrar voces por motor activo.
- En Voz IA avanzada, `usableInDocumentForEngine(...)` exige `referenceSampleSetByVoiceId(...).hasNeutral()`.
- Se agrega `registeredDocumentTones(...)` para leer los tonos desde `VoiceReferenceSampleSet.registeredTones()`.
- Se agrega `neutralFirst(...)` para ordenar Neutral al inicio sin mostrar el catálogo global.
- El panel muestra aviso humano cuando no hay voces avanzadas con Neutral.
- La Voz local simple conserva una explicación clara de que no tiene emociones ni muestras humanas.

## Guardarraíl

Nuevo test fuente:

```text
src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/VoiceTts5ADocumentRealVoiceToneSourceTest.java
```

Protege que:

- Documento no vuelva a usar `VoiceReferenceTone.values()`.
- Documento filtre voces avanzadas por muestra Neutral.
- Documento lea tonos desde `registeredTones()`.
- La Voz local simple siga sin controles expresivos.
- La documentación vigente registre la tanda.

## Validación recomendada en Windows

```bat
scripts\99-diagnostico-completo.bat
```

Smoke manual sugerido:

1. Abrir un proyecto con Voz IA avanzada activa.
2. Verificar que Documento no muestre voces avanzadas sin muestra Neutral.
3. Registrar/importar una voz con Neutral en Vista Voces.
4. Volver a Documento y confirmar que esa voz sí aparece.
5. Agregar tonos adicionales a esa voz y confirmar que solo esos tonos aparecen.
6. Cambiar a Voz local simple y confirmar que desaparecen los controles de emoción/tono.

## Fuera de alcance

Esta tanda no genera audio real con cada tono. Solo corrige la honestidad de selección en Documento. La generación TTS con voz + tono real queda para `VOZ-TTS5B`.
