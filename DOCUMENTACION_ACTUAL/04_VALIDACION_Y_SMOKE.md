# Validación y smoke actuales

## Comando principal

```bat
scripts\99-diagnostico-completo.bat
```

Si se quiere probar motores reales:

```bat
scripts\99-diagnostico-completo.bat --real-engines
```

o:

```bat
set DOCUPODCAST_RUN_REAL_ENGINES=1
scripts\99-diagnostico-completo.bat
```

## Smoke manual mínimo después de cada tanda

1. Abrir fuente Word/DOCX.
2. Confirmar que aparece en Documento sin bloquear permanentemente la UI.
3. Seleccionar una frase.
4. Confirmar que sidebar izquierdo y rail derecho se sincronizan.
5. Asignar imagen.
6. Copiar imagen a fragmento anterior/posterior desde menú contextual.
7. Generar audio con Voz local simple si está disponible.
8. Ocultar y reabrir el overlay de generación; el contador debe seguir actualizado.
9. Revisar una tabla larga; el texto debe envolver y crecer hacia abajo sin puntos suspensivos innecesarios.
10. Probar playbar: inicio, pausa, detener, anterior, siguiente, reproducir desde inicio.
11. Guardar proyecto.
12. Reabrir proyecto y comprobar visuales/audio.

## Smoke de motores reales pendiente

La aplicación no debe declarar motor listo solo por descargar archivos. Debe cumplir:

- recursos descargados;
- modelo/verificación local OK;
- motor seleccionable;
- prueba WAV generada;
- WAV reproducible dentro de la app.

## Criterio de salida para RC

- Maven compile/test verde.
- Smoke cerebro verde.
- Preflight motores sin falso positivo.
- Al menos un documento Word con imagen y tabla probado.
- Al menos una generación de audio reproducida.
- Exportación WAV final probada.
- Video simple probado si entra en RC.


## Smoke manual MOTOR-SMOKE4R / COQUI-DL1

1. Abrir Configuración > Motores y dependencias.
2. En Voz IA avanzada, pulsar `Verificar`.
3. Si falta runtime/modelo/voz neutral, usar `Preparar`, `Descargar` o `Importar`.
4. Pulsar `Usar` cuando quede seleccionable.
5. Pulsar `Probar`.
6. Confirmar que se genere `runtime/tts/xtts-smoke/xtts-readiness-smoke.wav`.
7. Revisar que el mensaje distinga entre WAV generado y reproducción pendiente.
8. Reproducir una prueba desde la app antes de documentos largos.

## Validación AUDIO-COMPRESS1

- Maven completo no fue ejecutado en entorno ChatGPT por falta de `mvn`.
- Se validó compilación focal de dominio/aplicación/infraestructura y tests fuente nuevos con stubs JUnit.
- En Windows debe ejecutarse `scripts\99-diagnostico-completo.bat`.

## Validación MOTOR-SMOKE4R-HF5

Validar que preparar o seleccionar Voz local simple no haga que Voz IA avanzada busque `models/tts/xtts/speakers/voz-local-simple.wav`. La inspección avanzada debe usar `models/tts/xtts/speakers/voz-por-defecto.wav` como muestra neutral por defecto.
