# Roadmap post T81C — Frontend de documento narrado

## Estado luego de T81C

El cerebro V1 permanece congelado desde T80. La navegación principal ya fue limpiada en T81A. El documento central fue limpiado en T81B. T81C agrega la barra flotante de lectura global sobre la hoja, usando componentes GUI transversales.

## Siguiente tanda: T81D — Sidebar izquierdo contextual

Objetivo: reemplazar el antiguo SideDock técnico por un inspector contextual permanente y compacto para la oración o fragmento seleccionado.

Módulos previstos:

```text
Detalles
Audio / Narración
Imagen
```

### Detalles

Debe mostrar:

- texto de la oración o fragmento;
- ubicación aproximada;
- estado de audio/imagen/capas;
- sección técnica colapsada si hace falta.

### Audio / Narración

Debe mostrar un selector de origen:

```text
Voz IA
Audio del computador
```

Si el usuario elige `Voz IA`, se habilitan voz predeterminada, elegir voz, agregar voz, generar audio y emoción/estilo.

Si el usuario elige `Audio del computador`, se muestran acciones directas:

```text
Elegir audio…
Extraer audio de video…
Quitar audio asignado
```

La emoción/estilo no debe mostrarse para audio externo ya grabado.

### Imagen

Debe permitir:

- elegir imagen;
- reemplazar imagen;
- quitar imagen;
- ir a miniatura si existe asignación.

## T81E — Sidebar derecho de miniaturas / medios

Objetivo: crear un rail visual de imágenes/medios asociados o pendientes. Cada tarjeta debe mostrar imagen, fragmento relacionado y descripción corta. Al hacer clic, el documento debe navegar al fragmento.

## T81F — Toolbar con iconos y grupos

Objetivo: simplificar la toolbar superior. Eliminar apariencia de ribbon técnico. Usar iconos y grupos:

```text
Documento
Lectura
Vista
Exportar
```

No deben aparecer como botones permanentes:

- Narración avanzada
- Voces
- Audio
- Storyboard
- Jobs
- Diagnóstico

## T81G — Retiro/degradación de vistas secundarias redundantes

Objetivo: sacar de la navegación principal vistas que ya no deben competir con Documento. El cerebro se conserva; la superficie visual se degrada a avanzada o se elimina si queda duplicada.

## T82 — Release Candidate

Objetivo: empaquetado, validación final, scripts de instalación, guía y smoke final.
