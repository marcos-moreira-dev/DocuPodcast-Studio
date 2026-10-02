package com.marcosmoreiradev.docupodcaststudio.application.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDisplayMode;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Saves a drawn storyboard frame as a project asset and keeps the active visual variant in sync. */
public final class UpsertTheatreStoryboardFrameVariantUseCase {
    public static final String OFFICIAL_IMAGE_ASSET_ID = "officialImageAssetId";
    public static final String GENERATED_IMAGE_ASSET_ID = "generatedImageAssetId";
    public static final String DRAWN_FRAME_ASSET_ID = "drawnFrameAssetId";
    public static final String SCENERY_IMAGE_ASSET_ID = "sceneryImageAssetId";
    public static final String DRAWN_FRAME_STATE_PATH = "drawnFrameStatePath";
    public static final String ACTIVE_VISUAL_VARIANT = "activeVisualVariant";
    public static final String VARIANT_OFFICIAL = "official";
    public static final String VARIANT_GENERATED = "generated";
    public static final String VARIANT_DRAWN = "drawn";
    public static final String VARIANT_SCENERY = "scenery";

    public Result upsertGeneratedFrame(DocuPodcastProject project,
                                       StoryboardDocument storyboard,
                                       NarrationScriptDocument script,
                                       String segmentId,
                                       ProjectAssetReference generatedAsset,
                                       boolean activateGenerated) throws IOException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(script, "script");
        Objects.requireNonNull(generatedAsset, "generatedAsset");
        if (!generatedAsset.isImage() || project.assets().byId(generatedAsset.id()).isEmpty()) {
            throw new IOException("La imagen IA debe existir como asset del proyecto antes de asignarla.");
        }
        String normalizedSegment = requireToken(segmentId, "segmentId");
        NarrationSegment segment = script.segmentById(normalizedSegment)
                .orElseThrow(() -> new IOException("No existe el segmento de storyboard: " + normalizedSegment));
        StoryboardDocument safeStoryboard = storyboard == null
                ? StoryboardDocument.createForScript(script)
                : storyboard;
        StoryboardBinding existing = safeStoryboard.bindingForSegment(normalizedSegment).orElse(null);
        Map<String, String> metadata = new LinkedHashMap<>(existing == null ? Map.of() : existing.metadata());
        captureCurrentOfficial(metadata, existing);
        metadata.put(GENERATED_IMAGE_ASSET_ID, generatedAsset.id());
        boolean activate = activateGenerated || !hasUsableVariant(project, metadata, existing);
        TheatreVisualVariant activeVariant = activate
                ? TheatreVisualVariant.GENERATED
                : TheatreVisualVariant.fromMetadata(metadata.get(ACTIVE_VISUAL_VARIANT));
        metadata.put(ACTIVE_VISUAL_VARIANT, activeVariant.metadataValue());
        String activeAssetId = activate ? generatedAsset.id() : activeAssetId(project, metadata, activeVariant, existing);
        if (activeAssetId.isBlank()) {
            activeVariant = TheatreVisualVariant.GENERATED;
            activeAssetId = generatedAsset.id();
            metadata.put(ACTIVE_VISUAL_VARIANT, activeVariant.metadataValue());
        }
        StoryboardBinding nextBinding = binding(existing, normalizedSegment, activeAssetId,
                activeVariant.displayName(), metadata);
        StoryboardDocument nextStoryboard = safeStoryboard.withBinding(nextBinding);
        DocuPodcastProject nextProject = project.withTheatre(syncTheatreVisual(
                project.theatre(), segment, activeAssetId, activeVariant));
        ProjectAssetReference activeAsset = project.assets().byId(activeAssetId).orElse(null);
        if (activeAsset == null) throw new IOException("Asset no encontrado: " + activeAssetId);
        return new Result(nextProject, nextStoryboard, activeAsset, activeVariant.metadataValue());
    }

    /** Registers the deterministic characters-over-scenery composition as a normal exportable variant. */
    public Result upsertSceneryFrame(DocuPodcastProject project,
                                     Path projectFile,
                                     StoryboardDocument storyboard,
                                     NarrationScriptDocument script,
                                     String segmentId,
                                     Path sceneryFramePng,
                                     boolean activateScenery) throws IOException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(projectFile, "projectFile");
        Objects.requireNonNull(script, "script");
        String normalizedSegment = requireToken(segmentId, "segmentId");
        NarrationSegment segment = script.segmentById(normalizedSegment)
                .orElseThrow(() -> new IOException("No existe el segmento de storyboard: " + normalizedSegment));
        if (!Files.isRegularFile(sceneryFramePng)) {
            throw new IOException("No se encontró el PNG compuesto de escenografía: " + sceneryFramePng);
        }

        Path projectDirectory = projectDirectory(projectFile);
        String safeSegment = safeSegmentId(normalizedSegment);
        String assetId = "SCENERY-" + safeSegment.toUpperCase(Locale.ROOT);
        Path frameDirectory = projectDirectory.resolve("storyboard").resolve("scenery-frames")
                .resolve(safeSegment).normalize();
        if (!frameDirectory.startsWith(projectDirectory.resolve("storyboard").normalize())) {
            throw new IOException("Ruta inválida para composición de escenografía");
        }
        Files.createDirectories(frameDirectory);
        Path frameTarget = frameDirectory.resolve("frame.png");
        Files.copy(sceneryFramePng, frameTarget, StandardCopyOption.REPLACE_EXISTING);
        ProjectAssetReference frameAsset = new ProjectAssetReference(
                assetId, ProjectAssetKind.IMAGE,
                "Personajes y escenografía " + normalizedSegment,
                relativePath(projectDirectory, frameTarget), "image/png",
                "Composición teatral determinista de personajes y escenografía",
                sha256(frameTarget), "segmentId=" + normalizedSegment + ";variant=scenery");

        StoryboardDocument safeStoryboard = storyboard == null
                ? StoryboardDocument.createForScript(script) : storyboard;
        StoryboardBinding existing = safeStoryboard.bindingForSegment(normalizedSegment).orElse(null);
        Map<String, String> metadata = new LinkedHashMap<>(existing == null ? Map.of() : existing.metadata());
        captureCurrentOfficial(metadata, existing);
        metadata.put(SCENERY_IMAGE_ASSET_ID, assetId);
        boolean activate = activateScenery || !hasUsableVariant(project, metadata, existing);
        TheatreVisualVariant activeVariant = activate
                ? TheatreVisualVariant.SCENERY
                : TheatreVisualVariant.fromMetadata(metadata.get(ACTIVE_VISUAL_VARIANT));
        String activeAssetId = activate ? assetId : activeAssetId(project, metadata, activeVariant, existing);
        if (activeAssetId.isBlank()) {
            activeVariant = TheatreVisualVariant.SCENERY;
            activeAssetId = assetId;
        }
        metadata.put(ACTIVE_VISUAL_VARIANT, activeVariant.metadataValue());
        StoryboardBinding nextBinding = binding(existing, normalizedSegment, activeAssetId,
                activeVariant.displayName(), metadata);
        StoryboardDocument nextStoryboard = safeStoryboard.withBinding(nextBinding);
        DocuPodcastProject nextProject = project.withAsset(frameAsset).withTheatre(syncTheatreVisual(
                project.theatre(), segment, activeAssetId, activeVariant));
        ProjectAssetReference activeAsset = nextProject.assets().byId(activeAssetId).orElse(frameAsset);
        return new Result(nextProject, nextStoryboard, activeAsset, activeVariant.metadataValue());
    }

    public Result upsertDrawnFrame(DocuPodcastProject project,
                                   Path projectFile,
                                   StoryboardDocument storyboard,
                                   NarrationScriptDocument script,
                                   String segmentId,
                                   Path drawnFramePng,
                                   String inkStateJson,
                                   boolean activateDrawn) throws IOException {
        return upsertDrawnFrame(project, projectFile, storyboard, script, segmentId, drawnFramePng,
                inkStateJson, activateDrawn, loadDrawingVault(projectFile));
    }

    public Result upsertDrawnFrame(DocuPodcastProject project,
                                   Path projectFile,
                                   StoryboardDocument storyboard,
                                   NarrationScriptDocument script,
                                   String segmentId,
                                   Path drawnFramePng,
                                   String inkStateJson,
                                   boolean activateDrawn,
                                   List<TheatreDrawingVaultItem> drawingVault) throws IOException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(projectFile, "projectFile");
        Objects.requireNonNull(script, "script");
        String normalizedSegment = requireToken(segmentId, "segmentId");
        NarrationSegment segment = script.segmentById(normalizedSegment)
                .orElseThrow(() -> new IOException("No existe el segmento de storyboard: " + normalizedSegment));
        if (!Files.isRegularFile(drawnFramePng)) {
            throw new IOException("No se encontro el PNG del frame dibujado: " + drawnFramePng);
        }

        Path projectDirectory = projectDirectory(projectFile);
        String safeSegment = safeSegmentId(normalizedSegment);
        String assetId = "FRAME-" + safeSegment.toUpperCase(Locale.ROOT);
        Path frameDirectory = projectDirectory.resolve("storyboard").resolve("drawn-frames").resolve(safeSegment).normalize();
        if (!frameDirectory.startsWith(projectDirectory.resolve("storyboard").normalize())) {
            throw new IOException("Ruta invalida para frame dibujado");
        }
        Files.createDirectories(frameDirectory);
        Path frameTarget = frameDirectory.resolve("frame.png");
        Path stateTarget = frameDirectory.resolve("ink-state.json");
        Files.copy(drawnFramePng, frameTarget, StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(stateTarget, inkStateJson == null ? "{}" : inkStateJson, StandardCharsets.UTF_8);
        saveDrawingVault(projectFile, drawingVault);

        String frameRelativePath = relativePath(projectDirectory, frameTarget);
        String stateRelativePath = relativePath(projectDirectory, stateTarget);
        ProjectAssetReference frameAsset = new ProjectAssetReference(
                assetId,
                ProjectAssetKind.IMAGE,
                "Frame dibujado " + normalizedSegment,
                frameRelativePath,
                "image/png",
                "Frame dibujado de storyboard teatral",
                sha256(frameTarget),
                "segmentId=" + normalizedSegment
        );

        StoryboardDocument safeStoryboard = storyboard == null
                ? StoryboardDocument.createForScript(script)
                : storyboard;
        StoryboardBinding existing = safeStoryboard.bindingForSegment(normalizedSegment).orElse(null);
        Map<String, String> metadata = new LinkedHashMap<>(existing == null ? Map.of() : existing.metadata());
        TheatreVisualVariant previousVariant = TheatreVisualVariant.fromMetadata(metadata.get(ACTIVE_VISUAL_VARIANT));
        String officialAssetId = firstNonBlank(metadata.get(OFFICIAL_IMAGE_ASSET_ID),
                existing != null && previousVariant == TheatreVisualVariant.OFFICIAL
                        && !assetId.equals(existing.imageAssetId()) ? existing.imageAssetId() : "");
        metadata.put(DRAWN_FRAME_ASSET_ID, assetId);
        metadata.put(DRAWN_FRAME_STATE_PATH, stateRelativePath);
        if (!officialAssetId.isBlank()) {
            metadata.put(OFFICIAL_IMAGE_ASSET_ID, officialAssetId);
        }
        boolean useDrawn = activateDrawn || officialAssetId.isBlank();
        metadata.put(ACTIVE_VISUAL_VARIANT, useDrawn ? VARIANT_DRAWN : VARIANT_OFFICIAL);
        String activeAssetId = useDrawn ? assetId : officialAssetId;

        String bindingId = existing == null ? "STB-" + normalizedSegment.replaceFirst("^SEG-", "") : existing.id();
        StoryboardDisplayMode mode = existing == null ? StoryboardDisplayMode.FIT_CONTAIN : existing.displayMode();
        StoryboardBinding nextBinding = new StoryboardBinding(
                bindingId,
                normalizedSegment,
                activeAssetId,
                mode,
                useDrawn ? "Frame dibujado" : existing.caption(),
                metadata
        );
        StoryboardDocument nextStoryboard = safeStoryboard.withBinding(nextBinding);
        TheatreVisualVariant activeVariant = useDrawn ? TheatreVisualVariant.DRAWN : TheatreVisualVariant.OFFICIAL;
        DocuPodcastProject nextProject = project.withAsset(frameAsset)
                .withTheatre(syncTheatreVisual(project.theatre(), segment, activeAssetId, activeVariant));
        ProjectAssetReference activeAsset = nextProject.assets().byId(activeAssetId).orElse(frameAsset);
        return new Result(nextProject, nextStoryboard, activeAsset, activeVariant.metadataValue());
    }

    public List<TheatreDrawingVaultItem> loadDrawingVault(Path projectFile) throws IOException {
        Path directory = drawingVaultDirectory(projectFile);
        if (!Files.isDirectory(directory)) return List.of();
        ArrayList<TheatreDrawingVaultItem> result = new ArrayList<>();
        try (var files = Files.list(directory)) {
            for (Path state : files.filter(path -> path.getFileName().toString().endsWith(".ink.json"))
                    .sorted().toList()) {
                String filename = state.getFileName().toString();
                String id = filename.substring(0, filename.length() - ".ink.json".length());
                if (!validVaultId(id)) continue;
                Path nameFile = directory.resolve(id + ".name.txt");
                String name = Files.isRegularFile(nameFile)
                        ? Files.readString(nameFile, StandardCharsets.UTF_8).strip()
                        : "Dibujo " + (result.size() + 1);
                result.add(new TheatreDrawingVaultItem(id, name, Files.readString(state, StandardCharsets.UTF_8)));
            }
        }
        return List.copyOf(result);
    }

    /** Replaces only the recognized drawing-vault sidecars; unrelated project files are never touched. */
    public void saveDrawingVault(Path projectFile, List<TheatreDrawingVaultItem> items) throws IOException {
        Path directory = drawingVaultDirectory(projectFile);
        Files.createDirectories(directory);
        List<TheatreDrawingVaultItem> safeItems = items == null ? List.of() : List.copyOf(items);
        java.util.HashSet<String> retained = new java.util.HashSet<>();
        for (TheatreDrawingVaultItem item : safeItems) {
            if (item == null || !validVaultId(item.id())) continue;
            retained.add(item.id());
            writeAtomically(directory.resolve(item.id() + ".ink.json"), item.inkStateJson());
            writeAtomically(directory.resolve(item.id() + ".name.txt"), item.name());
        }
        try (var files = Files.list(directory)) {
            for (Path file : files.toList()) {
                String filename = file.getFileName().toString();
                String id = filename.endsWith(".ink.json")
                        ? filename.substring(0, filename.length() - ".ink.json".length())
                        : filename.endsWith(".name.txt")
                        ? filename.substring(0, filename.length() - ".name.txt".length()) : "";
                if (!id.isBlank() && validVaultId(id) && !retained.contains(id)) Files.deleteIfExists(file);
            }
        }
    }

    private static Path drawingVaultDirectory(Path projectFile) throws IOException {
        Path projectDirectory = projectDirectory(projectFile);
        Path directory = projectDirectory.resolve("storyboard").resolve("drawing-vault").normalize();
        if (!directory.startsWith(projectDirectory.resolve("storyboard").normalize())) {
            throw new IOException("Ruta invalida para el baul de dibujos");
        }
        return directory;
    }

    private static boolean validVaultId(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{1,96}");
    }

    private static void writeAtomically(Path target, String content) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temporary, content == null ? "" : content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Result activateVariant(DocuPodcastProject project,
                                  StoryboardDocument storyboard,
                                  NarrationScriptDocument script,
                                  String segmentId,
                                  String variant) throws IOException {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(script, "script");
        StoryboardDocument safeStoryboard = storyboard == null
                ? StoryboardDocument.createForScript(script)
                : storyboard;
        String normalizedSegment = requireToken(segmentId, "segmentId");
        NarrationSegment segment = script.segmentById(normalizedSegment)
                .orElseThrow(() -> new IOException("No existe el segmento de storyboard: " + normalizedSegment));
        StoryboardBinding existing = safeStoryboard.bindingForSegment(normalizedSegment)
                .orElseThrow(() -> new IOException("El segmento no tiene variantes visuales: " + normalizedSegment));
        Map<String, String> metadata = new LinkedHashMap<>(existing.metadata());
        TheatreVisualVariant targetVariant = TheatreVisualVariant.fromMetadata(variant);
        String targetAsset = assetIdFor(metadata, targetVariant);
        if (targetAsset.isBlank() || project.assets().byId(targetAsset).isEmpty()) {
            throw new IOException("La variante visual no tiene asset disponible: " + targetVariant.metadataValue());
        }
        metadata.put(ACTIVE_VISUAL_VARIANT, targetVariant.metadataValue());
        StoryboardBinding nextBinding = existing.withImageAndMetadata(
                targetAsset,
                targetVariant.displayName(),
                existing.displayMode(),
                metadata
        );
        StoryboardDocument nextStoryboard = safeStoryboard.withBinding(nextBinding);
        DocuPodcastProject nextProject = project.withTheatre(syncTheatreVisual(
                project.theatre(), segment, targetAsset, targetVariant));
        ProjectAssetReference activeAsset = project.assets().byId(targetAsset)
                .orElseThrow(() -> new IOException("Asset no encontrado: " + targetAsset));
        return new Result(nextProject, nextStoryboard, activeAsset, targetVariant.metadataValue());
    }

    private static TheatreProjectLayer syncTheatreVisual(TheatreProjectLayer theatre,
                                                         NarrationSegment segment,
                                                         String activeAssetId,
                                                         TheatreVisualVariant variant) {
        String interventionId = interventionIdFor(theatre, segment).orElse("");
        if (interventionId.isBlank()) {
            return theatre;
        }
        ArrayList<TheatreProjectLayer.IntervencionVisual> visuals = new ArrayList<>();
        boolean replaced = false;
        for (TheatreProjectLayer.IntervencionVisual visual : theatre.intervencionesVisuales()) {
            if (visual.intervencionId().equals(interventionId)) {
                visuals.add(new TheatreProjectLayer.IntervencionVisual(
                        interventionId, activeAssetId, "Variante activa: " + variant.displayName().toLowerCase(Locale.ROOT)));
                replaced = true;
            } else {
                visuals.add(visual);
            }
        }
        if (!replaced) {
            visuals.add(new TheatreProjectLayer.IntervencionVisual(
                    interventionId, activeAssetId, "Variante activa: " + variant.displayName().toLowerCase(Locale.ROOT)));
        }
        return theatre.withIntervencionesVisuales(visuals);
    }

    private static StoryboardBinding binding(StoryboardBinding existing, String segmentId, String assetId,
                                              String caption, Map<String, String> metadata) {
        String bindingId = existing == null ? "STB-" + segmentId.replaceFirst("^SEG-", "") : existing.id();
        StoryboardDisplayMode mode = existing == null ? StoryboardDisplayMode.FIT_CONTAIN : existing.displayMode();
        return new StoryboardBinding(bindingId, segmentId, assetId, mode, caption, metadata);
    }

    private static void captureCurrentOfficial(Map<String, String> metadata, StoryboardBinding existing) {
        if (existing == null || existing.imageAssetId().isBlank()) return;
        TheatreVisualVariant current = TheatreVisualVariant.fromMetadata(metadata.get(ACTIVE_VISUAL_VARIANT));
        if (current == TheatreVisualVariant.OFFICIAL && metadata.getOrDefault(OFFICIAL_IMAGE_ASSET_ID, "").isBlank()) {
            metadata.put(OFFICIAL_IMAGE_ASSET_ID, existing.imageAssetId());
        }
    }

    private static boolean hasUsableVariant(DocuPodcastProject project, Map<String, String> metadata,
                                            StoryboardBinding existing) {
        TheatreVisualVariant current = TheatreVisualVariant.fromMetadata(metadata.get(ACTIVE_VISUAL_VARIANT));
        return !activeAssetId(project, metadata, current, existing).isBlank();
    }

    private static String activeAssetId(DocuPodcastProject project, Map<String, String> metadata,
                                        TheatreVisualVariant variant, StoryboardBinding existing) {
        String candidate = assetIdFor(metadata, variant);
        if (candidate.isBlank() && existing != null) candidate = existing.imageAssetId();
        return project.assets().byId(candidate).filter(ProjectAssetReference::isImage).map(ProjectAssetReference::id).orElse("");
    }

    private static String assetIdFor(Map<String, String> metadata, TheatreVisualVariant variant) {
        return switch (variant) {
            case OFFICIAL -> metadata.getOrDefault(OFFICIAL_IMAGE_ASSET_ID, "");
            case GENERATED -> metadata.getOrDefault(GENERATED_IMAGE_ASSET_ID, "");
            case DRAWN -> metadata.getOrDefault(DRAWN_FRAME_ASSET_ID, "");
            case SCENERY -> metadata.getOrDefault(SCENERY_IMAGE_ASSET_ID, "");
        };
    }

    private static Optional<String> interventionIdFor(TheatreProjectLayer theatre, NarrationSegment segment) {
        List<String> blockIds = segment.sourceBlockIds();
        if (blockIds.isEmpty()) {
            return Optional.empty();
        }
        return theatre.intervenciones().stream()
                .filter(intervention -> blockIds.contains(intervention.blockId()))
                .map(TheatreProjectLayer.Intervencion::id)
                .findFirst();
    }

    private static Path projectDirectory(Path projectFile) throws IOException {
        Path directory = projectFile.toAbsolutePath().normalize().getParent();
        if (directory == null) {
            throw new IOException("El proyecto debe estar guardado antes de guardar frames dibujados.");
        }
        return directory;
    }

    private static String relativePath(Path root, Path path) {
        return root.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private static String safeSegmentId(String segmentId) {
        String safe = segmentId == null ? "segment" : segmentId.strip().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "-")
                .replaceAll("-+", "-");
        return safe.isBlank() ? "segment" : safe;
    }

    private static String requireToken(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String firstNonBlank(String first, String second) {
        String a = first == null ? "" : first.strip();
        return a.isBlank() ? (second == null ? "" : second.strip()) : a;
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
            return "sha256:" + HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("SHA-256 no disponible", ex);
        }
    }

    public record Result(DocuPodcastProject project,
                         StoryboardDocument storyboard,
                         ProjectAssetReference activeAsset,
                         String activeVariant) {
    }
}
