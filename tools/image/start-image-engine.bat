@echo off
setlocal
cd /d "%~dp0ComfyUI"
if "%~1"=="" (
  "%~dp0venv\Scripts\python.exe" "main.py" --lowvram --disable-auto-launch --port 8188
) else (
  "%~dp0venv\Scripts\python.exe" "main.py" --disable-auto-launch %*
)
