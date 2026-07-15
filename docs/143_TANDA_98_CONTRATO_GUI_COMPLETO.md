# Tanda 98 — Documentación completa del contrato GUI

Se agrega documentación completa del contrato de interfaz gráfica previo al rediseño fuerte.

## Documentos agregados

```text
docs/productizacion/T98_CONTRATO_GUI_GLOBAL.md
docs/productizacion/T98A_CONTRATO_MENUBAR.md
docs/productizacion/T98B_CONTRATO_VISTAS_WORKSPACES.md
docs/productizacion/T98C_CONTRATO_RIBBON.md
docs/productizacion/T98D_CONTRATO_SIDEBAR_RAIL.md
docs/productizacion/T98E_CONTRATO_PLAYBAR_FLOTANTE.md
docs/productizacion/T98F_CONTRATO_CONFIGURACION_GUIA_DIALOGOS.md
docs/productizacion/T98G_ROADMAP_IMPLEMENTACION_GUI.md
```

## Decisiones centrales

- MenuBar separado por objetos: Archivo, Proyecto, Fuente documental, Ver, Lectura, Exportar, Configuración y Ayuda.
- Ribbon sobrio con Inicio, Lectura, Storyboard, Vista y Exportar.
- Vistas reales: Inicio, Documento y Voces.
- Guion, Audio Jobs, Diagnóstico, Storyboard y Exportación no son workspaces normales.
- Sidebar izquierdo configura la oración seleccionada.
- Rail derecho muestra/navega storyboard e imágenes asignadas.
- Playbar flotante es la acción primaria de escucha.
- `ReadingZoomControl` vive abajo a la derecha junto a StatusBar.
- No se reintroducen Whisper/STT ni opciones incompletas visibles.

## Estado

Esta tanda es documental. No implementa todavía el rediseño visual fuerte. Sirve como contrato para las siguientes tandas GUI.
