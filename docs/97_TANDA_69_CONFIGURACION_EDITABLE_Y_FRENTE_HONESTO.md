# Tanda 69 — Configuración editable y frente honesto

## Objetivo

Corregir la percepción de fachada detectada en la UI: la configuración ya no debe ser solo una lista informativa. Debe permitir editar valores operativos persistentes, guardar cambios y mantener separada la bodega técnica de la pantalla de lectura.

## Cambios principales

- `SettingsDialog` incorpora campos editables (`TextField`, `CheckBox`, `ComboBox`) para lectura, buffer, TTS, STT, video, almacenamiento y diagnóstico.
- Se agrega acción `Guardar cambios`, conectada a `SettingsApplicationServices.saveOperationalSettings().save(...)`.
- Se agrega acción `Restaurar predeterminados`, que recarga valores en pantalla sin escribir hasta que el usuario guarde.
- La barra superior y el menú cambian el lenguaje visible de `Abrir Word/DOCX` a `Abrir documento`.
- El selector de archivo acepta ahora DOCX, Markdown y TXT como fuentes de lectura actuales.
- Se agregan importadores reales para Markdown y TXT como documentos fuente solo lectura.
- El inicio deja de sobreprometer PDF como compatible actual: PDF sigue en contrato de fuente solo lectura, pero queda para un importador posterior.

## Regla de producto

La app no debe prometer compatibilidad visible con un formato que aún no tenga importador real. En V1 actual, las fuentes abribles son DOCX, Markdown y TXT. PDF queda preparado en el contrato y roadmap, pero no debe aparecer en el file chooser hasta tener implementación.

## No alcance

Esta tanda no intenta cerrar el diseño final del frontend. Solo corrige fachada evidente y alinea lenguaje visible con capacidad real.
