# T98G — Roadmap de implementación GUI posterior al contrato

Este roadmap se aplica después de cerrar el contrato GUI global. No debe empezar el rediseño fuerte sin respetar los contratos T98A–T98F.

## GUI-1 — MenuBar final

Implementar MenuBar aprobado:

```text
Archivo | Proyecto | Fuente documental | Ver | Lectura | Exportar | Configuración | Ayuda
```

Mapear cada acción a `AppCommandId`.

## GUI-2 — Ribbon base

Implementar Ribbon con pestañas:

```text
Inicio | Lectura | Storyboard | Vista | Exportar
```

Usar `RibbonButton`, `RibbonGroup`, `RibbonTab`, `RibbonView` y sistema de iconos transversal.

## GUI-3 — Workspace Documento limpio

Limpiar hoja:

- quitar etiquetas visibles `Párrafo/Título/Subtítulo`;
- señales mínimas por tipo/estado;
- lectura cómoda;
- reflow textual;
- posición estable de oración seleccionada/reproducida.

## GUI-4 — Sidebar izquierdo + rail derecho

Implementar:

- sidebar contextual/configurador;
- rail derecho visual/navegacional;
- rail redimensionable y retráctil;
- botón flotante tipo Blender;
- tarjetas de storyboard/imagen.

## GUI-5 — Playbar flotante

Implementar panel flotante con fondo semitransparente/difuminado, botones limpios y automatización de precondiciones.

## GUI-6 — Configuración / guía / diálogos

Limpiar:

- configuración secundaria;
- guía sin Markdown crudo;
- diálogos de exportación/progreso/integridad.

## GUI-7 — Iconografía / CSS / componentes

- eliminar emojis como iconografía final;
- iconos vectoriales consistentes;
- CSS modular;
- cero controles hardcodeados repetidos.

## GUI-8 — Smoke visual

Escenarios mínimos:

```text
Inicio
Abrir fuente documental
Escuchar documento
Seleccionar oración
Asignar voz
Asignar audio del computador
Asignar imagen
Mostrar/ocultar rail
Exportar paquete de storyboard
Abrir configuración
Guardar/cerrar/reabrir
```

Criterio de aceptación:

- no parece cabina técnica;
- la hoja sirve para estudiar;
- no hay botones falsos;
- no hay textos truncados;
- no hay promesas fuera del producto.
