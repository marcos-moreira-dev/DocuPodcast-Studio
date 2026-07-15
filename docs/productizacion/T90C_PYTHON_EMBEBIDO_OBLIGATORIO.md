# T90C — Python embebido obligatorio para Coqui/XTTS

## Objetivo

T90C cierra una regla de producto: **DocuPodcast no usa Python global para Coqui/XTTS**. El motor de voz de calidad alta debe ejecutarse únicamente con el runtime local administrado por la aplicación.

## Decisión

```text
Python global / PATH / py / python3  → prohibido para Coqui/XTTS
Python local en tools/xtts-wrapper/.venv → único runtime permitido
```

La app puede preparar ese runtime mediante scripts de onboarding, pero no debe depender de instalaciones externas del usuario.

## Cambios principales

- `XttsTtsCommandTemplate` ya no cae a `Path.of("python")`.
- `xtts-file-to-wav.ps1` rechaza un Python distinto al runtime local esperado.
- El smoke real T90 ya no admite `DOCUPODCAST_XTTS_PYTHON` ni `docupodcast.smoke.xtts.python`.
- Se agrega guardarraíl fuente para evitar regresión.

## Ruta obligatoria

```text
tools/xtts-wrapper/.venv/Scripts/python.exe
```

## Mensaje esperado si falta

```text
Falta preparar Python local para Coqui/XTTS.
Ejecuta scripts\20-preparar-python-portable-coqui.bat.
```

## Alcance

T90C no rediseña UI. Endurece el contrato de runtime para que la futura configuración no prometa algo frágil.
