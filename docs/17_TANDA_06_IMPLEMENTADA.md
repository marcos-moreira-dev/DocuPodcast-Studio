# Tanda 6 implementada

Se implementó el guion narrable como artefacto estructurado entre Documento y Audio.

Nueva cadena:

```text
DOCX → ReadableDocument → ReadingProfile → NarrationScriptDocument → script/narration-script.json
```

Validación local recomendada:

```bat
scripts\00-verificar-entorno.bat
scripts\03-verificar-toolchain.bat
scripts\02-ejecutar-tests.bat
scripts\01-ejecutar-app.bat
```
