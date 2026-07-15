# Tanda 7 - Visuales Transversales y Motor de Imagen IA

## Alcance

Esta tanda consolida la generacion visual local como capacidad transversal y corrige el flujo operativo del motor local de imagen IA. No cambia schema, `TheatreProjectLayer`, comandos/ribbon, modelos pesados, descargas ni ubicacion de runtime.

La regla implementada es:

- `application.visual` gobierna el cliente ComfyUI comun.
- Teatro conserva el contexto propio: unidad teatral, prompt, assets de referencia, cola por intervencion, aprobacion y asignacion teatral.
- El respaldo es solo lectura y no se copia codigo desde alli.

## Comparacion Con Respaldo

Referencia consultada:

- `C:\Users\MARCOS MOREIRA\Downloads\docupodcast studio respaldo estudiar`
- Componentes revisados: `LocalTheatreImageEngineManager` y `TheatreImageGenerationWorkflow`.

Resultado:

- El respaldo tiene los mismos componentes base que ya existian en `G`.
- No habia una version funcional perdida del motor local para copiar.
- La reparacion se hizo sobre el codigo actual de `G`, extrayendo comportamiento repetido hacia `application.visual`.

## Diagnostico Inicial Del Motor

Estado observado antes de la implementacion:

- Endpoint configurado: `http://127.0.0.1:8188`.
- El endpoint no respondia durante la planificacion.
- Runtime, checkpoint y workflow local existen en la instalacion actual segun auditoria previa.
- Logs previos mostraban que ComfyUI genero imagenes antes; el problema estaba en arranque, espera, diagnostico y cliente duplicado, no en una copia perdida del respaldo.

## Contrato Transversal

### `VisualEngineRequest`

Solicitud comun para una imagen:

- `prompt`
- `negativePrompt`
- `checkpointName`
- `steps`
- `cfg`
- `batchSize`
- `targetWidth`
- `targetHeight`
- `outputDirectory`
- `filenamePrefix`

Normaliza dimensiones y limita la generacion base para ComfyUI a una relacion segura; si la salida final pide 1080p, 2K o 4K, el cliente descarga PNG real y lo redimensiona tecnicamente con Java2D.

### `VisualEngineResult`

Resultado comun:

- `outputPath`
- `width`
- `height`
- `promptId`
- `diagnostic`

### `ComfyUiVisualEngineClient`

Cliente comun para:

- `test`: prueba `/system_stats`.
- `generate`: orquesta prueba de conexion, `/prompt`, espera `/history`, descarga `/view`, validacion PNG y resize.
- `waitForOutput`: espera salida de ComfyUI por `prompt_id`.
- `downloadPng`: descarga y valida PNG real.

Este cliente reemplaza el cliente duplicado que estaba en presentacion.

## Adaptador Teatral

`TheatreImageGenerationWorkflow` sigue siendo responsable de:

- planificar unidades teatrales;
- armar prompt teatral;
- seleccionar assets de contexto;
- bloquear presets sin workflow incorporado;
- importar candidato generado al catalogo de assets;
- aprobar o no aprobar la asignacion visual a una intervencion.

Ahora delega HTTP, `/prompt`, `/history`, `/view`, PNG, resize, retry y timeout a `ComfyUiVisualEngineClient`.

`TheatreFrameGenerationWorkflow` usa el mismo cliente comun para generacion de frames.

## Reparacion Del Arranque Local

`LocalTheatreImageEngineManager` ahora:

- lanza en Windows con `cmd /s /c start "" /min "<launcher>" ...`, preservando rutas con espacios;
- espera readiness con `min(240s, max(60s, image.timeoutSeconds))`;
- mantiene timeout corto solo para pruebas puntuales de endpoint;
- reporta diagnostico con `baseUrl`, launcher usado y salida capturada cuando existe;
- distingue entre fallo real de arranque y motor iniciado que aun no termina de responder.

## Decisiones

- `application.visual` queda como familia comun de Visuales para Documento/Narrativa/Teatro.
- Teatro no recibe motor propio nuevo.
- `TheatreImageGenerationPreset` y `TheatreImageAspectRatio` se conservan como compatibilidad teatral; el adaptador traduce hacia `VisualEngineRequest`.
- No se redisenan ribbon ni exportacion en esta tanda.
- No se cambia persistencia ni assets.

## Pruebas

Resultado final de la tanda:

- PASS: `mvn -q "-Dtest=ComfyUiVisualEngineClientTest,LocalTheatreImageEngineManagerSmokeTest,TheatreImageGenerationWorkflowTest,ComfyUiClientAspectRatioSourceTest,VisualTransversalTanda7SourceTest" test`
- PASS: `mvn -q "-Dtest=LocalTheatreImageEngineManagerSmokeTest,InspectLocalTheatreImageSetupReadinessUseCaseTest,ImageEnginePresetSupportPolicyTest,ImageEngineSmokeImageStoreTest,TheatreImageGenerationWorkflowTest,TheatreImageGenerationWorkspaceSourceTest,TheatreAiGenerationModularWorkspaceSourceTest,BuildVisualProductionProjectionUseCaseTest,TheatreFragmentVisualAssignmentServiceTest,TheatreImageEngineProductizationSourceTest,VisualTransversalTanda7SourceTest,ComfyUiVisualEngineClientTest" test`
- PASS: `mvn -q test`

Cobertura agregada o reforzada:

- `ComfyUiVisualEngineClientTest`: cliente comun genera contra servidor ComfyUI falso, descarga PNG real y redimensiona a la salida final.
- `LocalTheatreImageEngineManagerSmokeTest`: smoke PNG, perfiles/aspect ratio, workflow faltante, comando detached con rutas Windows con espacios y espera de readiness por configuracion.
- `VisualTransversalTanda7SourceTest`: protege que Visuales gobierna ComfyUI, Teatro delega HTTP al cliente comun y el cliente antiguo de presentacion no reaparece.

## Smoke Manual

Comando no destructivo ejecutado:

`Invoke-WebRequest -Uri 'http://127.0.0.1:8188/system_stats' -TimeoutSec 4 -UseBasicParsing`

Resultado:

`FAIL System.Net.WebException: No es posible conectar con el servidor remoto`

Interpretacion:

- ComfyUI no esta levantado en el endpoint local durante el cierre de la tanda.
- No se inicio descarga ni se modificaron modelos.
- La reparacion funcional queda validada con servidor falso y pruebas de contrato; el arranque real debe pasar por Configuracion/Iniciar motor en una sesion con runtime disponible.
