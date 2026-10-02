package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentBackgroundImageFit;
import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoBackgroundMode;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentMascotPosition;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentParagraphVisualAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyClosingSlide;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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
    /**
     * Source-only central policy for visual/structured semantic content. Speech
     * explains the object; it is deliberately not printed again over the ROI.
     * Global title, configured imagery and mascot remain transversal overlays.
     */
    public void composeSourceCapture(Path target, DocumentBlock block,
                                         Path sourceRoi,
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
            int availableWidth = width - margin * 2;
            int y = drawGlobalTitle(g, configuration.videoTitle(), options,
                    margin, margin, availableWidth);
            int imageY = y;
            int imageHeight = height - imageY - margin;
            BufferedImage selected = null;
            if (sourceRoi != null && Files.isRegularFile(sourceRoi)) {
                selected = ImageIO.read(sourceRoi.toFile());
            }
            if (selected == null) {
                selected = readAsset(project, projectDirectory,
                        visual.activeImageAssetId());
            }
            drawContainedImage(g, selected, margin, imageY, availableWidth, imageHeight, options);
            drawMascot(g, readAsset(project, projectDirectory, visual.mascotAssetId()),
                    visual, width, height, imageHeight, Math.max(70, availableWidth / 5));
        } finally {
            g.dispose();
        }
        write(target, image);
    }

    /** Compatibility alias retained for callers compiled against the earlier name. */
    public void composeSemanticComponent(Path target, DocumentBlock block,
                                         Path sourceRoi,
                                         DocumentParagraphVisualAssignment visual,
                                         DocumentStudyVideoConfiguration configuration,
                                         DocuPodcastProject project, Path projectDirectory,
                                         DocumentTextVideoOptions options) throws IOException {
        composeSourceCapture(target, block, sourceRoi, visual, configuration,
                project, projectDirectory, options);
    }
    public void composeParagraph(Path target, DocumentBlock block,
                                 DocumentParagraphVisualAssignment visual,
                                 DocumentStudyVideoConfiguration configuration,
                                 DocuPodcastProject project, Path projectDirectory,
                                 DocumentTextVideoOptions options) throws IOException {
        composeParagraph(target, block, visual, configuration, project, projectDirectory,
                options, null);
    }

    /** Composes a generated text frame whose complete body is the narration unit currently heard. */
    public void composeNarratedParagraph(Path target, DocumentBlock block,
                                         DocumentParagraphVisualAssignment visual,
                                         DocumentStudyVideoConfiguration configuration,
                                         DocuPodcastProject project, Path projectDirectory,
                                         DocumentTextVideoOptions options) throws IOException {
        composeParagraph(target, block, visual, configuration, project, projectDirectory,
                options, block.text());
    }

    /**
     * Keeps the complete Word paragraph on screen while emphasizing only the
     * acoustic unit currently narrated. The audio remains sentence-granular;
     * the visual context remains paragraph-granular.
     */
    public void composeNarratedParagraph(Path target, DocumentBlock block,
                                         String narratedFragment,
                                         DocumentParagraphVisualAssignment visual,
                                         DocumentStudyVideoConfiguration configuration,
                                         DocuPodcastProject project, Path projectDirectory,
                                         DocumentTextVideoOptions options) throws IOException {
        composeParagraph(target, block, visual, configuration, project, projectDirectory,
                options, narratedFragment);
    }

    public void composeIllustratedParagraph(Path target, DocumentBlock block, String narratedFragment,
            DocumentParagraphVisualAssignment visual, DocumentStudyVideoConfiguration configuration,
            DocuPodcastProject project, Path root, DocumentTextVideoOptions options, Path illustration) throws IOException {
        var appearance = configuration.aiIllustrationAppearance();
        if (appearance.background()) {
            composeNarratedParagraph(target, block, narratedFragment, visual, configuration, project, root,
                    options.withBackgroundImage(illustration.toAbsolutePath().toString(), appearance.opacity(),
                            DocumentBackgroundImageFit.valueOf(appearance.fit())));
            return;
        }
        BufferedImage image = canvas(options);
        Graphics2D g = graphics(image);
        try {
            paintBackground(g, image, options);
            int margin = Math.max(36, image.getWidth()/16);
            int available = image.getWidth()-2*margin;
            int y = drawGlobalTitle(g, configuration.videoTitle(), options, margin, margin, available);
            y = drawParagraphSubtitle(g, visual.subtitle(), options, margin, y, available, block.id());
            int height = image.getHeight()-margin-y;
            int gap = Math.max(16, margin/3);
            int pictureHeight = Math.min(600, height/2);
            String body = block.text();
            while (pictureHeight > Math.max(80,height/6) && !fitsText(g, body, options, available, height-pictureHeight-gap))
                pictureHeight -= Math.max(1,height/30);
            // Audio is already divided into narrated units: use that exact unit rather than inventing timing.
            if (!fitsText(g, body, options, available, height-pictureHeight-gap) && narratedFragment != null && !narratedFragment.isBlank())
                body = narratedFragment;
            int textHeight = height-pictureHeight-gap;
            drawJustifiedText(g, body, options, margin, y, available, textHeight, block.id(), narratedFragment);
            BufferedImage asset = ImageIO.read(illustration.toFile());
            if (asset != null) {
                double scale = Math.min(1, Math.min(available/(double)asset.getWidth(), pictureHeight/(double)asset.getHeight()));
                int w = Math.max(1,(int)(asset.getWidth()*scale)), h = Math.max(1,(int)(asset.getHeight()*scale));
                g.drawImage(asset, margin+(available-w)/2, y+textHeight+gap+(pictureHeight-h)/2, w,h,null);
            }
            // Existing mascot composition remains an explicit overlay, independent of the illustration.
            drawMascot(g, readAsset(project, root, visual.mascotAssetId()), visual,
                    image.getWidth(), image.getHeight(), height, Math.max(70,available/5));
        } finally { g.dispose(); }
        write(target,image);
    }

    private static boolean fitsText(Graphics2D g, String text, DocumentTextVideoOptions options, int width, int height) {
        int minimum = Math.max(18, options.resolution().height()/60);
        FontMetrics metrics = g.getFontMetrics(new Font(options.fontFamily(), Font.PLAIN, minimum));
        return wrapWithRanges(normalizeWhitespace(text),metrics,width).size()
                * (metrics.getHeight()+Math.max(3,minimum/5)) <= height;
    }

    private void composeParagraph(Path target, DocumentBlock block,
                                  DocumentParagraphVisualAssignment visual,
                                  DocumentStudyVideoConfiguration configuration,
                                  DocuPodcastProject project, Path projectDirectory,
                                  DocumentTextVideoOptions options,
                                  String narratedFragment) throws IOException {
        BufferedImage image = canvas(options);
        Graphics2D g = graphics(image);
        try {
            paintBackground(g, image, options);
            int width = image.getWidth(), height = image.getHeight();
            int margin = Math.max(36, width / 16);
            int y = margin;
            int availableWidth = width - margin * 2;
            y = drawGlobalTitle(g, configuration.videoTitle(), options, margin, y, availableWidth);
            BufferedImage mascot = readAsset(project, projectDirectory, visual.mascotAssetId());
            if (visual.illustrationOnly()) {
                int contentHeight = Math.max(1, height - margin - y);
                drawContainedAsset(g, project, projectDirectory, visual.activeImageAssetId(),
                        margin, y, availableWidth, contentHeight, options);
                drawMascot(g, mascot, visual, image.getWidth(), image.getHeight(), contentHeight,
                        Math.max(70, availableWidth / 5));
            } else {
                y = drawParagraphSubtitle(g, visual.subtitle(), options, margin, y, availableWidth, block.id());
                drawJustifiedText(g, block.text(), options, margin, y, availableWidth,
                        height - margin - y, block.id(), narratedFragment);
                drawMascot(g, mascot, visual, image.getWidth(), image.getHeight(),
                        height - y - margin, Math.max(70, availableWidth / 5));
            }
        } finally {
            g.dispose();
        }
        write(target, image);
    }

    public void composeCover(Path target, DocumentBlock block,
                             DocumentStudyVideoConfiguration configuration,
                             DocumentTextVideoOptions options) throws IOException {
        composeCover(target, block, configuration, options, false);
    }

    /** Composes a title/heading frame whose source block is the narration unit currently heard. */
    public void composeNarratedCover(Path target, DocumentBlock block,
                                     DocumentStudyVideoConfiguration configuration,
                                     DocumentTextVideoOptions options) throws IOException {
        composeCover(target, block, configuration, options, true);
    }

    private void composeCover(Path target, DocumentBlock block,
                              DocumentStudyVideoConfiguration configuration,
                              DocumentTextVideoOptions options,
                              boolean narratedText) throws IOException {
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
            drawCenteredFit(g, title, options.titleFontFamily(), Font.BOLD, options.fontSize() + 18,
                    Math.max(26, options.fontSize() / 2), margin, height / 3, width - margin * 2, height / 3,
                    color(options.textColor()), block.id(), narratedText && subtitle.isBlank(), options);
            if (!subtitle.isBlank() && !subtitle.equals(title)) {
                drawCenteredFit(g, subtitle, options.titleFontFamily(), Font.PLAIN, options.fontSize(),
                        20, margin, (int) (height * 0.70), width - margin * 2, height / 7,
                        color(options.textColor()), block.id(), narratedText, options);
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
                drawCenteredFit(g, slide.title(), options.titleFontFamily(), Font.BOLD,
                        options.fontSize() + 12, Math.max(22, options.fontSize() / 2),
                        margin, margin, width - margin * 2, titleHeight,
                        color(options.textColor()), slide.id(), false, options);
                imageY = margin + titleHeight + Math.max(18, height / 45);
                imageHeight = height - imageY - margin;
            }
            drawContainedAsset(g, project, projectDirectory, slide.imageAssetId(),
                    margin, imageY, width - margin * 2, imageHeight, options);
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
            DocumentBackgroundImagePainter.paint(g, bg, image.getWidth(), image.getHeight(),
                    options.backgroundImageOpacity(), options.backgroundImageFit());
        }
    }

    private static int drawGlobalTitle(Graphics2D g, String title, DocumentTextVideoOptions options,
                                       int x, int y, int width) throws IOException {
        if (title == null || title.isBlank()) return y;
        int preferred = Math.max(22, options.fontSize() - 14);
        int minimum = Math.max(18, options.resolution().height() / 60);
        for (int size = preferred; size >= minimum; size--) {
            Font font = new Font(options.titleFontFamily(), Font.BOLD, size);
            FontMetrics fm = g.getFontMetrics(font);
            List<String> lines = wrap(title, fm, width);
            if (lines.size() > 2) continue;
            g.setFont(font); g.setColor(color(options.textColor()));
            int baseline = y + fm.getAscent();
            for (String line : lines) {
                DocumentTextPainter.drawString(g, line, x, baseline, options);
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
                                          int x, int y, int width, int height, String blockId,
                                          String narratedFragment) throws IOException {
        String normalizedText = normalizeWhitespace(text);
        int[] narratedRange = narratedRange(normalizedText, narratedFragment);
        int minimum = Math.max(18, options.resolution().height() / 60);
        for (int size = options.fontSize(); size >= minimum; size--) {
            Font font = new Font(options.fontFamily(), Font.PLAIN, size);
            FontMetrics fm = g.getFontMetrics(font);
            List<TextLine> lines = wrapWithRanges(normalizedText, fm, width);
            int lineHeight = fm.getHeight() + Math.max(3, size / 5);
            if (lines.size() * lineHeight <= height) {
                g.setFont(font); g.setColor(color(options.textColor()));
                int baseline = y + fm.getAscent();
                for (int i = 0; i < lines.size(); i++) {
                    boolean last = i == lines.size() - 1;
                    TextLine positioned = lines.get(i);
                    String line = positioned.text();
                    drawJustifiedLine(g, line, x, baseline, width, last, fm, options);
                    int highlightedStart = Math.max(positioned.start(), narratedRange[0]);
                    int highlightedEnd = Math.min(positioned.end(), narratedRange[1]);
                    if (highlightedStart < highlightedEnd) {
                        int localStart = highlightedStart - positioned.start();
                        int localEnd = highlightedEnd - positioned.start();
                        int underlineX = x + justifiedAdvance(line, localStart, width, last, fm);
                        int underlineEnd = x + justifiedAdvance(line, localEnd, width, last, fm);
                        drawNarratedUnderline(g, options, underlineX, baseline,
                                Math.max(0, underlineEnd - underlineX), fm);
                    }
                    baseline += lineHeight;
                }
                return;
            }
        }
        throw new IOException("El parrafo " + blockId + " no cabe completo con un tamano de texto legible.");
    }

    private static int drawParagraphSubtitle(Graphics2D g, String subtitle, DocumentTextVideoOptions options,
                                              int x, int y, int width, String blockId) throws IOException {
        if (subtitle == null || subtitle.isBlank()) return y;
        int preferred = Math.max(22, options.fontSize() - 8);
        int minimum = Math.max(17, options.resolution().height() / 64);
        for (int size = preferred; size >= minimum; size--) {
            Font font = new Font(options.titleFontFamily(), Font.BOLD, size);
            FontMetrics fm = g.getFontMetrics(font);
            List<String> lines = wrap(subtitle, fm, width);
            if (lines.size() > 2) continue;
            g.setFont(font);
            g.setColor(color(options.textColor()));
            int baseline = y + fm.getAscent();
            for (String line : lines) {
                DocumentTextPainter.drawString(g, line, x, baseline, options);
                baseline += fm.getHeight();
            }
            return y + lines.size() * fm.getHeight() + Math.max(10, fm.getHeight() / 3);
        }
        throw new IOException("El subtitulo del parrafo " + blockId + " no cabe completo en la diapositiva.");
    }

    private static void drawJustifiedLine(Graphics2D g, String line, int x, int baseline, int width,
                                          boolean last, FontMetrics fm, DocumentTextVideoOptions options) {
        String[] words = line.split(" ");
        if (last || words.length < 2) {
            DocumentTextPainter.drawString(g, line, x, baseline, options);
            return;
        }
        int wordsWidth = 0;
        for (String word : words) wordsWidth += fm.stringWidth(word);
        double gap = (width - wordsWidth) / (double) (words.length - 1);
        double cursor = x;
        for (String word : words) {
            DocumentTextPainter.drawString(g, word, (int) Math.round(cursor), baseline, options);
            cursor += fm.stringWidth(word) + gap;
        }
    }

    private static int justifiedLineWidth(String line, int width, boolean last, FontMetrics fm) {
        String[] words = line.split(" ");
        return !last && words.length >= 2 ? width : fm.stringWidth(line);
    }

    private static int justifiedAdvance(String line, int offset, int width,
                                        boolean last, FontMetrics fm) {
        int safeOffset = Math.max(0, Math.min(offset, line.length()));
        String[] words = line.split(" ");
        if (last || words.length < 2) return fm.stringWidth(line.substring(0, safeOffset));
        int wordsWidth = 0;
        for (String word : words) wordsWidth += fm.stringWidth(word);
        double gap = (width - wordsWidth) / (double) (words.length - 1);
        double cursor = 0.0;
        int character = 0;
        for (int index = 0; index < words.length; index++) {
            String word = words[index];
            int wordEnd = character + word.length();
            if (safeOffset <= wordEnd) {
                return (int) Math.round(cursor
                        + fm.stringWidth(word.substring(0, safeOffset - character)));
            }
            cursor += fm.stringWidth(word);
            character = wordEnd;
            if (index + 1 < words.length) {
                if (safeOffset == character + 1) return (int) Math.round(cursor + gap);
                cursor += gap;
                character++;
            }
        }
        return (int) Math.round(cursor);
    }

    private static List<TextLine> wrapWithRanges(String normalizedText,
                                                 FontMetrics metrics, int maxWidth) {
        List<String> wrapped = wrap(normalizedText, metrics, maxWidth);
        ArrayList<TextLine> result = new ArrayList<>();
        int cursor = 0;
        for (String line : wrapped) {
            int start = normalizedText.indexOf(line, cursor);
            if (start < 0) start = cursor;
            int end = Math.min(normalizedText.length(), start + line.length());
            result.add(new TextLine(line, start, end));
            cursor = end;
        }
        return List.copyOf(result);
    }

    private static int[] narratedRange(String normalizedText, String narratedFragment) {
        String fragment = normalizeWhitespace(narratedFragment);
        if (normalizedText.isBlank() || fragment.isBlank()) return new int[]{0, 0};
        int start = normalizedText.indexOf(fragment);
        return start < 0 ? new int[]{0, 0}
                : new int[]{start, Math.min(normalizedText.length(), start + fragment.length())};
    }

    private static String normalizeWhitespace(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").strip();
    }

    private record TextLine(String text, int start, int end) { }

    private static void drawNarratedUnderline(Graphics2D g, DocumentTextVideoOptions options,
                                              int x, int baseline, int width, FontMetrics metrics) {
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

    private static void drawContainedAsset(Graphics2D g, DocuPodcastProject project, Path root, String assetId,
                                           int x, int y, int width, int height, DocumentTextVideoOptions options) throws IOException {
        BufferedImage asset = readAsset(project, root, assetId);
        drawContainedImage(g, asset, x, y, width, height, options);
    }

    private static void drawContainedImage(Graphics2D g, BufferedImage asset,
                                           int x, int y, int width, int height, DocumentTextVideoOptions options) {
        if (asset == null || width <= 0 || height <= 0) return;
        Graphics2D local = (Graphics2D)g.create(x, y, width, height);
        try { DocumentBackgroundImagePainter.paint(local, asset, width, height, 1.0, options.backgroundImageFit()); }
        finally { local.dispose(); }
    }

    private static void drawMascot(Graphics2D g, BufferedImage mascot,
                                   DocumentParagraphVisualAssignment visual,
                                   int canvasWidth, int canvasHeight,
                                   int regionHeight, int reservedWidth) {
        if (mascot == null) return;
        Rectangle bounds = mascotBounds(canvasWidth, canvasHeight, regionHeight, reservedWidth,
                mascot.getWidth(), mascot.getHeight(), visual.mascotPosition(), visual.mascotSizePercent());
        g.drawImage(mascot, bounds.x, bounds.y, bounds.width, bounds.height, null);
    }

    static Rectangle mascotBounds(int canvasWidth, int canvasHeight, int regionHeight, int reservedWidth,
                                   int sourceWidth, int sourceHeight, DocumentMascotPosition position) {
        return mascotBounds(canvasWidth, canvasHeight, regionHeight, reservedWidth,
                sourceWidth, sourceHeight, position, 15);
    }

    static Rectangle mascotBounds(int canvasWidth, int canvasHeight, int regionHeight, int reservedWidth,
                                   int sourceWidth, int sourceHeight, DocumentMascotPosition position,
                                   int sizePercent) {
        double factor = Math.max(5, Math.min(40, sizePercent)) / 15.0;
        int maxW = Math.max(36, (int) Math.round(reservedWidth * 0.84 * factor));
        int maxH = Math.max(36, (int) Math.round(regionHeight * 0.78 * factor));
        maxW = Math.min(maxW, Math.max(36, (int) Math.round(canvasWidth * 0.42)));
        maxH = Math.min(maxH, Math.max(36, (int) Math.round(canvasHeight * 0.64)));
        double scale = Math.min(maxW / (double) sourceWidth, maxH / (double) sourceHeight);
        int width = Math.max(1, (int) Math.round(sourceWidth * scale));
        int height = Math.max(1, (int) Math.round(sourceHeight * scale));
        int x = position == DocumentMascotPosition.BOTTOM_LEFT ? 0 : canvasWidth - width;
        return new Rectangle(x, canvasHeight - height, width, height);
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
            Font headerFont = new Font(options.titleFontFamily(), Font.BOLD, size);
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
                        DocumentTextPainter.drawString(g, line, cx + 10, baseline, options);
                        baseline += cellMetrics.getHeight() + 2;
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
                                        String blockId, boolean narratedText,
                                        DocumentTextVideoOptions options) throws IOException {
        for (int size = preferred; size >= minimum; size--) {
            Font font = new Font(family, style, size); FontMetrics fm = g.getFontMetrics(font);
            List<String> lines = wrap(text, fm, width);
            if (lines.size() * (fm.getHeight() + 5) <= height) {
                g.setFont(font); g.setColor(color);
                int baseline = y + fm.getAscent();
                for (String line : lines) {
                    int lineWidth = fm.stringWidth(line);
                    int lineX = x + (width - lineWidth) / 2;
                    DocumentTextPainter.drawString(g, line, lineX, baseline, options);
                    if (narratedText) drawNarratedUnderline(g, options, lineX, baseline, lineWidth, fm);
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
