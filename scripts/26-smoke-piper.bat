@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
if errorlevel 1 exit /b 1
call mvn -Dtest=RealEnginesSmokeScenarioTest -Ddocupodcast.realEnginesSmoke.enabled=true -Ddocupodcast.realEnginesSmoke.required=piper test
set "EXIT_CODE=%ERRORLEVEL%"
popd >nul
exit /b %EXIT_CODE%
