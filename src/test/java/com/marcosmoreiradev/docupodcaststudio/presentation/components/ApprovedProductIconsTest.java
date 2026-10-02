package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ApprovedProductIconsTest {
    @Test
    void packagesTheCompleteApprovedProductCatalog() throws Exception {
        var approved = Arrays.stream(AppIcon.values())
                .filter(icon -> icon.resourcePath().startsWith("/icons/product/"))
                .toList();

        assertEquals(25, approved.size());
        for (AppIcon icon : approved) {
            var resource = getClass().getResource(icon.resourcePath());
            assertNotNull(resource, icon.resourcePath());
            var image = ImageIO.read(resource);
            assertNotNull(image, icon.resourcePath());
            assertTrue(image.getWidth() >= 512, icon.resourcePath());
            assertEquals(image.getWidth(), image.getHeight(), icon.resourcePath());
            assertTrue(icon.isProductAsset(), icon.resourcePath());
            int maxX = image.getWidth() - 1;
            int maxY = image.getHeight() - 1;
            assertEquals(0, alpha(image.getRGB(0, 0)), icon.resourcePath());
            assertEquals(0, alpha(image.getRGB(maxX, 0)), icon.resourcePath());
            assertEquals(0, alpha(image.getRGB(0, maxY)), icon.resourcePath());
            assertEquals(0, alpha(image.getRGB(maxX, maxY)), icon.resourcePath());
        }
        assertNotNull(getClass().getResource("/icons/product/MANIFEST.txt"));
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xff;
    }
}
