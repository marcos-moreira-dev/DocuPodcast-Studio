# T120 — Limpieza de onboarding / primer uso

## Propósito

Inicio debe ayudar al usuario normal a empezar. No debe parecer una página técnica, ni una cabina de pruebas, ni una pantalla llena de promesas futuras. El primer uso debe orientar hacia el flujo real:

```text
Preparar voz → Abrir documento → Escuchar y estudiar → Exportar
```

## Cambios aplicados

`WelcomeWorkspaceView` se actualiza para incluir una acción real de **Preparar voz** conectada al comando `OPEN_SETTINGS`. Esto abre Configuración, donde ya existen asistentes verificables para Coqui/XTTS, Piper y FFmpeg.

El onboarding ahora prioriza:

- Abrir documento.
- Preparar voz.
- Abrir proyecto.
- Nuevo proyecto.
- Probar ejemplo.
- Guía rápida.

## Mensajes principales

El subtítulo orienta al usuario hacia motores preparados desde Configuración:

```text
Abre documentos y conviértelos en lectura escuchable local, con motores preparados desde Configuración.
```

Los pasos quedan:

1. Prepara la voz.
2. Abre el documento.
3. Escucha y estudia.
4. Exporta.

## Badges de promesa

Se reemplazan promesas viejas por señales alineadas con el producto:

- Documento portable en `source/`.
- Coqui/XTTS principal.
- FFmpeg para video.

## Lo que no debe aparecer

- Guion.
- Whisper.
- STT.
- Storyboard como módulo principal.
- Jobs técnicos.
- Manifest como concepto de usuario.

## Criterio de aceptación

Un usuario debe abrir la app y entender en menos de 30 segundos que debe preparar la voz, abrir un documento, escuchar y luego exportar si lo necesita.
