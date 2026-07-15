# Tanda 104 — Documento limpio tipo lector Word

## Resumen

T104 se aplica sobre T103A. La tanda corrige dos frentes antes de seguir:

1. Los tests fuente que quedaron desalineados tras convertir Inicio en pantalla desktop y el Ribbon en superficie real.
2. La estética del Ribbon y del workspace Documento según prueba visual local.

## Resultado

- Ribbon más respirable: más alto que T103A, botones más anchos y sin compresión excesiva.
- Documento con hoja centrada, más parecida a lector de escritorio que a panel técnico.
- Header del documento mínimo y humano.
- Scroll hacia selección/playback basado en bounds, no solo índice proporcional.
- Tests fuente actualizados a los textos vigentes de Inicio/Documento.

## Validación en entorno ChatGPT

- Verificación fuente de Ribbon y Documento.
- Compilación focal de tests fuente modificados con stubs mínimos de JUnit.
- Ejecución reflexiva de tests focales.
- ZIP íntegro.

No se ejecutó Maven completo porque `mvn` no está instalado en este entorno.

## Validación local recomendada

```bat
scripts\99-diagnostico-completo.bat
```
