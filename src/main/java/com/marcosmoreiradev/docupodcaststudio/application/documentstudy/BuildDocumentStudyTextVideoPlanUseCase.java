package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoFrame;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioSegmentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

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
import java.util.Map;

/** Builds renderable temporary text frames for Estudio documental without creating project assets. */
public final class BuildDocumentStudyTextVideoPlanUseCase {
    private static final String FRAME_DIRECTORY = "exports/document-study-frames";

    public SimpleVideoPlan build(
            NarrationScriptDocument script,
            List<AudioJobSnapshot> jobs,
            Path projectDirectory,
            DocumentTextVideoOptions options
    ) throws IOException {
        if (script == null || script.empty()) {
            throw new IllegalArgumentException("Se requiere lectura preparada para exportar video documental texto+audio.");
        }
        Path root = projectDirectory == null ? null : projectDirectory.toAbsolutePath().normalize();
        if (root == null) {
            throw new IllegalArgumentException("Se requiere carpeta de proyecto para crear frames temporales.");
        }
        Files.createDirectories(root.resolve(FRAME_DIRECTORY));
        DocumentTextVideoOptions effective = options == null ? DocumentTextVideoOptions.defaults() : options;
        Map<String, AudioSegmentSnapshot> audio = completedAudioBySegment(jobs, root);
        ArrayList<SimpleVideoFrame> frames = new ArrayList<>();
        int index = 1;
        for (NarrationSegment segment : script.segments()) {
            if (!segment.narratable() || secondaryReadUnit(segment)) {
                continue;
            }
            List<AudioSegmentSnapshot> clips = audioForSegment(audio, segment.id());
            if (clips.isEmpty()) {
                clips = new ArrayList<>();
                clips.add(null);
            }
            for (AudioSegmentSnapshot clip : clips) {
                String frameId = "DOC-FRAME-" + String.format(java.util.Locale.ROOT, "%03d", index);
                Path frameFile = root.resolve(FRAME_DIRECTORY).resolve(frameId.toLowerCase(java.util.Locale.ROOT) + ".png");
                writeTextFrame(frameFile, script.title(), segment, effective, index);
                String frameRelativePath = portableRelativePath(root, frameFile);
                frames.add(new SimpleVideoFrame(
                        frameId,
                        clip == null ? segment.id() : clip.segmentId(),
                        segment.title().isBlank() ? segment.id() : segment.title(),
                        segment.narrationText(),
                        "",
                        frameRelativePath,
                        clip == null ? "" : clip.audioRelativePath(),
                        clip == null ? 0.0 : clip.durationSeconds(),
                        0.35,
                        true,
                        clip != null && clip.completed()));
                index++;
            }
        }
        return new SimpleVideoPlan("Estudio documental texto+audio - " + script.title(), frames, 0.35, Instant.now());
    }

    private static Map<String, AudioSegmentSnapshot> completedAudioBySegment(List<AudioJobSnapshot> jobs, Path root) {
        LinkedHashMap<String, AudioSegmentSnapshot> result = new LinkedHashMap<>();
        if (jobs == null || jobs.isEmpty()) {
            return result;
        }
        jobs.stream()
                .filter(job -> job != null)
                .sorted(Comparator.comparing(AudioJobSnapshot::updatedAt))
                .forEach(job -> {
                    for (AudioSegmentSnapshot segment : job.segments()) {
                        if (usableAudio(root, segment)) {
                            result.put(segment.segmentId(), segment);
                        }
                    }
                });
        return result;
    }

    private static boolean usableAudio(Path root, AudioSegmentSnapshot segment) {
        if (segment == null || !segment.completed() || segment.audioRelativePath().isBlank()) {
            return false;
        }
        Path audio = root.resolve(segment.audioRelativePath()).toAbsolutePath().normalize();
        return audio.startsWith(root) && Files.isRegularFile(audio);
    }

    private static List<AudioSegmentSnapshot> audioForSegment(Map<String, AudioSegmentSnapshot> audio, String segmentId) {
        if (audio == null || audio.isEmpty() || segmentId == null || segmentId.isBlank()) {
            return List.of();
        }
        String unitPrefix = segmentId + "-";
        ArrayList<AudioSegmentSnapshot> units = new ArrayList<>();
        for (AudioSegmentSnapshot clip : audio.values()) {
            if (segmentId.equals(clip.segmentId())) {
                return units.isEmpty() ? List.of(clip) : List.copyOf(units);
            }
            if (clip.segmentId().startsWith(unitPrefix)) {
                units.add(clip);
            }
        }
        return List.copyOf(units);
    }

    private static boolean secondaryReadUnit(NarrationSegment segment) {
        return segment != null && Boolean.parseBoolean(segment.metadata().getOrDefault("secondaryReadUnit", "false"));
    }

    private static void writeTextFrame(Path target,
                                       String documentTitle,
                                       NarrationSegment segment,
                                       DocumentTextVideoOptions options,
                                       int index) throws IOException {
        int width = options.resolution().width();
        int height = options.resolution().height();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(color(options.backgroundColor()));
            g.fillRect(0, 0, width, height);
            drawBackgroundImageIfPresent(g, options, width, height);
            int margin = Math.max(64, width / 12);
            int top = Math.max(56, height / 10);
            g.setColor(color(options.accentColor()));
            g.fillRect(margin, top, Math.max(96, width / 10), Math.max(8, height / 180));
            g.setColor(color(options.textColor()));
            Font titleFont = new Font(options.titleFontFamily(), Font.BOLD, Math.max(24, options.fontSize() - 16));
            Font bodyFont = new Font(options.fontFamily(), Font.PLAIN, options.fontSize());
            g.setFont(titleFont);
            FontMetrics titleMetrics = g.getFontMetrics();
            String title = "Fragmento " + index + " - " + (segment.title().isBlank() ? documentTitle : segment.title());
            DocumentTextPainter.drawString(g, abbreviate(title, 96), margin,
                    top + Math.max(64, titleMetrics.getHeight() + 20), options);
            g.setFont(bodyFont);
            FontMetrics bodyMetrics = g.getFontMetrics();
            int y = top + Math.max(128, titleMetrics.getHeight() + 90);
            int lineHeight = Math.max(bodyMetrics.getHeight() + 8, options.fontSize() + 18);
            int maxWidth = width - margin * 2;
            int maxY = height - Math.max(64, height / 10);
            for (String line : wrap(segment.narrationText(), bodyMetrics, maxWidth)) {
                if (y + lineHeight > maxY) {
                    DocumentTextPainter.drawString(g, "...", margin, y, options);
                    break;
                }
                DocumentTextPainter.drawString(g, line, margin, y, options);
                drawNarratedUnderline(g, effectiveUnderlineWidth(line, bodyMetrics),
                        margin, y, bodyMetrics, options);
                y += lineHeight;
            }
        } finally {
            g.dispose();
        }
        Files.createDirectories(target.getParent());
        ImageIO.write(image, "png", target.toFile());
    }

    private static int effectiveUnderlineWidth(String line, FontMetrics metrics) {
        return line == null ? 0 : metrics.stringWidth(line);
    }

    private static void drawNarratedUnderline(Graphics2D g, int width, int x, int baseline,
                                              FontMetrics metrics, DocumentTextVideoOptions options) {
        if (!options.underlineNarratedText() || options.narratedUnderlineThicknessPx() <= 0 || width <= 0) {
            return;
        }
        var previousColor = g.getColor();
        var previousStroke = g.getStroke();
        int thickness = options.narratedUnderlineThicknessPx();
        int y = baseline + Math.max(thickness, metrics.getDescent() / 2);
        g.setColor(color(options.narratedUnderlineColor()));
        g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(x, y, x + width, y);
        g.setStroke(previousStroke);
        g.setColor(previousColor);
    }

    private static void drawBackgroundImageIfPresent(Graphics2D g, DocumentTextVideoOptions options, int width, int height) throws IOException {
        if (options.backgroundMode() != DocumentTextVideoBackgroundMode.IMAGE || options.backgroundImagePath().isBlank()) {
            return;
        }
        BufferedImage background = ImageIO.read(Path.of(options.backgroundImagePath()).toFile());
        if (background == null) {
            throw new IOException("No se pudo leer imagen de fondo documental: " + options.backgroundImagePath());
        }
        DocumentBackgroundImagePainter.paint(g, background, width, height,
                options.backgroundImageOpacity(), options.backgroundImageFit());
    }

    private static List<String> wrap(String text, FontMetrics metrics, int maxWidth) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").strip();
        if (normalized.isBlank()) {
            return List.of("");
        }
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : normalized.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(candidate) <= maxWidth || line.isEmpty()) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static Color color(String hex) {
        return Color.decode(hex == null || hex.isBlank() ? "#FFFFFF" : hex);
    }

    private static String portableRelativePath(Path root, Path file) {
        return root.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private static String abbreviate(String value, int max) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").strip();
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, max - 3)).strip() + "...";
    }
}
