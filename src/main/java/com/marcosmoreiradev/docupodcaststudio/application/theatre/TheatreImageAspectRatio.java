package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.video.VisualResolutionProfile;

import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualAspectRatio;

import java.util.Locale;

/** Selectable output aspect ratio for theatre image generation. */
public enum TheatreImageAspectRatio {
    WIDE_16_9("16:9", 16, 9, "16:9 widescreen composition"),
    VERTICAL_9_16("9:16", 9, 16, "9:16 vertical composition"),
    SQUARE_1_1("1:1", 1, 1, "1:1 square composition"),
    STANDARD_4_3("4:3", 4, 3, "4:3 standard composition"),
    PORTRAIT_3_4("3:4", 3, 4, "3:4 portrait composition"),
    CINEMA_21_9("21:9", 21, 9, "21:9 cinematic wide composition");

    private final String label;
    private final int widthRatio;
    private final int heightRatio;
    private final String promptText;

    TheatreImageAspectRatio(String label, int widthRatio, int heightRatio, String promptText) {
        this.label = label;
        this.widthRatio = widthRatio;
        this.heightRatio = heightRatio;
        this.promptText = promptText;
    }

    public String label() {
        return label;
    }

    public String promptText() {
        return promptText;
    }

    public int widthRatio() {
        return widthRatio;
    }

    public int heightRatio() {
        return heightRatio;
    }

    public int widthFor(ImageEnhancementOutputProfile profile) {
        return visualAspectRatio().widthFor(profile);
    }

    public int heightFor(ImageEnhancementOutputProfile profile) {
        return visualAspectRatio().heightFor(profile);
    }

    public com.marcosmoreiradev.docupodcaststudio.domain.video.VisualResolutionProfile.Dimensions
    deliveryDimensions(ImageEnhancementOutputProfile profile) {
        return visualAspectRatio().deliveryDimensions(profile);
    }

    public String workflowId() {
        return label.toLowerCase(Locale.ROOT).replace(':', 'x');
    }

    public VisualAspectRatio visualAspectRatio() {
        return VisualAspectRatio.fromLabel(label);
    }

    public static TheatreImageAspectRatio fromLabel(String value) {
        String normalized = value == null ? "" : value.strip();
        for (TheatreImageAspectRatio ratio : values()) {
            if (ratio.label.equals(normalized) || ratio.name().equalsIgnoreCase(normalized)) {
                return ratio;
            }
        }
        return WIDE_16_9;
    }

}
