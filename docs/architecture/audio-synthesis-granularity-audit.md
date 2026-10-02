# Auditoría de granularidad de síntesis y seguimiento

## Decisión

La mejora observada al sintetizar una intervención teatral completa no debe
convertir el párrafo en la unidad universal de DocuPodcast. El núcleo necesita
distinguir tres conceptos que hoy están fusionados:

1. **Segmento semántico**: intervención teatral, acotación o bloque documental.
2. **Toma de síntesis**: texto enviado al motor y WAV físico resultante.
3. **Marca de seguimiento**: rango de texto y tiempo usado para subrayado,
   navegación, subtítulos y cambios visuales.

La política recomendada es:

| Flujo | Toma de síntesis | Marca de seguimiento |
|---|---|---|
| Teatro | Intervención o acotación completa | Oración o suboración alineada dentro del WAV |
| Estudio documental | Oración o suboración | La misma oración o suboración |
| Audio humano importado | Archivo o recorte elegido | Una o varias marcas internas, si existen |

Esto conserva el comportamiento actual de Estudio documental y permite que
Teatro aproveche el contexto prosódico del párrafo.

### Decisión de producto confirmada

Teatro adopta la intervención o acotación completa como unidad física de audio,
tanto para TTS como para una interpretación humana importada. El usuario no
necesita dividir la grabación ni crear marcas temporales.

El seguimiento mínimo resalta toda la intervención mientras su audio está
activo. Las marcas internas por oración quedan como una mejora automática
opcional: su ausencia o una confianza baja nunca bloquean generación,
reproducción ni exportación. Estudio documental conserva la división por
oraciones porque allí la precisión del seguimiento sí es parte central de la
experiencia.

## Hallazgos en el código actual

- `BuildNarrationRenderPlanUseCase` aplica `DocumentSentenceSplitter` a todos los
  segmentos narrables. La decisión transversal ocurre antes de saber qué
  experiencia consumirá el resultado.
- `NarrationRenderUnit` y `RenderUnit` representan simultáneamente el rango de
  texto, la petición de TTS y el medio reproducible.
- `AudioGenerationUnit.id` se convierte en nombre de WAV y clave de cobertura.
  Por eso cambiar la segmentación también invalida caché y reanudación.
- `BuildPlaybackManifestUseCase` crea una `PlaybackCue` por WAV. La cue tiene un
  intervalo global, pero no un intervalo interno del archivo. Varias cues no
  pueden compartir hoy un WAV sin reiniciarlo.
- El seguimiento Word/PDF y `ResolvePdfPlaybackHighlightUseCase` dependen de IDs
  `SEG-...-U001`. Esta granularidad es necesaria para estudiar y no debe
  degradarse.
- `BuildDocumentStudyVideoPlanUseCase` crea frames usando cada unidad de audio y
  su texto reconciliado. Un cambio universal a párrafo reduciría la precisión del
  subrayado y de los recortes documentales.
- `BuildTheatreSpatialVideoPlanUseCase` también itera los WAV por intervención,
  aunque termina mostrando el texto completo. Teatro ya agrupa visualmente lo
  que el audio fragmenta físicamente.
- La cobertura reutilizable compara huellas por unidad. Es apta para tomas
  completas si la agrupación pasa a formar parte de la huella acústica.

## Modelo propuesto

Agregar tipos explícitos, sin reemplazar de inmediato los actuales:

```text
NarrationSegment
  ├─ SynthesisTake (TAKE-SEG-INTERVENCION-17)
  │    ├─ texto completo
  │    ├─ voz, tono y huella acústica
  │    └─ WAV físico
  └─ TrackingCue[]
       ├─ ScriptTextRange
       ├─ startSeconds / endSeconds dentro de la toma
       └─ DocumentTextRange opcional
```

`PlaybackCue` necesita una referencia a la toma y dos tiempos de medio
(`mediaStartSeconds`, `mediaEndSeconds`). El reproductor abre el WAV una vez y el
reloj publica cambios de marca sin detener ni reiniciar el audio. El exportador
inserta el WAV una sola vez y usa las marcas para subtítulos, subrayado o cambios
de frame.

## Política de agrupación teatral

Una intervención puede producir una sola toma cuando todas sus marcas tienen:

- el mismo personaje y la misma voz efectiva;
- el mismo tono o dirección prosódica;
- el mismo idioma y preprocesamiento;
- ausencia de un clip manual que sustituya solo una parte;
- longitud aceptada por el motor.

Debe abrirse una nueva toma al cambiar voz, tono, idioma, motor, audio manual o
cuando se alcance el límite seguro de texto. Las imágenes pueden cambiar dentro
de la toma: son una decisión visual y no deberían forzar una nueva síntesis.

Las acotaciones forman cada una una toma neutral con la voz reservada de
acotaciones. No se mezclan con el parlamento contiguo.

## Alineación interna

El experimento de `SEG-INTERVENCION-17` detectó silencios suficientes para crear
cuatro marcas dentro de un WAV continuo. La detección de silencios sirve como
prototipo, pero producción necesita esta jerarquía:

1. tiempos nativos del motor, si el motor los expone;
2. alineación forzada entre texto y WAV;
3. silencios más ponderación textual como respaldo;
4. una única marca para toda la toma si la confianza es insuficiente.

Cada marca debe guardar método, confianza y versión del alineador. Una alineación
defectuosa nunca invalida el WAV: solo degrada el subrayado a la intervención
completa.

## Compatibilidad y migración

- Los proyectos documentales mantienen `SENTENCE` como política predeterminada.
- Los proyectos teatrales nuevos usan `SEMANTIC_INTERVENTION`.
- Los WAV antiguos `SEG-...-U###` siguen siendo válidos y reproducibles.
- Al regenerar una intervención teatral se crea `TAKE-SEG-....wav`; no se borran
  las unidades antiguas hasta que la nueva toma sea válida.
- La huella de toma incluye textos, límites, voz, tono, motor y versión de
  preprocesamiento. Cambiar una imagen o una marca visual no regenera voz.
- El manifiesto de reproducción acepta temporalmente cues antiguas por WAV y
  cues nuevas agrupadas por toma.

## Secuencia de implementación

1. Introducir `SynthesisTake`, `TrackingCue` y una política de granularidad sin
   alterar la salida actual.
2. Adaptar el generador y la cobertura para persistir WAV por toma.
3. Extender reproducción para publicar marcas internas mientras conserva un
   único reproductor abierto.
4. Conectar el subrayado teatral y la exportación de video a esas marcas.
5. Activar `SEMANTIC_INTERVENTION` solo para Teatro y ejecutar pruebas A/B de
   regresión.
6. Mantener Estudio documental en `SENTENCE`; evaluar párrafos allí únicamente
   como opción explícita y experimental.

## Riesgo principal

El error más peligroso sería agrupar texto antes de separar las capas acústica y
visual. Eso produciría WAV más naturales, pero rompería navegación, selección,
caché, subrayado PDF/Word y sincronización de video. La separación propuesta
permite cambiar la prosodia teatral sin ese costo.
