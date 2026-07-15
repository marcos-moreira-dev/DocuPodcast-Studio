# T117 — Asistente real Coqui/XTTS

## Objetivo

Convertir Coqui/XTTS en un motor principal verificable desde la Configuración, sin depender de Python global ni de comandos manuales para el usuario normal.

## Contrato de producto

- Coqui/XTTS es el motor principal de lectura de calidad alta.
- La app usa Python local/portable bajo `tools/xtts-wrapper/.venv/Scripts/python.exe`.
- No usa Python global ni `PATH` del sistema como requisito de producto.
- El wrapper local es `tools/xtts-wrapper/synthesize_xtts.py`.
- El script de preparación esperado es `scripts/tts/setup-xtts-portable-python.ps1`.
- El modelo esperado vive bajo `models/tts/xtts`.
- La voz neutral prediseñada vive bajo `models/tts/xtts/speakers/voz-por-defecto.wav`.

## Cambios implementados

### Nuevos use cases

- `InspectXttsSetupReadinessUseCase`: verifica runtime Python local, wrapper, modelo XTTS y voz neutral.
- `SelectXttsAsEngineUseCase`: prepara `OperationalSettings` para seleccionar `engineMode=xtts` como motor principal.

### Nuevo reporte

- `XttsSetupReadinessReport`: resume rutas verificadas, faltantes, advertencias y estado listo/no listo.

### Configuración

La sección Motores de voz incluye un bloque **Asistente Coqui/XTTS** con acciones reales:

- `Verificar Coqui/XTTS`: inspecciona archivos locales y reporta faltantes.
- `Usar Coqui/XTTS`: selecciona el motor en pantalla y pide guardar cambios para persistirlo.

Este asistente no descarga modelos todavía. La descarga/importación verificable queda para la tanda de manifiestos y checksums. Mientras tanto, el flujo honesto es importar/copiar artefactos al layout esperado y verificarlos.

## Criterios de aceptación

- Coqui/XTTS puede verificarse desde Configuración.
- La UI no presenta botones falsos: verificar y seleccionar ejecutan acciones reales.
- La selección de Coqui/XTTS no requiere escribir comando externo.
- La verificación revisa Python local, wrapper, modelo y voz neutral.
- Si falta algo, el usuario recibe una lista concreta de faltantes.

## Pendientes relacionados

- T119 — Configuración honesta y accionable.
- T119C — Manifiestos, licencias y checksums de motores.
- T127 — Packaging portable/MSI/app-image.
