# Roadmap post T81D — Frontend restante

## Estado después de T81D

La pantalla Documento ya tiene:

- navegación/menu bar simplificados de T81A;
- hoja central sin metadatos técnicos de T81B;
- barra flotante de lectura global de T81C;
- inspector izquierdo contextual de T81D.

## Pendientes

### T81E — Sidebar derecho de miniaturas / medios

Objetivo: transformar el rail derecho en superficie visual. Debe mostrar:

```text
miniatura
fragmento relacionado
descripción breve
estado asignada/no asignada
```

El clic debe llevar el documento al fragmento asociado. No debe contener formularios ni botoneras de asignación.

### T81F — Toolbar con iconos y grupos

Objetivo: reducir la toolbar textual heredada. Debe agrupar acciones:

```text
Documento
Lectura
Vista
Exportar
```

No debe exponer como botones permanentes:

```text
Narración avanzada
Voces
Audio
Storyboard
Jobs
Diagnóstico
```

### T81G — Retiro o degradación de vistas secundarias redundantes

Objetivo: sacar vistas técnicas de navegación principal. Las funciones avanzadas pueden seguir existiendo en Herramientas o Configuración, pero Documento debe ser la raíz operativa.

### T82 — Release Candidate

Objetivo: validar app-image/MSI, smoke, documentación, guías de modelos, FFmpeg, hashes y experiencia de primer uso.
