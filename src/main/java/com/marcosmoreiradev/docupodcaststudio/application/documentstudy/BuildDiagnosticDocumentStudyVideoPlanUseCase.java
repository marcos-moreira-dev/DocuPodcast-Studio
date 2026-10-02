package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoFrame;
import com.marcosmoreiradev.docupodcaststudio.application.video.SimpleVideoPlan;
import com.marcosmoreiradev.docupodcaststudio.application.video.NarratedFrameBinding;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Adds a diagnostic overlay without rebuilding or changing the audiovisual timeline. */
public final class BuildDiagnosticDocumentStudyVideoPlanUseCase {
    private static final String DIRECTORY = "exports/document-study-frames-diagnostic";

    public SimpleVideoPlan build(SimpleVideoPlan normal, Path projectDirectory) throws IOException {
        if (normal == null) throw new IllegalArgumentException("Se requiere el plan normal.");
        Path root = projectDirectory.toAbsolutePath().normalize();
        Path directory = root.resolve(DIRECTORY).normalize();
        if (!directory.startsWith(root)) throw new IOException("Ruta diagnostica fuera del proyecto.");
        Files.createDirectories(directory);
        ArrayList<SimpleVideoFrame> frames = new ArrayList<>();
        double start = 0.0;
        for (int index = 0; index < normal.frames().size(); index++) {
            SimpleVideoFrame frame = normal.frames().get(index);
            double end = start + frame.frameDurationSeconds();
            Path source = root.resolve(frame.imageRelativePath()).normalize();
            if (!source.startsWith(root) || !Files.isRegularFile(source)) {
                throw new IOException("No existe el frame fuente diagnostico " + frame.id() + ".");
            }
            Path target = directory.resolve(String.format(Locale.ROOT, "%04d-%s.png",
                    index + 1, token(frame.id())));
            overlay(source, target, frame, start, end);
            String relative = root.relativize(target).toString().replace('\\', '/');
            List<SimpleVideoFrame.VisualPart> parts = List.of(new SimpleVideoFrame.VisualPart(
                    frame.imageAssetId(), relative, frame.frameDurationSeconds(), "diagnostic-overlay"));
            NarratedFrameBinding binding = diagnosticBinding(frame.visualBinding(), relative);
            frames.add(new SimpleVideoFrame(frame.id(), frame.segmentId(), frame.title(),
                    frame.narrationPreview(), frame.imageAssetId(), relative,
                    frame.audioRelativePath(), frame.audioDurationSeconds(),
                    frame.silenceAfterSeconds(), true, frame.audioReady(),
                    frame.silentVisual(), frame.characterLabels(), parts, binding));
            start = end;
        }
        SimpleVideoPlan diagnostic = new SimpleVideoPlan(normal.title() + " [diagnostico]", frames,
                normal.silenceAfterFrameSeconds(), normal.createdAt());
        new NarratedFrameBindingManifestWriter().writeDiagnostic(diagnostic, root);
        return diagnostic;
    }

    private static void overlay(Path source, Path target, SimpleVideoFrame frame,
                                double start, double end) throws IOException {
        BufferedImage image;
        try (var input = ImageIO.createImageInputStream(source.toFile())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Formato visual no reconocido: " + source);
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                image = reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new IOException("No se pudo leer el frame fuente diagnostico " + source + ".", ex);
        }
        if (image == null) throw new IOException("No se pudo leer " + source + ".");
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int padding = Math.max(10, image.getHeight() / 90);
            int fontSize = Math.max(13, image.getHeight() / 48);
            int lineHeight = fontSize + Math.max(4, fontSize / 4);
            int boxHeight = lineHeight * 6 + padding * 2;
            g.setColor(new Color(0, 0, 0, 178));
            g.fillRect(0, image.getHeight() - boxHeight, image.getWidth(), boxHeight);
            g.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
            g.setColor(Color.WHITE);
            int y = image.getHeight() - boxHeight + padding + fontSize;
            NarratedFrameBinding binding = frame.visualBinding();
            g.drawString("frame=" + frame.id() + "  segment=" + frame.segmentId()
                    + "  mode=" + (binding == null ? frame.frameModeLabel()
                    : binding.presentationMode()), padding, y);
            y += lineHeight;
            if (binding != null) {
                g.drawString("source=" + shortId(binding.sourceBlockId())
                        + "  region=" + shortId(binding.regionId())
                        + "  type=" + binding.sourceBlockType()
                        + "  page=" + binding.pageNumber(), padding, y);
                y += lineHeight;
                g.drawString("sourceRange=" + binding.sourceTextStart() + ".."
                        + binding.sourceTextEnd() + "  fragmentCovered="
                        + binding.fragmentCovered(), padding, y);
                y += lineHeight;
                g.drawString("visualSource=" + binding.visualSource()
                        + "  classification=" + binding.quality(), padding, y);
                y += lineHeight;
            }
            g.drawString(String.format(Locale.ROOT,
                    "timeline=%.3f..%.3f s  audio=%.3f s  frame=%.3f s",
                    start, end, frame.audioDurationSeconds(), frame.frameDurationSeconds()), padding, y);
            y += lineHeight;
            g.drawString("visual=" + frame.imageRelativePath(), padding, y);
            g.setColor(new Color(255, 196, 0));
            g.drawRect(1, 1, image.getWidth() - 3, image.getHeight() - 3);
        } finally {
            g.dispose();
        }
        if (!ImageIO.write(image, "png", target.toFile())) {
            throw new IOException("No se pudo escribir " + target + ".");
        }
    }

    private static String token(String value) {
        return value.replaceAll("[^A-Za-z0-9_-]", "-");
    }

    private static String shortId(String value) {
        if (value == null || value.length() <= 22) return value == null ? "" : value;
        return value.substring(0, 10) + "..." + value.substring(value.length() - 8);
    }

    private static NarratedFrameBinding diagnosticBinding(
            NarratedFrameBinding binding, String imageRelativePath) {
        if (binding == null) return null;
        return new NarratedFrameBinding(binding.frameId(), binding.segmentId(),
                binding.sourceBlockId(), binding.sourceBlockIds(), binding.regionId(), binding.sourceBlockType(),
                binding.pageNumber(), binding.readingOrder(), binding.presentationMode(),
                binding.visualSource(), binding.sourceBBox(), binding.cropBBox(),
                imageRelativePath, binding.audioPath(), binding.durationMillis(),
                binding.renderedTextHash(), binding.expectedSourceTextHash(),
                binding.sourceTextStart(), binding.sourceTextEnd(),
                binding.sourceTextPreview(), binding.sourceLineIndices(),
                binding.sourceWordIndices(), binding.expectedFragmentBboxes(),
                binding.fragmentCovered(),
                binding.quality(), binding.reason());
    }
}
