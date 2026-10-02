package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;

/** Shared text-effect renderer for documentary frames. */
final class DocumentTextPainter {
    private DocumentTextPainter() { }

    static void drawString(Graphics2D graphics, String text, int x, int baseline,
                           DocumentTextVideoOptions options) {
        Color foreground = graphics.getColor();
        Color effect = Color.decode(options.textEffectColor());
        int thickness = options.textEffectThicknessPx();
        switch (options.textEffect()) {
            case NONE -> graphics.drawString(text, x, baseline);
            case SHADOW -> {
                graphics.setColor(effect);
                graphics.drawString(text, x + thickness, baseline + thickness);
                graphics.setColor(foreground);
                graphics.drawString(text, x, baseline);
            }
            case SOLID_OUTLINE -> {
                Shape glyphs = graphics.getFont().createGlyphVector(
                        graphics.getFontRenderContext(), text).getOutline(x, baseline);
                var previousStroke = graphics.getStroke();
                graphics.setStroke(new BasicStroke(thickness * 2.0f,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.setColor(effect);
                graphics.draw(glyphs);
                graphics.setStroke(previousStroke);
                graphics.setColor(foreground);
                graphics.fill(glyphs);
            }
        }
        graphics.setColor(foreground);
    }
}
