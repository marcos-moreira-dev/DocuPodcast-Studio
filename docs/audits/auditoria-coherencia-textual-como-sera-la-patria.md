# Auditoría de coherencia textual — ¿Cómo será la patria?

Fecha: 23 de septiembre de 2026

Estado de corrección: aplicada el 23 de septiembre de 2026.

## Fuentes comparadas

- Documento humano: `COMO_SERA_LA_PATRIA_V2_OFICIAL_ILUSTRADO_FINAL_PORTADA_CORREGIDA.docx`.
- Fuente teatral canónica: `obra/obra.teatro.md`.
- Fuente copiada dentro del proyecto: `proyecto/source/obra.teatro.md`.
- Documento importado: `proyecto/document/document.json`.
- Guion de narración preparado: `proyecto/script/narration-script.json`.
- Proyecto activo: `proyecto/obra.docupodcast.json`.

La comparación normalizó únicamente espacios, identificadores de personajes y las transformaciones explícitas del formato teatral: retirada de paréntesis exteriores en acotaciones y separación de los emojis de interpretación del texto hablado. No se ignoraron palabras, signos de puntuación internos ni diferencias de orden.

## Resultado principal

El cuerpo dramático coincide exactamente.

| Comprobación | DOCX | Markdown | Proyecto importado | Resultado |
|---|---:|---:|---:|---|
| Parlamentos | 557 | 557 | 557 | Coincidencia exacta |
| Acotaciones | 196 | 196 | 196 | Coincidencia exacta |
| Total de unidades teatrales | 753 | 753 | 753 | Coincidencia exacta |
| Personaje/tipo y orden | 753 | 753 | 753 | 0 diferencias |
| Texto hablado o de acotación | 753 | 753 | 753 | 0 diferencias |
| Bloques de `document.json` | — | 753 | 753 | 0 ausencias; 0 diferencias |
| Segmentos de `narration-script.json` | — | 753 | 753 | 0 ausencias; 0 diferencias |

Por tanto, las frases que dicen los personajes y el texto de las acotaciones no fueron resumidos, reescritos ni truncados durante la importación.

## Por qué parecen distintos en la interfaz

Las 557 intervenciones del DOCX llevan emojis de interpretación después del nombre del personaje. Esos emojis no forman parte del texto que debe pronunciar la voz. DocuPodcast los retiró del texto visible y creó una asignación emocional independiente para cada intervención. El proyecto contiene 753 capas de emoción: 557 para parlamentos y 196 acotaciones neutrales.

Las 196 acotaciones aparecen entre paréntesis en el DOCX. En DocuPodcast se guardan sin los paréntesis y con `ACOTACIÓN` como tipo estructural. El contenido interior coincide exactamente.

Los nombres humanos también se convierten en identificadores estables. Por ejemplo, `Eloy / Soldado de Tarqui` se guarda como `ELOY`, y `Oveja asustada pero digna` como `OVEJA_ASUSTADA`. El parlamento no cambia.

## Brecha real: la canción completa no está configurada

El DOCX contiene, entre los párrafos 817 y 883, la canción completa **“Llegará el día”**: 58 versos y 9 rótulos de sección. Ninguna de esas 67 líneas existe en el Markdown ni en los 753 segmentos activos.

El proyecto conserva únicamente la acotación: “La canción empieza contenida, casi funeraria, y crece hacia una promesa activa. Se canta completa.” Esto no basta para reconstruir, mostrar, cantar ni exportar la letra de forma determinista.

También se excluyen tres rótulos editoriales (`Pareja 1`, `Pareja 2`, `Pareja 3`) y `FIN`. Estos cuatro rótulos no alteran el diálogo, pero podrían conservarse como divisores visuales si se desea reproducir la edición humana con fidelidad completa.

## Brecha estructural: Cuadro IX-B está vacío

En el Markdown, `Cuadro IX-B — Memorial y canción` se declara y, tres líneas después, se abre `Cuadro IX-C — Gags de Florindos`. Como resultado:

- IX-B no contiene intervenciones ni acotaciones.
- Las cinco acotaciones del memorial y la canción (`INTERVENCION-10183` a `INTERVENCION-10187`) quedan asociadas a IX-C.
- IX-B tiene asignado `assets/fondos/IMG-088.png`, pero las acotaciones que deberían usarlo están bajo IX-C, que no declara fondo.

Esta estructura explica que una acotación del memorial pueda mostrar “Escenografía sin asignar” aunque el recurso exista.

## Integridad de copias y codificación

La fuente canónica, `proyecto/source/obra.teatro.md` y el paquete activo `c-mo-ser-la-patria-7e57fff73f7af385` son idénticos byte por byte. El proyecto activo referencia ese paquete.

Existe un segundo paquete, `c-mo-ser-la-patria-8b318c2153c2d1f8`, con diferencias de metadatos y recursos. Sus 753 textos teatrales son iguales y el proyecto activo no lo referencia; es un artefacto anterior que puede inducir a confusión durante una inspección manual.

Los campos de texto dramático de `document.json` y `narration-script.json` no contienen caracteres de reemplazo ni mojibake. Sí hay codificación dañada en metadatos auxiliares del proyecto —por ejemplo, `Documento acadÃ©mico Word` y notas de las capas emocionales—. No cambia lo pronunciado, pero puede producir títulos o descripciones defectuosos en la interfaz.

## Dictamen

El supuesto desfase entre parlamentos/acotaciones del DOCX y DocuPodcast no existe en las 753 unidades teatrales importadas. La brecha material está concentrada en la secuencia musical final y en la frontera entre IX-B e IX-C. La corrección adecuada consiste en incorporar la canción como contenido teatral determinista y mover `INTERVENCION-10183` a `INTERVENCION-10187` dentro de IX-B, conservando IX-C desde la acotación que abre los focos por parejas.

## Corrección aplicada

- La canción quedó incorporada como nueve intervenciones de `OVEJA_CANTORA`, una por sección musical (`INTERVENCION-558` a `INTERVENCION-566`). Cada estrofa permanece completa como unidad semántica, con tono `SOLEMN`, fondo del memorial y contexto explícito de canción.
- Las acotaciones `INTERVENCION-10183` a `INTERVENCION-10187` ahora pertenecen a IX-B. IX-C comienza en `INTERVENCION-10188`.
- Las cuatro copias de `obra.teatro.md`, `document.json`, `narration-script.json` y `obra.docupodcast.json` quedaron sincronizadas.
- El proyecto activo pasó de 753 a 762 bloques, segmentos, intervenciones, ubicaciones y estados teatrales. Los índices son continuos del 1 al 762.
- Se reparó la codificación dañada de los metadatos activos; la verificación no encuentra `Ã`, `Â`, secuencias de emoji mal decodificadas ni caracteres de reemplazo.
- El lector y validador nativos de DocuPodcast aceptan el proyecto: `valid=true`, sin mensajes de error.
