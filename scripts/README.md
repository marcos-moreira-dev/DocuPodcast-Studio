# Scripts publicos de DocuPodcast Studio

Solo existen siete entrypoints publicos. Todos son envoltorios `.bat` aptos para doble clic y rutas con espacios; la logica compartida vive en `lib/DocuPodcast.Tasks.ps1`.

| Script | Funcion |
|---|---|
| `00-diagnosticar.bat` | Comprueba Java 21, Maven, reactor, roots y auxiliares obligatorios. |
| `01-ejecutar-app.bat` | Compila y abre el launcher productivo. `--smoke` muestra el shell y termina con codigo 0. |
| `02-ejecutar-tests.bat` | Ejecuta la suite determinista normal. |
| `03-verificar-completo.bat` | Ejecuta `verify`, GUI headless y smoke del launcher. Paridad solo con `--reference-root=<ruta>` explicito. |
| `04-smoke-capacidades-reales.bat` | Certifica runtimes detectados; `--required=piper,xtts,ffmpeg,image,video,ocr` permite fijarlos. |
| `05-generar-app-image.bat` | Genera una app-image ligera con Java y auxiliares pequenos, sin modelos pesados. |
| `06-exportar-diagnosticos.bat` | Produce diagnostico y ZIP local bajo `target/diagnostics`. |

Los tres scripts de `scripts/tts` no son entrypoints publicos: son auxiliares internos invocados por los adaptadores Piper/XTTS. Configuracion es la superficie para instalar, importar, reparar y probar motores.

Todos los accesos admiten `--help` sin ejecutar trabajo y rechazan opciones desconocidas. Requisitos y recorrido minimo: [guia de desarrollo](../docs/development/build-test.md).

`00-diagnosticar.bat` solo inspecciona por defecto; `--resolve-native` autoriza resolver el auxiliar nativo. `06-exportar-diagnosticos.bat` produce evidencia incluso ante fallos y admite esa opcion y `--reference-root=<ruta>`. Ningun acceso busca una copia hermana automaticamente. Las generaciones reales son optativas, no parte del onboarding minimo.

## Mantenimiento del workspace

`maintenance\clean-workspace.ps1` funciona en seco por defecto. Sin opciones enumera
solo derivados del compilador; `-Moderate` añade distribuciones antiguas y resultados
regenerables de pruebas, diagnósticos y experimentos que ya fueron revisados. Para
eliminar exige combinar `-Apply -ReferencesReviewed`, rechaza rutas versionadas,
enlaces o junctions y procesos activos, y registra cada objetivo bajo `target\cleanup`.
No incluye modelos, runtimes, ejemplos, voces, medios de usuario ni carpetas mixtas.

El smoke real de FFmpeg usa el flujo documental completo: crea diapositivas con imagen y texto,
incorpora audio WAV, publica el MP4 de forma atómica y lo valida con FFprobe. El smoke unitario
de XTTS resuelve una muestra oficial desde `installationRoot/samples/voices`; no depende del
directorio de trabajo del launcher.

## Roots

La precedencia es propiedad Java, variable de entorno y deteccion del layout:

- `docupodcast.app.root` / `DOCUPODCAST_APP_ROOT` para binarios y auxiliares pequenos.
- `docupodcast.runtime.root` / `DOCUPODCAST_RUNTIME_ROOT` para modelos, herramientas, staging y diagnosticos escribibles.

En desarrollo ambos apuntan al repositorio. En una app empaquetada el runtime predeterminado es `%LOCALAPPDATA%\DocuPodcastStudio\runtime`.

## Distribucion

La app-image no copia `models`, `tools`, `runtime` ni `voice-library`. Un MSI ligero podra envolverse en una tanda futura; no se genera ahora. Los activos estaticos se descargan o importan una sola vez desde Configuracion.
