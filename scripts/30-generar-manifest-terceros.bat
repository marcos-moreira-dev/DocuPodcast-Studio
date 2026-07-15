@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 (
  echo ERROR: No se pudo resolver la raiz del proyecto desde !SCRIPT_DIR!..
  exit /b 1
)

set "REPORT_DIR=target\legal"
set "REPORT=%REPORT_DIR%\THIRD_PARTY_MANIFEST.md"
set "ENGINE_REPORT=%REPORT_DIR%\ENGINE_ARTIFACTS_MANIFEST.md"
if not exist "%REPORT_DIR%" mkdir "%REPORT_DIR%" >nul

(
  echo # DocuPodcast Studio - third-party manifest
  echo.
  echo Este manifiesto no otorga permisos de redistribucion por si solo. Sirve como inventario auditable antes de crear app portable, MSI o RC.
  echo.
  echo ^| Componente ^| Ruta esperada ^| Licencia/estado ^| Redistribucion ^| Accion ^|
  echo ^|---^|---^|---^|---: ^|---^|
  echo ^| FFmpeg ^| `tools/ffmpeg/bin` ^| GPL/LGPL segun build usado ^| Pendiente ^| Confirmar build, licencia, avisos y SHA-256 antes de distribuir. ^|
  echo ^| Piper TTS ^| `tools/piper` ^| MIT/proyecto + licencias de voces ^| Pendiente ^| Verificar binario, voces, licencia y checksums. ^|
  echo ^| Coqui/XTTS ^| `tools/xtts-wrapper`, `models/tts/xtts` ^| Depende del modelo ^| Usuario/preparacion local ^| No asumir redistribucion automatica; requiere manifest de artefactos. ^|
  echo ^| Python portable ^| `tools/xtts-wrapper/.venv` ^| PSF + dependencias ^| Pendiente ^| Incluir avisos y checksum si se empaqueta. ^|
  echo ^| JavaFX/runtime jpackage ^| app-image/MSI ^| GPLv2 with Classpath Exception ^| Incluido por jpackage ^| Conservar avisos de runtime. ^|
  echo ^| Ejemplos internos ^| `src/main/resources/examples` ^| Propio del proyecto ^| Incluido ^| Demos de producto sin internet. ^|
  echo ^| Scripts TTS/preflight ^| `scripts/tts` ^| Propio del proyecto ^| Incluido ^| ASCII-safe para PowerShell 5.1. ^|
  echo.
  echo Resultado: todo binario/modelo externo queda pendiente hasta que exista evidencia concreta en `dist/legal`.
) > "%REPORT%"

(
  echo # DocuPodcast Studio - T119C engine artifacts manifest
  echo.
  echo Este manifiesto documenta los artefactos de motores que deben tener origen, licencia y SHA-256 antes de RC final.
  echo.
  echo ^| Artefacto ^| Ruta esperada ^| Motor ^| RC final ^| Checksum ^|
  echo ^|---^|---^|---^|---: ^|---^|
  echo ^| FFmpeg executable ^| `tools/ffmpeg/bin/ffmpeg.exe` ^| FFmpeg ^| Si ^| Pendiente hasta fijar binario. ^|
  echo ^| FFprobe executable ^| `tools/ffmpeg/bin/ffprobe.exe` ^| FFmpeg ^| Si ^| Pendiente hasta fijar binario. ^|
  echo ^| Python local ^| `tools/xtts-wrapper/.venv/Scripts/python.exe` ^| Coqui/XTTS ^| Si ^| Pendiente hasta preparar runtime. ^|
  echo ^| XTTS wrapper ^| `tools/xtts-wrapper/synthesize_xtts.py` ^| Coqui/XTTS ^| Si ^| Propio, fijar hash antes de RC. ^|
  echo ^| XTTS config ^| `models/tts/xtts/config.json` ^| Coqui/XTTS ^| Si ^| Pendiente segun modelo. ^|
  echo ^| XTTS weights ^| `models/tts/xtts/*.pth` o `*.safetensors` ^| Coqui/XTTS ^| Si ^| Pendiente segun modelo. ^|
  echo ^| XTTS vocab ^| `models/tts/xtts/vocab.json` o `vocab.txt` ^| Coqui/XTTS ^| Si ^| Pendiente segun modelo. ^|
  echo ^| Voz neutral ^| `models/tts/xtts/speakers/voz-por-defecto.wav` ^| Coqui/XTTS ^| Si ^| Fijar hash antes de RC. ^|
  echo ^| Piper executable ^| `tools/piper/piper.exe` ^| Piper ^| No/intermedio ^| Requerido si se distribuye Piper. ^|
  echo ^| Piper voice ^| `models/tts/piper/voices/*.onnx` ^| Piper ^| No/intermedio ^| Requerido si se distribuye Piper. ^|
  echo ^| Piper voice config ^| `models/tts/piper/voices/*.onnx.json` ^| Piper ^| No/intermedio ^| Requerido si se distribuye Piper. ^|
) > "%ENGINE_REPORT%"

if not exist "dist\legal" mkdir "dist\legal" >nul
copy /y "%REPORT%" "dist\legal\THIRD_PARTY_MANIFEST.md" >nul
copy /y "%ENGINE_REPORT%" "dist\legal\ENGINE_ARTIFACTS_MANIFEST.md" >nul

echo [DocuPodcast Studio] Manifest de terceros generado: %REPORT%
echo [DocuPodcast Studio] Manifest de artefactos de motores generado: %ENGINE_REPORT%
popd >nul
exit /b 0
