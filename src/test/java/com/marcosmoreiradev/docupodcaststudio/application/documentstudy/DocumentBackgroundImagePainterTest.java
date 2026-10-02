package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

final class DocumentBackgroundImagePainterTest {
    @Test void containLeavesTheBaseColorVisibleAsBars() {
        BufferedImage frame = frame();
        paint(frame, DocumentBackgroundImageFit.CONTAIN);
        assertEquals(Color.BLUE.getRGB(), frame.getRGB(50, 5));
        assertEquals(Color.RED.getRGB(), frame.getRGB(50, 50));
    }

    @Test void coverFillsTheFrameByCroppingTheSource() {
        BufferedImage frame = frame();
        paint(frame, DocumentBackgroundImageFit.COVER);
        assertEquals(Color.RED.getRGB(), frame.getRGB(50, 5));
        assertEquals(Color.RED.getRGB(), frame.getRGB(50, 95));
    }

    @Test void blurredContainFillsBarsAndKeepsTheSharpImageCentered() {
        BufferedImage frame = frame();
        paint(frame, DocumentBackgroundImageFit.BLUR_AND_CONTAIN);
        assertNotEquals(Color.BLUE.getRGB(), frame.getRGB(50, 5));
        assertEquals(Color.RED.getRGB(), frame.getRGB(50, 50));
    }

    private static BufferedImage frame() {
        BufferedImage result = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, 100, 100);
        graphics.dispose();
        return result;
    }

    private static void paint(BufferedImage frame, DocumentBackgroundImageFit fit) {
        BufferedImage source = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D sourceGraphics = source.createGraphics();
        sourceGraphics.setColor(Color.RED);
        sourceGraphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        sourceGraphics.dispose();
        Graphics2D graphics = frame.createGraphics();
        DocumentBackgroundImagePainter.paint(graphics, source, 100, 100, 1.0, fit);
        graphics.dispose();
    }
}
