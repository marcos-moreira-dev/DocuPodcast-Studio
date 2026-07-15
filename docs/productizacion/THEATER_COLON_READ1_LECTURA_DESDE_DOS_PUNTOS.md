# THEATER-COLON-READ1 — leer desde después de dos puntos

## Decisión

Documento agrega una opción operativa en el sidebar de Audio: **Leer desde después de ':' (requiere rehacer chunks)**.

Sirve para guiones de teatro o diálogo donde el texto fuente puede venir como:

```text
Vaquero: Voy a conquistar el viejo oeste.
```

Con la opción activa, la proyección interna de audio lee:

```text
Voy a conquistar el viejo oeste.
```

El documento fuente no se modifica. La opción solo afecta la lectura preparada y por eso los chunks de audio deben rehacerse para reflejar el cambio.

## Regla

La app solo omite prefijos breves antes del primer `:` cuando parecen etiquetas de personaje. Textos ambiguos o encabezados largos se conservan.
