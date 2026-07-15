package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobMaintenanceReport;
import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioJobRepository;
import com.marcosmoreiradev.docupodcaststudio.application.audio.InspectAudioJobMaintenanceUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds a non-binary integrity report for a project folder.
 *
 * <p>This is the T77 brain contract: the project can be OK, usable with warnings or
 * require repair. The use case inspects materialized document/script/storyboard artifacts,
 * project assets, checksums, narrative layers and persisted audio jobs without introducing
 * JavaFX or frontend decisions.</p>
 */
public final class InspectProjectIntegrityUseCase {
    private final AudioJobRepository audioJobRepository;
    private final InspectAudioJobMaintenanceUseCase inspectAudioJobMaintenance;

    public InspectProjectIntegrityUseCase() {
        this(null, new InspectAudioJobMaintenanceUseCase());
    }

    public InspectProjectIntegrityUseCase(AudioJobRepository audioJobRepository) {
        this(audioJobRepository, new InspectAudioJobMaintenanceUseCase());
    }

    public InspectProjectIntegrityUseCase(
            AudioJobRepository audioJobRepository,
            InspectAudioJobMaintenanceUseCase inspectAudioJobMaintenance
    ) {
        this.audioJobRepository = audioJobRepository;
        this.inspectAudioJobMaintenance = Objects.requireNonNull(inspectAudioJobMaintenance, "inspectAudioJobMaintenance");
    }

    public ProjectIntegrityReport inspect(DocuPodcastProject project, Path projectFile, ProjectWorkspaceHydration hydration) throws IOException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(projectFile, "projectFile");
        Path projectRoot = projectRoot(projectFile);
        List<AudioJobSnapshot> jobs = audioJobRepository == null || projectRoot == null ? List.of() : audioJobRepository.list(projectRoot);
        return inspect(project, projectFile, hydration, jobs);
    }

    public ProjectIntegrityReport inspect(
            DocuPodcastProject project,
            Path projectFile,
            ProjectWorkspaceHydration hydration,
            List<AudioJobSnapshot> audioJobs
    ) throws IOException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(projectFile, "projectFile");
        ProjectWorkspaceHydration safeHydration = hydration == null ? ProjectWorkspaceHydration.empty() : hydration;
        List<AudioJobSnapshot> safeJobs = audioJobs == null ? List.of() : List.copyOf(audioJobs);
        ArrayList<ProjectIntegrityIssue> issues = new ArrayList<>();

        Path projectRoot = validateProjectRoot(projectFile, issues);
        if (projectRoot != null) {
            validateAssetFiles(project, projectRoot, issues);
        }
        validateMaterializedArtifacts(project, safeHydration, issues);
        validateNarrativeLayers(project, safeHydration, issues);
        validateStoryboardBindings(project, safeHydration, issues);
        if (projectRoot != null) {
            validateAudioJobs(projectRoot, safeJobs, issues);
            validateVideoPackageIfPresent(projectRoot, issues);
        }

        return ProjectIntegrityReport.from(project.metadata().title(), issues);
    }

    private static Path validateProjectRoot(Path projectFile, List<ProjectIntegrityIssue> issues) {
        Path normalizedFile = projectFile.toAbsolutePath().normalize();
        Path root = normalizedFile.getParent();
        if (root == null) {
            issues.add(ProjectIntegrityIssue.error(
                    "PROJECT_ROOT_MISSING",
                    projectFile.toString(),
                    "El archivo .docupodcast debe estar dentro de una carpeta de proyecto.",
                    "Mueve o guarda el proyecto dentro de una carpeta propia."));
            return null;
        }
        if (!Files.isDirectory(root)) {
            issues.add(ProjectIntegrityIssue.error(
                    "PROJECT_ROOT_MISSING",
                    root.toString(),
                    "La carpeta del proyecto no existe.",
                    "Restaura la carpeta del proyecto o abre el archivo correcto."));
            return null;
        }
        return root;
    }

    private static Path projectRoot(Path projectFile) {
        Path root = projectFile.toAbsolutePath().normalize().getParent();
        return root != null && Files.isDirectory(root) ? root : null;
    }

    private static void validateAssetFiles(DocuPodcastProject project, Path projectRoot, List<ProjectIntegrityIssue> issues) throws IOException {
        for (ProjectAssetReference asset : project.assets().references()) {
            Path resolved = projectRoot.resolve(asset.relativePath()).normalize();
            if (!resolved.startsWith(projectRoot)) {
                issues.add(ProjectIntegrityIssue.error(
                        "ASSET_OUTSIDE_PROJECT",
                        asset.id(),
                        "El asset sale de la carpeta del proyecto: " + asset.relativePath() + ".",
                        "Reimporta el archivo para copiarlo dentro del proyecto."));
                continue;
            }
            if (!Files.exists(resolved)) {
                issues.add(ProjectIntegrityIssue.error(
                        "ASSET_FILE_MISSING",
                        asset.id(),
                        "Falta el archivo físico del asset " + asset.displayName() + " (" + asset.kind() + ").",
                        "Restaura el archivo o elimina/reasigna la capa que lo usa."));
                continue;
            }
            if (!Files.isRegularFile(resolved)) {
                issues.add(ProjectIntegrityIssue.error(
                        "ASSET_NOT_REGULAR_FILE",
                        asset.id(),
                        "El asset no apunta a un archivo regular: " + asset.relativePath() + ".",
                        "Reimporta el asset desde un archivo válido."));
                continue;
            }
            validateChecksum(asset, resolved, issues);
        }
    }

    private static void validateChecksum(ProjectAssetReference asset, Path resolved, List<ProjectIntegrityIssue> issues) throws IOException {
        if (asset.checksum().isBlank()) {
            return;
        }
        String expected = normalizeChecksum(asset.checksum());
        if (expected.isBlank()) {
            issues.add(ProjectIntegrityIssue.warning(
                    "ASSET_CHECKSUM_UNSUPPORTED",
                    asset.id(),
                    "El checksum del asset no usa formato SHA-256 verificable: " + asset.checksum() + ".",
                    "Reimporta el asset para registrar un checksum sha256 válido."));
            return;
        }
        String actual = sha256(resolved);
        if (!expected.equals(actual)) {
            issues.add(ProjectIntegrityIssue.error(
                    "ASSET_CHECKSUM_MISMATCH",
                    asset.id(),
                    "El archivo del asset cambió respecto al checksum registrado.",
                    "Reimporta el archivo, confirma el reemplazo o regenera los artefactos que dependen de ese asset."));
        }
    }

    private static String normalizeChecksum(String checksum) {
        String normalized = checksum == null ? "" : checksum.strip().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("sha256:")) {
            normalized = normalized.substring("sha256:".length()).strip();
        }
        return normalized.matches("[0-9a-f]{64}") ? normalized : "";
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("SHA-256 not available", ex);
        }
    }

    private static void validateMaterializedArtifacts(
            DocuPodcastProject project,
            ProjectWorkspaceHydration hydration,
            List<ProjectIntegrityIssue> issues
    ) {
        ProjectKind kind = project.metadata().kind();
        if (kind == ProjectKind.EMPTY) {
            return;
        }
        boolean declaresImportedDocument = project.assets().containsKind(ProjectAssetKind.IMPORTED_DOCUMENT);
        boolean declaresNarrationScript = project.assets().containsKind(ProjectAssetKind.NARRATION_SCRIPT);
        boolean declaresStoryboard = project.assets().containsKind(ProjectAssetKind.STORYBOARD_MANIFEST);

        if ((kind == ProjectKind.DOCUMENT_ONLY || declaresImportedDocument) && hydration.importedDocument().isEmpty()) {
            issues.add(ProjectIntegrityIssue.error(
                    "DOCUMENT_MATERIALIZATION_MISSING",
                    "document/document.json",
                    "El proyecto declara documento importado pero no se pudo rehidratar el documento narrable.",
                    "Usa Refrescar contenido o reimporta el documento fuente."));
        }
        if ((requiresScript(kind) || declaresNarrationScript) && hydration.narrationScript().isEmpty()) {
            issues.add(ProjectIntegrityIssue.error(
                    "SCRIPT_MATERIALIZATION_MISSING",
                    "script/narration-script.json",
                    "El proyecto declara lectura preparada interna pero no se pudo rehidratar la proyección de lectura.",
                    "Prepara lectura nuevamente desde el documento narrable."));
        }
        if ((requiresStoryboard(kind) || declaresStoryboard) && hydration.storyboard().isEmpty()) {
            issues.add(ProjectIntegrityIssue.error(
                    "STORYBOARD_MATERIALIZATION_MISSING",
                    "storyboard/storyboard.json",
                    "El proyecto declara storyboard pero no se pudo rehidratar el manifiesto.",
                    "Reconstruye el storyboard desde las capas de imagen o vuelve a asociar imágenes."));
        }
        if (kind == ProjectKind.AUDIO_PROJECT && !hasAnyAudioAsset(project)) {
            issues.add(ProjectIntegrityIssue.warning(
                    "AUDIO_PROJECT_WITHOUT_AUDIO_ASSETS",
                    project.metadata().id(),
                    "El proyecto de audio no tiene clips, audio final ni manifest registrados.",
                    "Genera audio o reabre un job persistido antes de exportar podcast."));
        }
    }

    private static boolean requiresScript(ProjectKind kind) {
        return kind == ProjectKind.NARRATION_SCRIPT
                || kind == ProjectKind.STORYBOARD
                || kind == ProjectKind.AUDIO_PROJECT
                || kind == ProjectKind.FULL_PROJECT;
    }

    private static boolean requiresStoryboard(ProjectKind kind) {
        return kind == ProjectKind.STORYBOARD || kind == ProjectKind.FULL_PROJECT;
    }

    private static boolean hasAnyAudioAsset(DocuPodcastProject project) {
        return project.assets().containsKind(ProjectAssetKind.AUDIO_CLIP)
                || project.assets().containsKind(ProjectAssetKind.AUDIO_FINAL)
                || project.assets().containsKind(ProjectAssetKind.AUDIO_MANIFEST);
    }

    private static void validateNarrativeLayers(
            DocuPodcastProject project,
            ProjectWorkspaceHydration hydration,
            List<ProjectIntegrityIssue> issues
    ) {
        Set<String> segmentIds = hydration.narrationScript()
                .map(NarrationScriptDocument::segments)
                .orElse(List.of())
                .stream()
                .map(segment -> segment.id())
                .collect(Collectors.toUnmodifiableSet());
        Set<String> blockIds = hydration.importedDocument()
                .map(document -> document.blocks().stream().map(DocumentBlock::id).collect(Collectors.toUnmodifiableSet()))
                .orElse(Set.of());
        boolean hasScript = hydration.narrationScript().isPresent();
        boolean hasDocument = hydration.importedDocument().isPresent();

        for (NarrativeLayerAssignment layer : project.narrativeLayerAssignments()) {
            if (hasScript && !segmentIds.contains(layer.textRange().segmentId())) {
                issues.add(ProjectIntegrityIssue.error(
                        "LAYER_SEGMENT_MISSING",
                        layer.id(),
                        "La capa referencia un segmento inexistente: " + layer.textRange().segmentId() + ".",
                        "Reasigna la capa desde el documento actual o elimina la capa obsoleta."));
            }
            if (hasDocument && layer.hasDocumentRange() && !blockIds.contains(layer.documentRange().blockId())) {
                issues.add(ProjectIntegrityIssue.error(
                        "LAYER_DOCUMENT_BLOCK_MISSING",
                        layer.id(),
                        "La capa referencia un bloque de documento inexistente: " + layer.documentRange().blockId() + ".",
                        "Reasigna la capa desde el documento refrescado."));
            }
            validateLayerTarget(project, layer, issues);
        }
    }

    private static void validateLayerTarget(DocuPodcastProject project, NarrativeLayerAssignment layer, List<ProjectIntegrityIssue> issues) {
        if (layer.kind() == NarrativeLayerKind.NOTE) {
            return;
        }
        switch (layer.kind()) {
            case VOICE -> {
                if (project.voiceLibrary().voiceById(layer.targetId()).isEmpty()) {
                    issues.add(ProjectIntegrityIssue.error(
                            "LAYER_TARGET_VOICE_MISSING",
                            layer.id(),
                            "La capa de voz referencia una voz inexistente: " + layer.targetId() + ".",
                            "Selecciona una voz existente o repara la biblioteca de voces."));
                }
            }
            case EMOTION -> {
                boolean styleExists = project.voiceLibrary().styleById(layer.targetId()).isPresent();
                boolean toneExists = VoiceReferenceTone.fromLayerTargetId(layer.targetId()).isPresent();
                if (!styleExists && !toneExists) {
                    issues.add(ProjectIntegrityIssue.error(
                            "LAYER_TARGET_STYLE_MISSING",
                            layer.id(),
                            "La capa de emoción/intención referencia un estilo o tono inexistente: " + layer.targetId() + ".",
                            "Selecciona una emoción o tono existente desde el documento."));
                }
            }
            case IMAGE, BRIDGE_IMAGE -> {
                boolean imageExists = project.assets().byId(layer.targetId()).filter(ProjectAssetReference::isImage).isPresent();
                if (!imageExists) {
                    issues.add(ProjectIntegrityIssue.error(
                            "LAYER_TARGET_IMAGE_MISSING",
                            layer.id(),
                            "La capa de imagen referencia un asset visual inexistente: " + layer.targetId() + ".",
                            "Asocia otra imagen o elimina la capa obsoleta."));
                }
            }
            case HUMAN_AUDIO, AMBIENT_AUDIO -> {
                boolean audioExists = project.assets().byId(layer.targetId()).filter(ProjectAssetReference::isAudio).isPresent();
                if (!audioExists) {
                    issues.add(ProjectIntegrityIssue.error(
                            "LAYER_TARGET_AUDIO_MISSING",
                            layer.id(),
                            "La capa de audio referencia un asset de audio inexistente: " + layer.targetId() + ".",
                            "Asigna un MP3/WAV válido o elimina la capa obsoleta."));
                }
            }
            case NOTE -> { }
        }
    }

    private static void validateStoryboardBindings(
            DocuPodcastProject project,
            ProjectWorkspaceHydration hydration,
            List<ProjectIntegrityIssue> issues
    ) {
        if (hydration.storyboard().isEmpty()) {
            return;
        }
        StoryboardDocument storyboard = hydration.storyboard().get();
        Set<String> segmentIds = hydration.narrationScript()
                .map(NarrationScriptDocument::segments)
                .orElse(List.of())
                .stream()
                .map(segment -> segment.id())
                .collect(Collectors.toUnmodifiableSet());
        boolean hasScript = hydration.narrationScript().isPresent();
        for (StoryboardBinding binding : storyboard.bindings()) {
            if (hasScript && !segmentIds.contains(binding.segmentId())) {
                issues.add(ProjectIntegrityIssue.error(
                        "STORYBOARD_SEGMENT_MISSING",
                        binding.id(),
                        "Storyboard referencia un segmento inexistente: " + binding.segmentId() + ".",
                        "Reconstruye el storyboard desde la narración actual."));
            }
            boolean imageAssetExists = project.assets().byId(binding.imageAssetId())
                    .filter(ProjectAssetReference::isImage)
                    .isPresent();
            if (!imageAssetExists) {
                issues.add(ProjectIntegrityIssue.error(
                        "STORYBOARD_IMAGE_MISSING",
                        binding.id(),
                        "Storyboard referencia un asset de imagen inexistente o no visual: " + binding.imageAssetId() + ".",
                        "Reasigna la imagen del segmento o reconstruye el storyboard."));
            }
        }
    }

    private void validateAudioJobs(Path projectRoot, List<AudioJobSnapshot> jobs, List<ProjectIntegrityIssue> issues) {
        for (AudioJobSnapshot job : jobs) {
            AudioJobMaintenanceReport report = inspectAudioJobMaintenance.inspect(projectRoot, job);
            switch (report.status()) {
                case READY -> { }
                case RESUMABLE -> issues.add(ProjectIntegrityIssue.warning(
                        "AUDIO_JOB_RESUMABLE",
                        job.jobId(),
                        report.compactLabel(),
                        "Reanuda el job antes de exportar audio final si necesitas completar todos los segmentos."));
                case STALE_SOURCE, MISSING_AUDIO -> issues.add(ProjectIntegrityIssue.error(
                        "AUDIO_JOB_REQUIRES_REPAIR",
                        job.jobId(),
                        report.compactLabel(),
                        "Regenera o repara el audio desde la narración actual."));
                case EMPTY_JOB -> issues.add(ProjectIntegrityIssue.warning(
                        "AUDIO_JOB_EMPTY",
                        job.jobId(),
                        report.compactLabel(),
                        "Genera audio antes de reproducir/exportar."));
                case REVIEW_REQUIRED -> issues.add(ProjectIntegrityIssue.warning(
                        "AUDIO_JOB_REVIEW_REQUIRED",
                        job.jobId(),
                        report.compactLabel(),
                        "Revisa el job antes de confiar en reproducción o exportación."));
            }
            if (job.state() == AudioJobState.FAILED) {
                issues.add(ProjectIntegrityIssue.warning(
                        "AUDIO_JOB_FAILED_STATE",
                        job.jobId(),
                        "El job quedó marcado como fallido.",
                        "Reanuda o regenera el job desde Audio/Documento."));
            }
        }
    }

    private static void validateVideoPackageIfPresent(Path projectRoot, List<ProjectIntegrityIssue> issues) {
        Path manifest = projectRoot.resolve("RENDER_MANIFEST.json");
        Path commands = projectRoot.resolve("render-commands.txt");
        Path plan = projectRoot.resolve("VIDEO_SIMPLE_PLAN.md");
        boolean anyVideoPackageFile = Files.exists(manifest) || Files.exists(commands) || Files.exists(plan);
        if (!anyVideoPackageFile) {
            return;
        }
        if (!Files.isRegularFile(manifest) || !Files.isRegularFile(commands) || !Files.isRegularFile(plan)) {
            issues.add(ProjectIntegrityIssue.warning(
                    "VIDEO_PACKAGE_INCOMPLETE",
                    projectRoot.toString(),
                    "Existe un paquete de video simple incompleto.",
                    "Vuelve a exportar el paquete de video simple antes de renderizar MP4."));
        }
    }
}
