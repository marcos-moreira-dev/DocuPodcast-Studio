package com.marcosmoreiradev.docupodcaststudio.localmedia;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageDeliveryNormalizerTest {
    @TempDir Path temporary;

    @Test
    void producesExactContainedPngWithoutModifyingSource() throws Exception {
        Path source = temporary.resolve("origen-á.png");
        BufferedImage input = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) input.setRGB(x, y, Color.MAGENTA.getRGB());
        }
        ImageIO.write(input, "png", source.toFile());
        byte[] original = Files.readAllBytes(source);
        Path target = temporary.resolve("salida.png");

        ImageDeliveryNormalizer.normalize(source, target, 200, 120, true);

        BufferedImage result = ImageIO.read(target.toFile());
        assertEquals(200, result.getWidth());
        assertEquals(120, result.getHeight());
        assertArrayEquals(original, Files.readAllBytes(source));
        assertEquals(0, (result.getRGB(0, 0) >>> 24) & 0xff);
        assertTrue(((result.getRGB(100, 60) >>> 24) & 0xff) > 0);
    }
}
