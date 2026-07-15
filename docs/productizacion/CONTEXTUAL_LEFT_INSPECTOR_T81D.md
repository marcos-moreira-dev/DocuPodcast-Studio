# Inspector izquierdo contextual — Contrato T81D

## Principio rector

El inspector izquierdo no es un panel de configuración general. Es una herramienta de operación contextual sobre el fragmento seleccionado del documento.

La composición base de la pantalla Documento queda así:

```text
[Inspector izquierdo] [Documento central] [Rail derecho de miniaturas/medios]
```

## Responsabilidades por zona

### Documento central

- Leer.
- Seleccionar oración, bloque o rango.
- Mostrar resaltado de lectura activa.
- Mantener la hoja limpia.

### Barra flotante

- Iniciar lectura global.
- Pausar.
- Reanudar.
- Detener.
- Refrescar fuente.

### Inspector izquierdo

- Operar la selección actual.
- Mostrar opciones por módulo.
- Mantener las acciones frecuentes cerca del usuario.

### Rail derecho

- Mostrar miniaturas y capas ya existentes.
- Navegar visualmente al texto asociado.
- No convertirse en formulario de edición.

## Módulos oficiales del inspector

### Detalles

Información mínima y humana:

- selección actual;
- bloque activo;
- rango exacto;
- contrato de solo lectura;
- ubicación conceptual del fragmento.

### Audio / Narración

Submodelo de origen:

```text
Voz IA
Audio del computador
```

Reglas:

- `Voz IA` habilita voz, generación de audio y emoción/estilo.
- `Audio del computador` habilita `Elegir audio…` y `Extraer audio de video…`.
- Emoción/estilo no aparece para audio del computador.
- Quitar audio asignado aplica a ambos orígenes.

### Imagen

Reglas:

- Imagen se maneja separada de audio.
- La imagen asignada alimenta mini storyboard.
- La asignación no modifica el documento fuente.

## Nomenclatura congelada

Usar:

```text
Voz IA
Audio del computador
Elegir audio…
Extraer audio de video…
Audio / Narración
```

No usar:

```text
voz con guía
efecto de sonido del computador
humano hablando del computador
audio ambiente como botón principal
guion de audio como menú normal
```

## Guardarraíles sugeridos

- `DocumentContextSidebarSourceTest`.
- `AudioNarrationSourceSelectorSourceTest`.
- `EmotionOnlyForAiVoiceSourceTest`.
- `MediaRailDoesNotContainAssignmentActionsSourceTest`.
- `NoHardcodedWorkspaceButtonsSourceTest`.

## Riesgos evitados

- Duplicar acciones en menu bar, toolbar y rail derecho.
- Hacer que el usuario entienda categorías técnicas de audio.
- Convertir el documento en cabina de producción.
- Mostrar configuración técnica donde el usuario solo quiere escuchar.
