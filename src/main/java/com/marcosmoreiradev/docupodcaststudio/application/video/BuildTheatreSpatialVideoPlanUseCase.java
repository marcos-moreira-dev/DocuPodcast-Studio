package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatrePrimaryVisualReference;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatrePrimaryVisualResolver;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreVisualContinuityResolver;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static com.marcosmoreiradev.docupodcaststudio.application.video.TheatreSpatialVideoPlanData.*;

/** Builds PNG frames for the theatre spatial-map video from the theatre MD layer. */
public final class BuildTheatreSpatialVideoPlanUseCase {
    private static final Color BLACK = new Color(3, 7, 18);
    private static final Color WHITE = Color.WHITE;
    private static final Color CREAM_TEXT = new Color(253, 236, 200);
    private static final Color TEXT_SHADOW = new Color(0, 0, 0, 190);
    private static final Color ARROW = new Color(15, 23, 42);
    private static final Color ARROW_ACTIVE_SHADOW = new Color(0, 0, 0, 120);
    private static final double ROLE_ICON_SCALE_FACTOR = 0.70;
    private static final double ARROW_TRIM_FACTOR = 0.56;
    private static final double SELF_LOOP_SCALE_FACTOR = 0.34;
    private static final double SELF_LOOP_RIGHT_OFFSET_FACTOR = 0.44;
    private static final double SELF_LOOP_MARKER_LEFT_SHIFT_FACTOR = 0.10;
    private static final Map<TheatreSpatialRoleIcon, BufferedImage> ROLE_ICONS =
            new EnumMap<>(TheatreSpatialRoleIcon.class);
    private static final TheatreSpatialParticipantResolver PARTICIPANT_RESOLVER =
            new TheatreSpatialParticipantResolver();
    private final TheatrePrimaryVisualResolver visualResolver = new TheatrePrimaryVisualResolver();

    public SimpleVideoPlan build(DocuPodcastProject project,
                                 NarrationScriptDocument script,
                                 List<AudioJobSnapshot> jobs,
                                 Path projectDirectory,
                                 SimpleVideoExportSettings settings,
                                 String frameMode) throws IOException {
        return build(project, script, null, jobs, projectDirectory, settings, frameMode, TheatreExportScope.all());
    }

    public SimpleVideoPlan build(DocuPodcastProject project,
                                 NarrationScriptDocument script,
                                 StoryboardDocument storyboard,
                                 List<AudioJobSnapshot> jobs,
                                 Path projectDirectory,
                                 SimpleVideoExportSettings settings,
                                 String frameMode) throws IOException {
        return build(project, script, storyboard, jobs, projectDirectory, settings, frameMode, TheatreExportScope.all());
    }

    public SimpleVideoPlan build(DocuPodcastProject project,
                                 NarrationScriptDocument script,
                                 List<AudioJobSnapshot> jobs,
                                 Path projectDirectory,
                                 SimpleVideoExportSettings settings,
                                 String frameMode,
                                 TheatreExportScope scope) throws IOException {
        return build(project, script, null, jobs, projectDirectory, settings, frameMode, scope, ignored -> { }, () -> false);
    }

    public SimpleVideoPlan build(DocuPodcastProject project,
                                 NarrationScriptDocument script,
                                 StoryboardDocument storyboard,
                                 List<AudioJobSnapshot> jobs,
                                 Path projectDirectory,
                                 SimpleVideoExportSettings settings,
                                 String frameMode,
                                 TheatreExportScope scope) throws IOException {
        return build(project, script, storyboard, jobs, projectDirectory, settings, frameMode, scope, ignored -> { }, () -> false);
    }

    public SimpleVideoPlan build(DocuPodcastProject project,
                                 NarrationScriptDocument script,
                                 List<AudioJobSnapshot> jobs,
                                 Path projectDirectory,
                                 SimpleVideoExportSettings settings,
                                 String frameMode,
                                 TheatreExportScope scope,
                                 Consumer<VideoRenderProgress> progress,
                                 BooleanSupplier cancellationRequested) throws IOException {
        return build(project, script, null, jobs, projectDirectory, settings, frameMode, scope, progress,
                cancellationRequested);
    }

    public SimpleVideoPlan build(DocuPodcastProject project,
                                 NarrationScriptDocument script,
                                 StoryboardDocument storyboard,
                                 List<AudioJobSnapshot> jobs,
                                 Path projectDirectory,
                                 SimpleVideoExportSettings settings,
                                 String frameMode,
                                 TheatreExportScope scope,
                                 Consumer<VideoRenderProgress> progress,
                                 BooleanSupplier cancellationRequested) throws IOException {
        if (project == null) {
            throw new IOException("No hay proyecto abierto para exportar video mapa.");
        }
        if (script == null || script.empty()) {
            throw new IOException("No hay lectura preparada cargada para exportar video mapa.");
        }
        Path root = projectDirectory == null ? null : projectDirectory.toAbsolutePath().normalize();
        if (root == null || !Files.isDirectory(root)) {
            throw new IOException("No se pudo resolver la carpeta del proyecto para video mapa.");
        }
        SimpleVideoExportSettings effective = settings == null ? SimpleVideoExportSettings.defaults() : settings;
        TheatreProjectLayer theatre = project.theatre();
        if (theatre == null || theatre.intervenciones().isEmpty()) {
            throw new IOException("No hay intervenciones teatrales para exportar video mapa.");
        }

        ProjectAssetCatalog assets = project.assets() == null ? ProjectAssetCatalog.empty() : project.assets();
        Map<String, List<NarrationSegment>> segmentsByBlock = segmentsByBlock(script);
        Map<String, AudioSegmentSnapshot> audioBySegment = completedAudioBySegment(jobs, root);
        Map<String, TheatreProjectLayer.TextActionPlacement> placements = placementsByIntervention(theatre);
        Map<String, TheatreProjectLayer.Scene> scenes = scenesById(theatre);
        Map<String, String> characterNames = characterNamesById(theatre);
        String mode = TheatreStageGeometry.normalizeFrameMode(frameMode);
        TheatreExportScope effectiveScope = scope == null ? TheatreExportScope.all() : scope;
        Map<String, ProjectAssetReference> intermediateFrames = intermediateFramesByPair(project, root);
        Map<String, String> nextIncludedIntervention = nextIncludedInterventions(theatre, placements, scenes, effectiveScope);

        Path frameRoot = root.resolve("exports/video-render-work").toAbsolutePath().normalize();
        Files.createDirectories(frameRoot);
        Path framesDir = Files.createTempDirectory(frameRoot, "theatre-map-");
        ArrayList<SimpleVideoFrame> frames = new ArrayList<>();
        LinkedHashSet<String> emittedAudioIds = new LinkedHashSet<>();
        Consumer<VideoRenderProgress> safeProgress = progress == null ? ignored -> { } : progress;
        BooleanSupplier cancel = cancellationRequested == null ? () -> false : cancellationRequested;
        int index = 1;
        boolean completed = false;
        try {
        for (TheatreProjectLayer.Intervencion intervention : theatre.intervenciones()) {
            TheatreProjectLayer.TextActionPlacement placement = placements.get(intervention.id());
            if (placement == null) {
                throw new IOException("No hay placement de mapa espacial para " + intervention.id() + ".");
            }
            TheatreProjectLayer.Scene scene = scenes.get(placement.sceneId());
            if (scene == null) {
                throw new IOException("No se encontro la escena del placement " + intervention.id() + ": " + placement.sceneId());
            }
            if (!scopeIncludes(effectiveScope, scene)) {
                continue;
            }
            ProjectAssetReference mapAsset = requireImageAsset(assets, scene.spatialMapAssetId(),
                    "mapa_espacial de escena " + scene.displayName());
            List<NarrationSegment> blockSegments = segmentsByBlock.getOrDefault(intervention.blockId(), List.of()).stream()
                    .filter(NarrationSegment::narratable)
                    .toList();
            if (blockSegments.isEmpty()) {
                throw new IOException("No se encontro el texto DOCX de " + intervention.id()
                        + " en la lectura preparada: " + intervention.blockId());
            }
            NarrationSegment segment = blockSegments.get(0);
            String fullInterventionText = fullInterventionText(blockSegments);
            List<AudioSegmentSnapshot> audioUnits = audioForSegments(audioBySegment, blockSegments);
            if (audioUnits.isEmpty()) {
                throw new IOException("No hay audio renderizado para " + intervention.id()
                        + " (" + segment.id() + "). Genera fragmentos de audio antes de exportar Video mapa.");
            }

            ProjectAssetReference visualAsset = visualResolver.resolve(project, storyboard, script,
                            intervention.id(), root)
                    .map(TheatrePrimaryVisualReference::asset)
                    .orElse(null);
            ProjectAssetReference inferredVisual = inferredVisualFor(effective, mode, intermediateFrames,
                    intervention.id(), nextIncludedIntervention.get(intervention.id()), visualAsset);
            for (AudioSegmentSnapshot audio : audioUnits) {
                if (!emittedAudioIds.add(audio.segmentId())) {
                    continue;
                }
                if (cancel.getAsBoolean()) {
                    throw new IOException("Exportacion de mapa teatral cancelada por el usuario.");
                }
                safeProgress.accept(VideoRenderProgress.composingMaps(
                        frames.size(), Math.max(1, audioBySegment.size()), "Componiendo " + audio.segmentId()));
                NarrationSegment frameSegment = segmentForAudio(blockSegments, audio.segmentId()).orElse(segment);
                Path output = framesDir.resolve("theatre-map-" + String.format(Locale.ROOT, "%03d", index) + ".png");
                FrameSpec spec = new FrameSpec(
                        effective.resolution().width(),
                        effective.resolution().height(),
                        mode,
                        scene,
                        placement,
                        frameSegment,
                        mapAsset,
                        visualAsset,
                        project,
                        root,
                        characterNames,
                        fullInterventionText);
                render(spec, output);
                double totalDurationSeconds = audio.durationSeconds() + effective.silenceAfterFrameSeconds();
                List<SimpleVideoFrame.VisualPart> visualParts = List.of();
                if (inferredVisual != null && totalDurationSeconds > 0.0) {
                    Path inferredOutput = framesDir.resolve("theatre-map-"
                            + String.format(Locale.ROOT, "%03d", index) + "-inferido.png");
                    FrameSpec inferredSpec = new FrameSpec(
                            effective.resolution().width(),
                            effective.resolution().height(),
                            mode,
                            scene,
                            placement,
                            frameSegment,
                            mapAsset,
                            inferredVisual,
                            project,
                            root,
                            characterNames,
                            fullInterventionText);
                    render(inferredSpec, inferredOutput);
                    double first = totalDurationSeconds / 2.0;
                    visualParts = List.of(
                            new SimpleVideoFrame.VisualPart("", relativeToProject(root, output), first, "principal"),
                            new SimpleVideoFrame.VisualPart(inferredVisual.id(), relativeToProject(root, inferredOutput),
                                    totalDurationSeconds - first, "inferido"));
                }

                frames.add(new SimpleVideoFrame(
                        "THEATRE-MAP-" + String.format(Locale.ROOT, "%03d", index),
                        audio.segmentId(),
                        intervention.id(),
                        fullInterventionText,
                        "",
                        relativeToProject(root, output),
                        audio.audioRelativePath(),
                        audio.durationSeconds(),
                        effective.silenceAfterFrameSeconds(),
                        true,
                        true,
                        false,
                        List.of(),
                        visualParts));
                index++;
            }
        }
        if (frames.isEmpty()) {
            throw new IOException("La porcion teatral seleccionada no tiene intervenciones exportables.");
        }
        safeProgress.accept(VideoRenderProgress.composingMaps(frames.size(), frames.size(), "Mapas teatrales listos."));
        SimpleVideoPlan plan = new SimpleVideoPlan("Video mapa teatral - " + script.title(), frames,
                effective.silenceAfterFrameSeconds(), Instant.now());
        completed = true;
        return plan;
        } finally {
            if (!completed) {
                deleteRecursively(framesDir);
            }
        }
    }

    private static void deleteRecursively(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // The export error remains the primary diagnostic.
                }
            });
        } catch (IOException ignored) {
            // Best-effort cleanup of failed or cancelled exports.
        }
    }

    private static boolean scopeIncludes(TheatreExportScope scope, TheatreProjectLayer.Scene scene) {
        if (scope == null || scope.isAll()) {
            return true;
        }
        return switch (scope.kind()) {
            case SCENE -> scene.id().equals(scope.id());
            case ACT -> scene.actId().equals(scope.id());
            case ALL -> true;
        };
    }

    private static Map<String, ProjectAssetReference> intermediateFramesByPair(DocuPodcastProject project, Path root) {
        LinkedHashMap<String, ProjectAssetReference> result = new LinkedHashMap<>();
        if (project == null || project.theatre() == null) {
            return result;
        }
        TheatreVisualContinuityResolver continuityResolver = new TheatreVisualContinuityResolver();
        for (TheatreProjectLayer.IntermediateFrame frame : project.theatre().intermediateFrames()) {
            if (!continuityResolver.canInterpolate(project.theatre(),
                    frame.fromIntervencionId(), frame.toIntervencionId())) {
                continue;
            }
            usableImageAsset(project.assets(), frame.assetId(), root)
                    .ifPresent(asset -> result.put(pairKey(frame.fromIntervencionId(), frame.toIntervencionId()), asset));
        }
        return result;
    }

    private static Map<String, String> nextIncludedInterventions(TheatreProjectLayer theatre,
                                                                 Map<String, TheatreProjectLayer.TextActionPlacement> placements,
                                                                 Map<String, TheatreProjectLayer.Scene> scenes,
                                                                 TheatreExportScope scope) {
        ArrayList<String> included = new ArrayList<>();
        for (TheatreProjectLayer.Intervencion intervention : theatre.intervenciones()) {
            TheatreProjectLayer.TextActionPlacement placement = placements.get(intervention.id());
            TheatreProjectLayer.Scene scene = placement == null ? null : scenes.get(placement.sceneId());
            if (scene != null && scopeIncludes(scope, scene)) {
                included.add(intervention.id());
            }
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < included.size(); i++) {
            result.put(included.get(i), included.get(i + 1));
        }
        return result;
    }

    private static ProjectAssetReference inferredVisualFor(SimpleVideoExportSettings settings,
                                                           String mode,
                                                           Map<String, ProjectAssetReference> intermediateFrames,
                                                           String currentInterventionId,
                                                           String nextInterventionId,
                                                           ProjectAssetReference visualAsset) {
        if (settings == null || !settings.includeInferredFrames()
                || !"fragments".equals(TheatreStageGeometry.normalizeFrameMode(mode))
                || visualAsset == null
                || currentInterventionId == null || currentInterventionId.isBlank()
                || nextInterventionId == null || nextInterventionId.isBlank()) {
            return null;
        }
        return intermediateFrames.get(pairKey(currentInterventionId, nextInterventionId));
    }

    private static Optional<ProjectAssetReference> usableImageAsset(ProjectAssetCatalog assets, String assetId, Path root) {
        return optionalImageAsset(assets, assetId).filter(asset -> {
            if (root == null) {
                return false;
            }
            Path path = root.resolve(asset.relativePath()).toAbsolutePath().normalize();
            return path.startsWith(root) && Files.isRegularFile(path);
        });
    }

    private static String pairKey(String fromIntervencionId, String toIntervencionId) {
        return (fromIntervencionId == null ? "" : fromIntervencionId) + "->"
                + (toIntervencionId == null ? "" : toIntervencionId);
    }

    public void render(FrameSpec spec, Path output) throws IOException {
        BufferedImage image = new BufferedImage(spec.width(), spec.height(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setColor(BLACK);
            g.fillRect(0, 0, spec.width(), spec.height());
            drawExportSurface(g, spec);
        } finally {
            g.dispose();
        }
        Files.createDirectories(output.toAbsolutePath().normalize().getParent());
        ImageIO.write(image, "png", output.toFile());
    }

    private static void drawExportSurface(Graphics2D g, FrameSpec spec) throws IOException {
        int margin = Math.max(32, spec.width() / 30);
        int gap = Math.max(18, spec.width() / 80);
        int bottomHeight = captionBandHeight(spec);
        int top = margin;
        int contentHeight = spec.height() - (margin * 3) - bottomHeight;
        int usableWidth = spec.width() - (margin * 2);
        if ("none".equals(spec.mode())) {
            drawMapPanel(g, spec, margin, top, usableWidth, contentHeight);
        } else if ("fragments".equals(spec.mode())) {
            int panelWidth = (usableWidth - gap) / 2;
            drawCompanion(g, spec, margin, top, panelWidth, contentHeight);
            drawMapPanel(g, spec, margin + panelWidth + gap, top, panelWidth, contentHeight);
        } else {
            int companionWidth = Math.max(360, (int) (usableWidth * 0.42));
            int mapWidth = usableWidth - companionWidth - gap;
            drawCompanion(g, spec, margin, top, companionWidth, contentHeight);
            drawMapPanel(g, spec, margin + companionWidth + gap, top, mapWidth, contentHeight);
        }
        drawCaption(g, spec, margin, top + contentHeight + gap, usableWidth, bottomHeight);
    }

    private static void drawCompanion(Graphics2D g, FrameSpec spec, int x, int y, int w, int h) throws IOException {
        g.setColor(WHITE);
        g.setFont(new Font("Georgia", Font.BOLD, Math.max(30, spec.width() / 54)));
        drawCenteredShadowed(g, companionTitle(spec.mode()), x, y + 46, w);
        int imageX = x + 18;
        int imageY = y + 72;
        int imageW = w - 36;
        int imageH = h - 104;
        if ("fragments".equals(spec.mode()) && spec.visualAsset() != null) {
            drawImageFit(g, assetPath(spec.projectDirectory(), spec.visualAsset()), imageX, imageY, imageW, imageH);
        } else if ("characters".equals(spec.mode())) {
            drawCharacterCompanion(g, spec, imageX, imageY, imageW, imageH);
        } else {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(24, spec.width() / 70)));
            drawCenteredShadowed(g, "Sin acompanante", imageX, imageY + imageH / 2, imageW);
        }
    }

    private static String companionTitle(String mode) {
        return switch (mode) {
            case "characters" -> "Personajes presentes";
            case "none" -> "Sin acompanante";
            default -> "Fragmento visual";
        };
    }

    private static int captionBandHeight(FrameSpec spec) {
        int base = Math.max(210, spec.height() / 4);
        String text = spec == null ? "" : spec.narrationText();
        int length = text == null ? 0 : text.length();
        if (length > 320) {
            return Math.max(base, (int) Math.round(spec.height() * 0.42));
        }
        if (length > 180) {
            return Math.max(base, spec.height() / 3);
        }
        return base;
    }

    private static void drawCharacterCompanion(Graphics2D g, FrameSpec spec, int x, int y, int w, int h) throws IOException {
        List<TheatreSpatialParticipantResolver.Participant> participants = PARTICIPANT_RESOLVER.resolve(
                spec.project(), spec.placement(), spec.scene().id(), spec.characterNames());
        if (participants.isEmpty()) {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(22, spec.width() / 74)));
            drawCenteredShadowed(g, "Sin personajes presentes", x, y + h / 2, w);
            return;
        }
        int slots = Math.min(4, participants.size());
        int cols = slots <= 1 ? 1 : 2;
        int rows = (int) Math.ceil(slots / (double) cols);
        int gap = 22;
        int slotW = cols == 1 ? w : (w - gap) / 2;
        int slotH = rows == 1 ? h : (h - gap) / 2;
        int startY = y + Math.max(0, (h - ((slotH * rows) + gap * (rows - 1))) / 2);
        for (int i = 0; i < slots; i++) {
            TheatreSpatialParticipantResolver.Participant p = participants.get(i);
            int row = i / cols;
            int col = i % cols;
            int sx = x + col * (slotW + gap);
            int sy = startY + row * (slotH + gap);
            Optional<ProjectAssetReference> photo = PARTICIPANT_RESOLVER.characterImage(
                    spec.project(), p.characterId(), spec.scene().id());
            if (photo.isPresent()) {
                drawImageFit(g, assetPath(spec.projectDirectory(), photo.get()), sx, sy, slotW, Math.max(80, slotH - 44));
            } else {
                g.setColor(new Color(15, 23, 42, 160));
                g.fillRoundRect(sx, sy, slotW, Math.max(80, slotH - 44), 18, 18);
                g.setColor(WHITE);
                g.setFont(new Font("Georgia", Font.BOLD, Math.max(28, spec.width() / 60)));
                drawCentered(g, initials(p.name()), sx, sy + Math.max(80, slotH - 44) / 2, slotW);
            }
            g.setColor(WHITE);
            g.setFont(new Font("Georgia", Font.BOLD, Math.max(18, spec.width() / 92)));
            drawCenteredShadowedFitting(g, p.name(), sx, sy + slotH - 16, slotW, Math.max(12, spec.width() / 120));
        }
        if (participants.size() > slots) {
            g.setColor(WHITE);
            g.setFont(new Font("Georgia", Font.BOLD, Math.max(18, spec.width() / 92)));
            drawCenteredShadowed(g, "+" + (participants.size() - slots) + " personajes", x, y + h - 10, w);
        }
    }

    private static void drawMapPanel(Graphics2D g, FrameSpec spec, int x, int y, int w, int h) throws IOException {
        g.setFont(new Font("Georgia", Font.BOLD, Math.max(30, spec.width() / 54)));
        int mapX = x + 4;
        int mapY = y + 4;
        int mapW = w - 8;
        int mapH = h - 8;
        DrawnImageBounds bounds = drawImageFit(g, assetPath(spec.projectDirectory(), spec.mapAsset()), mapX, mapY, mapW, mapH);
        drawPlacementOverlay(g, spec, bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

    private static void drawCaption(Graphics2D g, FrameSpec spec, int x, int y, int w, int h) {
        String speaker = PARTICIPANT_RESOLVER.speakerName(spec.placement(), spec.characterNames());
        String text = stripSpeaker(spec.narrationText());
        g.setColor(CREAM_TEXT);
        String prefix = (speaker.isBlank() ? "PERSONAJE" : speaker) + ": ";
        int fontSize = captionFontSize(g, prefix + text, w - 80, h - 22, spec.width());
        g.setFont(new Font("Georgia", Font.BOLD | Font.ITALIC, fontSize));
        List<String> lines = wrap(g, prefix + text, w - 80);
        int lineHeight = g.getFontMetrics().getHeight() + 4;
        int startY = y + Math.max(42, (h - (lines.size() * lineHeight)) / 2 + lineHeight);
        for (int i = 0; i < lines.size(); i++) {
            drawLeftShadowed(g, lines.get(i), x + 40, startY + (i * lineHeight));
        }
    }

    private static int captionFontSize(Graphics2D g, String text, int width, int height, int frameWidth) {
        int max = Math.max(28, frameWidth / 58);
        int min = Math.max(14, frameWidth / 132);
        for (int size = max; size >= min; size -= 2) {
            g.setFont(new Font("Georgia", Font.BOLD | Font.ITALIC, size));
            List<String> lines = wrap(g, text, width);
            int lineHeight = g.getFontMetrics().getHeight() + 4;
            if (lines.size() * lineHeight <= height) {
                return size;
            }
        }
        return min;
    }

    private static void drawPlacementOverlay(Graphics2D g, FrameSpec spec, int mapX, int mapY, int mapW, int mapH) {
        TheatreStageGeometry.StagePoint origin = TheatreStageGeometry.pointFor(spec.placement().origin());
        double ox = mapX + origin.scaledX(mapW);
        double oy = mapY + origin.scaledY(mapH);
        int iconSize = Math.max(118, (int) Math.round((spec.width() / 12.0) * ROLE_ICON_SCALE_FACTOR));
        ArrayList<String> selfLoopLocations = new ArrayList<>();
        for (String destination : PARTICIPANT_RESOLVER.destinationLocations(spec.project(), spec.placement())) {
            TheatreStageGeometry.StagePoint dest = TheatreStageGeometry.pointFor(destination);
            double dx = mapX + dest.scaledX(mapW);
            double dy = mapY + dest.scaledY(mapH);
            if (Math.abs(ox - dx) < 0.5 && Math.abs(oy - dy) < 0.5) {
                if (!containsIgnoreCase(selfLoopLocations, spec.placement().origin())) {
                    selfLoopLocations.add(spec.placement().origin());
                }
                drawSelfLoop(g, ox, oy, iconSize, spec, mapX, mapY, mapW, mapH);
            } else {
                double trim = iconSize * ARROW_TRIM_FACTOR;
                drawArrow(g, ox, oy, dx, dy, Math.max(11f, spec.width() / 160f), ARROW_ACTIVE_SHADOW,
                        Math.max(22, spec.width() / 84), trim, trim);
                drawArrow(g, ox, oy, dx, dy, Math.max(8f, spec.width() / 190f), WHITE,
                        Math.max(18, spec.width() / 96), trim, trim);
                drawArrow(g, ox, oy, dx, dy, Math.max(4f, spec.width() / 360f), ARROW,
                        Math.max(14, spec.width() / 128), trim, trim);
            }
        }
        drawParticipantGroups(g, PARTICIPANT_RESOLVER.resolve(
                        spec.project(), spec.placement(), spec.scene().id(), spec.characterNames()),
                mapX, mapY, mapW, mapH, iconSize, selfLoopLocations);
    }

    private static void drawSelfLoop(Graphics2D g,
                                     double x,
                                     double y,
                                     int iconSize,
                                     FrameSpec spec,
                                     int mapX,
                                     int mapY,
                                     int mapW,
                                     int mapH) {
        double size = iconSize * SELF_LOOP_SCALE_FACTOR;
        double loopX = clamp(x + iconSize * SELF_LOOP_RIGHT_OFFSET_FACTOR,
                mapX + 4,
                mapX + mapW - size * 1.14 - 4);
        double loopY = clamp(y + size * 0.06, mapY + 4, mapY + mapH - size - 4);
        strokeSelfLoop(g, loopX, loopY, size, Math.max(6f, spec.width() / 300f), ARROW_ACTIVE_SHADOW,
                Math.max(10, spec.width() / 220));
        strokeSelfLoop(g, loopX, loopY, size, Math.max(4f, spec.width() / 390f), WHITE,
                Math.max(8, spec.width() / 275));
        strokeSelfLoop(g, loopX, loopY, size, Math.max(2.5f, spec.width() / 640f), ARROW,
                Math.max(6, spec.width() / 370));
    }

    private static void strokeSelfLoop(Graphics2D g,
                                       double x,
                                       double y,
                                       double size,
                                       float strokeWidth,
                                       Color color,
                                       int headLength) {
        g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(color);
        double startX = x + size * 0.10;
        double startY = y + size * 0.72;
        double c1x = x + size * 1.08;
        double c1y = y + size * 0.94;
        double c2x = x + size * 1.08;
        double c2y = y + size * 0.08;
        double ex = x + size * 0.12;
        double ey = y + size * 0.24;
        Path2D.Double loop = new Path2D.Double();
        loop.moveTo(startX, startY);
        loop.curveTo(c1x, c1y, c2x, c2y, ex, ey);
        g.draw(loop);
        double tangentAngle = Math.atan2(ey - c2y, ex - c2x);
        fillArrowHead(g, ex, ey, tangentAngle, headLength);
    }

    private static void fillArrowHead(Graphics2D g,
                                      double tipX,
                                      double tipY,
                                      double angle,
                                      int headLength) {
        double spread = Math.toRadians(28);
        Path2D.Double head = new Path2D.Double();
        head.moveTo(tipX, tipY);
        head.lineTo(tipX - headLength * Math.cos(angle - spread),
                tipY - headLength * Math.sin(angle - spread));
        head.lineTo(tipX - headLength * Math.cos(angle + spread),
                tipY - headLength * Math.sin(angle + spread));
        head.closePath();
        g.fill(head);
    }

    private static void drawArrow(Graphics2D g, double ox, double oy, double dx, double dy,
                                  float strokeWidth, Color color, int headLength,
                                  double trimStart, double trimEnd) {
        double distance = Math.hypot(dx - ox, dy - oy);
        if (distance <= trimStart + trimEnd + 1) {
            return;
        }
        double sx = ox + (dx - ox) / distance * trimStart;
        double sy = oy + (dy - oy) / distance * trimStart;
        double ex = dx - (dx - ox) / distance * trimEnd;
        double ey = dy - (dy - oy) / distance * trimEnd;
        g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(color);
        g.draw(new Line2D.Double(sx, sy, ex, ey));
        double angle = Math.atan2(ey - sy, ex - sx);
        double spread = Math.toRadians(24);
        BasicStroke headStroke = new BasicStroke(Math.max(2f, strokeWidth * 0.78f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g.setStroke(headStroke);
        g.draw(new Line2D.Double(ex, ey,
                ex - headLength * Math.cos(angle - spread),
                ey - headLength * Math.sin(angle - spread)));
        g.draw(new Line2D.Double(ex, ey,
                ex - headLength * Math.cos(angle + spread),
                ey - headLength * Math.sin(angle + spread)));
    }

    private static void drawMarker(Graphics2D g, double x, double y, int size, TheatreSpatialRoleIcon role) {
        BufferedImage icon = roleIcon(role);
        int drawSize = Math.max(34, size);
        int drawX = (int) Math.round(x - drawSize / 2.0);
        int drawY = (int) Math.round(y - drawSize / 2.0);
        if (icon != null) {
            g.drawImage(icon, drawX, drawY, drawSize, drawSize, null);
            return;
        }
        g.setColor(new Color(255, 255, 255, 230));
        g.fillOval(drawX, drawY, drawSize, drawSize);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(ARROW);
        g.drawOval(drawX, drawY, drawSize, drawSize);
    }

    private static BufferedImage roleIcon(TheatreSpatialRoleIcon role) {
        TheatreSpatialRoleIcon resolved = role == null ? TheatreSpatialRoleIcon.ACTOR : role;
        synchronized (ROLE_ICONS) {
            return ROLE_ICONS.computeIfAbsent(resolved, BuildTheatreSpatialVideoPlanUseCase::loadRoleIcon);
        }
    }

    private static BufferedImage loadRoleIcon(TheatreSpatialRoleIcon role) {
        try (InputStream stream = BuildTheatreSpatialVideoPlanUseCase.class.getResourceAsStream(role.resourcePath())) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (IOException ex) {
            return null;
        }
    }

    private static TheatreSpatialRoleIcon mergeRole(TheatreSpatialRoleIcon current, TheatreSpatialRoleIcon incoming) {
        if (current == TheatreSpatialRoleIcon.NARRATOR || incoming == TheatreSpatialRoleIcon.NARRATOR) {
            return TheatreSpatialRoleIcon.NARRATOR;
        }
        if (current == TheatreSpatialRoleIcon.AUDIENCE || incoming == TheatreSpatialRoleIcon.AUDIENCE) {
            return TheatreSpatialRoleIcon.AUDIENCE;
        }
        return TheatreSpatialRoleIcon.ACTOR;
    }

    private static void drawParticipantGroups(Graphics2D g,
                                              List<TheatreSpatialParticipantResolver.Participant> participants,
                                              int mapX,
                                              int mapY,
                                              int mapW,
                                              int mapH,
                                              int iconSize,
                                              List<String> selfLoopLocations) {
        LinkedHashMap<String, MarkerGroup> byLocation = new LinkedHashMap<>();
        for (TheatreSpatialParticipantResolver.Participant participant : participants) {
            String location = participant.location() == null || participant.location().isBlank()
                    ? "centro" : participant.location();
            MarkerGroup group = byLocation.computeIfAbsent(location,
                    ignored -> new MarkerGroup(participant.role(), new ArrayList<>()));
            group.role = mergeRole(group.role, participant.role());
            if (containsIgnoreCase(selfLoopLocations, location)) {
                group.selfLoop = true;
            }
            if (!participant.name().isBlank() && !group.names.contains(participant.name())) {
                group.names.add(participant.name());
            }
            if (participant.speaking() && !participant.name().isBlank() && !group.speakers.contains(participant.name())) {
                group.speakers.add(participant.name());
            }
        }
        int index = 0;
        for (Map.Entry<String, MarkerGroup> entry : byLocation.entrySet()) {
            TheatreStageGeometry.StagePoint point = TheatreStageGeometry.pointFor(entry.getKey());
            double x = mapX + point.scaledX(mapW);
            double y = mapY + point.scaledY(mapH);
            if (entry.getValue().selfLoop) {
                x = markerXForSelfLoop(x, mapX, mapW, iconSize);
            }
            drawMarker(g, x, y, iconSize, entry.getValue().role);
            drawLabel(g, entry.getValue().names, entry.getValue().speakers, x, y, index++, mapX, mapW, iconSize);
        }
    }

    private static double markerXForSelfLoop(double x, int mapX, int mapW, int iconSize) {
        double half = iconSize / 2.0;
        return clamp(x - iconSize * SELF_LOOP_MARKER_LEFT_SHIFT_FACTOR, mapX + half + 4, mapX + mapW - half - 4);
    }

    private static void drawLabel(Graphics2D g, List<String> names, List<String> speakers, double x, double y,
                                  int index, int mapX, int mapW, int iconSize) {
        if (names == null || names.isEmpty()) {
            return;
        }
        int lineHeight = 18;
        int shown = Math.min(4, names.size());
        int labelH = Math.max(30, 12 + shown * lineHeight);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        FontMetrics metrics = g.getFontMetrics();
        int labelW = Math.max(156, Math.min(Math.max(180, mapW - 8), names.stream()
                .limit(shown)
                .mapToInt(name -> metrics.stringWidth(safeText(name)))
                .max()
                .orElse(132) + 22));
        int rawX = (int) Math.round(x - labelW / 2.0 + (index % 2) * 12);
        int lx = Math.max(mapX + 4, Math.min(mapX + mapW - labelW - 4, rawX));
        int ly = (int) Math.round(y + iconSize / 2.0 + 8 + index * 12);
        g.setColor(new Color(15, 23, 42, 210));
        g.fillRoundRect(lx, ly, labelW, labelH, 8, 8);
        g.setColor(WHITE);
        for (int i = 0; i < shown; i++) {
            String name = names.get(i);
            int textX = lx + 10;
            if (containsIgnoreCase(speakers, name)) {
                g.setColor(new Color(245, 158, 11));
                int cy = ly + 15 + i * lineHeight;
                g.fillOval(lx + 10, cy - 5, 9, 9);
                g.setColor(WHITE);
                textX = lx + 24;
            }
            drawFittingString(g, name, textX, ly + 20 + i * lineHeight, labelW - (textX - lx) - 10, 10);
        }
        if (names.size() > shown) {
            g.drawString("+" + (names.size() - shown), lx + 10, ly + 20 + shown * lineHeight);
        }
    }

    private static DrawnImageBounds drawImageFit(Graphics2D g, Path imagePath, int x, int y, int w, int h) throws IOException {
        BufferedImage image = ImageIO.read(imagePath.toFile());
        if (image == null) {
            throw new IOException("No se pudo leer imagen para video mapa: " + imagePath);
        }
        double scale = Math.min(w / (double) image.getWidth(), h / (double) image.getHeight());
        int drawW = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int drawH = Math.max(1, (int) Math.round(image.getHeight() * scale));
        int drawX = x + (w - drawW) / 2;
        int drawY = y + (h - drawH) / 2;
        g.drawImage(image, drawX, drawY, drawW, drawH, null);
        return new DrawnImageBounds(drawX, drawY, drawW, drawH);
    }

    private static boolean containsIgnoreCase(List<String> values, String candidate) {
        if (values == null || candidate == null || candidate.isBlank()) {
            return false;
        }
        return values.stream().anyMatch(value -> candidate.equalsIgnoreCase(value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String stripSpeaker(String text) {
        String value = text == null ? "" : text.strip();
        int colon = value.indexOf(':');
        if (colon > 0 && colon <= 42) {
            return value.substring(colon + 1).strip();
        }
        return value;
    }

    private static List<String> wrap(Graphics2D g, String text, int width) {
        ArrayList<String> lines = new ArrayList<>();
        String[] words = (text == null ? "" : text).split("\\s+");
        StringBuilder line = new StringBuilder();
        FontMetrics metrics = g.getFontMetrics();
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(candidate) <= width) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                }
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    private static void drawCentered(Graphics2D g, String text, int x, int baselineY, int width) {
        String safe = text == null ? "" : text;
        FontMetrics metrics = g.getFontMetrics();
        int tx = x + Math.max(0, (width - metrics.stringWidth(safe)) / 2);
        g.drawString(safe, tx, baselineY);
    }

    private static void drawCenteredShadowed(Graphics2D g, String text, int x, int baselineY, int width) {
        String safe = text == null ? "" : text;
        FontMetrics metrics = g.getFontMetrics();
        int tx = x + Math.max(0, (width - metrics.stringWidth(safe)) / 2);
        drawLeftShadowed(g, safe, tx, baselineY);
    }

    private static void drawCenteredShadowedFitting(Graphics2D g, String text, int x, int baselineY,
                                                    int width, int minFontSize) {
        String safe = safeText(text);
        Font original = g.getFont();
        Font fitting = fittingFont(g, safe, width, minFontSize);
        g.setFont(fitting);
        FontMetrics metrics = g.getFontMetrics();
        int tx = x + Math.max(0, (width - metrics.stringWidth(safe)) / 2);
        drawLeftShadowed(g, safe, tx, baselineY);
        g.setFont(original);
    }

    private static void drawFittingString(Graphics2D g, String text, int x, int baselineY,
                                          int width, int minFontSize) {
        String safe = safeText(text);
        Font original = g.getFont();
        g.setFont(fittingFont(g, safe, width, minFontSize));
        g.drawString(safe, x, baselineY);
        g.setFont(original);
    }

    private static Font fittingFont(Graphics2D g, String text, int width, int minFontSize) {
        Font font = g.getFont();
        int size = font.getSize();
        while (size > minFontSize && g.getFontMetrics(font).stringWidth(text) > width) {
            size--;
            font = font.deriveFont((float) size);
        }
        return font;
    }

    private static void drawLeftShadowed(Graphics2D g, String text, int x, int baselineY) {
        String safe = text == null ? "" : text;
        Color foreground = g.getColor();
        g.setColor(TEXT_SHADOW);
        g.drawString(safe, x + 2, baselineY + 2);
        g.setColor(foreground);
        g.drawString(safe, x, baselineY);
    }

    private static String initials(String name) {
        String[] parts = (name == null ? "" : name.strip()).split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                result.append(part.charAt(0));
            }
            if (result.length() == 2) {
                break;
            }
        }
        return result.isEmpty() ? "?" : result.toString().toUpperCase(Locale.ROOT);
    }

    private static String safeText(String text) {
        return text == null ? "" : text.strip();
    }

    public record FrameSpec(
            int width,
            int height,
            String mode,
            TheatreProjectLayer.Scene scene,
            TheatreProjectLayer.TextActionPlacement placement,
            NarrationSegment segment,
            ProjectAssetReference mapAsset,
            ProjectAssetReference visualAsset,
            DocuPodcastProject project,
            Path projectDirectory,
            Map<String, String> characterNames,
            String narrationText
    ) {
        public FrameSpec {
            width = Math.max(640, width);
            height = Math.max(360, height);
            mode = TheatreStageGeometry.normalizeFrameMode(mode);
            characterNames = characterNames == null ? Map.of() : Map.copyOf(characterNames);
            narrationText = narrationText == null || narrationText.isBlank()
                    ? (segment == null ? "" : segment.narrationText())
                    : narrationText.strip();
        }
    }

    private static final class MarkerGroup {
        private TheatreSpatialRoleIcon role;
        private final List<String> names;
        private final List<String> speakers = new ArrayList<>();
        private boolean selfLoop;

        private MarkerGroup(TheatreSpatialRoleIcon role, List<String> names) {
            this.role = role == null ? TheatreSpatialRoleIcon.ACTOR : role;
            this.names = names == null ? new ArrayList<>() : names;
        }
    }

    private record DrawnImageBounds(int x, int y, int width, int height) {
    }
}
