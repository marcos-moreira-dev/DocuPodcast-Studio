# PF4A — Ejecutable portable con launcher de usuario final

## Objetivo

Cerrar el primer corte de ejecutable portable para uso personal: el usuario no debe abrir scripts técnicos ni depender del directorio de trabajo de PowerShell. La carpeta `dist\portable\DocuPodcastStudio` debe poder ejecutarse con doble clic mediante un launcher pequeño que fija `DOCUPODCAST_APP_ROOT` a la propia carpeta portable.

## Implementado

- `scripts\14-app-image-completa.bat` genera `run-docupodcast-studio.bat` dentro del app-image.
- `scripts\32-preparar-app-portable-layout.bat` conserva/genera el mismo launcher dentro de la carpeta portable final.
- El launcher fija `DOCUPODCAST_APP_ROOT=%%~dp0` antes de abrir `DocuPodcastStudio.exe`.
- `APP_IMAGE_MANIFEST.txt` y `PORTABLE_MANIFEST.txt` declaran explícitamente el launcher y el runtime root esperado.
- Se retira del manifest de app-image el lenguaje heredado de `mock integrado salvo DOCUPODCAST_TTS_COMMAND`; el runtime se describe como configuración in-app de Voz IA avanzada, Voz local simple o Modo de prueba diagnóstico.

## Pendiente posterior

PF4B debe agregar smoke post-app-image que ejecute el launcher portable, verifique que `RuntimePathResolver` cae en la carpeta portable y confirme desde diagnóstico que tools/models/scripts se resuelven dentro del paquete.
