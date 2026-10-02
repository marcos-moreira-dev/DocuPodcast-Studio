package com.marcosmoreiradev.docupodcaststudio.localmedia;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Exact final sizing after AI enhancement; no generative model is involved here. */
final class ImageDeliveryNormalizer {
    private static final double LANCZOS_RADIUS = 3.0;

    private ImageDeliveryNormalizer() {
    }

    static void normalize(Path source, Path target, int targetWidth, int targetHeight, boolean containWithoutCrop)
            throws IOException {
        BufferedImage input = ImageIO.read(source.toFile());
        if (input == null) throw new IOException("El artefacto de superresolución no es una imagen compatible.");
        if (targetWidth < 1 || targetHeight < 1) throw new IOException("Dimensiones de entrega inválidas.");

        double containScale = Math.min(targetWidth / (double) input.getWidth(),
                targetHeight / (double) input.getHeight());
        double coverScale = Math.max(targetWidth / (double) input.getWidth(),
                targetHeight / (double) input.getHeight());
        double scale = containWithoutCrop ? containScale : coverScale;
        int scaledWidth = Math.max(1, (int) Math.round(input.getWidth() * scale));
        int scaledHeight = Math.max(1, (int) Math.round(input.getHeight() * scale));
        BufferedImage scaled = lanczos(input, scaledWidth, scaledHeight);

        BufferedImage canvas = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(scaled, (targetWidth - scaledWidth) / 2, (targetHeight - scaledHeight) / 2, null);
        } finally {
            graphics.dispose();
        }
        if (target.getParent() != null) Files.createDirectories(target.getParent());
        if (!ImageIO.write(canvas, "png", target.toFile())) {
            throw new IOException("No existe un escritor PNG disponible.");
        }
    }

    private static BufferedImage lanczos(BufferedImage source, int targetWidth, int targetHeight) {
        if (source.getWidth() == targetWidth && source.getHeight() == targetHeight) return source;
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int[] sourcePixels = source.getRGB(0, 0, sourceWidth, sourceHeight, null, 0, sourceWidth);
        int[] horizontal = new int[targetWidth * sourceHeight];
        double scaleX = targetWidth / (double) sourceWidth;
        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                horizontal[y * targetWidth + x] = sampleHorizontal(
                        sourcePixels, sourceWidth, y, x, scaleX);
            }
        }
        int[] output = new int[targetWidth * targetHeight];
        double scaleY = targetHeight / (double) sourceHeight;
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                output[y * targetWidth + x] = sampleVertical(
                        horizontal, targetWidth, sourceHeight, x, y, scaleY);
            }
        }
        BufferedImage result = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, targetWidth, targetHeight, output, 0, targetWidth);
        return result;
    }

    private static int sampleHorizontal(int[] pixels, int width, int y, int targetX, double scale) {
        double center = (targetX + 0.5) / scale - 0.5;
        double filterScale = Math.min(1.0, scale);
        double support = LANCZOS_RADIUS / filterScale;
        int start = Math.max(0, (int) Math.floor(center - support));
        int end = Math.min(width - 1, (int) Math.ceil(center + support));
        return weighted(pixels, y * width, 1, start, end, center, filterScale);
    }

    private static int sampleVertical(int[] pixels, int width, int height, int x, int targetY, double scale) {
        double center = (targetY + 0.5) / scale - 0.5;
        double filterScale = Math.min(1.0, scale);
        double support = LANCZOS_RADIUS / filterScale;
        int start = Math.max(0, (int) Math.floor(center - support));
        int end = Math.min(height - 1, (int) Math.ceil(center + support));
        return weighted(pixels, x, width, start, end, center, filterScale);
    }

    private static int weighted(int[] pixels, int offset, int stride, int start, int end,
                                double center, double filterScale) {
        double total = 0;
        double alpha = 0;
        double red = 0;
        double green = 0;
        double blue = 0;
        for (int sample = start; sample <= end; sample++) {
            double weight = lanczos((center - sample) * filterScale) * filterScale;
            if (weight == 0) continue;
            int argb = pixels[offset + sample * stride];
            total += weight;
            alpha += ((argb >>> 24) & 0xff) * weight;
            red += ((argb >>> 16) & 0xff) * weight;
            green += ((argb >>> 8) & 0xff) * weight;
            blue += (argb & 0xff) * weight;
        }
        if (Math.abs(total) < 1.0e-9) return pixels[offset + Math.max(start, Math.min(end, (int) Math.round(center))) * stride];
        return clamp(alpha / total) << 24
                | clamp(red / total) << 16
                | clamp(green / total) << 8
                | clamp(blue / total);
    }

    private static double lanczos(double value) {
        double x = Math.abs(value);
        if (x < 1.0e-9) return 1.0;
        if (x >= LANCZOS_RADIUS) return 0.0;
        double pix = Math.PI * x;
        return (Math.sin(pix) / pix) * (Math.sin(pix / LANCZOS_RADIUS) / (pix / LANCZOS_RADIUS));
    }

    private static int clamp(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value)));
    }
}
