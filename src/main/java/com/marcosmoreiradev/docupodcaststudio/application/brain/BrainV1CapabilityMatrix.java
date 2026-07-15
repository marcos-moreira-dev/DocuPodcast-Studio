package com.marcosmoreiradev.docupodcaststudio.application.brain;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Executable product contract for the V1 brain.
 *
 * <p>This matrix is intentionally independent from JavaFX. It freezes what the
 * core can do, what it can only do with limits, and what remains explicitly out
 * of scope before the visual redesign.</p>
 */
public record BrainV1CapabilityMatrix(List<BrainV1Capability> capabilities) {
    public BrainV1CapabilityMatrix {
        Objects.requireNonNull(capabilities, "capabilities");
        capabilities = List.copyOf(capabilities);
        ensureUniqueIds(capabilities);
    }

    public static BrainV1CapabilityMatrix current() {
        return new BrainV1CapabilityMatrix(List.of(
                cap("document-intake", BrainV1CapabilityArea.DOCUMENT_INTAKE,
                        "Abrir documento fuente",
                        BrainV1CapabilityStatus.V1_READY_WITH_LIMITS,
                        "DOCX, PDF con texto nativo u OCR local cuando no haya texto suficiente, Markdown/MD y TXT entran como fuentes solo lectura.",
                        "No hay fidelidad semántica Word/PDF perfecta; si OCR local falla, el PDF visual se renderiza como páginas.",
                        "DocumentSourceImportService, DocxDocumentImporter, PdfDocumentImporter, TesseractPdfOcrEngine, PdfBoxRenderEngine, PlainTextMarkdownDocumentImporter"),
                cap("document-refresh", BrainV1CapabilityArea.DOCUMENT_INTAKE,
                        "Refrescar fuente externa",
                        BrainV1CapabilityStatus.V1_READY_WITH_LIMITS,
                        "El usuario puede refrescar el contenido desde la fuente original sin modificarla dentro de DocuPodcast.",
                        "Los derivados pueden quedar obsoletos o en revisión; no se fusionan cambios manualmente como procesador de texto.",
                        "RefreshSourceDocumentUseCase, SourceDocumentChangeReport"),
                cap("project-container", BrainV1CapabilityArea.PROJECT_PERSISTENCE,
                        "Carpeta contenedora del proyecto",
                        BrainV1CapabilityStatus.V1_READY,
                        "Guardar como crea una carpeta de proyecto y guarda allí el .docupodcast.json y sus recursos.",
                        "Proyectos antiguos fuera del contenedor se pueden abrir, pero el contrato nuevo se aplica a Guardar como.",
                        "ProjectContainerPathPolicy"),
                cap("roundtrip", BrainV1CapabilityArea.PROJECT_PERSISTENCE,
                        "Guardar, cerrar y reabrir",
                        BrainV1CapabilityStatus.V1_READY,
                        "Documento, narración interna, storyboard, capas, assets y jobs sobreviven al round-trip.",
                        "La reparación avanzada todavía se reporta como sugerencia, no como asistente interactivo completo.",
                        "ProjectRoundTripUseCase, LoadProjectWorkspaceArtifactsUseCase"),
                cap("listen-document", BrainV1CapabilityArea.LISTENING_PLAYBACK,
                        "Escuchar documento",
                        BrainV1CapabilityStatus.V1_READY_WITH_LIMITS,
                        "La acción principal prepara narración interna, audio, buffer y playback desde Documento.",
                        "La calidad real depende del motor TTS configurado; el modo mock solo prueba flujo y persistencia.",
                        "PrepareDocumentListeningUseCase, DocumentNarrationCoordinator, PlaybackWorkflowCoordinator"),
                cap("playback-buffer", BrainV1CapabilityArea.LISTENING_PLAYBACK,
                        "Playback con buffer y cursor",
                        BrainV1CapabilityStatus.V1_READY_WITH_LIMITS,
                        "La reproducción puede avanzar por segmentos y esperar fragmentos pendientes sin romper el flujo.",
                        "El subrayado por oración exacta aún depende de la relación entre segmentos y rangos importados.",
                        "PlaybackManifest, StreamingPlaybackWindow, PlaybackCursor"),
                cap("audio-jobs", BrainV1CapabilityArea.LISTENING_PLAYBACK,
                        "Jobs de audio robustos",
                        BrainV1CapabilityStatus.V1_READY_WITH_LIMITS,
                        "Los jobs pueden inspeccionarse como listos, reanudables, obsoletos o con audio faltante.",
                        "No hay cola distribuida ni ejecución en segundo plano fuera de la app local.",
                        "InspectAudioJobMaintenanceUseCase, AudioJobFileRepository"),
                cap("narrative-layers", BrainV1CapabilityArea.NARRATIVE_LAYERS,
                        "Capas narrativas reales",
                        BrainV1CapabilityStatus.V1_READY_WITH_LIMITS,
                        "Voz, emoción/estilo, imagen y audio se guardan como capas del proyecto, no dentro del documento original.",
                        "La interfaz final debe mostrar pocas acciones y no dividir innecesariamente tipos de audio por botones.",
                        "NarrativeLayerAssignment, NarrativeLayerTargetResolver, NarrativeLayerCoordinator"),
                cap("media-input", BrainV1CapabilityArea.MEDIA_ASSETS,
                        "Audio flexible para capas",
                        BrainV1CapabilityStatus.V1_READY_WITH_LIMITS,
                        "MP3 y WAV se importan como audio; videos MP4/MOV/MKV/WEBM pueden usarse para extraer solo el audio.",
                        "La extracción de audio desde video requiere FFmpeg disponible y deja registro de asset original y derivado.",
                        "ImportUserMediaAssetUseCase, UserMediaFormatPolicy, FfmpegVideoAudioExtractionGateway"),
                cap("storyboard-layer", BrainV1CapabilityArea.STORYBOARD_VIDEO,
                        "Storyboard como capa del documento",
                        BrainV1CapabilityStatus.V1_READY_WITH_LIMITS,
                        "Imágenes reales asociadas a segmentos generan storyboard y pueden reutilizarse en varios fragmentos.",
                        "No es un editor de video; es una proyección visual simple del documento narrado.",
                        "BuildStoryboardFromImageLayersUseCase, StoryboardDocument"),
                cap("simple-video-package", BrainV1CapabilityArea.STORYBOARD_VIDEO,
                        "Paquete de video simple",
                        BrainV1CapabilityStatus.V1_READY_WITH_LIMITS,
                        "El cerebro genera plan, manifest, comandos y script para renderizar video simple con FFmpeg.",
                        "El MP4 final depende de FFmpeg y del estado del render; la app no promete editor ni render avanzado.",
                        "ExportSimpleVideoPackageUseCase, VideoRenderCommandPlan"),
                cap("export-readiness", BrainV1CapabilityArea.EXPORTS,
                        "Matriz de exportación",
                        BrainV1CapabilityStatus.V1_READY,
                        "El cerebro declara qué salidas son exportables, bloqueadas o exportables con advertencias.",
                        "No debe mostrarse una acción visible si el readiness indica bloqueo real.",
                        "InspectExportReadinessUseCase, ExportReadinessReport"),
                cap("auditable-bundle", BrainV1CapabilityArea.EXPORTS,
                        "Bundle auditable",
                        BrainV1CapabilityStatus.V1_READY_WITH_LIMITS,
                        "El paquete incluye input, editable, output, assets, jobs, reports, índices y hashes cuando existen.",
                        "Los artefactos inexistentes o bloqueados se explican, no se inventan.",
                        "FileSystemProjectBundleExporter, BUNDLE_FILE_INDEX.tsv, EXPORT_READINESS.md"),
                cap("project-integrity", BrainV1CapabilityArea.INTEGRITY_DIAGNOSTICS,
                        "Integridad y reparación reportada",
                        BrainV1CapabilityStatus.V1_READY,
                        "El proyecto puede reportarse como OK, con advertencias o requiere reparación.",
                        "La reparación automática guiada queda para una tanda posterior si se necesita UX dedicada.",
                        "InspectProjectIntegrityUseCase, ProjectIntegrityReport"),
                cap("operational-settings", BrainV1CapabilityArea.SETTINGS_COMPUTE,
                        "Configuración operativa persistente",
                        BrainV1CapabilityStatus.V1_READY_WITH_LIMITS,
                        "Lectura, buffer, TTS, FFmpeg, modelos, diagnóstico y compute viven en Configuración.",
                        "Documento no debe exponer rutas, comandos, CUDA, NVENC ni parámetros crudos salvo en modo avanzado.",
                        "OperationalSettings, PropertiesOperationalSettingsRepository, SettingsDialog"),
                cap("compute-device", BrainV1CapabilityArea.SETTINGS_COMPUTE,
                        "CPU/GPU como política del cerebro",
                        BrainV1CapabilityStatus.V1_READY_WITH_LIMITS,
                        "AUTO, CPU_ONLY, PREFER_GPU y SPECIFIC_DEVICE quedan modelados para TTS y video.",
                        "La detección es conservadora y no garantiza compatibilidad de todos los motores externos.",
                        "ComputeSettings, InspectComputeEnvironmentUseCase, VideoEncoderPolicy"),
                cap("brain-smoke", BrainV1CapabilityArea.SMOKE_VALIDATION,
                        "Smoke automático del cerebro",
                        BrainV1CapabilityStatus.V1_READY,
                        "Existe un escenario automático sin JavaFX que ejercita intake, narración, audio mock, integridad y exportaciones.",
                        "No reemplaza el smoke manual de UX, pero protege el núcleo antes del rediseño frontal.",
                        "BrainSmokeScenarioTest, scripts/18-smoke-automatico-cerebro.bat"),
                cap("ocr", BrainV1CapabilityArea.DOCUMENT_INTAKE,
                        "OCR para PDF escaneado",
                        BrainV1CapabilityStatus.V1_READY_WITH_LIMITS,
                        "Los PDF escaneados o solo-imagen pueden convertirse en bloques narrables mediante OCR local cuando no hay texto nativo suficiente.",
                        "Depende de Tesseract CLI local y no promete lectura profunda de formulas, tablas o diagramas.",
                        "PdfDocumentImporter, BuildPdfOcrTextLayerUseCase, TesseractPdfOcrEngine, PdfBoxRenderEngine"),
                cap("rich-word-editor", BrainV1CapabilityArea.V2_DEFERRED,
                        "Editor Word completo",
                        BrainV1CapabilityStatus.V2_DEFERRED,
                        "El documento fuente es solo lectura; las capas se guardan en el proyecto.",
                        "No hay edición WYSIWYG de DOCX, control de cambios ni fidelidad de procesador de texto.",
                        "CONTRATO_DOCUMENTOS_SOLO_LECTURA_V1"),
                cap("advanced-video-editor", BrainV1CapabilityArea.V2_DEFERRED,
                        "Editor de video avanzado",
                        BrainV1CapabilityStatus.V2_DEFERRED,
                        "El video V1 es paquete simple derivado de storyboard/audio.",
                        "No hay timeline multipista, transiciones avanzadas ni compositor visual.",
                        "VIDEO_RENDER_CONTRACT_T76"),
                cap("cloud-collaboration", BrainV1CapabilityArea.V2_DEFERRED,
                        "Nube y colaboración",
                        BrainV1CapabilityStatus.V2_DEFERRED,
                        "DocuPodcast V1 es app local autocontenida.",
                        "No hay cuentas, sincronización, backend remoto ni colaboración multiusuario.",
                        "MAPA_CEREBRO_APP")
        ));
    }

    public Optional<BrainV1Capability> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return capabilities.stream().filter(capability -> capability.id().equals(normalized)).findFirst();
    }

    public List<BrainV1Capability> availableInV1() {
        return capabilities.stream()
                .filter(BrainV1Capability::availableInV1)
                .toList();
    }

    public List<BrainV1Capability> deferredToV2() {
        return capabilities.stream()
                .filter(capability -> capability.status() == BrainV1CapabilityStatus.V2_DEFERRED)
                .toList();
    }

    public List<BrainV1Capability> byArea(BrainV1CapabilityArea area) {
        Objects.requireNonNull(area, "area");
        return capabilities.stream()
                .filter(capability -> capability.area() == area)
                .toList();
    }

    public String toMarkdown() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Matriz congelada del cerebro V1\n\n");
        builder.append("| ID | Área | Título | Estado | Contrato V1 | Límite | Evidencia |\n");
        builder.append("|---|---|---|---|---|---|---|\n");
        capabilities.stream()
                .sorted(Comparator.comparing(BrainV1Capability::area).thenComparing(BrainV1Capability::id))
                .forEach(capability -> builder.append("| ")
                        .append(capability.id()).append(" | ")
                        .append(capability.area()).append(" | ")
                        .append(escape(capability.title())).append(" | ")
                        .append(capability.status()).append(" | ")
                        .append(escape(capability.userContract())).append(" | ")
                        .append(escape(capability.limitation())).append(" | ")
                        .append(escape(capability.evidence())).append(" |\n"));
        return builder.toString();
    }

    private static BrainV1Capability cap(
            String id,
            BrainV1CapabilityArea area,
            String title,
            BrainV1CapabilityStatus status,
            String userContract,
            String limitation,
            String evidence) {
        return new BrainV1Capability(id, area, title, status, userContract, limitation, evidence);
    }

    private static void ensureUniqueIds(List<BrainV1Capability> capabilities) {
        Set<String> ids = new LinkedHashSet<>();
        List<String> duplicates = capabilities.stream()
                .map(BrainV1Capability::id)
                .filter(id -> !ids.add(id))
                .collect(Collectors.toList());
        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException("duplicated capability ids: " + duplicates);
        }
    }

    private static String escape(String value) {
        return value.replace("|", "\\|").replace("\n", " ");
    }
}
