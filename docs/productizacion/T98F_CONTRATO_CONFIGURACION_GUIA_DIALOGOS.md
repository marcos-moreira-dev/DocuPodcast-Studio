# T98F — Contrato de configuración, guía y diálogos secundarios

## Configuración

Configuración es ventana secundaria, no workspace. El menú `Configuración` debe abrirla directamente.

Contenido permitido:

```text
Coqui/XTTS
Piper
FFmpeg
almacenamiento
rendimiento
diagnóstico operativo
```

No debe mostrar Whisper/STT como producto.

## Motores

Cada motor visible debe tener:

- estado humano;
- ruta local esperada;
- acción real si existe;
- mensaje claro si falta algo.

No debe haber botones decorativos.

## Layout

- Botones no truncados.
- Ancho mínimo adecuado.
- Barra inferior responsive.
- Acciones principales siempre legibles.

## Guía de uso

La ayuda normal queda reducida a:

```text
Guía de uso
Acerca de DocuPodcast Studio
```

La guía debe hablar de capacidades reales. No debe ser Markdown crudo dentro de un `TextArea` como experiencia final. Debe tener visor legible con títulos, párrafos y listas.

## Diálogos secundarios

Usar diálogos para:

- exportar;
- revisar integridad;
- preparar motores;
- confirmar creación de proyecto;
- mostrar errores accionables.

Los diálogos deben respetar la misma regla: si aparece un botón, funciona.
