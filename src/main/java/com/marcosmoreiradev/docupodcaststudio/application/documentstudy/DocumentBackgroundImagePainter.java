package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Shared background fitting policy used by previews' exported documentary frames. */
final class DocumentBackgroundImagePainter {
    private DocumentBackgroundImagePainter() { }

    static void paint(Graphics2D graphics, BufferedImage source, int width, int height,
                      double opacity, DocumentBackgroundImageFit fit) {
        if (graphics == null || source == null || width <= 0 || height <= 0) return;
        DocumentBackgroundImageFit effective = fit == null ? DocumentBackgroundImageFit.COVER : fit;
        var previousComposite = graphics.getComposite();
        Object previousInterpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                (float) Math.max(0.05, Math.min(1.0, opacity))));
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        if (effective == DocumentBackgroundImageFit.BLUR_AND_CONTAIN) {
            paintBlurredCover(graphics, source, width, height);
            paintScaled(graphics, source, width, height, false);
        } else {
            paintScaled(graphics, source, width, height, effective == DocumentBackgroundImageFit.COVER);
        }
        graphics.setComposite(previousComposite);
        if (previousInterpolation != null) graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, previousInterpolation);
    }

    private static void paintBlurredCover(Graphics2D graphics, BufferedImage source, int width, int height) {
        int blurWidth = Math.max(24, width / 24);
        int blurHeight = Math.max(14, height / 24);
        BufferedImage reduced = new BufferedImage(blurWidth, blurHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D small = reduced.createGraphics();
        try {
            small.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            paintScaled(small, source, blurWidth, blurHeight, true);
        } finally {
            small.dispose();
        }
        graphics.drawImage(reduced, 0, 0, width, height, null);
    }

    private static void paintScaled(Graphics2D graphics, BufferedImage source,
                                    int width, int height, boolean cover) {
        double scale = cover
                ? Math.max(width / (double) source.getWidth(), height / (double) source.getHeight())
                : Math.min(width / (double) source.getWidth(), height / (double) source.getHeight());
        int drawWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
        graphics.drawImage(source, (width - drawWidth) / 2, (height - drawHeight) / 2,
                drawWidth, drawHeight, null);
    }
}
