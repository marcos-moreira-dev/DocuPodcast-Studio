@echo off
setlocal
set ROOT=%~dp0..
cd /d "%ROOT%" || exit /b 1
powershell -NoProfile -ExecutionPolicy Bypass -File "scripts\tts\setup-xtts-pytorch-cuda.ps1" %*
exit /b %ERRORLEVEL%
