# Tanda 38B — Namespace Marcos Moreira Dev y configuración de voz

## Objetivo

Alinear el proyecto con el namespace solicitado `com.marcosmoreiradev` y recuperar el build verde tras la Tanda 38 sin introducir nuevas funciones pesadas.

## Cambios

- Se migra el paquete Java y el módulo al namespace `com.marcosmoreiradev.docupodcaststudio`.
- Se actualiza `pom.xml` para usar `groupId` `com.marcosmoreiradev`.
- Se actualiza el `mainClass` modular de JavaFX y los scripts de empaquetado que referencian el módulo principal.
- Se alinean los source tests de Documento con la implementación real de `PrimaryActionStrip` y `SettingsDialog`.
- La configuración de TTS/voz documenta motores prioritarios y muestra un flujo de muestras de voz:
  - motor potente: XTTS / Coqui;
  - motor semipotente: Piper;
  - velocidad de habla como control global si el motor lo soporta;
  - muestras de voz neutral, enojada, triste, feliz e intrigada;
  - grabación de voz propia o autorizada con frases guía.

## No cambia

- No se implementa todavía descarga real de modelos.
- No se implementa todavía asistente funcional de grabación por emoción.
- No se cambia la reproducción por chunks.
- No se toca exportación WAV, persistencia de audio ni storyboard.
- No se escriben acotaciones dentro del Word original.

## Criterio de producto

La pantalla principal sigue enfocada en leer y escuchar el documento. La configuración de motores, velocidad, muestras y diagnóstico vive en la ventana de Configuración como bodega técnica del programa.
