# PF2E / PF3B — Progreso visible en configuración y cierre de guardarraíles de voces

## Objetivo

Convertir la preparación de Voz IA avanzada en una operación claramente visible desde Configuración, sin sensación de bloqueo ni dependencia de scripts manuales como ruta normal.

## Cambios

- Las operaciones largas de Configuración reciben eventos de progreso desde los casos de uso.
- Preparación de runtime emite latidos mientras el instalador interno sigue trabajando.
- Descarga de modelo emite avance por archivo y por bloques copiados.
- Importación de modelo informa validación, copia y verificación.
- El diagnóstico TTS remite al usuario a Configuración como ruta normal; el `.bat` queda como rescate técnico.
- `DocuPodcastShellViewModel` vuelve a quedar bajo el límite de deuda de RF2 al mover reglas de muestras de voz al coordinador.
- Vista Voces suma acciones directas para abrir Configuración de motores y refrescar estado de voces desde el hero, reduciendo la sensación de panel puramente informativo.

## Validación esperada en Windows

Ejecutar:

```bat
scripts\99-diagnostico-completo.bat
```

Luego probar en la app:

1. Configuración → Motores de voz.
2. Preparar automáticamente.
3. Verificar que el diálogo cambia su texto durante la operación.
4. Descargar/importar modelo y confirmar que muestra archivo o etapa actual.
