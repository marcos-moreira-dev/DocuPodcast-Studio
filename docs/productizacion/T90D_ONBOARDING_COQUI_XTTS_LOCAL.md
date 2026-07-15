# T90D — Onboarding Coqui/XTTS local autocontenido

## Objetivo

T90D mantiene el onboarding de Coqui/XTTS como flujo local y repetible, sin Python global y sin instalar dependencias en el sistema operativo.

## Scripts públicos

```bat
scripts\20-preparar-python-portable-coqui.bat
scripts\22-verificar-coqui-xtts-local.bat
scripts\21-probar-coqui-xtts.bat
```

## Flujo esperado

1. Descargar/extraer Python repo-local en `tools\python`.
2. Crear `tools\xtts-wrapper\.venv` usando ese Python local.
3. Instalar dependencias desde `tools\xtts-wrapper\requirements-xtts.txt`.
4. Verificar que el paquete `TTS` sea importable desde el venv local.
5. Verificar modelo y voz por defecto.
6. Generar WAV de prueba cuando el modelo esté disponible.

## Contrato de mantenimiento

- No se modifica `PATH`.
- No se usa `py`, `python`, `python3` ni una instalación global.
- El proyecto `.docupodcast` no guarda modelos ni runtimes.
- Los reportes se guardan en `target\docupodcast-engine-setup`.

## Resultado

T90D deja a Coqui/XTTS listo para ser preparado por scripts y, más adelante, por un asistente de configuración de la app.
