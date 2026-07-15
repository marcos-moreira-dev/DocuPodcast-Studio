# Tanda 5 - Experiencia teatral operativa

## Alcance aplicado

Esta tanda empieza por proteger el flujo diario del workspace `Teatro > Guion` sin redisenar el ribbon ni extraer capacidades transversales. El foco de este corte es que los modulos laterales expliquen el estado real del proyecto cuando faltan fuente, actos, escenas, lectura preparada o intervenciones teatrales detectables.

## Cambio funcional

- `TheatreWorkspaceEmptyState` centraliza los mensajes de estado vacio del workspace teatral.
- `TheatreSceneFoldList` distingue entre proyecto sin fuente, fuente sin intervenciones teatrales y obra lista para crear el primer acto.
- Las escenas vacias ahora explican para que sirve crear una escena: texto inicial/final, personajes, objetos y posiciones.
- `TheatreTextualMapPanel` y `TheatreSpatialActionMapPanel` usan el mismo estado para los canvas de intervenciones.
- Cuando hay intervenciones detectables pero no lectura preparada, los mapas muestran aviso operativo: se puede ubicar texto, pero audio, paquetes IA y exportacion dependen de preparar la lectura.
- `TheatreCharactersPanel` y `TheatreObjectsPanel` usan los mismos mensajes para actos y escenas, evitando criterios distintos entre modulos del sidebar.
- `TheatreInterventionNavigator`, usado por `Generacion IA teatral`, queda alineado con los mismos estados: sin fuente, sin intervenciones, sin escenas y lectura no preparada.
- `TheatreInterventionSelectionBridge` sincroniza la intervencion activa desde la seleccion real del documento y enfoca la escena cuando se selecciona una intervencion desde mapas o generacion IA.

## Estado de capacidades

| Capacidad | Estado tras este corte |
| --- | --- |
| Sidebar teatral dedicado | Conservado; se mejora la claridad de estados vacios. |
| Actos y escenas | Operativo; mensajes guian creacion de estructura. |
| Mapa textual | Operativo; diferencia fuente ausente, fuente no teatral y lectura no preparada. |
| Mapa espacial y acciones | Operativo; comparte el mismo criterio de estado que mapa textual y recibe foco de escena al seleccionar intervenciones. |
| Personajes, objetos e imagenes | Conservados; sus listas de actos/escenas comparten los estados operativos del workspace teatral. |
| Generacion IA teatral | Conservada; el navegador de intervenciones usa los mismos estados operativos y la misma sincronizacion de seleccion que el workspace de guion. |
| Ribbon | Sin cambios; se mantiene para Tanda 8. |

## Pruebas

Ejecutado en este corte:

```powershell
mvn -q "-Dtest=TheatreWorkspaceEmptyStateTest,TheatreTextMapsSourceTest" test
```

Resultado: pasa.

Ejecutado tras sincronizar seleccion documento/intervencion/escena:

```powershell
mvn -q "-Dtest=TheatreInterventionSelectionBridgeTest,TheatreTextMapsSourceTest,TheatreAiGenerationModularWorkspaceSourceTest" test
```

Resultado: el primer intento agoto tiempo sin salida; repetido con mas margen, pasa.

Tambien ejecutado por impacto ampliado de seleccion:

```powershell
mvn -q "-Dtest=TheatreInterventionSelectionBridgeTest,TheatreWorkspaceEmptyStateTest,TheatreTextMapsSourceTest,TheatreCharactersModuleSourceTest,TheatreObjectsModuleSourceTest,TheatreSpatialSpeakerIndicatorSourceTest,TheatreAiGenerationModularWorkspaceSourceTest,TheatreImageGenerationWorkspaceSourceTest,TheatreFullFunctionalSurfaceTanda4SourceTest" test
```

Resultado: pasa.

Ejecutado tras alinear el navegador de intervenciones IA:

```powershell
mvn -q "-Dtest=TheatreAiGenerationModularWorkspaceSourceTest,TheatreImageGenerationWorkspaceSourceTest" test
```

Resultado: pasa.

Tambien ejecutado por impacto en side dock:

```powershell
mvn -q "-Dtest=TheatreWorkspaceEmptyStateTest,TheatreTextMapsSourceTest,TheatreCharactersModuleSourceTest,TheatreObjectsModuleSourceTest,TheatreSpatialSpeakerIndicatorSourceTest,TheatreFullFunctionalSurfaceTanda4SourceTest" test
```

Resultado: pasa.

Validacion combinada final de la superficie tocada:

```powershell
mvn -q "-Dtest=TheatreInterventionSelectionBridgeTest,TheatreWorkspaceEmptyStateTest,TheatreTextMapsSourceTest,TheatreCharactersModuleSourceTest,TheatreObjectsModuleSourceTest,TheatreSpatialSpeakerIndicatorSourceTest,TheatreAiGenerationModularWorkspaceSourceTest,TheatreImageGenerationWorkspaceSourceTest,TheatreFullFunctionalSurfaceTanda4SourceTest" test
```

Resultado: pasa.

Validacion completa del repositorio, reejecutada tras sincronizar seleccion:

```powershell
mvn -q test
```

Resultado: pasa.
