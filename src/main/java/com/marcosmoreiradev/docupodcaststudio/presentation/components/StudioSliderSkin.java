package com.marcosmoreiradev.docupodcaststudio.presentation.components;

import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.control.skin.SliderSkin;
import javafx.scene.layout.Region;

/**
 * Product slider skin that preserves JavaFX keyboard, focus and pointer behaviour.
 * The additional value fill is laid out from the native track and thumb bounds and
 * therefore never changes their geometry.
 */
public final class StudioSliderSkin extends SliderSkin {
    private final Region valueFill = new Region();

    public StudioSliderSkin(Slider slider) {
        super(slider);
        valueFill.getStyleClass().add("ui-slider-value-fill");
        valueFill.setManaged(false);
        valueFill.setMouseTransparent(true);
        getChildren().add(valueFill);
    }

    @Override
    protected void layoutChildren(double x, double y, double width, double height) {
        super.layoutChildren(x, y, width, height);
        Node track = getSkinnable().lookup(".track");
        Node thumb = getSkinnable().lookup(".thumb");
        if (track == null || thumb == null) {
            valueFill.resizeRelocate(0, 0, 0, 0);
            return;
        }
        Bounds trackBounds = track.getBoundsInParent();
        Bounds thumbBounds = thumb.getBoundsInParent();
        if (getSkinnable().getOrientation() == Orientation.VERTICAL) {
            double trackMinY = clamp(trackBounds.getMinY(), y, y + height);
            double trackMaxY = clamp(trackBounds.getMaxY(), trackMinY, y + height);
            double trackMinX = clamp(trackBounds.getMinX(), x, x + width);
            double trackMaxX = clamp(trackBounds.getMaxX(), trackMinX, x + width);
            double thumbCenter = thumbBounds.getMinY() + thumbBounds.getHeight() / 2.0;
            double fillY = clamp(thumbCenter, trackMinY, trackMaxY);
            valueFill.resizeRelocate(trackMinX, fillY,
                    Math.max(0, trackMaxX - trackMinX), Math.max(0, trackMaxY - fillY));
        } else {
            double trackMinX = clamp(trackBounds.getMinX(), x, x + width);
            double trackMaxX = clamp(trackBounds.getMaxX(), trackMinX, x + width);
            double trackMinY = clamp(trackBounds.getMinY(), y, y + height);
            double trackMaxY = clamp(trackBounds.getMaxY(), trackMinY, y + height);
            double thumbCenter = thumbBounds.getMinX() + thumbBounds.getWidth() / 2.0;
            double fillEnd = clamp(thumbCenter, trackMinX, trackMaxX);
            valueFill.resizeRelocate(trackMinX, trackMinY,
                    Math.max(0, fillEnd - trackMinX), Math.max(0, trackMaxY - trackMinY));
        }
        valueFill.toFront();
        thumb.toFront();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
