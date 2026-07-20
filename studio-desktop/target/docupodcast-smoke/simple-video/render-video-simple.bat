@echo off
setlocal EnableExtensions EnableDelayedExpansion
echo DocuPodcast Studio - Render de video simple
echo Resolucion objetivo: 2560x1440 (2K)
echo Modo render: la aplicacion debe bloquear lectura y nuevas exportaciones mientras FFmpeg trabaja.
set "SCRIPT_DIR=%~dp0"
set "FFMPEG=%SCRIPT_DIR%tools\ffmpeg\bin\ffmpeg.exe"
if not exist "%FFMPEG%" set "FFMPEG=%SCRIPT_DIR%..\tools\ffmpeg\bin\ffmpeg.exe"
if not exist "%FFMPEG%" set "FFMPEG=ffmpeg"
echo Usando FFmpeg: %FFMPEG%
echo Manifest: RENDER_MANIFEST.json
echo Comandos: render-commands.txt
echo Estado: PACKAGE_NEEDS_REVIEW
echo Escala/pad objetivo: scale=2560:1440:force_original_aspect_ratio=decrease,pad=2560:1440:(ow-iw)/2:(oh-ih)/2
echo Esta exportacion preparo un paquete; no creo el MP4 final por si sola.
echo Ejecuta o revisa render-commands.txt para producir video-simple.mp4 cuando el paquete este listo.
echo Si FFmpeg no existe, instala el paquete con tools/ffmpeg o configura la ruta desde Configuracion.
