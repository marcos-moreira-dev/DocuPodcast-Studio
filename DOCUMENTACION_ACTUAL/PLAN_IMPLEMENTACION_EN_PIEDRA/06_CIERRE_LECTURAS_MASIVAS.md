# Cierre de lecturas masivas

Este documento resume por qué se cerró la fase de lectura y por qué el siguiente paso debe ser implementación por tandas.

## Conclusión general

No hace falta seguir leyendo antes de implementar. La lectura masiva cubrió:

- motor avanzado;
- CPU/GPU;
- playback por índice;
- velocidad 1.5x/1.75x;
- Vista Voces;
- sidebar de Documento;
- hardcoding;
- arquitectura/refactor;
- tests;
- documentación;
- PF9;
- audio export;
- video runtime;
- persistencia;
- export readiness;
- Settings/runtime;
- Javadoc/malos olores;
- scripts diagnóstico;
- onboarding;
- RC gate;
- terceros/legal;
- performance/memoria;
- portable/installer.

## Hallazgos que no deben perderse

### Voz IA avanzada

El problema no es solo descarga. El motor puede estar descargado pero no probado para generar documentos. La app debe distinguir descargado, preparado, probado, reproducido y usable.

### CPU/GPU

GPU detectada por Windows no significa GPU usable por el Python autocontenido. `torch.cuda.is_available()` dentro del venv local es la fuente de verdad.

### Índice y playback

El índice selecciona visualmente bien, pero si el guion/manifest no existe todavía, el flujo puede volver a generación global desde el inicio. Debe respetar el pivote seleccionado.

### Velocidad de playback

El audio sí se acelera, pero el avance al siguiente chunk puede caer en watchdog/deadline con márgenes conservadores. El fin real del WAV debe mandar.

### Vista Voces

No está mal. Necesita pulido: combos simples, menos microcopy y política común de labels.

### Documento/sidebar

Las reglas son buenas: Voz local simple sin emociones; Voz IA avanzada solo con tonos registrados. Falta simplificar mensajes.

### Hardcoding

El proyecto ya tiene base para runtime layout, pero rutas de motores, labels humanos, procesos externos, descargas y checksums siguen dispersos.

### Arquitectura

Las capas base están sanas. La deuda es concentración: `DocuPodcastShellViewModel`, `SettingsDialog`, `VoiceLibraryWorkspaceView`, procesos externos y factories.

### Tests

Hay muchos source tests e historia de tandas. No deben borrarse a ciegas, pero sí clasificarse.

### Documentación

`DOCUMENTACION_ACTUAL/` debe ser la fuente vigente. README/AI_HANDOFF/VALIDATION deben simplificarse más adelante.

### Exportaciones

Audio final existe, pero debe ir a segundo plano. Video MP4 final está bien armado, pero necesita smoke real local.

### Persistencia

Está mucho más madura, pero el smoke de RC debe validar reabrir proyecto con artefactos reales.

### Diagnóstico/RC

El diagnóstico base puede quedar verde sin motores reales porque esos smokes son opt-in. La RC debe decir explícitamente qué se ejecutó y qué se omitió.

### Packaging/memoria

La app ejecutada por script usa heap explícito, pero app-image/portable debe recibir el mismo tratamiento.

## Estado final de lectura

Lecturas masivas cerradas. Próxima acción:

**Implementar `MOTOR-ADV-READY-GATE1`.**

No iniciar limpieza documental, RC o refactor mayor antes de resolver los cuatro puntos funcionales iniciales:

1. Voz IA avanzada usable.
2. GPU honesta.
3. Índice → reproducir desde selección.
4. Velocidad playback sin silencio artificial.
