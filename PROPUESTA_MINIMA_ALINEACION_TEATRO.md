# Propuesta mínima de alineación teatral

Esta propuesta no rediseña las funciones existentes ni implica implementación en esta auditoría. Su objetivo es unir los contratos actuales con el menor cambio conceptual posible.

## Principio

Mantener como fuente interna de verdad a `TheatreProjectLayer` y conservar:

- `TheatreGrammarMarkdownParser` como parser humano;
- `JsonTheatrePackageScanner` como validador seguro de archivos;
- `TheatreProjectJsonCodec` como persistencia nativa;
- los paneles teatrales actuales como editores del mismo modelo.

No crear un segundo modelo teatral paralelo.

## Paquete mínimo propuesto

```text
PROYECTO_TEATRO/
  obra.teatro.md
  docupodcast-theatre.json
  assets/
    personajes/
    objetos/
    fondos/
    mapas/
    intervenciones/
    puentes/
    audio/
    voces/
```

En la primera versión, carpeta y ZIP deben representar exactamente el mismo contenido. El ZIP se extraería a un staging temporal seguro y, desde allí, usaría el mismo importador de carpeta.

## Alineación mínima por fases

### 1. Un coordinador de importación atómica

Crear un único caso de uso de aplicación que:

1. localice exactamente un `obra.teatro.md` y un `docupodcast-theatre.json`;
2. parsee ambos sin modificar el proyecto;
3. resuelva cada ruta Markdown contra el inventario SHA-256 del manifiesto;
4. valide todas las referencias del grafo;
5. produzca un `TheatreImportPlan` ampliado y un informe;
6. solo confirme el proyecto y copie assets si no hay errores bloqueantes.

El coordinador debe reutilizar `TheatreImportUseCase` y `ReconcileTheatreProjectUseCase`, no duplicar sus transformaciones.

### 2. Conectar el materializador existente al flujo público

`GrammarWorkflowCoordinator` debe dejar de aplicar manualmente solo fichas/actos/escenas y delegar la materialización completa. Antes de hacerlo hay que corregir las equivalencias espaciales y volver bloqueantes las referencias críticas no resueltas.

### 3. Extender la gramática solo para estados hoy imposibles

Agregar, con versión nueva de gramática, los mínimos campos tipados:

- `presentes` o ubicaciones completas por personaje;
- `entrada` / `salida`;
- `mirada` / `orientacion_personaje`;
- `objetos_presentes` y operaciones `toma`, `porta`, `suelta`, `entrega`;
- `variante_visual` o `vestuario` activo;
- `hereda_de` o, preferiblemente, `heredar_estado: true` con overrides explícitos.

No convertir notas libres existentes en semántica implícita.

### 4. Añadir tipos mínimos al dominio

La opción de menor impacto es incorporar a cada intervención un estado explícito compuesto, sin reemplazar las colecciones actuales:

- `CharacterStageState(characterId, present, location, orientation, gazeTarget, visualVariantId)`;
- `ObjectStageState(objectId, present, location, holderCharacterId, manipulation)`;
- `InterventionStageState(interventionId, characters, objects, inheritedFromInterventionId)`.

Los campos actuales `TextActionPlacement`, `SpatialPosition` y `TheatreAction` pueden seguir siendo vistas/editorial helpers mientras se migra.

### 5. Definir una conversión canónica de nueve zonas

Crear un catálogo único zona ↔ coordenadas normalizadas y usarlo en GUI, importador y render. No guardar la zona en `notes`. Debe distinguir “fuera de escena” de una coordenada válida.

### 6. Snapshot canónico

Añadir un resolutor puro:

```text
resolve(project, interventionId) -> TheatreInterventionSnapshot
```

El resultado debe incluir escena, fondo, cámara, hablante, interlocutores, coro, personajes presentes, ubicaciones, mirada, vestuario, objetos, portadores, acciones, voz, tono y assets resueltos. Debe emitir errores si falta una referencia y no consultar IA.

La GUI, los generadores visuales y los exportadores deberían consumir ese snapshot gradualmente. `TheatreVisualGenerationContext` puede derivarse de él.

### 7. Exportación simétrica

Implementar después un exportador del proyecto actual a `obra.teatro.md + docupodcast-theatre.json + assets/`. La prueba de aceptación debe ser:

```text
proyecto A -> paquete -> proyecto B -> snapshots(A) == snapshots(B)
```

Comparar por contenido canónico y SHA-256, no por timestamps ni orden accidental de mapas.

## Validación mínima obligatoria

Errores bloqueantes:

- ID duplicado;
- personaje, escena, voz, objeto, cámara o asset no resuelto;
- ruta absoluta, traversal o symlink fuera del root;
- coordenada fuera de rango;
- estado imposible: personaje ausente que habla o porta un objeto;
- objeto con dos portadores;
- entrada/salida contradictoria;
- herencia cíclica;
- propiedad desconocida en una versión estricta de gramática;
- archivo declarado con hash/tamaño distinto.

Advertencias:

- visual opcional ausente;
- vestuario heredado más allá de un límite de escena;
- interlocutor especial fuera de escena;
- tono sin muestra específica cuando existe fallback neutral explícito.

## Criterio de “listo”

La función estará lista cuando una carpeta y su ZIP equivalente produzcan el mismo proyecto, todos los snapshots sean completos, una exportación se pueda reimportar sin pérdida y el proceso no use IA ni dependa del orden de recorrido del sistema de archivos.
