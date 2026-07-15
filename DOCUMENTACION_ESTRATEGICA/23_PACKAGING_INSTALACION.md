# Packaging e instalación

Objetivo: app JavaFX autocontenida.

## JavaFX

Empaquetar con `jpackage` más adelante.

## Motor TTS

El motor debe estar encapsulado. Si usa Python, el usuario no debe ejecutar `pip install` manualmente para usar la app final.

Opciones:

- worker Python empaquetado;
- Piper/binario local;
- ONNX Runtime;
- gateway mock en desarrollo.

## Modelos grandes

No incluir modelos gigantes en exportaciones de proyecto.

Instalación del producto puede tener:

- paquete ligero;
- paquete completo con modelo;
- configuración de carpeta externa de modelos.

## Validación inicial

La app debe tener diagnóstico:

- motor encontrado;
- modelo encontrado;
- carpeta de salida escribible;
- test de voz;
- FFmpeg/encoder si se usa MP3.
