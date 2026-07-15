# Tanda 100 - Base PDF visual transversal, regiones y ComfyUI alto consumo

## Que se implemento

- Se agrego una politica transversal de uso PDF por modo de proyecto:
  - `DOCUMENTARY_STUDIO`: PDF visual de primera clase para estudio documental.
  - `THEATRE_PRODUCTION` y `NARRATIVE_VIDEO`: PDF permitido como referencia visual de solo lectura, con recomendacion de DOCX/Markdown para guion editable.
- Se agregaron contratos puros para seleccion rectangular sobre PDF renderizado:
  - `PdfViewportSelection`;
  - `PdfPageRegion`;
  - `PdfRegionCaptureRequest`;
  - `PdfRegionCaptureResult`;
  - `CapturePdfVisualRegionUseCase`.
- Se dejo preparada la capa textual futura de PDF:
  - `PdfTextLayer`;
  - `PdfTextLine`;
  - `PdfTextToken`;
  - `PdfTextLayerOrigin`.
- `StudySourceReference` ahora tiene factory `visualRegion(...)` para representar capturas PDF sin exigir texto.
- Se agrego perfil persistente de memoria para Imagen IA teatral:
  - `SAFE_LOW_VRAM`;
  - `NORMAL`;
  - `HIGH_MEMORY`.
- `ImageGenerationSettings` conserva compatibilidad con `lowVram` legacy y persiste `image.memoryProfile`.
- Configuracion muestra selector de perfil de memoria y diferencia mejor:
  - descargar modelo;
  - importar modelo local;
  - importar runtime;
  - importar workflow/adaptadores/componentes.
- Se agrego probe de runtime ComfyUI:
  - `ComfyUiRuntimeCapabilityProbe`;
  - `ComfyUiRuntimeCapabilities`;
  - `ComfyUiLaunchArgumentPlanner`.
- El arranque local de ComfyUI agrega flags de memoria segun perfil y solo cuando el runtime los soporta. En modo alto consumo no fuerza flags agresivos si no hay probe confiable.
- El diagnostico de Imagen IA incluye perfil de memoria y `lowVramLegacy`.
- La deteccion Windows de GPU ahora intenta leer `AdapterRAM` para mostrar VRAM aproximada cuando Windows la expone.

## Que quedo fuera

- No se cambio la interfaz del workspace central.
- No se implemento visor PDF visual en UI.
- No se implemento seleccion rectangular interactiva en pantalla.
- No se implemento OCR.
- No se reconstruye LaTeX editable.
- No se agrego UI para token Hugging Face.
- No se garantiza ejecucion de modelos Flux grandes; solo se deja perfil y diagnostico explicito.

## Decisiones tecnicas

- PDFBox sigue siendo el backend visual embebido transversal.
- La seleccion rectangular se modela como conversion viewport -> puntos PDF -> crop PNG.
- Las capturas futuras reutilizan el contrato v3 existente: `STUDY_SOURCE_CROP`, `sourcePage`, `bbox`, `sourceCropAssetId` y `selectedText` opcional.
- La capa visual y la capa textual PDF quedan separadas para evitar depender de OCR o texto nativo defectuoso.
- En Teatro y Video narrativo, PDF se trata como referencia; el material editable recomendado sigue siendo DOCX/Markdown.
- ComfyUI alto consumo es opt-in y se basa en capacidades detectadas por `main.py --help`, no en flags hardcodeados a ciegas.

## Archivos y sistemas tocados

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/project/ProjectPdfSourcePolicy.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfViewportSelection.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfPageRegion.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfRegionCaptureRequest.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfRegionCaptureResult.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/CapturePdfVisualRegionUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/document/PdfTextLayer*.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/study/StudySourceReference.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/settings/ImageGenerationSettings.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/settings/ImageGenerationMemoryProfile.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ComfyUiRuntimeCapabilityProbe.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ComfyUiRuntimeCapabilities.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ComfyUiLaunchArgumentPlanner.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/LocalTheatreImageEngineManager.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/settings/PropertiesOperationalSettingsRepository.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/compute/WindowsComputeDeviceDiscoveryGateway.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsFormModel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/ImageEngineSettingsCard.java`
- `docs/productizacion/DOCUMENT_SOURCE_UNIFICADO_PDF_T70.md`
- `docs/productizacion/ESTUDIO_DOCUMENTAL_PROBLEMAS_TECNICOS.md`
- `docs/productizacion/IMAGEN_IA_TEATRAL_COMFYUI.md`

## Tests ejecutados

- `mvn -q -DskipTests compile`
- `mvn -q -Dtest="ProjectPdfSourcePolicyTest,PdfVisualRegionCaptureUseCaseTest,ComfyUiLaunchArgumentPlannerTest,PropertiesOperationalSettingsRepositoryTest,TheatreImageEngineProductizationSourceTest,PdfVisualComfyT100SourceTest" test`
- `mvn -q test`

Resultado: verde. El test completo imprimio advertencias de PDFBox sobre offsets de streams en fixtures PDF, sin fallar.

## Proximos pasos exactos

1. Tanda 101: crear visor PDF visual central para fuentes PDF, usando `BuildPdfVisualDocumentUseCase`, sin seleccion rectangular todavia.
2. Mantener DOCX/Markdown/TXT con el lector por bloques actual.
3. Mostrar paginas PDF renderizadas con virtualizacion y cache segura.
4. Conservar SideDock izquierdo/derecho y playback sin cambiar contrato de proyecto.
5. Preparar Tanda 102 para seleccion rectangular sobre el visor PDF y acumulacion de capturas `STUDY_SOURCE_CROP`.
