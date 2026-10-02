package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Adapter-owned auxiliary interpolation path; it is deliberately not a selectable engine. */
final class RifeFrameInterpolator {
    static final String MODEL = "rife_v4.25_lite.safetensors";
    private final ComfyUiTransport transport;
    private final Path model;

    RifeFrameInterpolator(String baseUrl, Path model) {
        this.transport = new ComfyUiTransport(baseUrl);
        this.model = model;
    }

    Path smoke(Path outputDirectory, ExecutionContext context) throws IOException, InterruptedException {
        if (!Files.isRegularFile(model)) {
            throw new IOException("RESOURCE_MISSING: RIFE model=" + model);
        }
        String objectInfo = transport.objectInfo("FrameInterpolationModelLoader");
        if (!objectInfo.contains("FrameInterpolationModelLoader") || !objectInfo.contains(MODEL)) {
            throw new IOException("RESOURCE_MISSING: RIFE node or model is not published by ComfyUI");
        }
        Files.createDirectories(outputDirectory);
        Path first = outputDirectory.resolve("rife-input-a.png");
        Path second = outputDirectory.resolve("rife-input-b.png");
        writeInput(first, false);
        writeInput(second, true);
        Path target = interpolate(first, second, outputDirectory, "rife-middle-smoke", context);
        Files.deleteIfExists(first);
        Files.deleteIfExists(second);
        return target;
    }

    Path interpolate(Path first, Path second, Path outputDirectory, String filenamePrefix,
                     ExecutionContext context) throws IOException, InterruptedException {
        if (!Files.isRegularFile(model)) throw new IOException("RESOURCE_MISSING: RIFE model=" + model);
        BufferedImage reference = ImageIO.read(first.toFile());
        BufferedImage next = ImageIO.read(second.toFile());
        if (reference == null || next == null) throw new IOException("Las dos referencias RIFE deben ser imagenes validas.");
        if (reference.getWidth() != next.getWidth() || reference.getHeight() != next.getHeight()) {
            throw new IOException("Las referencias RIFE deben tener las mismas dimensiones.");
        }
        String objectInfo = transport.objectInfo("FrameInterpolationModelLoader");
        if (!objectInfo.contains("FrameInterpolationModelLoader") || !objectInfo.contains(MODEL)) {
            throw new IOException("RESOURCE_MISSING: RIFE node or model is not published by ComfyUI");
        }
        Files.createDirectories(outputDirectory);
        String firstName = transport.uploadImage(first, 1, context.policy().timeout());
        String secondName = transport.uploadImage(second, 2, context.policy().timeout());
        String safePrefix = filenamePrefix == null || filenamePrefix.isBlank() ? "middle-frame" : filenamePrefix;
        ComfyUiTransport.DownloadedArtifact artifact = transport.execute(
                workflow(firstName, secondName, safePrefix), context, "interpolacion RIFE");
        Path target = outputDirectory.resolve(safePrefix + ".png");
        Files.write(target, artifact.bytes());
        BufferedImage image = ImageIO.read(target.toFile());
        if (image == null || image.getWidth() != reference.getWidth() || image.getHeight() != reference.getHeight()) {
            throw new IOException("RIFE produjo un frame invalido o con dimensiones inesperadas.");
        }
        int darkest = 255;
        int lightest = 0;
        for (int y = 0; y < image.getHeight(); y += 4) {
            for (int x = 0; x < image.getWidth(); x += 4) {
                int rgb = image.getRGB(x, y);
                int luminance = (((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff) + (rgb & 0xff)) / 3;
                darkest = Math.min(darkest, luminance);
                lightest = Math.max(lightest, luminance);
            }
        }
        if (lightest - darkest < 20) {
            throw new IOException("RIFE produjo un frame casi uniforme; la continuidad visual no quedo demostrada.");
        }
        return target;
    }

    private static void writeInput(Path target, boolean right) throws IOException {
        BufferedImage image = new BufferedImage(512, 320, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(245, 240, 255));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(new Color(86, 54, 164));
            // Keep displacement realistic for a two-frame interpolation smoke so the middle frame
            // must retain the object instead of legitimately resolving a huge jump as background.
            graphics.fillOval(right ? 230 : 150, 90, 120, 120);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, "png", target.toFile());
    }

    private static String workflow(String first, String second, String prefix) {
        return "{"
                + "\"1\":{\"class_type\":\"LoadImage\",\"inputs\":{\"image\":\"" + esc(first) + "\"}},"
                + "\"2\":{\"class_type\":\"LoadImage\",\"inputs\":{\"image\":\"" + esc(second) + "\"}},"
                + "\"3\":{\"class_type\":\"ImageBatch\",\"inputs\":{\"image1\":[\"1\",0],\"image2\":[\"2\",0]}},"
                + "\"4\":{\"class_type\":\"FrameInterpolationModelLoader\",\"inputs\":{\"model_name\":\"" + MODEL + "\"}},"
                + "\"5\":{\"class_type\":\"FrameInterpolate\",\"inputs\":{\"interp_model\":[\"4\",0],\"images\":[\"3\",0],\"multiplier\":2}},"
                + "\"6\":{\"class_type\":\"ImageFromBatch\",\"inputs\":{\"image\":[\"5\",0],\"batch_index\":1,\"length\":1}},"
                + "\"7\":{\"class_type\":\"SaveImage\",\"inputs\":{\"images\":[\"6\",0],\"filename_prefix\":\"" + esc(prefix) + "\"}}"
                + "}";
    }

    private static String esc(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
