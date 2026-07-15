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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Builds composed PNG frames for the theatre-work video export. */
public final class BuildTheatreWorkVideoPlanUseCase {
    private static final Color DEFAULT_BACKGROUND = Color.WHITE;
    private static final Color DEFAULT_TEXT = new Color(17, 24, 39);
    private static final Color PANEL_BACKGROUND = new Color(248, 250, 252);
    private static final Color PANEL_BORDER = new Color(203, 213, 225);
    private static final Color MUTED_TEXT = new Color(71, 85, 105);
    private static final Color SHADOW = new Color(0, 0, 0, 150);
    private final TheatrePrimaryVisualResolver visualResolver = new TheatrePrimaryVisualResolver();

    public SimpleVideoPlan build(DocuPodcastProject project,
                                 NarrationScriptDocument script,
                                 List<AudioJobSnapshot> jobs,
                                 Path projectDirectory,
                                 SimpleVideoExportSettings settings,
                                 TheatreWorkVideoOptions options) throws IOException {
        return build(project, script, null, jobs, projectDirectory, settings, options);
    }

    public SimpleVideoPlan build(DocuPodcastProject project,
                                 NarrationScriptDocument script,
                                 StoryboardDocument storyboard,
                                 List<AudioJobSnapshot> jobs,
                                 Path projectDirectory,
                                 SimpleVideoExportSettings settings,
                                 TheatreWorkVideoOptions options) throws IOException {
        if (project == null) {
            throw new IOException("No hay proyecto abierto para exportar obra teatral.");
        }
        if (script == null || script.empty()) {
            throw new IOException("No hay lectura preparada cargada para exportar obra teatral.");
        }
        Path root = projectDirectory == null ? null : projectDirectory.toAbsolutePath().normalize();
        if (root == null || !Files.isDirectory(root)) {
            throw new IOException("No se pudo resolver la carpeta del proyecto para exportar obra teatral.");
        }
        TheatreProjectLayer theatre = project.theatre();
        if (theatre == null || theatre.intervenciones().isEmpty()) {
            throw new IOException("No hay intervenciones teatrales para exportar obra.");
        }

        SimpleVideoExportSettings effective = settings == null ? SimpleVideoExportSettings.defaults() : settings;
        TheatreWorkVideoOptions visualOptions = options == null ? TheatreWorkVideoOptions.defaults() : options;
        ProjectAssetCatalog assets = project.assets() == null ? ProjectAssetCatalog.empty() : project.assets();
        Map<String, List<NarrationSegment>> segmentsByBlock = segmentsByBlock(script);
        Map<String, AudioSegmentSnapshot> audioBySegment = completedAudioBySegment(jobs, root);
        Map<String, TheatreProjectLayer.TextActionPlacement> placements = placementsByIntervention(theatre);
        Map<String, TheatreProjectLayer.Scene> scenes = scenesById(theatre);
        Map<String, ProjectAssetReference> intermediateFrames = intermediateFramesByPair(project, root);
        Map<String, String> nextIntervention = nextInterventions(theatre);
        Map<String, String> characterNames = characterNamesById(theatre);

        Path framesDir = root.resolve("exports/video-render-work/theatre-work-frames").toAbsolutePath().normalize();
        Files.createDirectories(framesDir);
        ArrayList<SimpleVideoFrame> frames = new ArrayList<>();
        int index = 1;
        for (TheatreProjectLayer.Intervencion intervention : theatre.intervenciones()) {
            List<NarrationSegment> blockSegments = interventionSegments(script, intervention, segmentsByBlock, theatre.intervenciones());
            if (blockSegments.isEmpty()) {
                throw new IOException("No se encontro texto preparado para " + intervention.id()
                        + " en la obra teatral: " + intervention.blockId());
            }
            List<AudioSegmentSnapshot> audioUnits = audioForSegments(audioBySegment, blockSegments);
            if (audioUnits.isEmpty()) {
                throw new IOException("No hay audio renderizado para " + intervention.id()
                        + ". Genera fragmentos de audio antes de exportar la obra teatral.");
            }
            TheatreProjectLayer.TextActionPlacement placement = placements.get(intervention.id());
            TheatreProjectLayer.Scene scene = placement == null ? null : scenes.get(placement.sceneId());
            ProjectAssetReference visualAsset = visualOptions.useFragmentImages()
                    ? visualResolver.resolve(project, storyboard, script, intervention.id(), root)
                            .map(TheatrePrimaryVisualReference::asset)
                            .orElse(null)
                    : null;
            ProjectAssetReference inferredVisual = inferredVisualFor(effective, visualOptions, intermediateFrames,
                    intervention.id(), nextIntervention.get(intervention.id()), visualAsset);
            ProjectAssetReference mapAsset = optionalMapAsset(assets, visualOptions, scene).orElse(null);
            String fullText = fullInterventionText(blockSegments);
            for (AudioSegmentSnapshot audio : audioUnits) {
                NarrationSegment frameSegment = segmentForAudio(blockSegments, audio.segmentId()).orElse(blockSegments.get(0));
                Path output = framesDir.resolve("theatre-work-" + String.format(Locale.ROOT, "%03d", index) + ".png");
                render(new FrameSpec(
                        effective.resolution().width(),
                        effective.resolution().height(),
                        visualOptions,
                        root,
                        intervention,
                        frameSegment,
                        fullText,
                        scene,
                        placement,
                        visualAsset,
                        mapAsset,
                        characterNames), output);
                double totalDurationSeconds = audio.durationSeconds() + effective.silenceAfterFrameSeconds();
                List<SimpleVideoFrame.VisualPart> visualParts = List.of();
                if (inferredVisual != null && totalDurationSeconds > 0.0) {
                    Path inferredOutput = framesDir.resolve("theatre-work-"
                            + String.format(Locale.ROOT, "%03d", index) + "-inferido.png");
                    render(new FrameSpec(
                            effective.resolution().width(),
                            effective.resolution().height(),
                            visualOptions,
                            root,
                            intervention,
                            frameSegment,
                            fullText,
                            scene,
                            placement,
                            inferredVisual,
                            mapAsset,
                            characterNames), inferredOutput);
                    double first = totalDurationSeconds / 2.0;
                    visualParts = List.of(
                            new SimpleVideoFrame.VisualPart(visualAsset == null ? "" : visualAsset.id(),
                                    relativeToProject(root, output), first, "principal"),
                            new SimpleVideoFrame.VisualPart(inferredVisual.id(), relativeToProject(root, inferredOutput),
                                    totalDurationSeconds - first, "inferido"));
                }
                frames.add(new SimpleVideoFrame(
                        "THEATRE-WORK-" + String.format(Locale.ROOT, "%03d", index),
                        audio.segmentId(),
                        intervention.id(),
                        fullText,
                        visualAsset == null ? "" : visualAsset.id(),
                        relativeToProject(root, output),
                        audio.audioRelativePath(),
                        audio.durationSeconds(),
                        effective.silenceAfterFrameSeconds(),
                        true,
                        true,
                        false,
                        characterLabels(visualOptions, placement, characterNames),
                        visualParts));
                index++;
            }
        }
        if (frames.isEmpty()) {
            throw new IOException("La obra teatral no tiene intervenciones exportables.");
        }
        return new SimpleVideoPlan("Obra teatral - " + script.title(), frames,
                effective.silenceAfterFrameSeconds(), Instant.now());
    }

    private static void render(FrameSpec spec, Path output) throws IOException {
        BufferedImage image = new BufferedImage(spec.width(), spec.height(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            drawFrame(g, spec);
        } finally {
            g.dispose();
        }
        Files.createDirectories(output.toAbsolutePath().normalize().getParent());
        ImageIO.write(image, "png", output.toFile());
    }

    private static void drawFrame(Graphics2D g, FrameSpec spec) throws IOException {
        TheatreWorkVideoOptions options = spec.options();
        Color background = parseColor(options.backgroundColor(), DEFAULT_BACKGROUND);
        Color textColor = parseColor(options.textColor(), DEFAULT_TEXT);
        g.setColor(background);
        g.fillRect(0, 0, spec.width(), spec.height());

        int margin = Math.max(34, spec.width() / 34);
        int gap = Math.max(18, spec.width() / 80);
        boolean mapRequested = (options.showSpatialMap() || options.frameLayout() == TheatreWorkVideoOptions.FrameLayout.IMAGE_WITH_SPATIAL_MAP)
                && spec.mapAsset() != null;
        int mapWidth = mapRequested ? Math.max(300, (int) (spec.width() * 0.30)) : 0;
        int mainX = margin;
        int mainY = margin;
        int mainW = spec.width() - (margin * 2) - (mapRequested ? mapWidth + gap : 0);
        int mainH = spec.height() - (margin * 2);
        int mapX = mainX + mainW + gap;

        BufferedImage visual = optionalImage(spec.projectDirectory(), spec.visualAsset()).orElse(null);
        boolean imageLayout = visual != null && options.frameLayout() != TheatreWorkVideoOptions.FrameLayout.TEXT_ONLY;
        if (imageLayout) {
            drawImageContain(g, visual, mainX, mainY, mainW, mainH, background);
        } else {
            drawPanel(g, mainX, mainY, mainW, mainH, background);
        }
        if (options.showText() && (options.frameLayout() != TheatreWorkVideoOptions.FrameLayout.IMAGE_OR_TEXT || visual == null
                || options.frameLayout() == TheatreWorkVideoOptions.FrameLayout.IMAGE_WITH_TEXT
                || options.frameLayout() == TheatreWorkVideoOptions.FrameLayout.IMAGE_WITH_SPATIAL_MAP)) {
            drawText(g, spec.narrationText(), mainX, mainY, mainW, mainH, options, textColor, imageLayout);
        }
        drawHeader(g, spec, margin, textColor);
        if (mapRequested) {
            drawMapPanel(g, spec, mapX, mainY, mapWidth, mainH, textColor);
        }
    }

    private static void drawHeader(Graphics2D g, FrameSpec spec, int margin, Color textColor) {
        String scene = spec.scene() == null ? "Obra teatral" : spec.scene().displayName();
        String title = spec.intervention().id() + " - " + scene;
        g.setFont(new Font(spec.options().fontFamily(), Font.BOLD, Math.max(18, spec.width() / 60)));
        g.setColor(withAlpha(textColor, 220));
        g.drawString(title, margin, Math.max(26, margin - 10));
    }

    private static void drawText(Graphics2D g,
                                 String text,
                                 int x,
                                 int y,
                                 int w,
                                 int h,
                                 TheatreWorkVideoOptions options,
                                 Color color,
                                 boolean overImage) {
        int pad = Math.max(24, w / 32);
        int boxW = switch (options.textPosition()) {
            case LEFT, RIGHT -> Math.max(260, (int) (w * 0.42));
            default -> Math.max(260, (int) (w * 0.78));
        };
        int boxH = switch (options.textPosition()) {
            case TOP, BOTTOM -> Math.max(150, (int) (h * 0.34));
            default -> Math.max(180, (int) (h * 0.58));
        };
        int boxX = switch (options.textPosition()) {
            case RIGHT -> x + w - boxW - pad;
            case LEFT -> x + pad;
            default -> x + (w - boxW) / 2;
        };
        int boxY = switch (options.textPosition()) {
            case TOP -> y + pad;
            case BOTTOM -> y + h - boxH - pad;
            default -> y + (h - boxH) / 2;
        };
        if (overImage) {
            g.setColor(new Color(255, 255, 255, 218));
            g.fillRoundRect(boxX - 18, boxY - 18, boxW + 36, boxH + 36, 24, 24);
        }
        g.setFont(new Font(options.fontFamily(), Font.PLAIN, options.fontSize()));
        List<String> lines = wrap(text, g.getFontMetrics(), boxW);
        int lineHeight = Math.max(g.getFontMetrics().getHeight(), options.fontSize() + 6);
        int drawY = boxY + Math.max(lineHeight, (boxH - Math.min(lines.size(), boxH / lineHeight) * lineHeight) / 2);
        for (String line : lines) {
            if (drawY > boxY + boxH) {
                break;
            }
            drawEffectText(g, line, boxX, drawY, color, options.textEffect());
            drawY += lineHeight;
        }
    }

    private static void drawEffectText(Graphics2D g, String text, int x, int y, Color color, TheatreWorkVideoOptions.TextEffect effect) {
        if (effect == TheatreWorkVideoOptions.TextEffect.SHADOW) {
            g.setColor(SHADOW);
            g.drawString(text, x + 3, y + 3);
        } else if (effect == TheatreWorkVideoOptions.TextEffect.SOLID_BORDER) {
            g.setColor(Color.WHITE);
            g.drawString(text, x - 1, y);
            g.drawString(text, x + 1, y);
            g.drawString(text, x, y - 1);
            g.drawString(text, x, y + 1);
        }
        g.setColor(color);
        g.drawString(text, x, y);
    }

    private static void drawMapPanel(Graphics2D g, FrameSpec spec, int x, int y, int w, int h, Color textColor) throws IOException {
        drawPanel(g, x, y, w, h, PANEL_BACKGROUND);
        BufferedImage map = optionalImage(spec.projectDirectory(), spec.mapAsset()).orElse(null);
        int pad = Math.max(16, w / 18);
        int labelHeight = Math.max(92, h / 7);
        if (map != null) {
            drawImageContain(g, map, x + pad, y + pad, w - pad * 2, h - labelHeight - pad * 2, PANEL_BACKGROUND);
        }
        g.setColor(withAlpha(textColor, 230));
        g.setFont(new Font(spec.options().fontFamily(), Font.BOLD, Math.max(18, w / 18)));
        g.drawString("Mapa teatral", x + pad, y + h - labelHeight + 26);
        g.setFont(new Font(spec.options().fontFamily(), Font.PLAIN, Math.max(15, w / 24)));
        int lineY = y + h - labelHeight + 54;
        if (spec.options().showCharacters()) {
            g.drawString(characterLine(spec), x + pad, lineY);
            lineY += Math.max(18, w / 24);
        }
        if (spec.options().showDisplacements() && spec.placement() != null) {
            String movement = clean(spec.placement().origin()) + " -> " + clean(spec.placement().destination());
            g.drawString(movement, x + pad, lineY);
            drawArrow(g, x + pad, lineY + 18, x + w - pad, lineY + 18, textColor);
        }
    }

    private static String characterLine(FrameSpec spec) {
        if (spec.placement() == null || spec.placement().characterId().isBlank()) {
            return "Personajes: segun intervencion";
        }
        String name = spec.characterNames().getOrDefault(spec.placement().characterId(), spec.placement().characterId());
        return "Personaje: " + name;
    }

    private static void drawPanel(Graphics2D g, int x, int y, int w, int h, Color fill) {
        g.setColor(fill);
        g.fillRoundRect(x, y, w, h, 28, 28);
        g.setColor(PANEL_BORDER);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, 28, 28);
    }

    private static void drawImageContain(Graphics2D g, BufferedImage image, int x, int y, int w, int h, Color background) {
        g.setColor(background);
        g.fillRoundRect(x, y, w, h, 24, 24);
        double scale = Math.min(w / (double) image.getWidth(), h / (double) image.getHeight());
        int drawW = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int drawH = Math.max(1, (int) Math.round(image.getHeight() * scale));
        int drawX = x + (w - drawW) / 2;
        int drawY = y + (h - drawH) / 2;
        g.drawImage(image, drawX, drawY, drawW, drawH, null);
    }

    private static void drawArrow(Graphics2D g, int x1, int y1, int x2, int y2, Color color) {
        g.setColor(withAlpha(color, 180));
        g.setStroke(new BasicStroke(2.5f));
        g.drawLine(x1, y1, x2, y2);
        g.drawLine(x2, y2, x2 - 12, y2 - 7);
        g.drawLine(x2, y2, x2 - 12, y2 + 7);
    }

    private static List<String> wrap(String text, FontMetrics metrics, int maxWidth) {
        ArrayList<String> lines = new ArrayList<>();
        String[] words = clean(text).split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (metrics.stringWidth(candidate) <= maxWidth || current.isEmpty()) {
                current.setLength(0);
                current.append(candidate);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines.isEmpty() ? List.of("") : List.copyOf(lines);
    }

    private static Map<String, List<NarrationSegment>> segmentsByBlock(NarrationScriptDocument script) {
        LinkedHashMap<String, List<NarrationSegment>> result = new LinkedHashMap<>();
        if (script == null) {
            return result;
        }
        for (NarrationSegment segment : script.segments()) {
            if (segment == null) {
                continue;
            }
            for (String blockId : segment.sourceBlockIds()) {
                if (blockId != null && !blockId.isBlank()) {
                    result.computeIfAbsent(blockId, ignored -> new ArrayList<>()).add(segment);
                }
            }
            result.putIfAbsent(segment.id(), List.of(segment));
        }
        return result;
    }

    private static List<NarrationSegment> interventionSegments(NarrationScriptDocument script,
                                                               TheatreProjectLayer.Intervencion intervention,
                                                               Map<String, List<NarrationSegment>> segmentsByBlock,
                                                               List<TheatreProjectLayer.Intervencion> interventions) {
        if (script == null || intervention == null) {
            return List.of();
        }
        List<NarrationSegment> exact = segmentsByBlock.getOrDefault(intervention.blockId(), List.of());
        if (exact.isEmpty()) {
            return List.of();
        }
        List<NarrationSegment> ordered = script.segments().stream()
                .filter(segment -> segment != null && segment.narratable())
                .toList();
        int first = firstSegmentIndex(ordered, exact.get(0).id());
        if (first < 0) {
            return exact;
        }
        int start = first;
        if (!hasTheatreCue(ordered.get(first).narrationText())) {
            for (int i = first - 1; i >= 0; i--) {
                start = i;
                if (hasTheatreCue(ordered.get(i).narrationText())) {
                    break;
                }
            }
        }
        int end = Math.max(first + 1, first + exact.size());
        for (int i = Math.max(start + 1, end); i < ordered.size(); i++) {
            NarrationSegment candidate = ordered.get(i);
            if (hasTheatreCue(candidate.narrationText()) || sourcedFromDifferentIntervention(candidate, interventions, intervention)) {
                break;
            }
            end = i + 1;
        }
        return List.copyOf(ordered.subList(start, Math.max(start + 1, Math.min(end, ordered.size()))));
    }

    private static int firstSegmentIndex(List<NarrationSegment> ordered, String segmentId) {
        if (segmentId == null || segmentId.isBlank()) {
            return -1;
        }
        for (int i = 0; i < ordered.size(); i++) {
            if (segmentId.equals(ordered.get(i).id())) {
                return i;
            }
        }
        return -1;
    }

    private static boolean sourcedFromDifferentIntervention(NarrationSegment segment,
                                                            List<TheatreProjectLayer.Intervencion> interventions,
                                                            TheatreProjectLayer.Intervencion current) {
        if (segment == null || interventions == null || current == null) {
            return false;
        }
        for (String blockId : segment.sourceBlockIds()) {
            if (blockId == null || blockId.isBlank() || blockId.equals(current.blockId())) {
                continue;
            }
            for (TheatreProjectLayer.Intervencion intervention : interventions) {
                if (!intervention.id().equals(current.id()) && blockId.equals(intervention.blockId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasTheatreCue(String text) {
        String value = clean(text);
        if (value.isBlank() || value.startsWith("(") || value.startsWith("[")) {
            return !value.isBlank();
        }
        int colon = value.indexOf(':');
        if (colon < 2 || colon > 42) {
            return false;
        }
        String cue = value.substring(0, colon).strip();
        return !cue.equalsIgnoreCase("ESCENA") && !cue.equalsIgnoreCase("ACTO")
                && cue.chars().anyMatch(Character::isLetter);
    }

    private static Optional<NarrationSegment> segmentForAudio(List<NarrationSegment> segments, String audioSegmentId) {
        if (segments == null || audioSegmentId == null || audioSegmentId.isBlank()) {
            return Optional.empty();
        }
        return segments.stream()
                .filter(segment -> audioSegmentId.equals(segment.id()) || audioSegmentId.startsWith(segment.id() + "-"))
                .findFirst();
    }

    private static Map<String, AudioSegmentSnapshot> completedAudioBySegment(List<AudioJobSnapshot> jobs, Path projectDirectory) {
        LinkedHashMap<String, AudioSegmentSnapshot> result = new LinkedHashMap<>();
        if (jobs == null || jobs.isEmpty()) {
            return result;
        }
        jobs.stream()
                .sorted(Comparator.comparing(AudioJobSnapshot::updatedAt).reversed())
                .forEach(job -> {
                    for (AudioSegmentSnapshot segment : job.segments()) {
                        if (usableAudio(projectDirectory, segment)) {
                            result.putIfAbsent(segment.segmentId(), segment);
                        }
                    }
                });
        return result;
    }

    private static boolean usableAudio(Path projectDirectory, AudioSegmentSnapshot segment) {
        if (segment == null || !segment.completed() || segment.audioRelativePath().isBlank()) {
            return false;
        }
        Path root = projectDirectory.toAbsolutePath().normalize();
        Path audio = root.resolve(segment.audioRelativePath()).toAbsolutePath().normalize();
        return audio.startsWith(root) && Files.isRegularFile(audio);
    }

    private static List<AudioSegmentSnapshot> audioForSegments(Map<String, AudioSegmentSnapshot> audioBySegment,
                                                               List<NarrationSegment> segments) {
        ArrayList<AudioSegmentSnapshot> result = new ArrayList<>();
        for (NarrationSegment segment : segments == null ? List.<NarrationSegment>of() : segments) {
            String unitPrefix = segment.id() + "-";
            for (AudioSegmentSnapshot audio : audioBySegment.values()) {
                if (segment.id().equals(audio.segmentId()) || audio.segmentId().startsWith(unitPrefix)) {
                    if (result.stream().noneMatch(existing -> existing.segmentId().equals(audio.segmentId()))) {
                        result.add(audio);
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private static String fullInterventionText(List<NarrationSegment> segments) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < (segments == null ? 0 : segments.size()); i++) {
            NarrationSegment segment = segments.get(i);
            if (segment == null || segment.narrationText().isBlank()) {
                continue;
            }
            String part = i == 0 ? segment.narrationText().strip() : stripSpeaker(segment.narrationText());
            if (!part.isBlank()) {
                if (!text.isEmpty()) {
                    text.append(' ');
                }
                text.append(part);
            }
        }
        return text.toString().replaceAll("\\s+", " ").strip();
    }

    private static String stripSpeaker(String text) {
        String value = clean(text);
        int colon = value.indexOf(':');
        if (colon > 1 && colon < 42) {
            return value.substring(colon + 1).strip();
        }
        return value;
    }

    private static Map<String, TheatreProjectLayer.TextActionPlacement> placementsByIntervention(TheatreProjectLayer theatre) {
        LinkedHashMap<String, TheatreProjectLayer.TextActionPlacement> result = new LinkedHashMap<>();
        for (TheatreProjectLayer.TextActionPlacement placement : theatre.textActionPlacements()) {
            result.put(placement.intervencionId(), placement);
        }
        return result;
    }

    private static Map<String, TheatreProjectLayer.Scene> scenesById(TheatreProjectLayer theatre) {
        LinkedHashMap<String, TheatreProjectLayer.Scene> result = new LinkedHashMap<>();
        for (TheatreProjectLayer.Scene scene : theatre.scenes()) {
            result.put(scene.id(), scene);
        }
        return result;
    }

    private static Map<String, String> characterNamesById(TheatreProjectLayer theatre) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (TheatreProjectLayer.CharacterProfile character : theatre.characters()) {
            result.put(character.id(), character.displayName());
        }
        return result;
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

    private static Map<String, String> nextInterventions(TheatreProjectLayer theatre) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        if (theatre == null) {
            return result;
        }
        List<TheatreProjectLayer.Intervencion> interventions = theatre.intervenciones().stream()
                .sorted(Comparator.comparingInt(TheatreProjectLayer.Intervencion::sequenceIndex))
                .toList();
        for (int i = 0; i + 1 < interventions.size(); i++) {
            result.put(interventions.get(i).id(), interventions.get(i + 1).id());
        }
        return result;
    }

    private static ProjectAssetReference inferredVisualFor(SimpleVideoExportSettings settings,
                                                           TheatreWorkVideoOptions options,
                                                           Map<String, ProjectAssetReference> intermediateFrames,
                                                           String currentInterventionId,
                                                           String nextInterventionId,
                                                           ProjectAssetReference visualAsset) {
        if (settings == null || !settings.includeInferredFrames()
                || options == null || !options.useFragmentImages()
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

    private static Optional<ProjectAssetReference> optionalMapAsset(ProjectAssetCatalog assets,
                                                                    TheatreWorkVideoOptions options,
                                                                    TheatreProjectLayer.Scene scene) {
        if (scene == null || (!options.showSpatialMap()
                && options.frameLayout() != TheatreWorkVideoOptions.FrameLayout.IMAGE_WITH_SPATIAL_MAP)) {
            return Optional.empty();
        }
        return optionalImageAsset(assets, scene.spatialMapAssetId());
    }

    private static Optional<ProjectAssetReference> optionalImageAsset(ProjectAssetCatalog assets, String assetId) {
        if (assets == null || assetId == null || assetId.isBlank()) {
            return Optional.empty();
        }
        return assets.byId(assetId).filter(ProjectAssetReference::isImage);
    }

    private static Optional<BufferedImage> optionalImage(Path projectDirectory, ProjectAssetReference asset) throws IOException {
        if (asset == null) {
            return Optional.empty();
        }
        Path root = projectDirectory.toAbsolutePath().normalize();
        Path path = root.resolve(asset.relativePath()).toAbsolutePath().normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            return Optional.empty();
        }
        return Optional.ofNullable(ImageIO.read(path.toFile()));
    }

    private static String relativeToProject(Path projectDirectory, Path file) throws IOException {
        Path root = projectDirectory.toAbsolutePath().normalize();
        Path normalized = file.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) {
            throw new IOException("El frame generado quedo fuera del proyecto: " + normalized);
        }
        return root.relativize(normalized).toString().replace('\\', '/');
    }

    private static List<SimpleVideoFrame.CharacterLabel> characterLabels(TheatreWorkVideoOptions options,
                                                                         TheatreProjectLayer.TextActionPlacement placement,
                                                                         Map<String, String> characterNames) {
        if (!options.showCharacters() || placement == null || placement.characterId().isBlank()) {
            return List.of();
        }
        return List.of(new SimpleVideoFrame.CharacterLabel(
                characterNames.getOrDefault(placement.characterId(), placement.characterId()), 0.5, 0.5));
    }

    private static Color parseColor(String value, Color fallback) {
        String raw = value == null ? "" : value.strip();
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        }
        try {
            if (raw.length() == 6) {
                return new Color(Integer.parseInt(raw, 16));
            }
        } catch (NumberFormatException ignored) {
            return fallback;
        }
        return fallback;
    }

    private static Color withAlpha(Color color, int alpha) {
        Color safe = color == null ? DEFAULT_TEXT : color;
        return new Color(safe.getRed(), safe.getGreen(), safe.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static String clean(String text) {
        return text == null ? "" : text.strip();
    }

    private record FrameSpec(
            int width,
            int height,
            TheatreWorkVideoOptions options,
            Path projectDirectory,
            TheatreProjectLayer.Intervencion intervention,
            NarrationSegment segment,
            String narrationText,
            TheatreProjectLayer.Scene scene,
            TheatreProjectLayer.TextActionPlacement placement,
            ProjectAssetReference visualAsset,
            ProjectAssetReference mapAsset,
            Map<String, String> characterNames
    ) {
        private FrameSpec {
            width = Math.max(640, width);
            height = Math.max(360, height);
            options = options == null ? TheatreWorkVideoOptions.defaults() : options;
            narrationText = narrationText == null || narrationText.isBlank()
                    ? (segment == null ? "" : segment.narrationText())
                    : narrationText.strip();
            characterNames = characterNames == null ? Map.of() : Map.copyOf(characterNames);
        }
    }
}
