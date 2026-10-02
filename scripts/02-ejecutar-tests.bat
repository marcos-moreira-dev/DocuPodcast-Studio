@echo off
setlocal EnableExtensions
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0lib\DocuPodcast.Tasks.ps1" -Task Test %*
exit /b %ERRORLEVEL%
