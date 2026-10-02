package com.marcosmoreiradev.docupodcaststudio.application.batch;

import com.marcosmoreiradev.docupodcaststudio.application.project.CreateProjectUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.project.ProjectRepository;
import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoBackgroundMode;
import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchBranding;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemStage;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.BatchItemState;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchItem;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentVideoBatchProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Creates the complete recoverable container without modifying the selected source folder. */
public final class CreateDocumentVideoBatchProjectUseCase {
    private final DiscoverDocumentVideoBatchSourcesUseCase discovery;
    private final DocumentVideoBatchRepository batchRepository;
    private final ProjectRepository projectRepository;
    private final CreateProjectUseCase createProject = new CreateProjectUseCase();

    public CreateDocumentVideoBatchProjectUseCase(DiscoverDocumentVideoBatchSourcesUseCase discovery,
                                                   DocumentVideoBatchRepository batchRepository,
                                                   ProjectRepository projectRepository) {
        this.discovery = discovery;
        this.batchRepository = batchRepository;
        this.projectRepository = projectRepository;
    }

    public CreatedBatch create(String title, Path sourceRoot, Path destinationParent,
                               DocumentVideoBatchProfile requestedProfile) throws IOException {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("El proyecto por lotes necesita un nombre.");
        }
        BatchSourceInventory inventory = discovery.discover(sourceRoot);
        if (inventory.documents().isEmpty()) {
            throw new IOException("No se encontraron documentos DOCX o PDF en la carpeta seleccionada.");
        }
        Path destination = destinationParent.toAbsolutePath().normalize();
        Files.createDirectories(destination);
        Path finalRoot = DocumentVideoBatchPathPolicy.projectRoot(destination, title);
        if (finalRoot.startsWith(inventory.sourceRoot())) {
            throw new IOException("El proyecto de salida no puede crearse dentro de la carpeta de documentos. "
                    + "Elige su carpeta padre o una ubicación diferente.");
        }
        if (Files.exists(finalRoot)) {
            throw new IOException("Ya existe un proyecto con ese nombre: " + finalRoot);
        }
        Path staging = destination.resolve("." + finalRoot.getFileName() + ".creating-" + UUID.randomUUID());
        Files.createDirectory(staging);
        try {
            Files.createDirectories(staging.resolve("fuentes"));
            Files.createDirectories(staging.resolve("proyectos"));
            Files.createDirectories(staging.resolve("jobs/batch"));
            Files.createDirectories(staging.resolve("informes"));
            Files.createDirectories(staging.resolve("assets/branding"));
            Files.createDirectories(staging.resolve("assets/background"));

            DocumentVideoBatchProfile profile = copyVisualAssets(requestedProfile, staging, finalRoot);
            Files.createDirectories(staging.resolve(profile.outputDirectory()));
            List<DocumentVideoBatchItem> items = createChildren(inventory, staging, profile);
            Instant now = Instant.now();
            String descriptorName = DocumentVideoBatchPathPolicy.safeName(title).toLowerCase()
                    + DocumentVideoBatchPathPolicy.DESCRIPTOR_SUFFIX;
            DocumentVideoBatchProject project = new DocumentVideoBatchProject(
                    DocumentVideoBatchProject.CURRENT_FORMAT_VERSION,
                    "BATCH-" + UUID.randomUUID(), title.strip(), inventory.sourceRoot().toString(),
                    finalRoot.toString(), descriptorName, profile, items, inventory.ignoredFileCount(), now, now);
            Path stagingDescriptor = staging.resolve(descriptorName);
            batchRepository.save(project, stagingDescriptor);
            moveDirectory(staging, finalRoot);
            return new CreatedBatch(project, finalRoot, finalRoot.resolve(descriptorName), inventory);
        } catch (IOException | RuntimeException failure) {
            deleteTree(staging);
            throw failure;
        }
    }

    private DocumentVideoBatchProfile copyVisualAssets(DocumentVideoBatchProfile profile, Path staging,
                                                        Path finalRoot) throws IOException {
        DocumentVideoBatchProfile normalized = profile == null ? DocumentVideoBatchProfile.defaults() : profile;
        DocumentTextVideoOptions video = normalized.video();
        if (!normalized.audioOnly() && video.backgroundMode() == DocumentTextVideoBackgroundMode.IMAGE) {
            Path source = Path.of(video.backgroundImagePath()).toAbsolutePath().normalize();
            if (!Files.isRegularFile(source) || Files.isSymbolicLink(source)) {
                throw new IOException("La imagen de fondo no es un archivo válido: " + source);
            }
            String filename = DocumentVideoBatchPathPolicy.safeName(source.getFileName().toString());
            String relative = "assets/background/" + filename;
            Files.copy(source, staging.resolve(relative), StandardCopyOption.COPY_ATTRIBUTES);
            video = new DocumentTextVideoOptions(video.resolution(), video.backgroundMode(), video.backgroundColor(),
                    finalRoot.resolve(relative).toString(), video.textColor(), video.accentColor(), video.fontFamily(),
                    video.titleFontFamily(),
                    video.fontSize(), video.underlineNarratedText(), video.narratedUnderlineColor(),
                    video.narratedUnderlineThicknessPx(), video.backgroundImageOpacity(), video.backgroundImageFit(), video.textEffect(),
                    video.textEffectColor(), video.textEffectThicknessPx());
        }
        BatchBranding branding = normalized.branding();
        if (branding.enabled()) {
            Path source = Path.of(branding.sourcePath()).toAbsolutePath().normalize();
            if (!Files.isRegularFile(source) || Files.isSymbolicLink(source)) {
                throw new IOException("El logo o mascota no es un archivo válido: " + source);
            }
            String filename = DocumentVideoBatchPathPolicy.safeName(source.getFileName().toString());
            String relative = "assets/branding/" + filename;
            Files.copy(source, staging.resolve(relative), StandardCopyOption.COPY_ATTRIBUTES);
            branding = new BatchBranding(true, source.toString(), relative, branding.placement(),
                    branding.sizePercent(), branding.opacity());
        }
        var backgrounds = new java.util.LinkedHashMap<String, com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentBackgroundOverride>();
        for (var entry : normalized.documentBackgrounds().entrySet()) {
            if (normalized.audioOnly()) { backgrounds.put(entry.getKey(), entry.getValue()); continue; }
            Path source = Path.of(entry.getValue().imagePath());
            if (!Files.isRegularFile(source) || Files.isSymbolicLink(source)) throw new IOException("Imagen personalizada no disponible para " + entry.getKey() + ": " + source);
            String relative = "assets/background/" + UUID.randomUUID() + "-" + DocumentVideoBatchPathPolicy.safeName(source.getFileName().toString());
            Files.copy(source, staging.resolve(relative));
            backgrounds.put(entry.getKey(), new com.marcosmoreiradev.docupodcaststudio.domain.batch.DocumentBackgroundOverride(finalRoot.resolve(relative).toString(), entry.getValue().visibility()));
        }
        return new DocumentVideoBatchProfile(video, normalized.imageSlideSeconds(),
                normalized.interpretImages(), branding, normalized.outputKind(), normalized.audioFormat(),
                normalized.voiceEngineId(), normalized.aiEngineId(), backgrounds);
    }

    private List<DocumentVideoBatchItem> createChildren(BatchSourceInventory inventory, Path staging, DocumentVideoBatchProfile profile) throws IOException {
        List<DocumentVideoBatchItem> result = new ArrayList<>();
        Map<String, Integer> childNames = new HashMap<>();
        Map<String, Integer> videoNames = new HashMap<>();
        int order = 0;
        for (BatchSourceCandidate candidate : inventory.documents()) {
            String copiedRelative = "fuentes/" + candidate.relativePath();
            Path copied = safeResolve(staging, copiedRelative);
            Files.createDirectories(copied.getParent());
            Files.copy(candidate.absolutePath(), copied, StandardCopyOption.COPY_ATTRIBUTES);
            if (!candidate.sha256().equals(DiscoverHash.sha256(copied))) {
                throw new IOException("La copia no superó la verificación SHA-256: " + candidate.relativePath());
            }

            Path relativePath = Path.of(candidate.relativePath());
            String preferred = relativePath.getNameCount() > 1
                    ? relativePath.getName(relativePath.getNameCount() - 2).toString()
                    : DocumentVideoBatchPathPolicy.stem(relativePath.getFileName().toString());
            String childFolder = unique(DocumentVideoBatchPathPolicy.safeName(preferred), childNames);
            Path childRoot = staging.resolve("proyectos").resolve(childFolder);
            Files.createDirectories(childRoot.resolve("source"));
            Files.copy(copied, childRoot.resolve("source").resolve(candidate.absolutePath().getFileName()),
                    StandardCopyOption.COPY_ATTRIBUTES);

            String childDescriptorName = childFolder + ".docupodcast.json";
            DocuPodcastProject base = createProject.create(DocumentVideoBatchPathPolicy.stem(
                    candidate.absolutePath().getFileName().toString()), ProjectMode.DOCUMENTARY_STUDIO);
            Map<String, String> state = new LinkedHashMap<>(base.viewState());
            state.put("batch.source", "source/" + candidate.absolutePath().getFileName());
            state.put("batch.sourceSha256", candidate.sha256());
            DocuPodcastProject child = new DocuPodcastProject(base.metadata(), base.assets(), base.readingProfile(),
                    base.voiceLibrary(), base.narrativeLayerAssignments(), base.narrative(), base.theatre(), base.study(),
                    base.visualProcessing(), state);
            projectRepository.save(child, childRoot.resolve(childDescriptorName));

            String videoStem = unique(DocumentVideoBatchPathPolicy.stem(candidate.absolutePath().getFileName().toString()), videoNames);
            String childProjectRelative = portable(staging.relativize(childRoot.resolve(childDescriptorName)));
            result.add(new DocumentVideoBatchItem("ITEM-" + UUID.randomUUID(), order++,
                    DocumentVideoBatchPathPolicy.stem(candidate.absolutePath().getFileName().toString()),
                    candidate.relativePath(), copiedRelative, childProjectRelative,
                    profile.outputDirectory() + "/" + videoStem + profile.outputExtension(), candidate.sha256(), candidate.bytes(),
                    candidate.embeddedMediaCount(), BatchItemState.PENDING, BatchItemStage.PROJECT_CREATED,
                    0.0, "Listo para preparar", Instant.now()));
        }
        return List.copyOf(result);
    }

    private static Path safeResolve(Path root, String relative) throws IOException {
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) throw new IOException("Ruta relativa insegura: " + relative);
        return resolved;
    }

    private static String unique(String base, Map<String, Integer> names) {
        int count = names.merge(base.toLowerCase(), 1, Integer::sum);
        return count == 1 ? base : base + "-" + count;
    }

    private static String portable(Path path) { return path.toString().replace('\\', '/'); }

    private static void moveDirectory(Path source, Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException unsupported) { Files.move(source, target); }
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var stream = Files.walk(root)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException ignored) { } });
        } catch (IOException ignored) { }
    }

    public record CreatedBatch(DocumentVideoBatchProject project, Path projectRoot, Path descriptor,
                               BatchSourceInventory inventory) { }

    private static final class DiscoverHash {
        private static String sha256(Path path) throws IOException {
            try {
                var digest = java.security.MessageDigest.getInstance("SHA-256");
                try (var in = Files.newInputStream(path)) {
                    byte[] buffer = new byte[65536];
                    for (int read; (read = in.read(buffer)) >= 0;) if (read > 0) digest.update(buffer, 0, read);
                }
                return java.util.HexFormat.of().formatHex(digest.digest());
            } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        }
    }
}
