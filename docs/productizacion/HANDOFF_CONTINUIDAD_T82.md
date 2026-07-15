# Handoff de continuidad T82

Este handoff existe para que otra ventana de chat o una sesión futura pueda continuar el proyecto sin perder contexto.

## Estado técnico actual

Base: Tanda 81G verde según validación local del usuario.

Estado funcional:

- Cerebro V1 congelado desde T80.
- Proyecto con carpeta contenedora.
- Integridad y reparación reportada.
- Export readiness.
- Smoke automático del cerebro.
- CPU/GPU modelado en configuración.
- Entrada flexible de media: MP3, WAV y video para extraer audio.
- Documento limpio sin metadatos técnicos visibles.
- Barra flotante de lectura global.
- Inspector izquierdo contextual.
- Rail derecho de miniaturas/medios.
- Toolbar con iconos y grupos.
- Vistas secundarias degradadas a avanzadas.

## Estructura de superficies vigente

```text
Inicio                  → presentación del producto y acciones iniciales.
Documento               → superficie principal de trabajo.
Configuración           → ventana técnica abierta solo desde menú.
Ayuda                   → guía y primeros pasos.
Herramientas avanzadas  → guion, audio jobs, voces y storyboard completo.
```

## Regla de oro

No volver a convertir Documento en cabina técnica.

Documento debe permitir:

```text
abrir documento
leer
escuchar
seleccionar oración
asignar audio/imagen/emoción si hace falta
exportar
```

Todo lo demás debe vivir en configuración, herramientas avanzadas o diagnóstico.

## Pendientes exactos después de T82

### Pendiente 1 — Integración real TTS

Implementar Piper como motor real liviano y opcionalmente XTTS por wrapper externo.

### Pendiente 2 — Integración real STT

Completar experiencia Whisper.cpp desde configuración y prueba corta.

### Pendiente 3 — FFmpeg operativo

Cerrar preflight real, extracción audio de video y registro de ruta efectiva.

### Pendiente 4 — UX de reproducción real

Cuando el usuario pulse Escuchar documento, debe quedar claro si:

```text
falta guardar proyecto
falta motor TTS
falta modelo
se está generando audio
se está reproduciendo
audio listo
hubo error
```

### Pendiente 5 — Pulido visual fino

Ajustar layout de barra flotante, transparencias, efecto tipo cristal suave si es viable en JavaFX/CSS, espaciado de hoja, ancho de sidebars, tooltips y textos truncados.

### Pendiente 6 — Pruebas manuales con documentos reales

Probar con:

```text
DOCX largo
PDF con texto nativo
PDF escaneado rechazado
TXT
Markdown
obra teatral con voces
imágenes por fragmento
audio MP3/WAV por fragmento
video para extraer audio
```

### Pendiente 7 — Packaging real

Decidir distribución de motores/modelos: embebidos, externos, asistidos o mixtos.

## Carpetas de referencia agregadas

Las capturas usadas para planificar están en:

```text
docs/referencias/capturas-chat/
```

Y el índice visual está en:

```text
docs/referencias/capturas-chat/INDICE_REFERENCIAS_VISUALES.md
```

## Documentos obligatorios para leer antes de continuar

```text
docs/productizacion/NOTAS_CONVERSACION_T81_T82.md
docs/productizacion/PLAN_EXACTO_IA_TTS_STT_REAL.md
docs/referencias/capturas-chat/INDICE_REFERENCIAS_VISUALES.md
docs/productizacion/ROADMAP_POST_T82_IA_REAL_Y_RC.md
AI_HANDOFF.md
VALIDATION.md
```

