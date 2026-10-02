# Auditoría de PNG y referencias emocionales — 22 de septiembre de 2026

## Proyecto revisado

`C:/Users/MARCOS MOREIRA/Downloads/PATRIA DOCUPODCAST VALIDADO/carpeta-11608597433774934950/proyecto/obra.docupodcast.json`

Se verificaron 24 imágenes PNG de personajes. Diez referencias del catálogo todavía apuntaban a JPG. Se corrigieron las rutas, MIME, nombre y huellas de contenido, junto con las referencias internas del paquete teatral. No se convirtieron ni retocaron imágenes. Copia previa: `../png-repair-backup-20260922-153620`, respecto al directorio `proyecto`.

El proyecto queda configurado en `theatre.presentationMode=scenery`. El compositor conserva el canal alfa, coloca al hablante y sus destinatarios horizontalmente sobre el fondo y reserva una banda para el parlamento. Los personajes concretos sin imagen tienen un recuadro negro; destinatarios colectivos sin ficha, como GRUPO, se identifican mediante una etiqueta. El modo se conserva al exportar/importar la carpeta oficial.

## Hallazgos de audio

- Las 557 intervenciones tienen el tono vacío. No hay asignaciones de capa EMOTION; las 550 asignaciones existentes son IMAGE.
- Hay 27 muestras ANGRY registradas y sus archivos existen. Los perfiles de voz de los personajes están asignados.
- La importación utilizaba `enum.valueOf` y convertía valores españoles como `enojado` silenciosamente en NEUTRAL. Se corrigió para reconocer nombres del catálogo, etiquetas españolas y referencias TONE/STY. Un tono desconocido ahora produce un error explícito.
- El parser admite `tono`, `emocion`, `emoción` y `emotion`. Al importar una obra sin tonos se avisa al usuario.
- La selección de muestra conserva el perfil de voz del personaje y busca el tono solicitado dentro de ese perfil. La huella de generación incluye la referencia elegida, evitando reutilizar audio neutral cuando cambia el tono.
- Piper utiliza voces nativas y no estas muestras de referencia. La ruta de voz avanzada pasa la muestra elegida a motores con referencia, como XTTS/Qwen. Esta auditoría no acredita cuál fue el motor utilizado en los audios antiguos ni certifica perceptualmente su actuación.

Ejemplo de metadato operativo, fuera del texto narrable:

```markdown
ELOY: ¡Eso no lo voy a permitir!
> tono=enojado
```

No se asignaron emociones por interpretación automática del diálogo ni se regeneraron los audios de la obra. Para obtener actuaciones emocionales hace falta declarar los tonos pertinentes y volver a generar con un motor compatible con referencias.

## Validación y límites

Compilación e instalación Maven correctas: 59 pruebas seleccionadas, cero fallos y un caso omitido por su condición de entorno. Registro: `target/scenery-png-emotion-final.log`. Carpeta oficial regenerada: `C:/Users/MARCOS MOREIRA/Downloads/PATRIA ESCENOGRAFIA PNG FINAL/carpeta-13174457334870648638/obra`; proyecto de ida y vuelta en el directorio hermano `proyecto`.

Las pruebas cubren selección de la muestra enojada de la misma voz, alias de tono, errores tipográficos, composición alfa completa y parcial, ausencia de mapa cuadriculado en modo escenografía, fondos entre escenas y partición del texto largo sin pérdidas. La obra real se exporta, importa y reabre conservando sus 557 parlamentos.

El escritor de gramática serializa el tono del estado teatral. La paridad de exportación de emociones asignadas únicamente mediante capas manuales EMOTION requiere una auditoría adicional; no se afirma cobertura de ese caso aquí.

El resultado gráfico fue inspeccionado mediante `escenografia-preview.png` y en la aplicación actualizada: proyecto original abierto, opción restaurada, primera intervención seleccionada y pantalla completa con NARRADOR transparente sobre la plaza y texto legible. El registro de apertura identifica Qwen (`qwen3-tts-local`) y 834 unidades acústicas con tono NEUTRAL; no se inició síntesis.

La apertura del proyecto original detectó además un cambio previo de contenido en la imagen de utilería `THEATRE-IMG-039` (`assets/objetos/IMG-039.png`): su checksum no coincide. No pertenece a las imágenes de personajes reparadas. No se aceptó silenciosamente ese reemplazo ni se alteró el archivo. El paquete regenerado calcula las huellas de los archivos que exporta.

La calidad artística final de voces y la revisión de todos los tamaños de ventana no están certificadas por estas pruebas automatizadas.
