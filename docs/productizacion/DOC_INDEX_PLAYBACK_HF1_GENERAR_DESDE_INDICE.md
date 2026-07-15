# DOC-INDEX-PLAYBACK-HF1 — generar/reproducir desde el índice seleccionado

## Motivo

El índice lateral del Documento ya navegaba visualmente al bloque correcto, pero en un caso importante el audio podía volver al inicio:

1. el usuario abría un documento largo;
2. hacía clic en una entrada del índice;
3. todavía no existía guion/proyección o manifest de audio;
4. pulsaba `Escuchar` o `Reproducir`;
5. la app preparaba la lectura, pero al no encontrar audio llamaba a la generación global.

Resultado percibido: la selección visual estaba en el fragmento correcto, pero el primer chunk generado/reproducido podía ser el primero del documento.

## Corrección

`DocuPodcastShellViewModel.listenToDocument()` ahora conserva el bloque elegido en el índice como pivote de reproducción/generación.

Tras construir la proyección de narración, si existe `selectedDocumentBlockId` y se puede resolver el primer segmento narrable asociado, la generación usa:

```java
submitAudioGenerationFromSegment(selectedStart.get(), true)
```

en vez de caer directamente en:

```java
submitAudioGeneration()
```

La generación global queda solo como fallback cuando no existe pivote válido.

## Alcance

La tanda no cambia:

- el render de Documento;
- la generación TTS por motor;
- el manifiesto de playback;
- la semántica de `Generar chunks de audio` para documento completo;
- la selección manual de texto.

Solo corrige el caso `Índice → Escuchar` cuando todavía falta audio/proyección útil.

## Guardarraíl

Se agrega `DocumentIndexPlaybackHf1SourceTest`, que exige que `listenToDocument()` resuelva el bloque seleccionado antes de usar generación global y que la generación desde selección conserve el suffix script.

## Resultado esperado

En un documento largo:

1. abrir Documento;
2. hacer clic en una entrada del índice;
3. pulsar `Escuchar`;
4. si falta audio, el primer chunk preparado debe corresponder a ese bloque/segmento;
5. cuando el chunk esté listo, la reproducción debe iniciar desde ahí.
