package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatrePrimaryVisualReference;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatrePrimaryVisualResolver;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreVisualContinuityResolver;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Builds the clean theatre sequence: active image plus exact rendered narration units. */
public final class BuildTheatreCleanVideoPlanUseCase {
    private final TheatrePrimaryVisualResolver visualResolver = new TheatrePrimaryVisualResolver();

    public SimpleVideoPlan build(DocuPodcastProject project,
                                 NarrationScriptDocument script,
                                 StoryboardDocument storyboard,
                                 List<AudioJobSnapshot> jobs,
                                 Path projectDirectory,
                                 SimpleVideoExportSettings settings) throws IOException {
        if (project == null || script == null || script.empty()) {
            throw new IOException("No hay lectura teatral preparada para exportar video limpio.");
        }
        Path root = projectDirectory == null ? null : projectDirectory.toAbsolutePath().normalize();
        if (root == null || !Files.isDirectory(root)) {
            throw new IOException("No se pudo resolver la carpeta del proyecto teatral.");
        }
        SimpleVideoExportSettings effective = settings == null ? SimpleVideoExportSettings.defaults() : settings;
        Map<String, AudioSegmentSnapshot> audio = completedAudioBySegment(jobs, root);
        Map<String, ProjectAssetReference> intermediateFrames = intermediateFramesByPair(project, root);
        Map<String, String> interventionBySegment = interventionBySegment(project.theatre(), script);
        Map<String, String> nextInterventionBySegment = nextInterventionBySegment(script, interventionBySegment);
        LinkedHashSet<String> emittedAudioIds = new LinkedHashSet<>();
        ArrayList<SimpleVideoFrame> frames = new ArrayList<>();
        Path placeholder = null;
        int index = 1;
        for (NarrationSegment segment : script.segments()) {
            if (segment == null || !segment.narratable()) {
                continue;
            }
            List<AudioSegmentSnapshot> units = audioForSegment(audio, segment.id());
            if (units.isEmpty()) {
                throw new IOException("Falta audio renderizado para " + segment.id() + ".");
            }
            Optional<ProjectAssetReference> resolvedImage = visualResolver
                    .resolveForSegment(project, storyboard, segment, root)
                    .map(TheatrePrimaryVisualReference::asset);
            String imageAssetId;
            String imageRelativePath;
            if (resolvedImage.isPresent()) {
                imageAssetId = resolvedImage.get().id();
                imageRelativePath = resolvedImage.get().relativePath();
            } else if (effective.renderUnassignedVisuals()) {
                if (placeholder == null) {
                    placeholder = createUnassignedVisual(root, effective.resolution());
                }
                imageAssetId = "THEATRE-CLEAN-UNASSIGNED";
                imageRelativePath = relativeToProject(root, placeholder);
            } else {
                throw new IOException("Falta una imagen o frame dibujado para " + segment.id() + ".");
            }
            for (AudioSegmentSnapshot unit : units) {
                if (!emittedAudioIds.add(unit.segmentId())) {
                    continue;
                }
                double spokenTailSeconds = TheatreVideoTimingPolicy.spokenTailSeconds();
                double totalDurationSeconds = unit.durationSeconds() + spokenTailSeconds;
                List<SimpleVideoFrame.VisualPart> visualParts = visualPartsFor(
                        effective,
                        intermediateFrames,
                        interventionBySegment.getOrDefault(segment.id(), ""),
                        nextInterventionBySegment.getOrDefault(segment.id(), ""),
                        imageAssetId,
                        imageRelativePath,
                        totalDurationSeconds);
                frames.add(new SimpleVideoFrame(
                        "THEATRE-CLEAN-" + String.format(java.util.Locale.ROOT, "%03d", index++),
                        unit.segmentId(),
                        segment.title(),
                        segment.preview(180),
                        imageAssetId,
                        imageRelativePath,
                        unit.audioRelativePath(),
                        unit.durationSeconds(),
                        spokenTailSeconds,
                        true,
                        true,
                        false,
                        List.of(),
                        visualParts));
            }
        }
        if (frames.isEmpty()) {
            throw new IOException("No hay unidades teatrales con audio e imagen para exportar.");
        }
        return new SimpleVideoPlan("Video teatral limpio - " + script.title(), frames,
                TheatreVideoTimingPolicy.spokenTailSeconds(), Instant.now());
    }

    public List<String> missingVisualSegmentIds(DocuPodcastProject project,
                                                 NarrationScriptDocument script,
                                                 StoryboardDocument storyboard,
                                                 Path projectDirectory) {
        if (project == null || script == null || script.empty() || projectDirectory == null) {
            return List.of();
        }
        Path root = projectDirectory.toAbsolutePath().normalize();
        return script.segments().stream()
                .filter(NarrationSegment::narratable)
                .filter(segment -> visualResolver.resolveForSegment(project, storyboard, segment, root).isEmpty())
                .map(NarrationSegment::id)
                .distinct()
                .toList();
    }

    private static Path createUnassignedVisual(Path root, SimpleVideoResolutionPreset resolution) throws IOException {
        Path workRoot = root.resolve("exports/video-render-work");
        Files.createDirectories(workRoot);
        Path directory = Files.createTempDirectory(workRoot, "theatre-clean-placeholder-");
        Path output = directory.resolve("sin-fragmento-visual.png");
        BufferedImage image = new BufferedImage(resolution.width(), resolution.height(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            int fontSize = Math.max(28, image.getWidth() / 32);
            graphics.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            graphics.setColor(Color.WHITE);
            String text = "Sin fragmento visual asignado";
            FontMetrics metrics = graphics.getFontMetrics();
            int x = Math.max(24, (image.getWidth() - metrics.stringWidth(text)) / 2);
            int y = (image.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            graphics.drawString(text, x, y);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, "png", output.toFile());
        return output;
    }

    private static String relativeToProject(Path root, Path file) {
        return root.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private static Map<String, ProjectAssetReference> intermediateFramesByPair(
            DocuPodcastProject project, Path root) {
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
            usableImage(project.assets(), frame.assetId(), root)
                    .ifPresent(image -> result.put(pairKey(frame.fromIntervencionId(), frame.toIntervencionId()), image));
        }
        return result;
    }

    private static Map<String, String> interventionBySegment(TheatreProjectLayer theatre,
                                                             NarrationScriptDocument script) {
        LinkedHashMap<String, String> blockToIntervention = new LinkedHashMap<>();
        if (theatre != null) {
            theatre.intervenciones().stream()
                    .sorted(Comparator.comparingInt(TheatreProjectLayer.Intervencion::sequenceIndex))
                    .forEach(intervention -> blockToIntervention.putIfAbsent(intervention.blockId(), intervention.id()));
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        if (script == null) {
            return result;
        }
        for (NarrationSegment segment : script.segments()) {
            for (String blockId : segment.sourceBlockIds()) {
                String interventionId = blockToIntervention.get(blockId);
                if (interventionId != null) {
                    result.put(segment.id(), interventionId);
                    break;
                }
            }
        }
        return result;
    }

    private static Map<String, String> nextInterventionBySegment(NarrationScriptDocument script,
                                                                 Map<String, String> interventionBySegment) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        if (script == null || interventionBySegment.isEmpty()) {
            return result;
        }
        ArrayList<String> segmentIds = new ArrayList<>();
        ArrayList<String> interventionIds = new ArrayList<>();
        String previousIntervention = "";
        for (NarrationSegment segment : script.segments()) {
            if (!segment.narratable()) {
                continue;
            }
            String interventionId = interventionBySegment.getOrDefault(segment.id(), "");
            if (interventionId.isBlank() || interventionId.equals(previousIntervention)) {
                continue;
            }
            segmentIds.add(segment.id());
            interventionIds.add(interventionId);
            previousIntervention = interventionId;
        }
        for (int i = 0; i + 1 < segmentIds.size(); i++) {
            result.put(segmentIds.get(i), interventionIds.get(i + 1));
        }
        return result;
    }

    private static List<SimpleVideoFrame.VisualPart> visualPartsFor(SimpleVideoExportSettings settings,
                                                                    Map<String, ProjectAssetReference> intermediateFrames,
                                                                    String currentInterventionId,
                                                                    String nextInterventionId,
                                                                    String imageAssetId,
                                                                    String imageRelativePath,
                                                                    double totalDurationSeconds) {
        if (settings == null || !settings.includeInferredFrames()
                || currentInterventionId == null || currentInterventionId.isBlank()
                || nextInterventionId == null || nextInterventionId.isBlank()
                || imageRelativePath == null || imageRelativePath.isBlank()
                || totalDurationSeconds <= 0.0) {
            return List.of();
        }
        ProjectAssetReference intermediate = intermediateFrames.get(pairKey(currentInterventionId, nextInterventionId));
        if (intermediate == null) {
            return List.of();
        }
        double first = totalDurationSeconds / 2.0;
        double second = totalDurationSeconds - first;
        return List.of(
                new SimpleVideoFrame.VisualPart(imageAssetId, imageRelativePath, first, "principal"),
                new SimpleVideoFrame.VisualPart(intermediate.id(), intermediate.relativePath(), second, "inferido"));
    }

    private static String pairKey(String fromIntervencionId, String toIntervencionId) {
        return (fromIntervencionId == null ? "" : fromIntervencionId) + "->"
                + (toIntervencionId == null ? "" : toIntervencionId);
    }

    private static Optional<ProjectAssetReference> usableImage(
            ProjectAssetCatalog assets, String assetId, Path root) {
        if (assets == null || assetId == null || assetId.isBlank()) {
            return Optional.empty();
        }
        return assets.byId(assetId)
                .filter(ProjectAssetReference::isImage)
                .filter(asset -> {
                    Path file = root.resolve(asset.relativePath()).toAbsolutePath().normalize();
                    return file.startsWith(root) && Files.isRegularFile(file);
                });
    }

    private static Map<String, AudioSegmentSnapshot> completedAudioBySegment(
            List<AudioJobSnapshot> jobs, Path root) {
        LinkedHashMap<String, AudioSegmentSnapshot> result = new LinkedHashMap<>();
        if (jobs == null) {
            return result;
        }
        jobs.stream().sorted(Comparator.comparing(AudioJobSnapshot::updatedAt).reversed()).forEach(job -> {
            for (AudioSegmentSnapshot segment : job.segments()) {
                Path file = root.resolve(segment.audioRelativePath()).toAbsolutePath().normalize();
                if (segment.completed() && !segment.audioRelativePath().isBlank()
                        && file.startsWith(root) && Files.isRegularFile(file)) {
                    result.putIfAbsent(segment.segmentId(), segment);
                }
            }
        });
        return result;
    }

    private static List<AudioSegmentSnapshot> audioForSegment(
            Map<String, AudioSegmentSnapshot> audio, String segmentId) {
        ArrayList<AudioSegmentSnapshot> result = new ArrayList<>();
        for (AudioSegmentSnapshot unit : audio.values()) {
            if (unit.segmentId().equals(segmentId) || unit.segmentId().startsWith(segmentId + "-")) {
                result.add(unit);
            }
        }
        return List.copyOf(result);
    }
}
