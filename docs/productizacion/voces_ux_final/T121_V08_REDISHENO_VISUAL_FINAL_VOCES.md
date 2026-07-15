# T121-V08 — Rediseño visual final de VoiceLibraryWorkspaceView

## Objetivo

Rehacer la vista Voces con una estructura moderna, limpia, sobria y coherente con el look and feel existente.

## Layout

### Panel izquierdo

Ancho contenido y fijo/ajustable.

Secciones:

1. Motor de voz.
2. Biblioteca de voces.
3. Acciones rápidas.

Ejemplo:

```text
Motor de voz
[Voz IA avanzada] Recomendado
[Voz local simple] Básico
[Modo de prueba] Sin voz real

Voces
- Voz neutral prediseñada
- María
- Juanito

[Crear voz]
[Importar muestra]
[Grabar muestra]
```

### Panel derecho

Amplio, con scroll interno.

Secciones:

1. Encabezado de voz.
2. Estado.
3. Muestras de referencia.
4. Prueba generada.
5. Acciones destructivas/secundarias.

## Tarjeta de voz

Debe mostrar:

- nombre;
- tipo;
- estado;
- cantidad de muestras;
- protección;
- motor visible.

Ejemplo:

```text
María
Voz IA avanzada · 8/31 muestras
Lista
```

## Tarjeta de muestra

Cada muestra:

```text
Feliz
maria-feliz.wav
Lista

[Reproducir] [Reemplazar] [Descargar] [Eliminar]
```

Si pendiente:

```text
Heroica
Pendiente

[Grabar] [Importar]
```

## Estados

- Lista.
- Incompleta.
- Sin muestra neutral.
- Pendiente.
- Protegida.
- No disponible en este motor.
- Modo de prueba.

## Estilo

- moderno;
- limpio;
- sobrio;
- tarjetas suaves;
- buen espaciado;
- botones consistentes;
- badges discretos;
- sin saturación visual.

## Prohibiciones

- No `script-title`.
- No `script-summary`.
- No `script-segment-text`.
- No `setStyle`.
- No botones estilizados manualmente.
- No `Coqui` visible.
- No asignar segmento.

## Tests recomendados

- `VoiceLibraryFinalLayoutSourceTest`
- `VoiceLibraryNoLegacyScriptStylesSourceTest`
- `VoiceLibraryNoCoquiVisibleSourceTest`
- `VoiceLibraryHasSampleActionsSourceTest`

## Criterios de aceptación

- La vista se siente final.
- La vista es clara.
- La vista no mezcla Documento.
- La vista no parece placeholder.
