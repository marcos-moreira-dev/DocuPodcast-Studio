# Resumen de producto — DocuPodcast Studio

DocuPodcast Studio no debe entenderse como un “mini Word”. Esa formulación empuja a competir con Word y abre un alcance imposible. La app debe entenderse como un **estudio local de lectura, guion, voz, audio y storyboard**.

## Problema

El usuario tiene documentos largos, especialmente notas en Word. Leerlos en pantalla tiene fricción. Escucharlos como audio natural permite estudiar caminando, descansando la vista o reduciendo la resistencia psicológica a textos extensos.

Los lectores TTS simples suelen sonar robóticos. La idea es usar voces locales de mejor calidad, aunque la generación tarde varios minutos. No se exige tiempo real.

## Propuesta

DocuPodcast Studio permite:

- abrir Word/DOCX y convertirlo en texto estructurado;
- detectar títulos, subtítulos, párrafos, listas, tablas e imágenes;
- configurar qué se considera título/subtítulo;
- crear un guion narrable;
- asignar voces, personajes y estilos;
- generar audio por segmentos;
- ver progreso, ETA, logs y reintentos;
- asociar imágenes a segmentos para un storyboard vivo;
- reproducir texto + audio + imagen de forma sincronizada;
- exportar podcast, guion, proyecto y reportes.

## Diferenciador

El valor no es solo “leer texto”. El valor es preservar una relación trazable:

```text
texto → segmento → voz/personaje/estilo → audio → imagen/storyboard → playback/exportación
```

Esa relación permite regenerar solo una parte, corregir un segmento, cambiar una voz, reintentar fallidos y exportar sin perder contexto.

## MVP realista

El primer MVP debe demostrar:

1. Word/DOCX → documento estructurado.
2. Documento → guion narrable.
3. Guion → audio por segmentos con progreso.
4. Guardado/reapertura del proyecto.
5. Exportación de audio final.

Storyboard, voces autorizadas y estilos emocionales pueden crecer después, pero la arquitectura debe reservarles lugar desde el inicio.
