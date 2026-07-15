# T88C — Limpieza de alcance visible de motores y GUI

## Decisión de producto

DocuPodcast Studio no es una aplicación para editar documentos ni para construir documentos desde audio. Su promesa principal es:

```text
Abrir documento → escuchar → estudiar leyendo/oyendo → asignar clips o imágenes si aportan → exportar
```

Por tanto, la ruta visible de producto queda reducida a:

- **Coqui/XTTS** para voz de calidad alta.
- **Piper** como respaldo rápido/liviano.
- **FFmpeg** para preparar audio/video y normalizar clips.

**Whisper no pertenece al producto DocuPodcast.** Si el código histórico de infraestructura existe, puede conservarse encapsulado y sin exposición en UI; no debe aparecer en la pantalla principal, toolbar, configuración protagonista, guía de usuario, roadmap visible ni criterios de Release Candidate.

## Regla de shortlist de motores

- No agregar más motores al producto visible.
- Coqui/XTTS, Piper y FFmpeg son el alcance de motores de producto.
- no se incrustan modelos pesados dentro del .docupodcast; viven en instalación/configuración local, mientras los artefactos generados viven dentro del proyecto.

## Regla de honestidad UI

```text
Si no se implementa, no aparece.
Si aparece, debe funcionar.
No usar “avanzado” como sinónimo de promesa incompleta.
```

Esto evita llenar el proyecto de labia, botones decorativos y superficies que incrementan el riesgo de alucinación en futuras tandas.

## Cambios de T88C

- Se retira `Audio a texto` de toolbar, workspace de guion y flujo visible.
- Se retira Whisper/STT de Configuración visible y del catálogo guiado de motores.
- El preflight visible de motores se limita a Coqui/XTTS, Piper y FFmpeg.
- Se mantiene `Audio del computador` como clip genérico elegido por el usuario: puede ser voz, música, ambiente, pájaros, ruido o cualquier audio. El programa no clasifica su contenido.
- Se ocultan páginas de asistente/modelos con botones no operativos hasta que exista flujo real.
- Se actualizan tests de guardarraíl para bloquear la reaparición de STT/Whisper en presentación.

## Alcance que se conserva

La infraestructura histórica de STT puede permanecer mientras no contamine producto ni UX. No debe bloquear T89/T90 ni formar parte del RC.

## Próximo paso

T89 debe enfocarse en **Configuración humana para Coqui/Piper/FFmpeg**:

```text
Probar Coqui
Probar Piper
Probar FFmpeg
Guardar configuración
Abrir carpeta de modelos/herramientas
```

No debe agregar botones decorativos ni capacidades que no ejecuten un caso de uso real.
