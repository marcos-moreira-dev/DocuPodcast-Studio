# Hotfix Tanda 34B — Compilación de edición STT de segmentos

Este hotfix corrige un fallo de compilación detectado al ejecutar `scripts\02-ejecutar-tests.bat` sobre la Tanda 34.

## Error corregido

`DocuPodcastShellViewModel` llamaba a:

```java
applicationServices.script().updateSegmentText()
```

pero `ScriptApplicationServices` es un `record` cuyo accessor real se genera desde el componente:

```java
UpdateNarrationSegmentTextUseCase updateNarrationSegmentText
```

Por lo tanto el método correcto es:

```java
applicationServices.script().updateNarrationSegmentText()
```

## Archivos modificados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/stt/SpeechToTextUiSourceTest.java`

## Validación en entorno ChatGPT

No se ejecutó Maven porque el entorno no tiene `mvn`.

Se validó con `javac --release 21` la capa no-JavaFX (`domain`, `application`, `infrastructure`), incluyendo `ScriptApplicationServices` y `UpdateNarrationSegmentTextUseCase`.

La validación completa debe ejecutarse en Windows con:

```bat
scripts\02-ejecutar-tests.bat
scripts\01-ejecutar-app.bat
```
