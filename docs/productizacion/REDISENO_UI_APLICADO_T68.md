# T68 — Rediseño UI aplicado sobre Documento/Inicio

## Decisión de producto

La interfaz principal de DocuPodcast Studio debe comportarse como un lector narrado de documentos, no como una cabina técnica. El usuario normal debe entender cuatro acciones: abrir documento, escuchar, refrescar contenido y usar capas opcionales cuando lo necesite.

## Alcance de esta tanda

T68 aplica una primera poda visible sin cambiar el cerebro ya estabilizado:

- El Documento mantiene la acción principal inteligente y agrega `Refrescar contenido` como acción secundaria clara.
- El resumen técnico de bloques, narrables, tablas, imágenes y advertencias deja de mostrarse en la página principal.
- La fuente se declara como solo lectura en la propia página: si el usuario edita el Word/PDF/Markdown/TXT fuera de la app, debe usar `Refrescar contenido`.
- Los bloques ya no muestran etiquetas técnicas como `texto narrable` ni `estilo Word` en la lectura principal.
- La bienvenida habla de documentos Word, PDF, Markdown y TXT como fuente solo lectura, sin convertir la app en procesador de texto.

## Lo que se mantiene fuera del flujo principal

- Diagnóstico de importación.
- Métricas de estructura.
- Detalles de buffer, jobs, motores y manifest.
- Guion/proyección interna de narración.
- Configuración técnica de TTS/STT/FFmpeg.

Estos elementos siguen existiendo, pero pertenecen a paneles avanzados, configuración o diagnóstico.

## Guardarraíl

El rediseño aplicado no puede romper la regla de V1:

```text
Documento fuente externo = solo lectura
Documento narrable del proyecto = raíz operativa
Capas/audio/storyboard = artefactos del proyecto
```

