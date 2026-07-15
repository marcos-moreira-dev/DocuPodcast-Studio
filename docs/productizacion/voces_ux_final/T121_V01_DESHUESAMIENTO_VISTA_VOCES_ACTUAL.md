# T121-V01 — Deshuesamiento de la vista Voces actual

## Objetivo

Retirar de la vista Voces todo lo que pertenece a Documento, a conceptos legacy o a una UX anterior. Esta tanda prepara el terreno para rehacer la pantalla sin arrastrar deuda.

## Elementos a eliminar de la vista Voces

- `Uso desde Documento`
- `Asignar al segmento seleccionado`
- `Personaje`
- `Personajes / roles`
- `Estilo` como combo de asignación directa
- `Estilos de interpretación` como lista principal
- Cualquier texto que diga que la asignación fina se hace desde Voces
- Cualquier referencia visible a `segmento`
- Cualquier referencia visible a `Coqui`
- Cualquier promesa de emociones si no existen muestras grabadas
- Cualquier opción avanzada mientras Piper esté activo

## Elementos que pueden conservarse temporalmente

- Lista de voces, solo como placeholder de biblioteca.
- Botones de importar/grabar, siempre que pasen a ser acciones de muestra.
- Estado de motor, si se renombra a `Voz IA avanzada`, `Voz local simple`, `Modo de prueba`.

## Resultado visual transitorio

Después de esta tanda, la vista puede quedar más simple, aunque todavía no final:

- título: `Biblioteca de voces`;
- subtítulo: `Administra voces y muestras para la lectura del documento`;
- lista de voces;
- detalle básico;
- acciones mínimas.

No importa si aún no tiene wizard final, pero no debe seguir mostrando asignación a fragmentos.

## Archivos probables

- `VoiceLibraryWorkspaceView.java`
- `voice-library.css`
- tests source de `presentation/voice`
- documentación T121

## Tests recomendados

- `VoiceLibraryNoFragmentAssignmentSourceTest`
- `VoiceLibraryNoCoquiVisibleSourceTest`
- `VoiceLibraryNoLegacyRolesSourceTest`
- `VoiceLibraryPiperMinimalSourceTest`

## Criterios de aceptación

- Voces ya no asigna al fragmento.
- Voces ya no muestra `segmento`.
- Voces ya no muestra personajes/roles como núcleo.
- Voces ya no dice `Coqui`.
- Piper no muestra opciones avanzadas.
