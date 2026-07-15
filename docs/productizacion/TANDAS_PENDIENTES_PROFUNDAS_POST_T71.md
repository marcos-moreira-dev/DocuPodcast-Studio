# Tandas pendientes profundas post T71 — cierre del cerebro y transición al frente

Este documento deja escrito el mapa completo de tandas pendientes después de T71. La prioridad inmediata sigue siendo cerrar el **cerebro** de DocuPodcast Studio antes de declarar cerrada la interfaz. El frente visual puede seguir iterando, pero no debe ocultar deuda del núcleo.

## Principio rector vigente

DocuPodcast Studio V1 se organiza alrededor de un **Documento narrable** creado desde una fuente externa de solo lectura. El usuario abre DOCX, PDF con texto nativo, Markdown/MD o TXT; la app importa una versión narrable dentro del proyecto y trabaja con capas, audio, storyboard y exportaciones sin modificar nunca la fuente.

La app debe sentirse como lector narrado, no como cabina técnica. Pero el cerebro debe ser robusto aunque la UI siga en debate.

---

## T72 — Escuchar documento end-to-end

### Objetivo
Cerrar el flujo principal que un usuario entiende sin entrar en pantallas avanzadas: abrir documento, leerlo, escucharlo, pausar, reanudar y reproducir desde una selección.

### Alcance funcional
- Usar Documento narrable como raíz visible.
- Generar o recuperar la proyección interna de narración solo cuando haga falta.
- Reutilizar audio ya generado si sigue vigente.
- Iniciar reproducción cuando exista buffer inicial suficiente.
- Reanudar después de pausas por falta de fragmentos.
- Resaltar bloque/oración activa sin exponer `manifest`, `job`, `segmentId` ni `chunk` al usuario normal.
- Reproducir desde selección documental siempre que pueda mapearse a la proyección interna.

### Criterios de salida
- El usuario puede abrir un documento y pulsar **Escuchar documento** sin pasar por “Narración avanzada”.
- El sistema sabe explicar: “preparando audio”, “reproduciendo”, “esperando siguiente fragmento”, “listo”.
- El estado técnico queda para diagnóstico.

### Guardarraíles esperados
- `DocumentListenEndToEndUseCaseTest` o equivalente de aplicación.
- `DocumentListenFlowProductSourceTest` para evitar regreso a lenguaje técnico visible.

---

## T73 — Audio jobs robustos

### Objetivo
Cerrar el manejo de generación de audio como una cola de trabajos confiable y recuperable.

### Alcance funcional
- Crear jobs por documento/proyección interna.
- Persistir estado por segmento.
- Reanudar jobs parciales.
- Cancelar sin dejar archivos corruptos.
- Detectar segmentos faltantes o fallidos.
- Reutilizar segmentos ya válidos.
- Marcar audio obsoleto si el documento fuente cambia.
- Separar motor real, mock y diagnóstico.

### Criterios de salida
- Un cierre inesperado no debe destruir la posibilidad de continuar.
- La app debe distinguir `pendiente`, `generando`, `completo`, `fallido`, `cancelado`, `obsoleto`.
- El usuario normal solo ve progreso útil; el detalle técnico vive en Audio/Diagnóstico.

### Guardarraíles esperados
- `AudioJobRecoveryUseCaseTest`.
- `AudioJobStalenessPolicyTest`.
- `AudioWorkflowCoordinatorRecoverySourceTest`.

---

## T74 — Capas narrativas reales

### Objetivo
Eliminar el comportamiento de placeholder en capas y asegurar que cada asignación apunte a una intención o asset real.

### Alcance funcional
- Voz IA: asignar una voz real o un perfil explícito.
- Audio humano: apuntar a un asset de audio real.
- Imagen: apuntar a un asset de imagen real.
- Emoción/estilo: usar catálogo claro de intención narrativa.
- Ambiente: asset o intención explícita, no texto pendiente ambiguo.
- Nota: libre, pero separada de capas que afectan render/audio.
- Resolver conflictos entre voz principal y audio humano.

### Criterios de salida
- Si el usuario pulsa “asociar imagen”, debe terminar con una imagen real o cancelar sin crear capa falsa.
- Las capas deben sobrevivir a guardar/reabrir y a refrescos de fuente con estado vigente/en revisión.

### Guardarraíles esperados
- `NarrativeLayerRealTargetTest`.
- `NarrativeLayerRoundTripRealAssetsTest`.
- `NoPlaceholderNarrativeLayerSourceTest`.

---

## T75 — Storyboard como capa del documento

### Objetivo
Hacer que storyboard sea una extensión natural del documento narrado: imagen asociada a texto que dura lo que dura el texto hablado.

### Alcance funcional
- Asociar una imagen a un bloque, oración o rango documentado.
- Resolver duración desde audio/segmento asociado.
- Permitir reutilizar la misma imagen en varios fragmentos.
- Mostrar fragmentos sin imagen con fallback visual neutro.
- Guardar y reabrir storyboard con assets relativos.
- Evitar duplicar imágenes cuando se reutilizan.

### Criterios de salida
- “Esta imagen acompaña este texto” debe ser la regla mental, no “edito una línea de timeline”.
- El storyboard no debe convertir la app en editor de video complejo.

### Guardarraíles esperados
- `StoryboardImageReuseTest`.
- `StoryboardDurationFromNarrationTest`.
- `StoryboardRoundTripDocumentLayerTest`.

---

## T76 — Video package / render contract

### Objetivo
Cerrar el contrato de video de forma honesta: paquete renderizable siempre; MP4 real solo si FFmpeg está operativo.

### Alcance funcional
- Construir plan de video desde Documento narrable + audio + storyboard.
- Resolver resolución, duración por fragmento, assets de imagen y audio.
- Validar FFmpeg disponible cuando se pida MP4 real.
- Generar paquete auditable si FFmpeg no está listo.
- Registrar progreso por etapas: preparar, resolver assets, renderizar, unir, finalizar.
- Cancelar render de forma segura.

### Criterios de salida
- La app nunca debe decir “MP4 generado” si solo preparó un paquete.
- Si FFmpeg falta, el usuario recibe explicación clara y salida útil.

### Guardarraíles esperados
- `VideoRenderPlanUseCaseTest`.
- `VideoFfmpegPreflightTest`.
- `VideoRenderHonestySourceTest`.

---

## T77 — Integridad y reparación del proyecto

### Objetivo
Permitir que el proyecto se audite a sí mismo y reporte si está listo, con advertencias o requiere reparación.

### Alcance funcional
- Validar documento materializado.
- Validar fuente solo lectura y snapshot.
- Validar capas con targets reales.
- Validar assets existentes y checksums.
- Validar jobs completos, reanudables u obsoletos.
- Validar storyboard consistente.
- Validar configuración mínima requerida.
- Producir reporte legible.

### Criterios de salida
- Estado posible: `OK`, `Con advertencias`, `Requiere reparación`.
- El proyecto no debe fallar silenciosamente al reabrir.

### Guardarraíles esperados
- `ProjectIntegrityReportUseCaseTest`.
- `MissingAssetIntegrityTest`.
- `StaleAudioIntegrityTest`.

---

## T78 — Exportaciones del cerebro

### Objetivo
Cerrar las salidas principales sin depender de botones concretos del frontend.

### Alcance funcional
- Exportar paquete del proyecto.
- Exportar audio/podcast o paquete de audio segmentado.
- Exportar narración Markdown compatible.
- Exportar storyboard/video plan.
- Exportar diagnóstico técnico.
- Exportar evidencia de fuente/capas/snapshots.
- Crear manifiestos con rutas, formatos, hashes y limitaciones.

### Criterios de salida
- Cada exportación debe declarar qué contiene y qué no contiene.
- No se debe prometer formato sin cadena real.

### Guardarraíles esperados
- `BrainExportManifestTest`.
- `ExportedPackageIntegrityTest`.
- `ExportHonestySourceTest`.

---

## T79 — Smoke automático del cerebro

### Objetivo
Probar el núcleo sin depender de capturas ni de la estética del frontend.

### Escenarios mínimos
- DOCX simple.
- TXT.
- Markdown.
- PDF con texto nativo.
- PDF escaneado rechazado.
- Documento largo.
- Refresco con cambios externos.
- Capa de voz/imagen/audio.
- Guardar/reabrir.
- Audio mock.
- Storyboard con imagen reutilizada.
- Exportación de paquete.

### Criterios de salida
- Smoke ejecutable o semiautomatizado con evidencia clara.
- Si algo falla, se clasifica como problema de cerebro, infraestructura o UI.

---

## T80 — Congelación del cerebro V1

### Objetivo
Cerrar oficialmente las capacidades del núcleo antes de entrar al rediseño visual serio.

### Entregables
- Matriz de capacidades V1.
- Matriz de limitaciones V1.
- Casos de uso cerrados.
- Contratos de fuente, refresco, audio, capas, storyboard, video y exportación.
- Lista explícita de cosas V2: OCR, fidelidad Word/PDF completa, editor de texto completo, editor de video avanzado, nube/colaboración.

### Criterio de salida
- El equipo puede rediseñar la interfaz sin reinterpretar el producto.

---

## T81 — Debate y rediseño frontal guiado

### Objetivo
Volver al frontend después de cerrar el cerebro, usando los criterios ya escritos.

### Alcance funcional
- Inspirarse en interfaces ofimáticas solo donde sirva: página centrada, grupos de acciones, estado, zoom, lectura cómoda.
- Evitar imitar Word como procesador completo.
- Separar usuario normal vs avanzado.
- Diseñar Inicio, Documento, Configuración, Audio, Storyboard y Exportación con jerarquía clara.

### Criterio de salida
- Prototipo visual coherente con el cerebro real.
- Ninguna pantalla debe exponer “cabina de avión” al usuario normal.

---

## T82 — Release Candidate

### Objetivo
Empaquetar una V1 instalable o candidata, solo después del smoke y congelación del cerebro.

### Alcance
- App-image/MSI.
- Icono/branding.
- Hashes SHA-256.
- Manifiestos de binarios y licencias.
- Guía de instalación.
- Guía de motores/modelos/FFmpeg.
- Limitaciones conocidas.
- Checklist final.


## Actualización T73

T73 ejecuta la parte de audio jobs robustos: inspección de mantenimiento, detección de WAVs faltantes, audio obsoleto tras refrescar fuente y reanudación. Las tandas pendientes activas comienzan ahora en T74.


## Actualización T75 implementada

T75 quedó ejecutada: storyboard como capa del documento, bindings desde capas IMAGE reales y reutilización de imágenes en varios fragmentos. El siguiente foco del cerebro es T76: video package / render contract.

---

## Actualización T76

T76 ejecuta la tanda de video package / render contract. Desde esta tanda, el cerebro de video simple tiene `docupodcast-simple-video-render-v1`, `RENDER_MANIFEST.json`, `render-commands.txt`, `RENDER_STATE.md` y un contrato explícito de bloqueo operativo/cancelación segura.

Quedan pendientes después de T76:

1. T77 — Integridad y reparación del proyecto.
2. T78 — Exportaciones del cerebro.
3. T79 — Smoke automático del cerebro.
4. T80 — Congelación del cerebro V1.
5. T81 — Rediseño frontal guiado.
6. T82 — Release Candidate.
