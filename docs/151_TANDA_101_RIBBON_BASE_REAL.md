# 151 - Tanda 101 - Ribbon base real

T101 reemplaza la toolbar superior transicional por un ribbon real con pestanas. La tanda se basa en T100 verde y no toca el cerebro del sistema.

## Archivos principales

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/RibbonButton.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/AppStyles.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/GuiComponentCatalog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java`
- `src/main/java/module-info.java`
- `src/main/resources/css/components/ribbon.css`
- `src/main/resources/css/docupodcast-light.css`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonBaseT101SourceTest.java`

## Resultado

La parte superior del shell queda compuesta por:

```text
MenuBar
RibbonView
```

`MainToolbarView` permanece como clase legacy/puente para no mezclar cambios masivos, pero ya no es la superficie montada por el shell.

## Pestanas

- Inicio
- Lectura
- Storyboard
- Vista
- Exportar

## Reglas

- Toda accion visible del ribbon despacha `AppCommandId`.
- Ninguna pestana abre `Audio Jobs`, `Storyboard` o `Guion` como workspace.
- La accion principal de escuchar existe en ribbon como comando util, pero su dueno visual final seguira siendo la playbar flotante de T105.
- No se introducen dependencias de Whisper/STT en la GUI visible.

## Validacion

Validacion estatica en entorno ChatGPT:

- Revision fuente del shell y nuevo ribbon.
- Tests fuente focales con stubs JUnit.
- ZIP integro.

Validacion local recomendada:

```bat
scripts\99-diagnostico-completo.bat
```
