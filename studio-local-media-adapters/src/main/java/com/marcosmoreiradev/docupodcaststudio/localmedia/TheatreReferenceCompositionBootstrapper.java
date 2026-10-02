package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReference;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReferenceRole;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a neutral composition guide from canonical context assets when no
 * authored storyboard is active. It does not invent pixels: the subsequent
 * img2img pass remains responsible for lighting, integration and rendering.
 */
final class TheatreReferenceCompositionBootstrapper {
    Path compose(List<MediaReference> references,
                 int width,
                 int height,
                 Path output) throws IOException {
        MediaReference environment = references.stream()
                .filter(reference -> MediaReferenceRole.COMPOSITION_GUIDE.equals(reference.role())
                        || MediaReferenceRole.ENVIRONMENT.equals(reference.role()))
                .findFirst()
                .orElseThrow(() -> new IOException(
                        "No existe una referencia de escenario para construir la composición."));
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        configure(graphics);
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, width, height);
        drawCover(graphics, read(environment.file()), width, height);

        references.stream()
                .filter(reference -> MediaReferenceRole.OBJECT.equals(reference.role()))
                .forEach(reference -> drawUnchecked(graphics, reference, width, height, false));

        LinkedHashMap<String, MediaReference> identities = new LinkedHashMap<>();
        references.stream()
                .filter(reference -> MediaReferenceRole.REGIONAL_IDENTITY.equals(reference.role()))
                .forEach(reference -> identities.putIfAbsent(
                        reference.metadata().getOrDefault("subjectId", reference.id()), reference));
        identities.values().forEach(reference ->
                drawUnchecked(graphics, reference, width, height, true));
        graphics.dispose();

        Files.createDirectories(output.toAbsolutePath().normalize().getParent());
        if (!ImageIO.write(canvas, "png", output.toFile())) {
            throw new IOException("No se pudo codificar la composición contextual en PNG.");
        }
        return output;
    }

    void restoreIdentityFaces(List<MediaReference> references, Path generated) throws IOException {
        BufferedImage canvas = read(generated);
        Graphics2D graphics = canvas.createGraphics();
        configure(graphics);
        LinkedHashMap<String, MediaReference> identities = new LinkedHashMap<>();
        references.stream()
                .filter(reference -> MediaReferenceRole.REGIONAL_IDENTITY.equals(reference.role()))
                .forEach(reference -> identities.putIfAbsent(
                        reference.metadata().getOrDefault("subjectId", reference.id()), reference));
        for (MediaReference reference : identities.values()) {
            BufferedImage foreground = removeConnectedLightBackground(read(reference.file()));
            Bounds bounds = contentBounds(foreground);
            if (bounds == null) continue;
            Region region = Region.from(
                    reference.metadata(), canvas.getWidth(), canvas.getHeight());
            int availableWidth = Math.max(8, (int) Math.round(region.width * 0.72));
            int availableHeight = Math.max(8, (int) Math.round(region.height * 0.94));
            double scale = Math.min(
                    availableWidth / (double) bounds.width,
                    availableHeight / (double) bounds.height);
            int drawWidth = Math.max(1, (int) Math.round(bounds.width * scale));
            int drawHeight = Math.max(1, (int) Math.round(bounds.height * scale));
            int drawX = region.x + (region.width - drawWidth) / 2;
            int drawY = region.y + region.height - drawHeight;

            int sourceX = bounds.x + (int) Math.round(bounds.width * 0.16);
            int sourceY = bounds.y;
            int sourceWidth = Math.max(1, (int) Math.round(bounds.width * 0.68));
            int sourceHeight = Math.max(1, (int) Math.round(bounds.height * 0.34));
            int targetX = drawX + (int) Math.round(drawWidth * 0.16);
            int targetY = drawY;
            int targetWidth = Math.max(1, (int) Math.round(drawWidth * 0.68));
            int targetHeight = Math.max(1, (int) Math.round(drawHeight * 0.34));
            BufferedImage face = new BufferedImage(
                    targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D faceGraphics = face.createGraphics();
            configure(faceGraphics);
            faceGraphics.drawImage(
                    foreground,
                    0,
                    0,
                    targetWidth,
                    targetHeight,
                    sourceX,
                    sourceY,
                    sourceX + sourceWidth,
                    sourceY + sourceHeight,
                    null);
            faceGraphics.dispose();
            feather(face);
            graphics.drawImage(face, targetX, targetY, null);
        }
        graphics.dispose();
        if (!ImageIO.write(canvas, "png", generated.toFile())) {
            throw new IOException("No se pudo publicar la restauración facial por ROI.");
        }
    }

    private static void drawUnchecked(Graphics2D graphics,
                                      MediaReference reference,
                                      int width,
                                      int height,
                                      boolean character) {
        try {
            BufferedImage foreground = character
                    ? removeConnectedLightBackground(read(reference.file()))
                    : removeAllNearWhiteBackground(read(reference.file()));
            Bounds bounds = contentBounds(foreground);
            if (bounds == null) return;
            Region region = Region.from(reference.metadata(), width, height);
            int availableWidth = character
                    ? Math.max(8, (int) Math.round(region.width * 0.72))
                    : region.width;
            int availableHeight = character
                    ? Math.max(8, (int) Math.round(region.height * 0.94))
                    : region.height;
            double scale = Math.min(
                    availableWidth / (double) bounds.width,
                    availableHeight / (double) bounds.height);
            int drawWidth = Math.max(1, (int) Math.round(bounds.width * scale));
            int drawHeight = Math.max(1, (int) Math.round(bounds.height * scale));
            int drawX = region.x + (region.width - drawWidth) / 2;
            int drawY = character
                    ? region.y + region.height - drawHeight
                    : region.y + (region.height - drawHeight) / 2;
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.drawImage(
                    foreground,
                    drawX,
                    drawY,
                    drawX + drawWidth,
                    drawY + drawHeight,
                    bounds.x,
                    bounds.y,
                    bounds.x + bounds.width,
                    bounds.y + bounds.height,
                    null);
        } catch (IOException ignored) {
            // The readiness layer reports unusable references. A single optional
            // scenic asset must not destroy a composition built from valid assets.
        }
    }

    private static BufferedImage removeConnectedLightBackground(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D copy = result.createGraphics();
        copy.drawImage(source, 0, 0, null);
        copy.dispose();

        boolean[] background = new boolean[width * height];
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        for (int x = 0; x < width; x++) {
            enqueueIfLight(result, x, 0, background, pending);
            enqueueIfLight(result, x, height - 1, background, pending);
        }
        for (int y = 0; y < height; y++) {
            enqueueIfLight(result, 0, y, background, pending);
            enqueueIfLight(result, width - 1, y, background, pending);
        }
        while (!pending.isEmpty()) {
            int index = pending.removeFirst();
            int x = index % width;
            int y = index / width;
            enqueueIfLight(result, x - 1, y, background, pending);
            enqueueIfLight(result, x + 1, y, background, pending);
            enqueueIfLight(result, x, y - 1, background, pending);
            enqueueIfLight(result, x, y + 1, background, pending);
        }
        for (int index = 0; index < background.length; index++) {
            if (!background[index]) continue;
            result.setRGB(index % width, index / width, 0);
        }
        return result;
    }

    private static BufferedImage removeAllNearWhiteBackground(BufferedImage source) {
        BufferedImage result = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int alpha = argb >>> 24;
                int red = argb >>> 16 & 0xff;
                int green = argb >>> 8 & 0xff;
                int blue = argb & 0xff;
                if (alpha < 16 || red >= 246 && green >= 246 && blue >= 246) {
                    result.setRGB(x, y, 0);
                } else {
                    result.setRGB(x, y, argb | 0xff000000);
                }
            }
        }
        return result;
    }

    private static void feather(BufferedImage image) {
        double featherX = Math.max(2.0, image.getWidth() * 0.10);
        double featherY = Math.max(2.0, image.getHeight() * 0.10);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = argb >>> 24;
                if (alpha == 0) continue;
                double horizontal = Math.min(x + 1.0, image.getWidth() - x) / featherX;
                double vertical = Math.min(y + 1.0, image.getHeight() - y) / featherY;
                double factor = Math.min(1.0, Math.min(horizontal, vertical));
                int softened = Math.max(0, Math.min(255, (int) Math.round(alpha * factor)));
                image.setRGB(x, y, softened << 24 | argb & 0x00ffffff);
            }
        }
    }

    private static void enqueueIfLight(BufferedImage image,
                                       int x,
                                       int y,
                                       boolean[] background,
                                       ArrayDeque<Integer> pending) {
        if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) return;
        int index = y * image.getWidth() + x;
        if (background[index]) return;
        int argb = image.getRGB(x, y);
        int alpha = argb >>> 24;
        int red = argb >>> 16 & 0xff;
        int green = argb >>> 8 & 0xff;
        int blue = argb & 0xff;
        if (alpha < 16 || red >= 226 && green >= 226 && blue >= 226) {
            background[index] = true;
            pending.add(index);
        }
    }

    private static Bounds contentBounds(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) == 0) continue;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        return maxX < minX ? null : new Bounds(
                minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static BufferedImage read(Path file) throws IOException {
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null) throw new IOException("Imagen no decodificable: " + file);
        return image;
    }

    private static void drawCover(Graphics2D graphics,
                                  BufferedImage image,
                                  int width,
                                  int height) {
        double scale = Math.max(width / (double) image.getWidth(),
                height / (double) image.getHeight());
        int drawWidth = (int) Math.round(image.getWidth() * scale);
        int drawHeight = (int) Math.round(image.getHeight() * scale);
        graphics.drawImage(image, (width - drawWidth) / 2, (height - drawHeight) / 2,
                drawWidth, drawHeight, null);
    }

    private static void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private record Bounds(int x, int y, int width, int height) { }

    private record Region(int x, int y, int width, int height) {
        static Region from(Map<String, String> metadata, int targetWidth, int targetHeight) {
            double x = value(metadata, "regionX", 0.25);
            double y = value(metadata, "regionY", 0.18);
            double width = value(metadata, "regionWidth", 0.50);
            double height = value(metadata, "regionHeight", 0.64);
            int px = clamp((int) Math.round(x * targetWidth), 0, targetWidth - 1);
            int py = clamp((int) Math.round(y * targetHeight), 0, targetHeight - 1);
            int pw = clamp((int) Math.round(width * targetWidth), 8, targetWidth - px);
            int ph = clamp((int) Math.round(height * targetHeight), 8, targetHeight - py);
            return new Region(px, py, pw, ph);
        }

        private static double value(Map<String, String> metadata, String key, double fallback) {
            try {
                return Double.parseDouble(metadata.getOrDefault(key, Double.toString(fallback)));
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }

        private static int clamp(int value, int minimum, int maximum) {
            return Math.max(minimum, Math.min(Math.max(minimum, maximum), value));
        }
    }
}
