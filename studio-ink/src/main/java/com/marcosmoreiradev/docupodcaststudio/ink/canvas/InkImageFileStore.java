package com.marcosmoreiradev.docupodcaststudio.ink.canvas;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelFormat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/** Shared PNG codec and file boundary for reusable ink compositions. */
public final class InkImageFileStore {
    private InkImageFileStore() { }

    public static Path writePng(Image image, Path target) throws IOException {
        if (image == null) throw new IOException("No hay imagen para exportar.");
        return writePng(toBuffered(image), target);
    }

    public static Path writePng(BufferedImage image, Path target) throws IOException {
        if (image == null) throw new IOException("No hay imagen para exportar.");
        if (target == null) throw new IOException("No hay destino para exportar la imagen.");
        Path normalized = target.toAbsolutePath().normalize();
        if (normalized.getParent() != null) Files.createDirectories(normalized.getParent());
        if (!ImageIO.write(image, "png", normalized.toFile())) throw new IOException("No se pudo codificar la imagen como PNG.");
        return normalized;
    }

    /** Creates an owned temporary PNG destination for an ink export. */
    public static Path createTemporaryPng(String prefix) throws IOException {
        String safePrefix = prefix == null || prefix.isBlank() ? "studio-ink-" : prefix;
        return Files.createTempFile(safePrefix, ".png").toAbsolutePath().normalize();
    }

    /** Best-effort cleanup for a temporary destination created by this boundary. */
    public static void deleteTemporaryPng(Path target) {
        if (target == null) return;
        try {
            Files.deleteIfExists(target.toAbsolutePath().normalize());
        } catch (IOException ignored) {
            // Cancellation cleanup must never mask the user action.
        }
    }

    public static Path normalizedTempPng(Path source, String prefix) throws IOException {
        if (source == null || !Files.isRegularFile(source)) throw new IOException("La imagen no existe.");
        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null) throw new IOException("El archivo no contiene una imagen compatible.");
        Path temp = Files.createTempFile(prefix == null || prefix.isBlank() ? "studio-ink-" : prefix, ".png");
        return writePng(image, temp);
    }

    public static String encodePngBase64(Image image) throws IOException {
        if (image == null) return "";
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(toBuffered(image), "png", output)) throw new IOException("No se pudo codificar PNG.");
            return Base64.getEncoder().encodeToString(output.toByteArray());
        }
    }

    public static Image decodePngBase64(String data) {
        if (data == null || data.isBlank()) return null;
        try { return new Image(new ByteArrayInputStream(Base64.getDecoder().decode(data))); }
        catch (RuntimeException failure) { return null; }
    }

    private static BufferedImage toBuffered(Image image) throws IOException {
        int width = Math.max(1, (int) Math.ceil(image.getWidth()));
        int height = Math.max(1, (int) Math.ceil(image.getHeight()));
        PixelReader reader = image.getPixelReader();
        if (reader == null) throw new IOException("La imagen no expone píxeles para exportar.");
        int[] pixels = new int[Math.multiplyExact(width, height)];
        reader.getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        buffered.setRGB(0, 0, width, height, pixels, 0, width);
        return buffered;
    }
}
