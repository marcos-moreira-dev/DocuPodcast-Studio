# Estándares pendientes para cierre RC — DocuPodcast Studio

## Propósito de este documento

Este archivo es el mapa de continuidad para las tandas pendientes después de la base verde validada por el usuario con diagnóstico completo OK y demo teatral estable.

Base asumida:

```text
DocuPodcast-Studio-DEMO-PLAY-BODY-BINDINGS-DOCX-HEADING-HF1-v1.zip
```

Estado local reportado:

```text
scripts\99-diagnostico-completo.bat → OK
Maven compile → OK
Maven tests → OK
Smoke automático cerebro → OK
Preflight arranque motores → OK
Piper y FFmpeg locales → OK
Demo teatral estable
```

Actualización de tanda:

```text
RUNTIME-PROCESOS-INTEGRACION1
  RuntimeArtifactPaths, ExternalProcessRunner y excepciones de proceso ya no quedan solo como contratos base.
  DefaultExternalProcessRunner gobierna los procesos externos productivos.
  Las rutas embebidas operativas de XTTS, Piper, FFmpeg/FFprobe, modelos, smoke tests y voces se resuelven desde RuntimeArtifactPaths.
  Quedan para limpieza posterior la documentación histórica y catálogos declarativos que conservan rutas como texto de manifiesto.
```

Este documento no debe usarse para reabrir decisiones de producto ya cerradas. Su objetivo es dejar por escrito, con máximo detalle, **qué estándares faltan**, **por qué importan**, **cómo implementarlos sin ensuciar la arquitectura** y **qué criterios de aceptación deben protegerse con tests**.

---

# 1. Principios obligatorios para cualquier tanda restante

## 1.1. Principio de superficie operativa

Todo lo visible en DocuPodcast Studio debe justificar su presencia por una necesidad operativa.

Un control, región, panel, texto, tarjeta, indicador, badge, tooltip, separador o microcopy solo puede entrar si responde afirmativamente a por lo menos una de estas preguntas:

```text
1. ¿Permite ejecutar una acción?
2. ¿Ayuda a tomar una decisión?
3. ¿Comunica un estado necesario para operar?
4. ¿Advierte un bloqueo, riesgo o cambio de comportamiento?
5. ¿Guía el siguiente paso inmediato del usuario?
```

Si una región no cumple ninguna de esas funciones, no debe existir.

### Prohibido

```text
- Tarjetas decorativas.
- Métricas sin consecuencia operativa.
- Dashboard de relleno.
- Textos técnicos para aparentar profundidad.
- Indicadores que no cambian acciones disponibles.
- Botones sin handler real.
- Estados visuales que no expliquen qué hacer.
- Secciones futuras sin implementación real.
```

### Permitido

```text
- Microcopy corto que explique una acción necesaria.
- Estado de motor si habilita/bloquea generación.
- Estado de exportación si permite decidir si continuar.
- Advertencia de fallback si la app cambiará lo pedido por el usuario.
- Guía de siguiente paso cuando una acción está bloqueada.
```

### Checklist obligatorio antes de agregar UI

Toda tanda visual debe documentar internamente esta tabla para cada nueva región:

```text
Región:
Acción del usuario:
Decisión que permite:
Estado que comunica:
Riesgo que evita:
Cuándo aparece:
Cuándo se oculta:
Qué pasa si se elimina:
Componente transversal usado:
```

Si no se puede responder, la región no entra.

---

## 1.2. Principio de decisiones defensivas visibles

La app no puede cambiar silenciosamente una intención explícita del usuario.

Regla resumida: **Si la app cambia lo que el usuario pidió, debe mostrar message box.**

Ejemplos de intención explícita:

```text
- Elegir GPU.
- Elegir Voz IA avanzada.
- Elegir un tono/emoción.
- Pedir exportar video.
- Pedir exportar audio comprimido.
- Abrir un proyecto esperando que esté íntegro.
- Crear un demo esperando visuales preasignados.
```

Si la aplicación decide hacer otra cosa para proteger estabilidad, debe avisar.

### Ejemplos de fallback que deben avisarse

```text
GPU solicitada → CPU efectiva.
Voz IA avanzada solicitada → Voz local simple o Modo de prueba.
Tono Feliz solicitado → Neutral efectivo.
Exportación MP4 solicitada → bloqueada por FFmpeg faltante.
Video completo solicitado → fragmentos sin imagen serán omitidos.
Proyecto abierto → assets faltantes o checksums rotos.
Demo teatral creado → imágenes sin asociación automática.
```

### Cuándo usar message box

Debe usarse message box si:

```text
- El usuario pidió una acción explícita.
- La acción no podrá hacerse como la pidió.
- La app puede continuar, pero con resultado distinto.
- El usuario necesita decidir continuar/cancelar/reparar.
```

### Cuándo basta status bar

Basta status bar si:

```text
- La acción terminó como se esperaba.
- Solo se informa progreso no crítico.
- No hay cambio de intención.
- No hay riesgo de pérdida, sustitución ni exportación parcial.
```

---

## 1.3. Principio de excepciones tipadas

No usar excepciones genéricas para errores de producto.

### Clasificación esperada

```text
ApplicationPreconditionException
```

Para precondiciones operativas no cumplidas:

```text
- falta proyecto guardado;
- falta documento preparado;
- falta selección;
- no hay motor usable;
- no hay audio listo;
- no hay frames renderizables;
- FFmpeg no está listo para formato solicitado.
```

```text
InfrastructureOperationException
```

Para fallos de infraestructura:

```text
- no se pudo copiar archivo;
- no se pudo escribir JSON;
- no se pudo leer asset;
- no se pudo calcular checksum;
- no se pudo crear carpeta de runtime;
- no se pudo guardar muestra de voz.
```

```text
ExternalProcessFailedException
```

Para procesos externos:

```text
- Python XTTS falló;
- Piper falló;
- FFmpeg falló;
- PowerShell wrapper falló;
- smoke CUDA falló por exit code;
- proceso externo venció timeout.
```

```text
EngineUnavailableException
```

Para motores no usables:

```text
- XTTS sin modelo válido;
- XTTS con modelo pero sin WAV de prueba;
- Piper sin voz o binario;
- FFmpeg no preparado;
- GPU solicitada sin CUDA aprobado cuando la operación exige GPU.
```

```text
UserVisibleDecision
```

Para fallback defensivo que permite continuar:

```text
- GPU→CPU;
- tono solicitado→Neutral;
- voz avanzada→voz local;
- export parcial;
- demo con advertencias.
```

---

## 1.4. Principio de componentes transversales

No improvisar controles JavaFX complejos si ya existe un componente transversal.

### Usar preferentemente

```text
ActionButtonFactory
ActionBar
RibbonButton
RibbonGroup
SectionHeader
InfoBadge
RailActionRow
MediaThumbnailCard
CollapsibleMediaRail
SettingsPageView
VoiceSampleRow
VoiceGeneratedTestPanel
LongProcessOverlayView
StatusBarView
TransportControls
```

### Cuándo crear un nuevo componente

Crear componente nuevo solo si:

```text
1. El patrón se repetirá.
2. Tiene propósito operativo.
3. Reduce duplicación visual/lógica.
4. No es relleno estético.
5. Puede testearse como contrato reusable.
```

### Cuándo usar JavaFX simple directamente

Se permite usar JavaFX simple si:

```text
- Es un Label, TextField, ComboBox o layout básico.
- No replica un patrón complejo.
- No introduce una estética paralela.
- No se repetirá en varias pantallas.
```

---

## 1.5. Principio de documento unificado con capacidades especializadas

La experiencia visible puede seguir llamándose **Documento**, pero la arquitectura no debe fingir que Word, PDF, TXT y Markdown ofrecen lo mismo.

### Regla

```text
Vista Documento = experiencia común.
DocumentSourceCapabilities = capacidades reales por formato.
```

### Ejemplos

```text
DOCX:
- estructura rica;
- títulos;
- estilos;
- imágenes fuente;
- tablas;
- bloques visuales.

Markdown:
- encabezados;
- texto narrable;
- estructura textual;
- menos fidelidad visual.

PDF nativo:
- texto extraído;
- estructura limitada;
- no OCR en V1;
- visuales fuente limitados.

TXT:
- lectura lineal;
- sin estilos;
- sin visuales fuente;
- jerarquía mínima o inexistente.
```

### Prohibido

```text
if (sourceKind == DOCX) disperso por presentación.
```

### Correcto

```text
capabilities.supportsEmbeddedImages()
capabilities.supportsHeadingTree()
capabilities.supportsPreciseStyles()
capabilities.supportsNativeTextOnly()
```

---

# 2. Estándares técnicos pendientes

---

## 2.1. Estándar de rutas runtime — RUNTIME-PATHS-RF1

### Problema

Rutas críticas siguen dispersas en clases, scripts y configuraciones.

Rutas a centralizar:

```text
tools/xtts-wrapper
tools/xtts-wrapper/.venv
tools/xtts-wrapper/synthesize_xtts.py
scripts/tts/xtts-file-to-wav.ps1
models/tts/xtts
runtime/tts/xtts-smoke
tools/piper
tools/ffmpeg
voice-library/samples
voice-library/tmp-recordings
media/images
media/audio
media/video
jobs
exports
```

### Estándar esperado

Debe existir una autoridad única:

```text
RuntimeArtifactPaths
```

Responsabilidades:

```text
- Resolver rutas en modo desarrollo.
- Resolver rutas en modo portable.
- Normalizar separadores.
- Evitar escapes fuera del root.
- Evitar duplicaciones como model.pth/model.pth.
- Dar rutas a readiness, comandos, diagnósticos y UI.
```

### Métodos mínimos esperados

```text
applicationRoot()
toolsRoot()
modelsRoot()
runtimeRoot()
xttsWrapperDirectory()
xttsPythonExecutable()
xttsSynthesizeScript()
xttsPowerShellScript()
xttsModelDirectory()
xttsSmokeDirectory()
xttsReadinessSmokeJson()
xttsCudaSmokeJson()
piperDirectory()
piperExecutable()
ffmpegDirectory()
ffmpegExecutable()
voiceLibraryRoot()
voiceSamplesDirectory()
voiceTempRecordingsDirectory()
projectMediaImages(projectFile)
projectMediaAudio(projectFile)
projectMediaVideo(projectFile)
projectJobsDirectory(projectFile)
projectExportsDirectory(projectFile)
```

### Tests mínimos

```text
RuntimePathsRf1SourceTest
RuntimeArtifactPathsTest
```

### Criterio de aceptación

```text
- No se agregan nuevas rutas hardcodeadas para XTTS/Piper/FFmpeg.
- Readiness y comandos consultan la misma ruta.
- Diagnóstico muestra la misma ruta usada por el comando real.
- Tests verdes.
```

---

## 2.2. Estándar de procesos externos — EXTERNAL-PROCESS-RUNNER-RF1

### Problema

La app ejecuta Python, FFmpeg, Piper, PowerShell y probes con lógica repetida.

### Estándar esperado

Debe existir un runner transversal:

```text
ExternalProcessRunner
```

Con request/response tipados:

```text
ExternalProcessRequest
ExternalProcessResult
ExternalProcessCancellationToken
ExternalProcessTimeoutPolicy
```

### Request mínimo

```text
command
workingDirectory
timeout
environment
auditLabel
captureStdout
captureStderr
redactArguments
```

### Result mínimo

```text
exitCode
timedOut
cancelled
stdoutTail
stderrTail
combinedOutputTail
startedAt
finishedAt
duration
commandAudit
diagnosticLogPath
```

### Reglas

```text
- Nunca bloquear por stdout/stderr.
- Capturar última salida.
- Matar proceso y descendientes al cancelar.
- No filtrar secretos.
- Registrar comando auditado.
- No lanzar RuntimeException genérica.
```

### Tests mínimos

```text
ExternalProcessRunnerRf1SourceTest
DefaultExternalProcessRunnerTest
```

---

## 2.3. Estándar de artefactos de motor — MODEL-ARTIFACT-CONTRACT-RF1

### Problema

XTTS, Piper y FFmpeg validan archivos de manera distinta.

### Estándar esperado

Crear contrato común:

```text
ModelArtifactContract
ModelArtifactRequirement
RuntimeArtifactInspection
RuntimeArtifactStatus
```

### XTTS requerido

```text
config.json
model.pth
vocab.json
speakers_xtts.pth
dvae.pth
mel_stats.pth
```

### Piper requerido

```text
piper executable
voice .onnx
voice .json
```

### FFmpeg requerido

```text
ffmpeg executable
ffprobe executable si se usa probe avanzado
```

### Criterio de aceptación

```text
- Readiness reporta faltantes exactos.
- No se marca motor listo si falta archivo obligatorio.
- Contrato sirve para diagnóstico y UI.
- Tests verdes.
```

---

## 2.4. Estándar de descargas — MANAGED-DOWNLOAD-RF1

### Problema

Descarga/reanudación/extracción/progreso se repiten por motor.

### Estándar esperado

Crear:

```text
ManagedDownloadService
DownloadRequest
DownloadResult
DownloadProgress
DownloadResumePolicy
ArchiveExtractionService
```

### Reglas

```text
- No descargar en startup.
- Confirmar antes de descargas grandes.
- Mostrar progreso real.
- Permitir cancelar.
- No marcar parciales como listos.
- Validar destino final.
- Escribir diagnóstico.
```

### Criterio UX

Si la descarga falla:

```text
El mensaje debe decir qué recurso falló, dónde está el diagnóstico y qué puede hacer el usuario.
```

---

## 2.5. Estándar de readiness de motores — ENGINE-READINESS-UI-HF1

### Estados obligatorios

```text
No preparado
Preparado parcialmente
Archivos encontrados
Prueba WAV aprobada
Usable en Documento
Error de modelo
Error de Python
Error de CUDA
CPU efectivo
GPU confirmada
```

### Regla por superficie

```text
Documento:
  Solo motores usables.

Configuración:
  Todos los motores, con estado y acción.

Voces:
  Motor activo, si soporta muestras/tonos y prueba corta.

Diagnóstico:
  Detalle técnico completo.
```

### Prohibición

```text
No decir “listo” si no generó WAV real.
```

---

## 2.6. Estándar PyTorch CUDA local — XTTS-PYTORCH-CUDA-INSTALL-HF1

### Objetivo

Permitir reparar PyTorch CUDA dentro del Python autocontenido.

### Reglas

```text
- No tocar Python global.
- Instalar en tools/xtts-wrapper/.venv.
- Confirmar tamaño/tiempo antes de descargar.
- Permitir cancelar.
- No marcar GPU usable sin smoke CUDA.
- Mostrar fallback CPU si CUDA no queda aprobada.
```

### Evidencia requerida

```text
torch.__version__
torch.version.cuda
torch.cuda.is_available()
torch.cuda.device_count()
torch.cuda.get_device_name(0)
device solicitado
device usado
última generación cpu/cuda
```

---

# 3. Estándares de comandos, Ribbon y Settings

## 3.1. COMMAND-AUDIT-RC1

Todo comando visible debe tener:

```text
id
label
handler
availability
motivo humano si está deshabilitado
superficie permitida
```

No puede existir:

```text
botón visible sin handler
comando visible que termine en showPlaceholder
acción visible que dependa de una implementación futura
```

## 3.2. RIBBON-CATALOG-RF1

`RibbonView` debe renderizar. La estructura debe vivir en:

```text
RibbonDefinitionCatalog
```

Con:

```text
RibbonTabDefinition
RibbonGroupDefinition
RibbonCommandDefinition
```

## 3.3. SETTINGS-SPLIT-RF1

`SettingsDialog` debe dividirse en paneles operativos:

```text
EngineSetupPanel
ComputeDevicePanel
VideoRuntimePanel
DownloadsPanel
DiagnosticsPanel
StoragePanel
SettingsSaveController
```

Cada panel debe responder:

```text
¿Qué puede hacer el usuario aquí?
¿Qué estado comunica?
¿Qué decisión permite?
¿Qué acción resuelve?
```

---

# 4. Estándares UI/UX pendientes

## 4.1. VOICE-REC-HF1

Queda pulido fino de grabación:

```text
contador 00:08
estado “Grabando ahora”
voz actual
tono actual
botones según estado
```

No agregar:

```text
grÁficos de onda decorativos
tarjetas grandes
métricas falsas
```

## 4.2. DOC-VISUAL-RAIL-RC1

El rail derecho debe comunicar y permitir:

```text
fragmentos con imagen
fragmentos sin imagen
imagen seleccionada
clic → navegar al fragmento
copiar imagen a anterior/posterior
asset faltante
```

No debe contener:

```text
estadísticas decorativas
texto técnico de storyboard
acciones que pertenecen al sidebar izquierdo
```

## 4.3. OPERATIONAL-MICROCOPY-HF1

Revisar microcopy de:

```text
Documento
Voces
Configuración
Exportación
Integridad
Demo
Diagnóstico
```

Regla:

```text
Corto, humano, accionable.
```

---

# 5. Estándares de persistencia y compatibilidad

## 5.1. PERSISTENCE-RC1

Debe proteger roundtrip de:

```text
documento fuente
documento materializado
capas narrativas
anclas ricas
imágenes
audio local
muestras de voz
audio jobs
readiness
demo visual bindings
settings relevantes
```

## 5.2. VOICE-SAMPLES-PATH-HF1

Debe resolver:

```text
muestra app-library absoluta
muestra legacy relativa al proyecto
archivo faltante
voz sin Neutral
```

## 5.3. PROJECT-ASSET-REPAIR-HF1

Debe permitir:

```text
reubicar asset
remover referencia
mantener para revisar después
regenerar audio
```

## 5.4. LEGACY-PROJECT-MIGRATION-HF1

Migrar:

```text
XTTS model path viejo
muestras relativas viejas
storyboard legacy
viewState de workspaces ocultos
motor activo no usable
rutas FFmpeg/Piper viejas
```

---

# 6. Estándares de validación y RC

## 6.1. Tests específicos pendientes

```text
STATUSBAR-HOVER-TEST1
ENGINE-CATALOG-TEST1
XTTS-PATH-TEST1
GPU-SMOKE-TEST1
VOICE-WORKSPACE-UX-TEST1
EXPORT-READINESS-TEST1
```

## 6.2. Diagnóstico RC

Scripts a consolidar:

```text
99-diagnostico-completo.bat
16-release-candidate.bat
18-smoke-automatico-cerebro.bat
19-smoke-motores-reales.bat
37-smoke-cuda-xtts.bat
38-smoke-video-final.bat
```

## 6.3. Packaging RC

Validar:

```text
heap -Xmx2048m
runtime portable
tools
models
scripts
voice-library
logs
diagnostics
dist
app-image
```

## 6.4. RC-GATE1

Criterios de salida:

```text
mvn test verde
diagnóstico completo OK
smoke automático cerebro OK
smoke motores preparados OK
smoke app portable OK
demo teatral abre y muestra rail visual
export readiness correcto
guardar/reabrir proyecto conserva capas/assets
sin warnings CSS críticos
sin comandos visibles muertos
sin motores falsamente listos
sin placeholders visibles
```

---

# 7. Advertencia CSS actual

El usuario reportó:

```text
Could not resolve '-dp-text-secondary'
from rule '*.example-project-readiness'
```

Resolución aplicada en la tanda documental:

```css
-dp-text-secondary: -docu-text-muted;
```

Este alias debe permanecer en `tokens.css` mientras `examples.css` lo use.

---

# 8. Tandas pendientes reales

Pendientes reales al momento de este documento:

```text
RUNTIME-PATHS-RF1
EXTERNAL-PROCESS-RUNNER-RF1
EXTERNAL-PROCESS-EXCEPTIONS-HF1
MODEL-ARTIFACT-CONTRACT-RF1
MANAGED-DOWNLOAD-RF1
ENGINE-READINESS-UI-HF1
XTTS-PYTORCH-CUDA-INSTALL-HF1
RUNTIME-ARCH-RC1
COMMAND-AUDIT-RC1
RIBBON-CATALOG-RF1
SETTINGS-SPLIT-RF1
GUI-COMPONENTS-RF1
VOICE-WORKSPACE-SPLIT-RF1
NO-HARDCODED-JAVAFX-RC1
VOICE-REC-HF1
DOC-VISUAL-RAIL-RC1
OPERATIONAL-MICROCOPY-HF1
PERSISTENCE-RC1
VOICE-SAMPLES-PATH-HF1
PROJECT-ASSET-REPAIR-HF1
LEGACY-PROJECT-MIGRATION-HF1
STATUSBAR-HOVER-TEST1
ENGINE-CATALOG-TEST1
XTTS-PATH-TEST1
GPU-SMOKE-TEST1
VOICE-WORKSPACE-UX-TEST1
EXPORT-READINESS-TEST1
DIAGNOSTIC-SCRIPTS-RC1
PACKAGING-MEMORY-RC1
RC-GATE1
AUDIO-UNIT-MODEL-POSTRC
```

---

# 9. Próxima tanda recomendada

La siguiente tanda recomendada es:

```text
RUNTIME-PATHS-RF1
```

Motivo:

```text
Es la base para limpiar hardcodeos, scripts, Python local, XTTS, Piper, FFmpeg, smoke CUDA, descargas, packaging y diagnóstico. Si no se hace antes, las tandas de procesos externos y runtime seguirán duplicando rutas.
```

## Actualización de continuidad: MODEL-ARTIFACT-CONTRACT-RF1 y MANAGED-DOWNLOAD-RF1

Tras la tanda `MODEL-ARTIFACT-CONTRACT-RF1 + MANAGED-DOWNLOAD-RF1`, los contratos base de artefactos y descargas ya existen. Lo pendiente no es crear los contratos, sino integrarlos más profundamente en readiness de motores y migrar descargas reales de XTTS/Piper/FFmpeg hacia `ManagedDownloadService`.

## Actualización de continuidad: ENGINE-READINESS-UI-HF1, COMMAND-AUDIT-RC1 y RIBBON-CATALOG-RF1

Tras esta tanda, las tres bases quedaron aplicadas:

- `ENGINE-READINESS-UI-HF1`: existen `AudioEngineReadinessUiItem` e `InspectAudioEngineReadinessUiUseCase`. Documento y Voces pueden consultar líneas humanas de readiness sin duplicar labels ni mostrar motores rotos como si fueran operativos.
- `COMMAND-AUDIT-RC1`: existen `CommandAuditInspector` y `CommandAuditReport`; el Shell audita al arrancar que todo comando visible tenga handler real. Los comandos heredados ocultos pueden permanecer en catálogo, pero no deben exponerse.
- `RIBBON-CATALOG-RF1`: existen `RibbonDefinitionCatalog`, `RibbonTabDefinition`, `RibbonGroupDefinition` y `RibbonCommandDefinition`. `RibbonView` queda como renderizador de catálogo.

Lo pendiente después de esta tanda se concentra en integración profunda de runtime, división de Settings/Voces, persistencia/compatibilidad y RC.

## Actualización aplicada: SETTINGS-SPLIT-RF1 + GUI-COMPONENTS-RF1

Estas dos tandas quedan parcialmente iniciadas con una extracción segura:

- `SettingsActionBar` concentra la barra inferior de Configuración.
- `OperationalStatusStrip` queda como componente transversal para estado operativo breve.

No se consideran totalmente cerradas las divisiones mayores de Configuración ni de componentes GUI; todavía faltan paneles específicos y auditoría completa anti-hardcodeo.
