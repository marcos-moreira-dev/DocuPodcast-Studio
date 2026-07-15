# Tanda 39 — Reproducción por chunks con prebuffer

## Objetivo

La pantalla principal debe permitir escuchar documentos largos sin esperar a que todo el Word quede renderizado en audio. Esta tanda introduce el contrato operativo de prebuffer tipo YouTube: iniciar cuando hay suficientes fragmentos listos y continuar mientras la fábrica interna genera más audio.

## Cambios

- Nuevo `PlaybackBufferPolicy` en dominio de playback.
- Política por defecto: 5 fragmentos listos antes de iniciar y 10 fragmentos de lookahead como objetivo de configuración.
- `DocuPodcastShellViewModel` conserva la pantalla Documento como superficie principal mientras la cola de audio prepara segmentos.
- Si el usuario pulsa `Escuchar documento` y el audio todavía se está generando, la app intenta iniciar automáticamente cuando el buffer inicial está listo.
- Si la reproducción alcanza el final de los fragmentos disponibles mientras el job sigue generando, pausa el transporte, espera el siguiente fragmento y continúa automáticamente cuando aparezca en el manifest parcial.
- La configuración de Reproducción / buffer deja de ser solo contrato documental y queda alineada con la nueva política operativa.

## Límites

Esta tanda no implementa todavía selección exacta por oración, asignación de voz/audio/imagen por rango, descarga de modelos, ni mini rail multimedia. La generación sigue usando la cola existente; el cambio importante es que el lector puede reproducir con manifest parcial.

## Validación esperada

Ejecutar:

```bat
scripts\02-ejecutar-tests.bat
```

Se agregan tests para la política de buffer y para proteger la integración del playback incremental en el shell.
