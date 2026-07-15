# Congelación del cerebro V1 — T80

Esta tanda congela el **cerebro V1** de DocuPodcast antes de entrar al rediseño frontal fuerte. La intención es separar lo que ya está contratado en dominio/aplicación/infraestructura de lo que debe resolverse después en la presentación.

La regla rectora permanece:

```text
Documento narrable primero.
Pantalla principal limpia.
Configuración = ajustes operativos avanzados.
Cerebro auditable antes de rediseñar cara.
```

DocuPodcast debe ser lector narrado, no cabina de avión: no debe sentirse como cabina de avión para quien solo quiere abrir un documento, escucharlo y asignar una emoción, una imagen o un audio a una oración. La complejidad existe, pero debe vivir en Configuración, Diagnóstico, manifiestos y reportes.

## Pieza ejecutable de congelación

T80 agrega una matriz ejecutable:

```text
application.brain.BrainV1CapabilityMatrix
application.brain.BrainV1Capability
application.brain.BrainV1CapabilityArea
application.brain.BrainV1CapabilityStatus
```

La matriz no depende de JavaFX. Declara qué capacidades están disponibles en V1, cuáles están disponibles con límites y cuáles quedan explícitamente diferidas a V2.

Estados:

```text
V1_READY
V1_READY_WITH_LIMITS
V1_INTERNAL_ADVANCED
V2_DEFERRED
```

## Capacidades congeladas de V1

| Área | Contrato congelado |
|---|---|
| Documento | DOCX, PDF con texto nativo u OCR local cuando no haya texto suficiente, Markdown/MD y TXT como fuentes solo lectura. |
| Refresco | La fuente externa puede refrescarse sin editar el archivo original dentro de DocuPodcast. |
| Proyecto | Guardar como crea carpeta contenedora: `.docupodcast.json` + recursos internos. |
| Round-trip | Documento, narración interna, capas, storyboard, assets y jobs sobreviven al reabrir. |
| Escucha | Escuchar documento prepara narración interna, audio/buffer y playback desde Documento. |
| Playback | Cursor, manifest y ventana de buffer protegen reproducción por segmentos. |
| Audio jobs | Jobs listos, reanudables, obsoletos o con WAV faltante se inspeccionan de forma explícita. |
| Capas narrativas | Voz, emoción/estilo, imagen y audio son capas del proyecto, no texto insertado en Word/PDF. |
| Media | MP3/WAV/video→audio quedan modelados como entrada flexible para capas de audio. |
| Storyboard | Storyboard es capa visual opcional derivada del documento narrado. |
| Video simple | Se genera paquete auditable con manifest, plan, comandos y script de FFmpeg. |
| Exportaciones | `InspectExportReadinessUseCase` declara exportable, bloqueado o exportable con advertencias. |
| Integridad | `InspectProjectIntegrityUseCase` reporta OK, advertencias o reparación requerida. |
| Configuración | TTS, STT, FFmpeg, modelos, buffer, CPU/GPU y diagnóstico viven en Configuración. |
| Compute | CPU/GPU se modela como política: AUTO, CPU_ONLY, PREFER_GPU, SPECIFIC_DEVICE. |
| smoke automático | `BrainSmokeScenarioTest` protege el núcleo sin JavaFX. |

## Límites congelados de V1

Estos límites son intencionales. No deben mostrarse como errores ni promesas incompletas.

| Límite | Decisión V1 |
|---|---|
| OCR | OCR local por Tesseract puede convertir PDF escaneado o solo-imagen en bloques narrables; si falla, se conserva el fallback visual con aviso. |
| Word completo | No hay editor DOCX WYSIWYG ni control de cambios. |
| PDF perfecto | Solo texto nativo básico; no fidelidad visual completa. |
| Video avanzado | No hay timeline multipista ni editor de video. |
| Nube | No hay backend remoto, cuentas ni colaboración. |
| Descarga de modelos | No hay descarga automática obligatoria de XTTS/Piper/Whisper. |
| GPU perfecta | La detección CPU/GPU es conservadora; motores externos pueden requerir configuración propia. |
| Reparación interactiva | La integridad reporta y sugiere; el asistente visual de reparación puede venir después. |

## Reglas para el frontend posterior

La congelación del cerebro impone estas reglas a T81:

```text
Documento no debe exponer chunks, job ids, manifests, gateway, checksum ni FFmpeg como lenguaje principal.
Capas debe ofrecer acciones simples: asignar emoción, asignar imagen, asignar audio.
Asignar audio no debe dividirse en botones redundantes para voz humana, efecto, ambiente o música.
La nota de responsabilidad sobre nombres coherentes de archivos debe ser no invasiva.
Configuración puede ser técnica porque funciona como configuración avanzada.
No se debe reintroducir botonera JavaFX hardcodeada en workspaces.
```

Microcopy sugerida para la futura UI:

```text
Consejo: usa nombres claros para tus archivos de audio, por ejemplo “Lucía hablando” o “pájaros cantando”, para recordar qué representa cada capa.
```

## Evidencia de cierre del cerebro

La evidencia mínima para considerar congelado el cerebro V1 es:

```text
ProjectRoundTripUseCase
InspectProjectIntegrityUseCase
InspectExportReadinessUseCase
BrainSmokeScenarioTest
BrainV1CapabilityMatrix
OperationalSettings + ComputeSettings
ImportUserMediaAssetUseCase
ExportSimpleVideoPackageUseCase
```

## Resultado

T80 no rediseña la interfaz. Deja el núcleo en estado congelado y auditable para pasar a:

```text
Lectura 15 — frontend quirúrgico
T81 — rediseño frontal guiado
T82 — release candidate
```
