package com.marcosmoreiradev.docupodcaststudio.application.video;

/** Resolution presets exposed to users for the simple video export. */
public enum SimpleVideoResolutionPreset {
    HD_VERTICAL_720X1280("Vertical 720p", 720, 1280),
    FULL_HD_VERTICAL_1080X1920("Vertical 1080p", 1080, 1920),
    HD_720("720p", 1280, 720),
    FULL_HD_1080("1080p", 1920, 1080),
    QHD_2K("2K", 2560, 1440),
    UHD_4K("4K", 3840, 2160);

    private final String label;
    private final int width;
    private final int height;

    SimpleVideoResolutionPreset(String label, int width, int height) {
        this.label = label;
        this.width = width;
        this.height = height;
    }

    public String label() {
        return label;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public String ffmpegScaleExpression() {
        return "scale=" + width + ":" + height + ":force_original_aspect_ratio=decrease,pad="
                + width + ":" + height + ":(ow-iw)/2:(oh-ih)/2";
    }

    public static SimpleVideoResolutionPreset defaultPreset() {
        return QHD_2K;
    }
}
