# UX-SETUP3 — Motores vivos, refresco de Configuración y Voz local automática

Esta tanda corrige el comportamiento observado tras descargar/preparar motores desde la aplicación.

## Problemas cerrados

- Configuración no reflejaba inmediatamente que un motor/modelo ya había sido preparado.
- La preparación automática existía para Voz IA avanzada, pero no para Voz local simple.
- La generación de audio podía seguir usando la configuración antigua creada al arrancar la app; por eso, después de preparar un motor en la misma sesión, el job podía no ejecutar el proceso real hasta reiniciar.

## Cambios de producto

- La pantalla de Motores de voz refresca el estado al terminar preparar, descargar, importar o seleccionar.
- La Voz local simple tiene preparación automática con progreso, igual que Voz IA avanzada.
- Si la preparación queda lista, la app guarda la selección del motor automáticamente.
- El texto visible evita rutas, extensiones y nombres técnicos; esos detalles quedan para logs o diagnóstico.

## Cambios técnicos

- `DownloadPiperPortableRuntimeUseCase` descarga el runtime liviano y una voz española predeterminada en carpetas locales de la app.
- `SettingsAwareAudioGenerationGateway` reconstruye el motor efectivo al iniciar cada job de audio, leyendo la configuración persistida en ese momento.
- `SettingsAwareVoiceTestSynthesisGateway` hace lo mismo para pruebas cortas de voz.
- `PiperTtsCommandTemplate`, `InspectPiperSetupReadinessUseCase` y `SelectPiperAsEngineUseCase` aceptan el alias visible `voz-local-simple` sin exponer nombres de archivos al usuario.

## Guardarraíles

- `PiperAutomaticSetupUxSetup3SourceTest`
- `SettingsAwareTtsRuntimeUxSetup3SourceTest`

## Validación esperada

Ejecutar en Windows:

```bat
scripts\99-diagnostico-completo.bat
```

Luego, desde la app:

1. Abrir Configuración inicial o Configuración → Motores de voz.
2. Preparar Voz IA avanzada o Voz local simple.
3. Confirmar que la pantalla cambia a listo sin cerrar/reabrir.
4. Generar una prueba de voz o un audio corto de documento.
