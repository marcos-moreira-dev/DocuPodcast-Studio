package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextVideoOptions;
import com.marcosmoreiradev.docupodcaststudio.domain.video.DocumentTextEffect;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentTextPainterTest {
    @Test void solidOutlinePaintsConfiguredEffectColorAroundReadableText() {
        BufferedImage image = new BufferedImage(360, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setFont(new Font("SansSerif", Font.BOLD, 52));
            graphics.setColor(Color.BLACK);
            DocumentTextVideoOptions base = DocumentTextVideoOptions.defaults();
            DocumentTextVideoOptions options = new DocumentTextVideoOptions(base.resolution(), base.backgroundMode(),
                    base.backgroundColor(), base.backgroundImagePath(), "#000000", base.accentColor(), base.fontFamily(),
                    base.fontSize(), base.underlineNarratedText(), base.narratedUnderlineColor(),
                    base.narratedUnderlineThicknessPx(), base.backgroundImageOpacity(),
                    DocumentTextEffect.SOLID_OUTLINE, "#FF0000", 4);

            DocumentTextPainter.drawString(graphics, "Texto", 30, 78, options);
        } finally {
            graphics.dispose();
        }

        int redPixels = 0;
        int blackPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y) & 0xFFFFFF;
                if (rgb == 0xFF0000) redPixels++;
                if (rgb == 0x000000) blackPixels++;
            }
        }
        assertTrue(redPixels > 0);
        assertTrue(blackPixels > 0);
    }
}
