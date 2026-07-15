# MOTOR-GPU-SMOKE1-HF2 — probe CUDA sin `python -c` frágil

## Motivo

Tras `PLAYBACK-SPEED-HF9` verde, la prueba manual en Configuración > Rendimiento / dispositivo mostró que el smoke CUDA de Voz IA avanzada no estaba fallando por CUDA todavía, sino por el propio comando de prueba:

```text
SyntaxError: unterminated string literal (detected at line 5)
```

El problema venía de ejecutar el probe como argumento `python -c` con un bloque Python largo. En Windows, esa ruta puede ser frágil por comillas, saltos y normalización de argumentos. El resultado era un falso negativo: la UI decía que CUDA no estaba disponible, pero el Python ni siquiera había llegado a importar PyTorch.

## Cambio aplicado

`ProcessXttsCudaRuntimeProbeGateway` ya no ejecuta el probe con `python -c`. Ahora:

1. crea un archivo temporal `.py`;
2. escribe ahí el probe CUDA completo en UTF-8;
3. ejecuta:

```text
python.exe <probe-temporal.py> cuda:0
```

4. lee stdout/stderr;
5. elimina el archivo temporal al terminar.

Esto evita que las comillas del script Python se rompan en Windows.

## Qué cambia para el usuario

Al pulsar `Probar GPU para Voz IA avanzada`, la prueba debe mostrar un resultado real:

- si PyTorch no se puede importar, dirá que falta PyTorch en el Python local;
- si PyTorch existe pero es CPU-only, dirá que CUDA no está disponible para ese runtime;
- si PyTorch tiene CUDA y detecta la GPU, marcará GPU usable para Voz IA avanzada.

## Importante

Esta tanda no instala PyTorch con CUDA. Solo corrige el smoke para que la respuesta sea fiable. Si después de este hotfix el resultado dice que CUDA no está disponible, entonces el problema real será el runtime Python autocontenido: probablemente tiene PyTorch CPU-only o una instalación sin CUDA usable.

## Validación recomendada

1. Ejecutar:

```bat
scripts\99-diagnostico-completo.bat
```

2. Abrir Configuración > Rendimiento / dispositivo.
3. Pulsar `Probar GPU para Voz IA avanzada`.
4. Confirmar que ya no aparece `SyntaxError: unterminated string literal`.
5. Si el resultado sigue en CPU, revisar si el detalle dice PyTorch CPU-only o CUDA no disponible.
