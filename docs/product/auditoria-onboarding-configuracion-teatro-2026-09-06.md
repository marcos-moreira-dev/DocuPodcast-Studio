# Auditoría y plan: onboarding, configuración e IA teatral local

Fecha: 6 de septiembre de 2026. Estado: propuesta para implementar, no implementación realizada.

## Alcance y evidencia

Prioridad: simplificar el primer uso y hacer que Configuración comunique y prepare capacidades locales completas. El experimento de composición teatral queda al final. Estudio documental se conserva sin cambios funcionales; el tercer tipo de proyecto queda fuera de esta intervención, sin eliminar sus datos ni formatos.

Se revisaron código de presentación, preparación de dependencias, catálogo de modelos, adaptador ComfyUI y archivos locales. Se consultaron fuentes oficiales para el acceso a FLUX. No se descargaron modelos, no se ejecutaron generaciones, no se modificó configuración ni se ejecutó la suite de pruebas. El árbol de trabajo contiene cambios previos: este documento no los sustituye.

La inspección visual en vivo quedó pendiente: DocuPodcast no estaba abierto; el ejecutable existente en `dist/app-image/DocuPodcastStudio` no mantuvo una ventana seleccionable tras el intento de arranque. Eso no identifica por sí solo la causa ni prueba un defecto de la versión fuente actual. Antes de aprobar el diseño, hace falta una ejecución identificada de la compilación vigente en un perfil de prueba.

## 1. Hallazgos principales

### P1 — Primer uso y administración técnica son el mismo destino

[SettingsDialog.java](/D:/Proyectos/g/src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java:91) implementa `showFirstUseSetup` abriendo directamente Motores y dependencias. No hay un recorrido específico de preparación por objetivo en ese punto de entrada.

[WelcomeWorkspaceView.java](/D:/Proyectos/g/src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java:173) reúne configuración, abrir, crear, problema técnico, ejemplo y guía; además repite orientación documental en cuatro pasos y distintivos. Teatro no tiene una orientación equivalente. La limpieza debe reducir carga inicial y adaptar el recorrido al tipo de trabajo, no quitar capacidades existentes.

### P1 — Lo recomendado no corresponde necesariamente al objetivo teatral

[EngineAdministrationPane.java](/D:/Proyectos/g/src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/EngineAdministrationPane.java:417) prepara Piper y FFmpeg al pulsar «Preparar dependencias recomendadas». No prepara por ese camino todo lo necesario para componer personajes en un escenario.

La ruta de imagen separa descarga de checkpoint y preparación del backend. [PrepareLocalTheatreImageRuntimeUseCase.java](/D:/Proyectos/g/src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/PrepareLocalTheatreImageRuntimeUseCase.java:11) crea carpetas y documentación, explícitamente no instala runtime. [ComfyUiImageEngineAdministration.java](/D:/Proyectos/g/studio-local-media-adapters/src/main/java/com/marcosmoreiradev/docupodcaststudio/localmedia/ComfyUiImageEngineAdministration.java:77) ofrece importar ComfyUI y descargar componentes de identidad, pero no presenta ahí una instalación integral de ComfyUI desde cero.

Conclusión: hay infraestructura útil, pero la promesa «instalo DocuPodcast y desde la app preparo lo que necesito» tiene una brecha real; no se resuelve solo renombrando botones.

### P1 — Preparación, integridad y funcionamiento no están suficientemente separados

[EngineAdministrationPane.java](/D:/Proyectos/g/src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/EngineAdministrationPane.java:177) excluye las acciones `SMOKE_TEST` al renderizar el panel genérico, aunque varios adaptadores publican pruebas reales. El usuario puede comprobar disponibilidad sin una vía equivalente visible para validar un resultado real desde ese panel.

No confundir: archivo encontrado, hash correcto, backend iniciado, workflow compatible y generación satisfactoria. Son estados distintos.

### P1 — Riesgo de bloqueos durante verificaciones y seguimiento incompleto

El preflight de descargas se invoca síncronamente desde la acción de UI en [EngineAdministrationPane.java](/D:/Proyectos/g/src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/EngineAdministrationPane.java:297). [ManagedDownloadPreflightInspector.java](/D:/Proyectos/g/studio-local-media-adapters/src/main/java/com/marcosmoreiradev/docupodcaststudio/localmedia/ManagedDownloadPreflightInspector.java:64) calcula SHA-256 cuando no hay caché. En archivos de varios GB, esa ruta puede bloquear la interfaz; es un riesgo demostrado por el flujo de llamadas, no un congelamiento medido en esta auditoría.

Hay dos presentaciones de operaciones: acciones del adaptador con cancelación y `runPreparation` con una etiqueta compartida, sin control equivalente de cancelación/doble inicio. El progreso estructurado de acciones se reduce a texto. Al cambiar de sección se crea otro panel; hay que comprobar continuidad y recuperación del seguimiento.

### P1 — Catálogo comercial y capacidad ejecutable pueden divergir

[ImageModelPackageProfile.java](/D:/Proyectos/g/src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ImageModelPackageProfile.java:7) anuncia SDXL con referencias y FLUX Kontext. En el adaptador actual, [ComfyUiImageEngine.java](/D:/Proyectos/g/studio-local-media-adapters/src/main/java/com/marcosmoreiradev/docupodcaststudio/localmedia/ComfyUiImageEngine.java:165) reserva la ruta condicionada al preset regional SD 1.5; las otras rutas rechazan referencias explícitas. Es correcto no ignorarlas, pero el nombre del modelo no debe prometer un workflow que esta integración no ejecuta.

El flujo regional ya contempla referencias, regiones e inpainting secuencial. Debe probarse y reutilizarse antes de construir otro backend.

### P2 — Semántica de edición, navegación y mensajes

- Configuración abre con «Cambios pendientes» aun antes de editar: [SettingsDialog.java](/D:/Proyectos/g/src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java:98).
- «Descargar modelo visual» elige el preset desde configuración persistida; hay que mostrar el perfil exacto antes de ejecutar, y aclarar si aplica cambios pendientes.
- La lista de motores crece verticalmente con su cantidad, seguida de formularios, acciones y cola: exceso de desplazamiento previsto por la estructura. Verificarlo visualmente antes de decidir tamaños definitivos.
- La cabecera «Administración transversal de capacidades y ajustes operativos» describe arquitectura, no una tarea del usuario.
- Existe una descarga visual de checkpoint con HTTP y `.part` propios: [DownloadLocalTheatreImagePackageUseCase.java](/D:/Proyectos/g/src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/DownloadLocalTheatreImagePackageUseCase.java:146). No ofrece ahí la misma verificación y seguimiento de las descargas administradas del adaptador. Consolidar comportamiento sin crear otro descargador.

## 2. Modelos y equipo: qué sabemos realmente

Equipo medido: NVIDIA GeForce GTX 1650, 4096 MiB de VRAM, 31,8 GiB de RAM instalada. Esto no certifica velocidad ni suficiencia de memoria para cada workflow.

| Recurso | Evidencia local | Implicación para la UX |
|---|---|---|
| SD 1.5 | Checkpoint de 2,13 GB encontrado | Candidato a prueba inicial; no prometer identidad solo por tener el checkpoint |
| DreamShaper 8 | Checkpoint de 2,13 GB encontrado | Variante estética ya disponible, pendiente de prueba real |
| IP-Adapter y CLIP Vision | Archivos de ~98 MB y ~2,53 GB, más carpeta de nodo encontrados | Mostrar dependencias conjuntas del flujo de identidad |
| FLUX.1-dev | Archivo de 23.802.932.552 bytes, aproximadamente 22,17 GiB, encontrado | Coincide probablemente con el modelo recordado; no descargarlo otra vez por defecto |
| ComfyUI | Carpetas de backend, entorno local y lanzador encontradas | Presencia no equivale a arranque ni compatibilidad verificada |
| Qwen3-TTS | Código declara instalación fijada de runtime, CUDA, talker y codec Q8 con hashes | Buen patrón de paquete; no implica instalación probada aquí |
| Coqui XTTS | Adaptador expone preparación/importación y pruebas | Auditar recorrido de extremo a extremo; no cambiar voz o muestras en este trabajo |

El tamaño del checkpoint no es el tamaño total de instalación ni el consumo de VRAM. FLUX necesita además codificadores y VAE. Los catálogos deben distinguir MB/GB decimales de MiB/GiB.

Los repositorios oficiales de [FLUX.1-dev](https://huggingface.co/black-forest-labs/FLUX.1-dev) y [FLUX.1-Kontext-dev](https://huggingface.co/black-forest-labs/FLUX.1-Kontext-dev) requieren aceptar condiciones para acceder a sus archivos. Presentar ese requisito como acceso del proveedor, no como avería de DocuPodcast. No asumir que todos los demás recursos son públicos para siempre: verificar cada URL/licencia al publicar el catálogo.

## 3. Fase A — Limpieza del onboarding

### Experiencia propuesta

Inicio breve: «Abrir proyecto», «Nuevo proyecto» y recientes. Ejemplos, guía y preparación del equipo como acciones secundarias. No repetir el mismo mensaje en pasos, subtítulos y distintivos.

Flujo nuevo:

`Elegir tipo de trabajo → crear/abrir proyecto → ofrecer recursos necesarios → empezar o preparar después`

Para teatro, explicar personajes, escenarios y composición; permitir empezar con imágenes existentes sin instalar IA. La preparación de IA es opcional y recuperable desde Configuración. El estudio documental mantiene su comportamiento. El tercer tipo no se elimina; no se amplía su onboarding en esta fase.

### Trabajo técnico

1. Inventariar entradas de Inicio, Nuevo proyecto, ejemplos, guía y configuración inicial; detectar instrucciones duplicadas o desactualizadas.
2. Separar pantalla de bienvenida, estado de primer uso y plan de preparación por capacidad. Reutilizar comandos y servicios existentes.
3. Guardar «preparar después» y reanudar un proceso interrumpido sin obligar a repetir pasos ni descargar recursos válidos.
4. Hacer que la primera apertura sin red siga permitiendo abrir proyectos y usar recursos instalados.
5. Retirar código/textos obsoletos solo tras demostrar referencias y equivalencias; no hacer una limpieza masiva por nombre o antigüedad.

Aceptación: desde Inicio se entra al proyecto sin recorrer un catálogo técnico; la ausencia de un modelo opcional no bloquea el trabajo básico; las rutas documentales existentes no cambian.

## 4. Fase B — Configuración y recursos locales

### Organización propuesta

- **Mi equipo y recursos:** resumen, capacidades disponibles y preparación pendiente.
- **Voces:** motores de voz y sus modelos, diferenciados de las muestras/personajes.
- **Imágenes y teatro:** generar, integrar personajes y mejorar resolución como capacidades diferentes.
- **Rendimiento y almacenamiento:** política de memoria, ubicaciones, espacio y cola.
- **Opciones avanzadas y diagnóstico:** workflows, rutas técnicas, logs y reparación.

Conservar Lectura/Reproducción/Video final y sus enlaces actuales; agrupar o reetiquetar no debe modificar sus valores. Qwen de voz y análisis visual deben distinguirse claramente. FFmpeg no es un generador de imágenes ni de voz.

Cada capacidad tendrá una ficha breve: para qué sirve, motor/perfil, qué hay instalado, qué falta, descarga pendiente, espacio final y temporal, acceso/licencia, compatibilidad estimada y última prueba realizada. Detalles técnicos plegables.

Acciones inequívocas: «Preparar esta capacidad», «Usar lo instalado», «Importar desde disco», «Comprobar archivos», «Probar con un ejemplo» y «Reparar…». Reinstalar debe ser explícito. Autenticación solo para recursos que realmente la requieren; no pedir una cuenta de Hugging Face para usar recursos públicos.

### Estado y ejecución

Separar disponibilidad del recurso —ausente, parcial, válido, incompatible— del trabajo en curso —descargando, verificando, instalando, probando— y del resultado de una prueba. «Listo para integrar personajes» requiere el conjunto de dependencias y una prueba del workflow, no un solo archivo.

Plan de preparación visible antes de confirmar: reutilizados, nuevas descargas, dependencias, espacio, licencias y posibles límites del equipo. La instalación pública debe ser automática después de esa elección; no implica descargar todo al abrir la aplicación.

Para ComfyUI, cerrar expresamente la brecha de instalación desde cero: runtime privado/versionado, dependencias compatibles, modelos y nodos permitidos, manifiesto y verificación. Reutilizar servicios administrados y scheduler compartido. No depender silenciosamente de Python global, no modificar PATH y no permitir que una opción visual instale paquetes sin mostrarlo.

Mover inspecciones costosas al fondo. Mostrar por operación etapa, porcentaje cuando existe denominador, bytes, tiempo transcurrido y estimación prudente. Si no se puede estimar, explicar qué está haciendo; nunca fabricar porcentajes. Cerrar Configuración no debe perder el estado del trabajo ni su cancelación recuperable.

Unificar borrador/guardado: «Sin cambios», «Cambios sin guardar», «Guardado». Las acciones que necesiten valores pendientes deben ofrecer aplicarlos o usar los guardados, mostrando cuál. Enter en un campo valida el campo, no instala ni inicia una prueba. Mantener foco, scroll y selección; evitar reconstruir toda la página por cada actualización.

### Pruebas de aceptación antes de cerrar A y B

| Caso | Resultado requerido |
|---|---|
| Instalación limpia, sin modelos | Se puede entrar al programa; el asistente explica y prepara solo lo elegido |
| Recursos ya instalados, sin red | Se reutilizan sin descarga ni solicitud de login |
| Perfil público frente a restringido | Solo el restringido pide acción de acceso; errores 401/403 explicados |
| Poco disco, descarga parcial, corte de red | No destruir el recurso publicado; recuperación explícita y estado persistente |
| Archivo alterado o tamaño correcto/hash incorrecto | No declarar integridad válida; reparar solo el recurso afectado |
| Cambio de pestaña/cierre/reapertura durante operación | Seguimiento y cancelación disponibles; ningún trabajo duplicado |
| Hash grande, detección de equipo o backend lento | UI utilizable; trabajo técnico fuera del hilo JavaFX |
| Cambios sin guardar y Enter | Sin instalación ni generación accidental; semántica coherente al cerrar |
| Prueba rápida | Resultado real identificable; fallo no convertido en «listo» |
| Escalado Windows y teclado | Validar 100/125/150/200 %, ventanas pequeñas, foco visible y cierre accesible |
| Proyecto documental existente | Abrir/guardar y valores de motores/rutas/voz sin migraciones destructivas |

Añadir pruebas unitarias de planes/estados, integración de descarga con servidor local simulado y pruebas de UI sobre una compilación vigente. Aprovechar las pruebas existentes de configuración, migración, presets, perfiles de memoria y transporte ComfyUI. Los tests simulados no sustituyen el ensayo de un instalable limpio. La guía de QA se usa como estructura; sus plantillas web no se trasladan a JavaFX.

## 5. Fase C — Experimento teatral local, después

Material localizado:

- Personaje: [carpeta Florindo](</C:/Users/MARCOS MOREIRA/Desktop/03_BURRO_FLORINDO>).
- Personaje: [carpeta señoras](</C:/Users/MARCOS MOREIRA/Desktop/09_SENORAS_DRAMATICAS>), elegir una señora y referencias coherentes de esa misma identidad.
- Escenario candidato: [teatro vacío](/D:/Proyectos/g/src/main/resources/examples/aviadores-comicos/assets/mapas/teatro-vacio.png). Inspeccionarlo visualmente antes de elegirlo definitivamente.

Ensayo independiente, sin editar el proyecto documental ni sobrescribir imágenes originales. Todo el procesamiento local; ningún envío de personajes a servicios de generación externos.

1. Verificar versiones, integridad, espacio disponible y arranque real de ComfyUI; confirmar endpoint local y nodos necesarios.
2. Ejecutar una prueba pequeña de generación y después una referencia regional. Registrar carga inicial por separado del tiempo de inferencia.
3. Componer Florindo y una señora en regiones separadas del escenario, con el preset regional SD 1.5 existente. Empezar con una imagen y resolución de prueba baja; evaluar calidad antes de aumentar carga. No asumir que un preset de texto a imagen conserva personajes.
4. Medir VRAM, RAM, uso de memoria comprometida/paginación, tiempo por etapa, errores y capacidad de cancelar. Proponer inicialmente un solo trabajo pesado; umbral de parada por falta de memoria o ausencia prolongada de progreso, no por mera lentitud.
5. Evaluar rostro/silueta/vestuario, escala, pies apoyados, posición, ausencia de duplicaciones y preservación del escenario. Guardar semilla, workflow, parámetros y versión de modelos para reproducibilidad.
6. Solo tras una composición aceptable, probar mayor resolución y, si procede, otro modelo. FLUX queda como alternativa posterior: tener 32 GB de RAM o tolerar seis horas no garantiza que su workflow quepa o conserve identidad.

Éxito: una imagen con el burro y una señora reconocibles, bien colocados en el teatro. No se requiere caminar, actuar, animar ni producir video. No declarar una prueba de IA aprobada por haber pegado recortes: si se usa composición inicial como guía, debe quedar distinguida del resultado generativo.

## Orden de entrega

1. Validar compilación/UI y cerrar inventario de pantallas y contratos de recursos.
2. Implementar y verificar onboarding breve, sin alterar flujos documentales.
3. Implementar y verificar configuración por capacidades y preparación integral local.
4. Validar instalación limpia y reutilización offline; resolver discrepancias entre catálogo y workflows.
5. Ejecutar el experimento con referencias y entregar resultado junto con mediciones.

No iniciar ahora una descarga masiva, una nueva generación de cinco horas, una limpieza de modelos ni una reescritura de todo el shell. La prioridad es que el producto explique y cumpla exactamente qué prepara para el usuario.
