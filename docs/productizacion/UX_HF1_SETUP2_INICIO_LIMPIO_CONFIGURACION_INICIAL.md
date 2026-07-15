# UX-HF1 + UX-SETUP2 — Inicio limpio y configuración inicial

Estado: implementada sobre PF5C/PF6B verde.

## UX-HF1 — corrección inmediata

- La pantalla Inicio ya no muestra preparación técnica como lenguaje principal.
- **Guía rápida** queda cableada a la guía real.
- **Configuración inicial** abre la configuración directamente en motores de voz/video.
- La configuración normal deja fuera auditorías, smoke GUI, rutas internas, nombres de archivos técnicos y andamios de release.
- Los detalles técnicos se conservan en scripts, reportes y diagnóstico avanzado, no como experiencia normal del usuario final.

## UX-SETUP2 — primer uso guiado

- `SettingsDialog.showFirstUseSetup(...)` abre Motores de voz y pregunta si se desea preparar el equipo cuando faltan componentes obligatorios.
- `runInitialSetup(...)` reutiliza la preparación y descarga existentes de Voz IA avanzada.
- El progreso se muestra con mensajes humanos y sin rutas internas.
- Si la voz queda lista, se selecciona y guarda como motor principal.

## Regla de producto

El alcance está cerrado. La aplicación no debe presentar features futuras ni andamios técnicos. Toda acción visible debe ser útil para operar el producto actual o ir a Diagnóstico avanzado.
