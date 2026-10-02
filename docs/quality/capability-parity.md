# Matriz de paridad funcional

Esta matriz relaciona las capacidades observadas en la referencia de solo lectura `g - copia` con la ruta productiva refactorizada. La auditoria generada se escribe en `target/parity/reference-capabilities.md`; nunca se ejecutan Maven, scripts ni la aplicacion dentro de la referencia.

## Auditoría de Estudio, video y exportación — 29 de julio de 2026

| Superficie de la referencia | Clasificación | Implementación actual | Evidencia de cierre |
|---|---|---|---|
| Contenido del video documental | Capacidad conservada | `DocumentStudyVideoPanel` y `BuildDocumentStudyVideoPlanUseCase` | navegación visible “Editar selección”, persistencia de configuración y pruebas del plan |
| Ajustes del video por proyecto | Capacidad conservada, acceso restaurado | `DocumentStudyVideoSettingsPanel` dentro del módulo documental | botón rotulado, encabezado dinámico y estado activo durante la sesión |
| Video final global | Regresión real restaurada | página `VIDEO_FINAL` de `SettingsDialog` | resolución, duración silenciosa, preferencia por runtime administrado, rutas y versiones FFmpeg/FFprobe, codificador efectivo |
| Instalación de FFmpeg | Reemplazo arquitectónico | `EngineAdministrationPane` y administrador FFmpeg | importar, reparar, verificar y smoke; no se reintroducen campos de destino libres |
| Centro de exportaciones | Capacidad conservada | `ExportCenterCoordinator` y `ExportExecutionRoute` | seis rutas creativas únicas hacia operaciones concretas del `ExportController` |
| Video documental texto+audio | Capacidad conservada | `ExportController.exportDocumentStudyTextAudioVideo` | DOCX-only, timeline real y smoke FFmpeg con imagen, texto, WAV y validación FFprobe |
| Video narrativo | Capacidad conservada | operación `NARRATIVE_VIDEO` | ruta visible → comando → controlador → MP4 |
| Obra, mapa y porción teatrales | Capacidades conservadas | operaciones teatrales del `ExportController` | tres rutas MP4 distintas y pruebas de catálogo |
| Cinco módulos teatrales | Capacidad conservada | `TheatreSideDock` sobre `SideDockHost` | fragmentos, personajes, mapa textual, mapa espacial y objetos |
| Muestras oficiales XTTS | Regresión real restaurada | `ApplicationRuntimeRoots` y `VoiceReferenceSamplePathResolver` | instalación, runtime y proyecto se resuelven por raíces distintas; nunca desde `user.dir` |
| PDF V1 | Retirada intencional | PDF V2 por manifest y página | no se restaura |
| Tinta duplicada | Traslado de módulo | `studio-ink` | no se restaura en el módulo monolítico |
| Operaciones administrativas antiguas | Reemplazo arquitectónico | fachada neutral de capacidades | no se restauran helpers ni botones desconectados |

La comparación automatizada también exige igualdad de los identificadores de comandos, de los
61 handlers registrados en el shell y de los identificadores de módulos laterales. Cada clase Java
que existe solo en la referencia debe quedar clasificada como retirada intencional, traslado o
reemplazo; una diferencia sin clasificar hace fallar la auditoría.

| Capacidad | Ruta productiva actual | Evidencia determinista | Evidencia real/local |
|---|---|---|---|
| Importar DOCX, PDF, TXT y Markdown | `DocumentSourceImportService` y gateways documentales | tests de importadores y fixtures | apertura manual de muestras |
| PDF nativo y escaneado/OCR | gateway PDF + provisionamiento documental; no `EngineRegistry` | tests de parser, locator e importacion | Tesseract cuando runtime e idiomas estan presentes |
| Voz Piper | `VoiceSynthesisEngine` + jobs neutrales | contract/fakes y catalogo administrativo | WAV mediante `04-smoke-capacidades-reales.bat` |
| Voz XTTS individual y batch | `VoiceSynthesisEngine.synthesize/synthesizeBatch` | contract/fakes, texto auditable y lote optimizado | unidad y matriz de puntuacion mediante script 04; la escucha humana se registra aparte |
| Playback | controlador de reproduccion y servicios estrechos | tests de reproduccion/controladores | recorrido de shell |
| Jobs, cancelacion y staging | `GenerationJobService` + repositorio/scheduler inyectados | tests de estados, recursos y cancelacion | artefactos de smokes reales |
| Guardado y reapertura v1 | codecs JSON por seccion | golden tests y round-trip de los tres tipos | reapertura manual de muestras |
| Recuperacion tras cierre | `GenerationJobRecoveryService` | repositorios temporales y estados recuperables | smoke de proceso en verificacion completa |
| Imagen local | `ImageGenerationEngine` ComfyUI | contract/fake, preset neutral y rechazo explicito de conditioning no soportado | imagen real preset por preset si modelo/workflow estan presentes |
| Interpolacion RIFE | servicio auxiliar del adaptador ComfyUI, fuera de `EngineRegistry` | preflight de nodo/modelo, dimensiones A/B y rol central | frame central real mediante `--engine=rife` |
| Video generativo y continuidad | `VideoGenerationEngine` separado del render | contract/fake, presets y artefactos | clip + frame cuando un workflow instalado esta presente |
| Render Documentary | `VideoTimelinePlan` + `VideoRenderEngine` | plan documental y contract FFmpeg | render corto FFmpeg y smoke de producto |
| Render Theatre | mismo timeline neutral, politica Theatre | tests de planes limpio/espacial/obra | render corto FFmpeg y smoke de producto |
| Render Narrative | mismo timeline neutral, politica Narrative | tests de plan narrativo | render corto FFmpeg y smoke de producto |
| Tinta: problema, parrafo, composicion y frame | `studio-ink`, cuatro perfiles, `InkCanvasViewport` e `InkEditorSession` | presion cruda/normalizada, coordenadas dinamicas, segundo mosaico, historial, restauracion, zoom, exportacion y cierre | LectureStudio/Windows Pointer tienen prioridad; mouse/touch permanecen como respaldo segun `InkInputPolicy` |
| Administracion de motores | administradores Piper, XTTS, imagen, video y FFmpeg | prueba de catalogo y confinamiento | instalar/importar/start/stop/reparar/smoke desde Configuracion |
| Arranque | `studio-launcher` y roots explicitos | `01-ejecutar-app.bat --smoke` | ventana real mostrada antes del cierre limpio |

## Inventario de activos

La evidencia del 21 de julio de 2026 confirma igualdad de ruta y tamano con la referencia:

- `models`: 29 archivos, 38.207.929.115 bytes.
- `tools`: 84.465 archivos, 62.148.182.612 bytes.
- `runtime`: 9 archivos, 232.474 bytes.
- `voice-library`: 6 archivos, 1.530.264 bytes.

Los hashes coinciden para Piper, FFmpeg, el helper XTTS individual y el workflow de imagen. El helper XTTS batch difiere deliberadamente de la referencia: omite `--device` cuando la autodeteccion no aporta valor y esa correccion esta cubierta por el smoke batch real. Los workflows WAN/LTX no estaban en la referencia y se modelan honestamente como recursos instalables: la ruta productiva existe, pero permanece `resource missing` hasta importarlos desde Configuracion.

## Hallazgos de la auditoria post-refactoring

- La aplicacion legacy podia convertir una elipsis en la palabra `pausa` y devolver literalmente `Pausa breve.` cuando el texto quedaba vacio. Ambas inserciones se eliminaron. Una unidad vacia se marca `SKIPPED` y no llega al motor.
- XTTS conserva puntos, elipsis, decimales e interrogaciones. Si una escucha humana detecta “pausa breve” sin que aparezca en `payload-neutral.json`, se tratara como defecto acustico reproducible del adaptador; solo entonces se ajustara el punto terminal concreto.
- Los presets Theatre ya no colapsan todos a `draft`. SD 1.5, DreamShaper, SDXL, Flux y custom conservan su identificador neutral y publican disponibilidad por recurso.
- La prueba y la ejecucion productiva de Theatre resuelven el motor por `ImageGenerationEngine`, pasan por `GenerationJobService` y usan `EngineAdministrationRegistry` para start/smoke/stop. La vista y el bootstrap ya no componen el cliente ComfyUI directo.
- El descriptor Flux historico no se considera workflow ejecutable. El adaptador construye el workflow Flux solo cuando modelo, CLIP-L, T5 y VAE estan presentes.
- Las referencias teatrales no se descartan silenciosamente: mientras no exista un workflow condicionado compatible, la operacion termina `RESOURCE_MISSING`.
- El proceso ComfyUI respeta el perfil efectivo: `SAFE_LOW_VRAM` usa `--lowvram`, `VRAM_RAM_OFFLOAD` usa `--novram`, `HIGH_MEMORY` usa `--highvram` y `NORMAL` no fuerza flag.
- WAN/LTX siguen sin workflows en ambos arboles y, por tanto, no tienen evidencia real. RIFE se conserva como preparacion multimedia auxiliar y se certifica independientemente.

Cada ejecucion escribe evidencia bajo `target/certification/<timestamp>`: matriz declarada, payload neutral, manifest por preset, log, firma de reanudacion, artefactos y copia del reporte de integridad de la referencia. Los estados validos son `PASS`, `FAIL`, `RESOURCE_MISSING`, `NOT_INSTALLED`, `CANCELLED` e `INCONCLUSIVE`; solo `PASS` es aprobacion.

## Certificacion local del 22 de julio de 2026

El entrypoint `04-smoke-capacidades-reales.bat` produjo artefactos reales mediante las mismas rutas neutrales usadas por el launcher:

- Piper: WAV.
- XTTS: WAV individual y seis WAV de auditoria en batch —sin puntuacion, punto terminal, dos oraciones, elipsis, decimal y control literal— con autodeteccion de dispositivo.
- ComfyUI imagen: PNG 512x512 tras start/readiness/generacion/stop administrados.
- RIFE: frame central real despues de validar nodo, modelo y contraste del artefacto; permanece como capacidad auxiliar.
- FFmpeg: MP4 de timeline neutral; la captura concurrente y acotada evita bloquear procesos verbosos.
- Tesseract: lectura de PDF nativo y OCR de un PDF escaneado generado para la prueba.
- Tinta: sesiones, presion sintetica y proveedores se verificaron sin requerir hardware fisico.

El video generativo local no se marco como certificado: ninguno de los workflows WAN/LTX requeridos existe en la copia de referencia ni en el layout actual. La UI debe mostrar recurso ausente hasta su importacion explicita.

Flux queda certificado con dos generaciones reales mediante el adaptador y la administracion usados por el launcher: `flux-kontext` con prompt libre y `flux-high-quality` a partir de una unica intervencion teatral. Ambas usaron `VRAM_RAM_OFFLOAD`/`--novram`, prompts persistidos, seeds fijas y manifests `PASS`. DreamShaper y SDXL permanecen `RESOURCE_MISSING` en este equipo.

## Sustituciones de legado

| Legado retirado | Sustitucion |
|---|---|
| main class/JAR monolitico en scripts | `studio-launcher` |
| roots basados en `user.dir` | `LocalMediaLayout` + `LauncherLayoutResolver` |
| administracion generica con comando/destino libre | administradores especificos con destinos confinados |
| scripts publicos de preparacion y preflight | diagnostico unico y administracion declarativa |
| alias duplicado de XTTS | `xtts-file-to-wav.ps1` interno |
| empaquetado de aproximadamente 100 GB | app-image ligera; recursos posteriores desde Configuracion |
| clamp global al ancho inicial de 980 px | limites vivos de `InkCanvasViewport`; perfiles crecientes expanden mosaicos |
| proveedor mouse-first unico para todos los perfiles | resolucion por `InkInputPolicy` con arbitraje nativo y diagnostico observable |

Ningun archivo legacy se considera reemplazado solo por nombre: su fila exige una ruta productiva y una prueba. Los contratos de medios permanecen congelados.

## Evidencia final de restauración — 29 de julio de 2026

La restauración de Estudio, video y exportación se cerró con evidencia sobre las rutas productivas:

- Suite completa y perfil JavaFX: 1.066 pruebas, 0 fallos, 0 errores y 10 pruebas opt-in omitidas.
- Launcher: `scripts/01-ejecutar-app.bat --smoke` mostró el shell real y terminó limpiamente.
- XTTS: el smoke unitario utilizó la muestra oficial instalada
  `samples/voices/advanced-presets/hombre_40_popular_ecuador_dialogo/neutral.wav`;
  el smoke batch produjo seis WAV y no dejó procesos Python hijos.
- FFmpeg: el smoke documental compuso dos imágenes, texto y un WAV en un MP4 publicado
  atómicamente; FFprobe confirmó video H.264, audio AAC, 640×360 y 2,02 segundos.
- Auditoría contra `g - copia`: 62 comandos, 61 handlers y 19 identificadores de módulos
  laterales coinciden. Las 73 clases Java exclusivas de la referencia quedaron clasificadas;
  no existe ninguna diferencia sin clasificar.

La referencia `D:\Proyectos\g - copia` fue tratada como solo lectura. La auditoría únicamente
enumera sus archivos; todos los reportes y artefactos se escriben bajo `D:\Proyectos\g\target`.
