# Scripts vigentes — DocuPodcast Studio

Todos los scripts resuelven automaticamente la raiz del repositorio, por lo que pueden ejecutarse desde `scripts\` o desde la raiz.

## Desarrollo local

- `00-verificar-entorno.bat`: valida Java, Maven y Maven Toolchain.
- `01-ejecutar-app.bat`: ejecuta la app JavaFX.
- `02-ejecutar-tests.bat`: ejecuta tests y muestra `Tests OK` si pasan.
- `03-verificar-toolchain.bat`: inspecciona toolchains y valida Temurin 21.
- `04-verificar-tts-config.bat`: muestra configuracion TTS local o confirma uso de mock.

## Cierre / release candidate

- `13-revalidacion-local-completa.bat`: entorno + toolchain + TTS + `mvn clean test package`.
- `14-app-image-completa.bat`: genera app-image con `jpackage`.
- `15-msi-completo.bat`: genera MSI con `jpackage` si el entorno Windows tiene lo necesario.
- `16-release-candidate.bat`: revalidacion + app-image + manifest de RC.
- `17-smoke-exploratorio-minimo.bat`: muestra rutas del smoke T58C y puede abrir la app para la prueba manual.
- `18-smoke-automatico-cerebro.bat`: ejecuta solo el smoke automático del cerebro sin JavaFX y genera evidencia en `target/docupodcast-smoke`.
- `19-smoke-motores-reales.bat`: smoke opt-in de Coqui/XTTS, Piper y FFmpeg reales; genera evidencia en `target/docupodcast-real-engines-smoke`.
- `20-preparar-python-portable-coqui.bat`: descarga Python NuGet repo-local, crea `tools\xtts-wrapper\.venv` e instala dependencias XTTS.
- `21-probar-coqui-xtts.bat`: genera un WAV corto con Coqui/XTTS usando la voz por defecto.
- `22-verificar-coqui-xtts-local.bat`: verifica Python local, paquete TTS, modelo XTTS y voz de referencia.
- `37-smoke-cuda-xtts.bat`: prueba si el Python local de Voz IA avanzada tiene PyTorch con CUDA disponible.
- `31-generar-javadoc.bat`: genera JavaDoc en `target/site/apidocs`.


## Diagnóstico unificado

- `99-diagnostico-completo.bat`: ejecuta una pasada amplia sin detenerse en el primer fallo. Revisa Java/Maven, entorno, toolchain, TTS config, compilación Maven, tests, smoke automático del cerebro, preflight de motores y verificación local Piper/FFmpeg. Genera logs y resumen en `target\diagnostico-completo\<fecha>`.
- Para incluir smokes de motores reales, ejecuta `scripts\99-diagnostico-completo.bat --real-engines` o define `DOCUPODCAST_RUN_REAL_ENGINES=1`.

## Nota sobre Maven runtime

`mvn -version` puede mostrar un JDK distinto, pero el build debe usar Java 21 Temurin mediante Maven Toolchains.

## Robustez con rutas de Windows

Los `.bat` publicos soportan rutas con espacios y paréntesis, por ejemplo carpetas extraídas como `DocuPodcast-Studio-tanda16B-v1(1)`. Para evitar errores de `cmd.exe` en bloques `if (...)`, los scripts usan `EnableDelayedExpansion` y `!SCRIPT_DIR!` en mensajes internos de error.

## T90C–T90E — runtime local y preflight de arranque

- `20-preparar-python-portable-coqui.bat`: prepara Python repo-local y venv XTTS.
- `21-probar-coqui-xtts.bat`: genera WAV de prueba con Coqui/XTTS.
- `22-verificar-coqui-xtts-local.bat`: verifica runtime/modelo/voz local.
- `23-preflight-arranque-motores.bat`: verifica requisitos de arranque para Coqui/Piper/FFmpeg y genera reporte.

Coqui/XTTS no usa Python global ni PATH. GPU para Voz IA avanzada solo se considera usable si `scripts\37-smoke-cuda-xtts.bat` o la prueba de Configuración confirman CUDA dentro del venv local.

## T90F/T90G — Motores locales y smoke modular

```bat
scripts\24-verificar-piper-ffmpeg-local.bat
scripts\25-smoke-coqui.bat
scripts\26-smoke-piper.bat
scripts\27-smoke-ffmpeg.bat
scripts\28-smoke-motores-producto.bat
```

Todos los motores del producto deben resolverse desde carpetas locales del repo/app. No se usa PATH ni instalaciones globales como fallback de producto.

## TP3 — Runtime layout

- `29-verificar-runtime-layout.bat`: valida la estructura `tools/`, `models/`, `scripts/tts` y wrappers locales preparada para producto portable/instalable. Genera `target/runtime-layout/TP3_RUNTIME_LAYOUT_REPORT.md`.

## Productización TP4-TP6

- `30-generar-manifest-terceros.bat`: genera `target/legal/THIRD_PARTY_MANIFEST.md` y copia evidencia a `dist/legal`.
- `32-preparar-app-portable-layout.bat`: construye `dist/portable/DocuPodcastStudio` desde el app-image y agrega runtime/legal cuando existen.
- `33-smoke-rc-instalable.bat`: escribe `dist/release-candidate/TP6_RC_SMOKE_REPORT.md` con gates de RC.
- `34-smoke-app-portable-runtime.bat`: valida app-image, carpeta portable, launcher y `DOCUPODCAST_APP_ROOT` tras jpackage.
- `35-auditar-artefactos-motores.bat`: genera `target/legal/ENGINE_ARTIFACTS_AUDIT.md` con presencia y SHA-256 de artefactos locales.
- `16-release-candidate.bat`: encadena revalidación, runtime layout, manifest legal, app-image, portable, smoke RC, smoke portable PF4B y auditoría de motores PF5A.

- `packaging/windows/docupodcast-icon.ico` y `src/main/resources/branding/`: icono de producto derivado de la imagen IA aprobada, usado por Stage, app-image, MSI y portable.

- `36-smoke-gui-asistido.bat`: genera el checklist técnico PF6B y recuerda que la validación real se completa desde Configuración dentro de la app.

## Smoke CUDA Voz IA avanzada

`scripts\37-smoke-cuda-xtts.bat` verifica si el Python autocontenido puede importar PyTorch y usar CUDA. La app usa el mismo criterio desde Configuración > Rendimiento / dispositivo. Tras `MOTOR-GPU-SMOKE1-HF2`, el probe de la app se ejecuta como `.py` temporal para evitar errores de comillas en Windows.

