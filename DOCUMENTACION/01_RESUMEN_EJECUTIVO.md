# 01 — Resumen ejecutivo

**DocuPodcast Studio** nace de una necesidad concreta: estudiar con menor fricción convirtiendo documentos largos, especialmente Word/DOCX, en audio local con una voz natural y poco robótica. La idea evolucionó hacia un estudio de guion narrable que permite también voces por personaje, estilos de interpretación, imágenes asociadas y storyboard vivo.

## Producto

La app debe permitir:

1. Abrir documentos Word/DOCX, Markdown, TXT y PDF simple.
2. Mostrar el texto importado con scroll.
3. Detectar títulos, subtítulos, párrafos, listas, tablas simples e imágenes.
4. Configurar reglas de lectura: qué es título, subtítulo, cuerpo, imagen, tabla.
5. Convertir el documento en guion narrable.
6. Editar segmentos del guion.
7. Asignar voces, personajes y estilos si el motor lo permite.
8. Generar audio por segmentos, no en tiempo real.
9. Mostrar progreso claro, ETA, segmento actual, fallos y reintentos.
10. Asociar imágenes del usuario a segmentos para crear storyboard vivo.
11. Reproducir audio resaltando texto e imagen activa.
12. Guardar proyecto portable con assets, jobs, manifests y exportaciones.

## Decisión técnica central

JavaFX será la aplicación principal. Si la mejor voz requiere Python internamente, se encapsula como worker/binario local sin API visible. La UI no debe depender de Python directamente.

```text
JavaFX app
  → application services
  → AudioGenerationGateway
  → implementación real: mock / Piper / XTTS worker / futuro ONNX
```

## Principio de honestidad funcional

No mostrar botones ni prometer capacidades sin cadena real. Ejemplos:

- No mostrar “Exportar MP3” si no hay encoder.
- No mostrar “leer llorando” si el motor no soporta estilos/emociones.
- No marcar una plantilla IA como importable si tiene placeholders.
- No prometer video/storyboard animado si solo existe preview estático.

## Prioridad MVP

El primer producto útil debe ser:

```text
Abrir Word → detectar estructura → crear guion → generar WAV por segmentos → progreso + reintentos → exportar audio
```

Storyboard, voces avanzadas y recursos IA son importantes, pero no deben bloquear el flujo Word→Audio.
