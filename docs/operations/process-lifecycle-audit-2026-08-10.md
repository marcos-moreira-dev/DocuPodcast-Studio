# Auditoría de lifecycle de procesos — 2026-08-10

Estado del lector PDF semántico: **FROZEN = YES**. Esta intervención no cambia Block V1,
prompts, validadores, umbrales, recuperación, perfil 8K/Q8/Flash ni concurrencia Qwen.

## AA. Árbol observado en desarrollo

Durante un smoke iniciado por `scripts/01-ejecutar-app.bat --smoke` se observó este árbol:

```text
cmd.exe (script .bat)
└─ powershell.exe (DocuPodcast.Tasks.ps1)
   └─ cmd.exe (mvn.cmd)
      └─ java.exe (JVM de Maven)
         └─ java.exe (StudioLauncher / JavaFX)
```

Los cuatro primeros niveles pertenecen únicamente al lanzamiento de desarrollo. No deben ser
terminados por código de producto. En una distribución instalada el proceso principal es la JVM
del launcher, sin el árbol `cmd → PowerShell → Maven`.

El primer comando de smoke excedió el límite externo de 120 s mientras todavía atravesaba el
árbol de build/launch. Se resolvieron sus PIDs exactos y se cerró solo ese árbol de prueba. Un
segundo smoke ya compilado terminó normalmente en 10,608 s; 35 s después no quedaba ningún PID
del árbol. Tras añadir el fallback de `Application.stop()`, el smoke final volvió a terminar en
12,232 s, registró `Application lifecycle closed cleanly` y dejó cero PIDs del Studio.

## AB. Procesos owned por DocuPodcast

| Tipo | Propietario | Modalidad | Terminación esperada |
|---|---|---|---|
| Ollama privado | `ManagedOllamaProcess` | residente | cierre de plataforma o stop explícito |
| XTTS worker | `XttsBatchWorker` | residente durante un lote | fin/cancelación del lote o shutdown |
| Piper/XTTS script | `LocalProcessExecutor` | one-shot | fin, cancelación o timeout |
| FFmpeg | `LocalProcessExecutor` | one-shot | fin, cancelación o timeout |
| PP-Structure/Python | `PpStructureV3Engine` | one-shot | fin, cancelación o timeout |
| ComfyUI administrado | `ComfyUiManagedProcess` | residente | stop explícito o shutdown |
| ComfyUI ya existente | externo/adoptado | no owned | nunca se termina desde Studio |
| helpers de aplicación | `DefaultExternalProcessRunner` | one-shot | fin, cancelación o timeout |
| curl de instalación | `SafeRuntimeOperations` | one-shot | fin, cancelación o timeout |

Cada inicio registra PID, parent PID, ejecutable sin argumentos sensibles, owner, timestamp,
condición de terminación y mecanismo de contención. No se adopta ownership a partir del nombre.

## AC. Procesos vivos y motivo

En la inspección idle posterior no había `java`, `javaw`, `ollama`, `llama-server`, `piper`,
`python` ni `ffmpeg` del Studio. Solo estaba vivo el PowerShell interno del entorno Codex, que no
pertenece a DocuPodcast y no fue tocado. Durante el smoke sí estaban vivos los cinco niveles del
árbol de desarrollo porque el comando seguía activo; no eran un leak de medios.

Ollama puede permanecer vivo mientras DocuPodcast está abierto y una residencia de modelo está
retenida. Esto se registra como `process.resident ... reason=MODEL_RUNTIME`, no como leak.

## AD. Riesgos/leaks corregidos

- El cierre global antes recorría únicamente motores de análisis. Ahora cierra, por identidad y
  una sola vez por instancia, motores de voz, imagen, refinamiento, video, análisis y administración.
- ComfyUI administrado participa en el shutdown; un ComfyUI externo sigue intacto.
- Los procesos one-shot usan Job Object en Windows. Al terminar el script padre se eliminan hijos
  exclusivamente contenidos en ese job.
- Un fallo durante el constructor del worker XTTS ya no puede dejar Python vivo.
- El probe de `curl` ya no queda vivo si vence su timeout.
- `Application.stop()` ejecuta el mismo coordinator idempotente que el cierre normal de ventana.

## AE. Después de cancelar

Cancelar un job marca su token, cierra el stream HTTP Qwen, libera el request/lease marginal y
termina el proceso one-shot exacto con `destroy()`, grace period y `destroyForcibly()` solo como
último recurso. No descarga automáticamente Ollama si el lote/documento conserva residencia.

Prueba física Windows: un PowerShell owned cancelado terminó (`reason=CANCELLED`, `alive=false`),
mientras otro PowerShell externo creado por la prueba continuó vivo. La limitación de Ollama sigue
siendo física: cerrar el stream cancela el consumidor HTTP, pero Ollama 0.32.5 no ofrece en este
contrato un endpoint separado y fiable para abortar por request ID; el runtime se conserva salvo
shutdown o muerte categorizada.

## AF. Después de cerrar

La secuencia es: dejar de aceptar trabajo, solicitar cancelación, espera acotada, persistencia,
cierre inverso de participantes, cierre de engines/runtimes y salida JavaFX. El coordinator es
idempotente. El smoke controlado terminó sin árbol residual; la prueba de plataforma confirmó el
cierre de un runtime de administración owned.

## AG. CPU/RAM/VRAM

Snapshot idle inicial: RAM total 31,83 GiB; usada 16,17 GiB; libre 15,66 GiB; GTX 1650 con
17 MiB/4096 MiB dedicados y 0 % de uso. No había proceso del Studio al que atribuir CPU/RAM.
Después del smoke final: 16,21 GiB usados, 15,62 GiB libres, 17 MiB/4096 MiB de VRAM y 0 % GPU;
no quedó ningún proceso DocuPodcast.

No se ejecutó otra inferencia física larga para este cierre. Cada request Qwen registra ahora heap
JVM usado/committed, RAM física libre/total, memoria virtual comprometida del proceso, `/api/ps`,
VRAM efectiva reportada por Ollama, contexto, tokens y tiempos. Si una fuente no existe se registra
como no disponible; nunca se inventa VRAM libre ni working set.

## AH. Contexto independiente por página

Cada llamada genera un `requestId` nuevo y usa ese mismo valor como `contextId`; el request contiene
una sola entrada `messages[0]` de rol `user`. Los diagnósticos fijan `historyPages=0` y
`freshContext=true`. La prueba secuencial verifica que P1 y P2 tienen context IDs distintos.

## AI. Residencia compartida

`modelResidencyKey` deriva de engine, modelo Q8, perfil `q8_0-kvq8-flash` y dispositivo, no del
request. La prueba secuencial verifica context IDs distintos con la misma residency key. Ollama
permanece residente dentro del batch y se registra como tal.

## AJ. Formato de consola

```text
process.start kind=OLLAMA pid=... parentPid=... executable=ollama.exe arguments=1 startedByDocuPodcast=true owner=ManagedOllamaProcess ...
process.resident kind=OLLAMA pid=... owner=ManagedOllamaProcess reason=MODEL_RUNTIME
[PDF][P2] semantic cache MISS action=STARTED reason=MANUAL_RETRY_AFTER_TECHNICAL_FAILURE
[PDF][P2] AI START requestId=... contextId=... historyPages=0 freshContext=true modelResidencyKey=... context=8192 ...
[PDF][P2][AI] working elapsedSeconds=45 chunks=... backendAlive=true
[PDF][P2] AI END requestId=... reason=... outputTokens=... requestContextReleased=true ...
process.stop kind=OLLAMA pid=... reason=RUNTIME_STOP exitCode=... elapsedMs=... alive=false
```

El heartbeat se emite cada 45 s como máximo. `chunks` solo representa actividad recibida; no se
presenta como token count.

## AK. Secuencia P1 → P2 → P3

La clasificación persistida produce este flujo verificable en la próxima ejecución física:

```text
[PDF][P1] semantic cache HIT action=SKIPPED reason=CANONICAL_CACHE_HIT
[PDF][P2] semantic cache MISS action=STARTED reason=MANUAL_RETRY_AFTER_TECHNICAL_FAILURE
[PDF][P3] semantic cache MISS action=STARTED reason=MANUAL_RETRY_AFTER_SEMANTIC_REJECTION
```

P1 cache HIT quedó observado en la regresión integrada. Las razones P2/P3 quedaron verificadas con
estados persistidos simulados en prueba unitaria. No se etiqueta este bloque como captura de una
nueva inferencia física P1→P3: la aplicación estaba cerrada y no se inició trabajo Qwen costoso sin
necesidad. La próxima ejecución real producirá las mismas líneas con PID, memoria y tiempos reales.

La UI recibe mensajes compactos: `Página N · ya preparada`, `Página N · iniciando análisis` o
`Página N · reintentando análisis`; los detalles técnicos permanecen en consola.

## AL. Procesos externos

Confirmado: no existe kill por nombre para `java.exe`, `python.exe`, `powershell.exe`, `cmd.exe` ni
`explorer.exe`. Los stops operan sobre el objeto `Process` creado por DocuPodcast, su ProcessHandle
y, en Windows, el Job Object al que ese proceso fue adjuntado. PID files son solo diagnóstico: un
PID stale o reutilizado nunca autoriza terminar un proceso.

## Pruebas

- Ownership/aislamiento/cancelación/contención Windows: 9 tests, 0 fallos.
- Lifecycle, shutdown global y regresión semántica focal: 10 tests, 0 fallos.
- Suite Maven completa: 1294 tests, 0 fallos, 0 errores, 15 opt-in omitidos.
- Regresión Word focal: 16 tests, 0 fallos, 0 errores.
- Revalidación posterior del runner/lifecycle/retry: 7 tests, 0 fallos.
- `git diff --check`: exit 0; solo avisos esperados LF→CRLF del working tree. El alcance
  modificado no contiene whitespace final.
- Inspección final: ningún proceso DocuPodcast encontrado.
