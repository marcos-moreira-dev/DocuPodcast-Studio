# Tanda 112 — Smoke visual UX / RC visual

## Base

T112 parte de T111-HF4. Incluye un hotfix pequeño para alinear el source test de iconografía T111 con la nueva iconografía PNG grande: `ribbon.css` conserva el marcador histórico `T111-HF2 — PNG icon polish` además del ajuste T111-HF4.

## Alcance

T112 no introduce una superficie nueva. Su objetivo es estabilizar y documentar el recorrido visual de usuario antes de pasar a ejemplos internos y productización.

## Checklist central

- Inicio sin cabina técnica.
- Ribbon con iconos PNG visibles.
- Documento como superficie principal.
- Imagen embebida DOCX mostrada cuando está disponible.
- Guardado guiado antes de escuchar/reproducir/generar.
- Overlay de procesos largos compacto, ocultable y recuperable desde status bar.
- Sidebar izquierdo con Texto/Audio/Imagen mediante componentes transversales.
- Rail derecho visual/navegacional, sin `Asignadas`.
- Voces como biblioteca usable.
- Guía renderizada, sin Markdown crudo ni STT/Whisper visible.
- Configuración para motores/rutas sin cabina técnica.
- Exportación honesta según readiness real.

## Validación local requerida

```bat
cd C:\Users\MARCOS MOREIRA\Downloads\mo\scripts
.\99-diagnostico-completo.bat
```

Debe quedar verde antes de avanzar a T113.

## Siguiente

T113 — Menú Ejemplos + proyectos demo internos.
