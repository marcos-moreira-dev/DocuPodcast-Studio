# VOZ-UX4R-2B — Configurar motor real dentro de Voces

## Objetivo

Completar el módulo **Configurar motor** de la microaplicación Voces con una superficie humana y sincronizada con la configuración interna del programa.

Esta tanda corrige además el fallo de compilación detectado tras VOZ-UX4R-2A: `VoiceLibraryWorkspaceView` no debe llamar métodos inexistentes de `VoiceProfile` ni de `VoiceProfileCapability`.

## Contrato de módulo

El módulo **Configurar motor** contiene un selector de motor activo y un selector de dispositivo de renderizado.

El módulo **Configurar motor** contiene:

1. Selector de motor activo.
2. Selector de dispositivo de renderizado.
3. Estado honesto de CPU/GPU detectadas.
4. Textbox de prueba de voz.
5. Botón para reproducir la última prueba.
6. Acceso a Configuración completa cuando haga falta preparar/descargar componentes.

## Motores visibles

La UI normal usa nombres humanos:

```text
Voz IA avanzada
Voz local simple
Modo de prueba
```

Los nombres técnicos de motor pueden existir internamente, logs o diagnósticos, pero no son el lenguaje principal del workspace.

## Selector de dispositivo

El selector de dispositivo aplica a todos los motores, porque el usuario necesita decidir honestamente si desea usar CPU o GPU. La aplicación solo muestra opciones detectadas o seguras:

```text
Automático
CPU
GPU NVIDIA detectada
GPU AMD detectada
GPU Intel detectada
```

Si no se detecta una GPU compatible, se mantiene CPU/Automático y la UI informa que no hay GPU compatible detectada.

## Persistencia

Las selecciones se escriben en `OperationalSettings`, la misma fuente usada por la ventana Configuración. La Vista Voces no mantiene una configuración paralela.

## Límites

Esta tanda no resuelve todavía la descarga de Voz IA avanzada/Coqui. Esa línea queda al final del bloque de voces, en `MOTOR-SMOKE4R / COQUI-DL1`.

Tampoco implementa todavía crear/renombrar/eliminar voces completas; eso queda en `VOZ-UX4R-3`.
