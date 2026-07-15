package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentMascotPosition;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentParagraphVisualAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyClosingSlide;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Composes stable paragraph, cover and table PNGs for documentary export. */
public final class DocumentStudySlideCompositor {
    public void composeParagraph(Path target, DocumentBlock block,
                                 DocumentParagraphVisualAssignment visual,
                                 DocumentStudyVideoConfiguration configuration,
                                 DocuPodcastProject project, Path projectDirectory,
                                 DocumentTextVideoOptions options) throws IOException {
        BufferedImage image = canvas(options);
        Graphics2D g = graphics(image);
        try {
            paintBackground(g, image, options);
            int width = image.getWidth(), height = image.getHeight();
            int margin = Math.max(36, width / 16);
            int y = margin;
            y = drawGlobalTitle(g, configuration.videoTitle(), options, margin, y, width - margin * 2);
            int imageHeight = Math.max(height / 3, (int) (height * 0.40));
            int gap = Math.max(18, height / 50);
            int textBottom = height - margin - imageHeight - gap;
            drawJustifiedText(g, block.text(), options, margin, y, width - margin * 2,
                    textBottom - y, block.id());
            int imageY = textBottom + gap;
            drawContainedAsset(g, project, projectDirectory, visual.activeImageAssetId(),
                    margin, imageY, width - margin * 2, imageHeight);
            drawMascot(g, project, projectDirectory, visual, margin, imageY,
                    width - margin * 2, imageHeight);
        } finally {
            g.dispose();
        }
        write(target, image);
    }

    public void composeCover(Path target, DocumentBlock block,
                             DocumentStudyVideoConfiguration configuration,
                             DocumentTextVideoOptions options) throws IOException {
        BufferedImage image = canvas(options);
        Graphics2D g = graphics(image);
        try {
            paintBackground(g, image, options);
            int width = image.getWidth(), height = image.getHeight();
            int margin = Math.max(48, width / 10);
            g.setColor(color(options.accentColor()));
            g.fillRect(margin, height / 5, Math.max(90, width / 9), Math.max(7, height / 170));
            String title = configuration.videoTitle().isBlank() ? block.text() : configuration.videoTitle();
            String subtitle = configuration.videoTitle().isBlank() ? "" : block.text();
            drawCenteredFit(g, title, options.fontFamily(), Font.BOLD, options.fontSize() + 18,
                    Math.max(26, options.fontSize() / 2), margin, height / 3, width - margin * 2, height / 3,
                    color(options.textColor()), block.id());
            if (!subtitle.isBlank() && !subtitle.equals(title)) {
                drawCenteredFit(g, subtitle, options.fontFamily(), Font.PLAIN, options.fontSize(),
                        20, margin, (int) (height * 0.70), width - margin * 2, height / 7,
                        color(options.textColor()), block.id());
            }
        } finally { g.dispose(); }
        write(target, image);
    }

    public void composeTable(Path target, DocumentBlock block,
                             DocumentStudyVideoConfiguration configuration,
                             DocumentTextVideoOptions options) throws IOException {
        BufferedImage image = canvas(options);
        Graphics2D g = graphics(image);
        try {
            paintBackground(g, image, options);
            int width = image.getWidth(), height = image.getHeight();
            int margin = Math.max(30, width / 18);
            int y = margin;
            y = drawGlobalTitle(g, configuration.videoTitle(), options, margin, y, width - margin * 2);
            TableData table = tableData(block);
            drawTable(g, table, options, margin, y, width - margin * 2, height - margin - y, block.id());
        } finally { g.dispose(); }
        write(target, image);
    }

    public void composeClosing(Path target, DocumentStudyClosingSlide slide,
                               DocuPodcastProject project, Path projectDirectory,
                               DocumentTextVideoOptions options) throws IOException {
        BufferedImage image = canvas(options);
        Graphics2D g = graphics(image);
        try {
            paintBackground(g, image, options);
            int width = image.getWidth(), height = image.getHeight();
            int margin = Math.max(44, width / 14);
            int imageY = margin;
            int imageHeight = height - margin * 2;
            if (!slide.title().isBlank()) {
                int titleHeight = Math.max(height / 8, 100);
                drawCenteredFit(g, slide.title(), options.fontFamily(), Font.BOLD,
                        options.fontSize() + 12, Math.max(22, options.fontSize() / 2),
                        margin, margin, width - margin * 2, titleHeight,
                        color(options.textColor()), slide.id());
                imageY = margin + titleHeight + Math.max(18, height / 45);
                imageHeight = height - imageY - margin;
            }
            drawContainedAsset(g, project, projectDirectory, slide.imageAssetId(),
                    margin, imageY, width - margin * 2, imageHeight);
        } finally {
            g.dispose();
        }
        write(target, image);
    }

    private static BufferedImage canvas(DocumentTextVideoOptions options) {
        return new BufferedImage(options.resolution().width(), options.resolution().height(), BufferedImage.TYPE_INT_RGB);
    }

    private static Graphics2D graphics(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        return g;
    }

    private static void paintBackground(Graphics2D g, BufferedImage image, DocumentTextVideoOptions options)
            throws IOException {
        g.setColor(color(options.backgroundColor()));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        if (options.backgroundMode() == DocumentTextVideoBackgroundMode.IMAGE
                && !options.backgroundImagePath().isBlank()) {
            BufferedImage bg = ImageIO.read(Path.of(options.backgroundImagePath()).toFile());
            if (bg == null) throw new IOException("No se pudo leer el fondo documental configurado.");
            double scale = Math.max(image.getWidth() / (double) bg.getWidth(), image.getHeight() / (double) bg.getHeight());
            int w = (int) Math.ceil(bg.getWidth() * scale), h = (int) Math.ceil(bg.getHeight() * scale);
            g.drawImage(bg, (image.getWidth() - w) / 2, (image.getHeight() - h) / 2, w, h, null);
            g.setColor(new Color(255, 255, 255, 185));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
        }
    }

    private static int drawGlobalTitle(Graphics2D g, String title, DocumentTextVideoOptions options,
                                       int x, int y, int width) throws IOException {
        if (title == null || title.isBlank()) return y;
        int preferred = Math.max(22, options.fontSize() - 14);
        int minimum = Math.max(18, options.resolution().height() / 60);
        for (int size = preferred; size >= minimum; size--) {
            Font font = new Font(options.fontFamily(), Font.BOLD, size);
            FontMetrics fm = g.getFontMetrics(font);
            List<String> lines = wrap(title, fm, width);
            if (lines.size() > 2) continue;
            g.setFont(font); g.setColor(color(options.textColor()));
            int baseline = y + fm.getAscent();
            for (String line : lines) {
                g.drawString(line, x, baseline);
                baseline += fm.getHeight();
            }
            int next = y + lines.size() * fm.getHeight() + Math.max(12, fm.getHeight() / 3);
            g.setColor(color(options.accentColor()));
            g.fillRect(x, next - 5, Math.max(80, width / 9), Math.max(5, fm.getHeight() / 10));
            return next + Math.max(10, fm.getHeight() / 4);
        }
        throw new IOException("El titulo global no cabe completo en la zona superior.");
    }

    private static void drawJustifiedText(Graphics2D g, String text, DocumentTextVideoOptions options,
                                          int x, int y, int width, int height, String blockId) throws IOException {
        int minimum = Math.max(18, options.resolution().height() / 60);
        for (int size = options.fontSize(); size >= minimum; size--) {
            Font font = new Font(options.fontFamily(), Font.PLAIN, size);
            FontMetrics fm = g.getFontMetrics(font);
            List<String> lines = wrap(text, fm, width);
            int lineHeight = fm.getHeight() + Math.max(3, size / 5);
            if (lines.size() * lineHeight <= height) {
                g.setFont(font); g.setColor(color(options.textColor()));
                int baseline = y + fm.getAscent();
                for (int i = 0; i < lines.size(); i++) {
                    drawJustifiedLine(g, lines.get(i), x, baseline, width, i == lines.size() - 1, fm);
                    baseline += lineHeight;
                }
                return;
            }
        }
        throw new IOException("El parrafo " + blockId + " no cabe completo con un tamano de texto legible.");
    }

    private static void drawJustifiedLine(Graphics2D g, String line, int x, int baseline, int width,
                                          boolean last, FontMetrics fm) {
        String[] words = line.split(" ");
        if (last || words.length < 2) { g.drawString(line, x, baseline); return; }
        int wordsWidth = 0;
        for (String word : words) wordsWidth += fm.stringWidth(word);
        double gap = (width - wordsWidth) / (double) (words.length - 1);
        double cursor = x;
        for (String word : words) {
            g.drawString(word, (int) Math.round(cursor), baseline);
            cursor += fm.stringWidth(word) + gap;
        }
    }

    private static void drawContainedAsset(Graphics2D g, DocuPodcastProject project, Path root, String assetId,
                                           int x, int y, int width, int height) throws IOException {
        BufferedImage asset = readAsset(project, root, assetId);
        if (asset == null) return;
        double scale = Math.min(width / (double) asset.getWidth(), height / (double) asset.getHeight());
        int w = Math.max(1, (int) Math.round(asset.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(asset.getHeight() * scale));
        g.drawImage(asset, x + (width - w) / 2, y + (height - h) / 2, w, h, null);
    }

    private static void drawMascot(Graphics2D g, DocuPodcastProject project, Path root,
                                   DocumentParagraphVisualAssignment visual,
                                   int x, int y, int width, int height) throws IOException {
        BufferedImage mascot = readAsset(project, root, visual.mascotAssetId());
        if (mascot == null) return;
        int maxW = Math.max(70, width / 5), maxH = Math.max(70, height / 2);
        double scale = Math.min(maxW / (double) mascot.getWidth(), maxH / (double) mascot.getHeight());
        int w = Math.max(1, (int) Math.round(mascot.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(mascot.getHeight() * scale));
        int inset = Math.max(8, width / 100);
        int drawX = visual.mascotPosition() == DocumentMascotPosition.BOTTOM_LEFT
                ? x + inset : x + width - w - inset;
        g.drawImage(mascot, drawX, y + height - h - inset, w, h, null);
    }

    private static BufferedImage readAsset(DocuPodcastProject project, Path root, String assetId) throws IOException {
        if (project == null || root == null || assetId == null || assetId.isBlank()) return null;
        ProjectAssetReference ref = project.assets().byId(assetId).orElse(null);
        if (ref == null || !ref.isImage()) return null;
        Path file = root.resolve(ref.relativePath()).toAbsolutePath().normalize();
        Path safeRoot = root.toAbsolutePath().normalize();
        if (!file.startsWith(safeRoot) || !Files.isRegularFile(file)) return null;
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null) throw new IOException("No se pudo leer el asset de imagen " + assetId + ".");
        return image;
    }

    private static void drawTable(Graphics2D g, TableData table, DocumentTextVideoOptions options,
                                  int x, int y, int width, int height, String blockId) throws IOException {
        int rows = table.rows().size(), columns = table.columns();
        if (rows == 0 || columns == 0) throw new IOException("La tabla " + blockId + " no contiene celdas renderizables.");
        int minimum = Math.max(12, options.resolution().height() / 90);
        for (int size = Math.min(options.fontSize() - 16, 34); size >= minimum; size--) {
            Font font = new Font(options.fontFamily(), Font.PLAIN, size);
            Font headerFont = font.deriveFont(Font.BOLD);
            FontMetrics fm = g.getFontMetrics(font);
            int columnWidth = width / columns;
            ArrayList<Integer> rowHeights = new ArrayList<>();
            int total = 0;
            for (List<String> row : table.rows()) {
                int lineCount = 1;
                for (int c = 0; c < columns; c++) {
                    String cell = c < row.size() ? row.get(c) : "";
                    lineCount = Math.max(lineCount, wrap(cell, fm, columnWidth - 20).size());
                }
                int rowHeight = lineCount * (fm.getHeight() + 2) + 18;
                rowHeights.add(rowHeight); total += rowHeight;
            }
            if (total > height) continue;
            int cy = y;
            for (int r = 0; r < rows; r++) {
                int rh = rowHeights.get(r);
                for (int c = 0; c < columns; c++) {
                    int cx = x + c * columnWidth;
                    g.setColor(r == 0 ? color(options.accentColor()) : (r % 2 == 0 ? new Color(246, 247, 250) : Color.WHITE));
                    g.fillRect(cx, cy, columnWidth, rh);
                    g.setColor(r == 0 ? Color.WHITE : color(options.textColor()));
                    g.setFont(r == 0 ? headerFont : font);
                    FontMetrics cellMetrics = g.getFontMetrics();
                    String cell = c < table.rows().get(r).size() ? table.rows().get(r).get(c) : "";
                    int baseline = cy + 9 + cellMetrics.getAscent();
                    for (String line : wrap(cell, cellMetrics, columnWidth - 20)) {
                        g.drawString(line, cx + 10, baseline); baseline += cellMetrics.getHeight() + 2;
                    }
                    g.setColor(new Color(190, 195, 205));
                    g.drawRect(cx, cy, columnWidth, rh);
                }
                cy += rh;
            }
            return;
        }
        throw new IOException("La tabla " + blockId + " no cabe completa con un tamano legible.");
    }

    private static TableData tableData(DocumentBlock block) {
        Map<String, String> meta = block.metadata();
        int rows = integer(meta.get("table.rowCount"), integer(meta.get("rows"), 0));
        int columns = integer(meta.get("table.columnCount"), integer(meta.get("columns"), 0));
        ArrayList<List<String>> data = new ArrayList<>();
        if (rows > 0 && columns > 0) {
            ArrayList<String> header = new ArrayList<>();
            for (int c = 0; c < columns; c++) header.add(meta.getOrDefault("table.header." + c, "Columna " + (c + 1)));
            data.add(List.copyOf(header));
            for (int r = 1; r < rows; r++) {
                ArrayList<String> row = new ArrayList<>();
                for (int c = 0; c < columns; c++) row.add(meta.getOrDefault("table.cell." + r + "." + c, ""));
                data.add(List.copyOf(row));
            }
        }
        return new TableData(columns, List.copyOf(data));
    }

    private static void drawCenteredFit(Graphics2D g, String text, String family, int style, int preferred,
                                        int minimum, int x, int y, int width, int height, Color color,
                                        String blockId) throws IOException {
        for (int size = preferred; size >= minimum; size--) {
            Font font = new Font(family, style, size); FontMetrics fm = g.getFontMetrics(font);
            List<String> lines = wrap(text, fm, width);
            if (lines.size() * (fm.getHeight() + 5) <= height) {
                g.setFont(font); g.setColor(color);
                int baseline = y + fm.getAscent();
                for (String line : lines) {
                    g.drawString(line, x + (width - fm.stringWidth(line)) / 2, baseline);
                    baseline += fm.getHeight() + 5;
                }
                return;
            }
        }
        throw new IOException("El titulo " + blockId + " no cabe completo en la portada.");
    }

    private static List<String> wrap(String text, FontMetrics fm, int width) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").strip();
        if (normalized.isBlank()) return List.of("");
        ArrayList<String> lines = new ArrayList<>(); StringBuilder line = new StringBuilder();
        for (String word : normalized.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (line.isEmpty() || fm.stringWidth(candidate) <= width) {
                line.setLength(0); line.append(candidate);
            } else { lines.add(line.toString()); line.setLength(0); line.append(word); }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return List.copyOf(lines);
    }

    private static int integer(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception ignored) { return fallback; }
    }

    private static Color color(String value) { return Color.decode(value); }
    private static void write(Path target, BufferedImage image) throws IOException {
        Files.createDirectories(target.getParent());
        if (!ImageIO.write(image, "png", target.toFile())) throw new IOException("No se pudo escribir " + target);
    }
    private record TableData(int columns, List<List<String>> rows) { }
}
