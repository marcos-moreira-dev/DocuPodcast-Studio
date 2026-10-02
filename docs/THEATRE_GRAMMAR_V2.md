# Gramática teatral v2

La gramática es una serialización humana de `TheatreProjectLayer`, no un modelo paralelo. El encabezado obligatorio es `DocuPodcast Teatro Grammar v2` y `grammarVersion: theatre-v2`.

Perfiles aceptan `id`, nombre visible, `aliases`, `voz`, `tono`, `nota` e imágenes opcionales. Actos y escenas usan encabezados Markdown con ID estable opcional (`## Acto: Nombre | id=ACT-ID`, `### Escena: Nombre | id=SCN-ID`). Cada línea `PERSONAJE: texto` crea una intervención y la metadata `>` siguiente define su delta.

`id=INTERVENCION-N` es la identidad canónica persistente que enlaza estado, frame, audio y metadata. `Bxxxx` identifica bloque fuente y `SEG-xxxx` segmento de narración; no sustituyen el ID teatral.

El modo v2 es estricto para propiedades de intervención desconocidas. Las nueve zonas se validan con `TheatreStageZone`. “Fuera de escena” se representa mediante ausencia, nunca con coordenadas falsas.

La lista completa y un ejemplo válido están en `plantilla-gramatica-teatral.md`.
