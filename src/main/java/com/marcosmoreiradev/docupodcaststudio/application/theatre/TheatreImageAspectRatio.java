package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;

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

    public int widthFor(ImageEnhancementOutputProfile profile) {
        ImageEnhancementOutputProfile current = profile == null ? ImageEnhancementOutputProfile.FHD_1080 : profile;
        return heightRatio >= widthRatio
                ? multipleOfEight(current.height())
                : multipleOfEight((int) Math.round(current.height() * (widthRatio / (double) heightRatio)));
    }

    public int heightFor(ImageEnhancementOutputProfile profile) {
        ImageEnhancementOutputProfile current = profile == null ? ImageEnhancementOutputProfile.FHD_1080 : profile;
        return widthRatio >= heightRatio
                ? multipleOfEight(current.height())
                : multipleOfEight((int) Math.round(current.height() * (heightRatio / (double) widthRatio)));
    }

    public String workflowId() {
        return label.toLowerCase(Locale.ROOT).replace(':', 'x');
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

    private static int multipleOfEight(int value) {
        return Math.max(8, Math.round(value / 8.0f) * 8);
    }
}
