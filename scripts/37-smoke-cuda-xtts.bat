@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0.."
set "ROOT=%CD%"
set "PY=%ROOT%\tools\xtts-wrapper\.venv\Scripts\python.exe"
set "OUTDIR=%ROOT%\runtime\tts\xtts-smoke"
set "OUT=%OUTDIR%\xtts-cuda-smoke-cli.txt"
if not exist "%OUTDIR%" mkdir "%OUTDIR%" >nul 2>nul
if not exist "%PY%" (
  echo [DocuPodcast Studio] No existe Python local de Voz IA avanzada: %PY%
  echo Ejecuta scripts\20-preparar-python-portable-coqui.bat primero.
  exit /b 1
)
"%PY%" -c "import json, torch; print(json.dumps({'torch_version': getattr(torch, '__version__', ''), 'torch_cuda_version': getattr(torch.version, 'cuda', None), 'cuda_available': torch.cuda.is_available(), 'device_count': torch.cuda.device_count() if torch.cuda.is_available() else 0}, ensure_ascii=False))" > "%OUT%" 2>&1
set "EC=%ERRORLEVEL%"
type "%OUT%"
if not "%EC%"=="0" (
  echo [DocuPodcast Studio] Smoke CUDA no pudo importar/verificar PyTorch. Revisa: %OUT%
  exit /b %EC%
)
findstr /C:"\"cuda_available\": true" "%OUT%" >nul
if errorlevel 1 (
  echo [DocuPodcast Studio] GPU no disponible para Voz IA avanzada en este Python local. Revisa: %OUT%
  exit /b 2
)
echo [DocuPodcast Studio] CUDA disponible para Voz IA avanzada en Python local.
exit /b 0
