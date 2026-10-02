package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReference;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReferenceRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreReferenceCompositionBootstrapperTest {
    @TempDir Path temporary;

    @Test
    void composesCutOutContextAssetsWithoutTheWhiteReferenceCanvas() throws Exception {
        Path environment = image("environment.png", Color.BLUE, null);
        Path actor = image("actor.png", Color.WHITE, Color.RED);
        Path output = temporary.resolve("bootstrap.png");
        var references = List.of(
                new MediaReference("stage", environment, MediaReferenceRole.COMPOSITION_GUIDE,
                        1.0, Map.of("activeVariant", "environment-bootstrap")),
                new MediaReference("actor", actor, MediaReferenceRole.REGIONAL_IDENTITY,
                        1.0, Map.of(
                                "subjectId", "actor",
                                "regionX", "0.2",
                                "regionY", "0.1",
                                "regionWidth", "0.4",
                                "regionHeight", "0.8")));

        new TheatreReferenceCompositionBootstrapper().compose(references, 200, 100, output);

        BufferedImage result = ImageIO.read(output.toFile());
        assertEquals(200, result.getWidth());
        assertEquals(100, result.getHeight());
        assertEquals(Color.BLUE.getRGB() & 0xffffff, result.getRGB(5, 50) & 0xffffff);
        boolean containsRed = false;
        boolean containsWhite = false;
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                int rgb = result.getRGB(x, y) & 0xffffff;
                containsRed |= rgb == (Color.RED.getRGB() & 0xffffff);
                containsWhite |= rgb == (Color.WHITE.getRGB() & 0xffffff);
            }
        }
        assertTrue(containsRed);
        assertTrue(!containsWhite);

        BufferedImage generated = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D generatedGraphics = generated.createGraphics();
        generatedGraphics.setColor(Color.GREEN);
        generatedGraphics.fillRect(0, 0, 200, 100);
        generatedGraphics.dispose();
        ImageIO.write(generated, "png", output.toFile());

        new TheatreReferenceCompositionBootstrapper().restoreIdentityFaces(references, output);

        BufferedImage restored = ImageIO.read(output.toFile());
        boolean restoredCanonicalPixels = false;
        for (int y = 0; y < restored.getHeight(); y++) {
            for (int x = 0; x < restored.getWidth(); x++) {
                restoredCanonicalPixels |= (restored.getRGB(x, y) & 0x00ff0000) != 0;
            }
        }
        assertTrue(restoredCanonicalPixels);
    }

    private Path image(String name, Color background, Color foreground) throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(background);
        graphics.fillRect(0, 0, 100, 100);
        if (foreground != null) {
            graphics.setColor(foreground);
            graphics.fillOval(35, 10, 30, 80);
        }
        graphics.dispose();
        Path path = temporary.resolve(name);
        ImageIO.write(image, "png", path.toFile());
        return path;
    }
}
