# T90G — Smoke real modular de motores

## Objetivo

Permitir validar motores locales por separado antes del smoke completo del producto. Esto evita que una herramienta pendiente bloquee la validación de otra.

## Scripts

```bat
scripts\25-smoke-coqui.bat
scripts\26-smoke-piper.bat
scripts\27-smoke-ffmpeg.bat
scripts\28-smoke-motores-producto.bat
```

También se puede usar el script general con argumento:

```bat
scripts\19-smoke-motores-reales.bat coqui
scripts\19-smoke-motores-reales.bat piper
scripts\19-smoke-motores-reales.bat ffmpeg
scripts\19-smoke-motores-reales.bat coqui,piper,ffmpeg
```

## Comportamiento

`RealEnginesSmokeScenarioTest` solo ejecuta los motores listados en `docupodcast.realEnginesSmoke.required`.

Ejemplos:

```bat
mvn -Dtest=RealEnginesSmokeScenarioTest ^
-Ddocupodcast.realEnginesSmoke.enabled=true ^
-Ddocupodcast.realEnginesSmoke.required=coqui ^
test
```

## Evidencia

Todos los modos escriben en:

```text
target/docupodcast-real-engines-smoke/
```

Incluye reporte, entradas, salidas y logs por proceso.

## No alcance

No valida transcripción, STT ni Whisper. El producto se limita a Coqui/XTTS, Piper y FFmpeg.
