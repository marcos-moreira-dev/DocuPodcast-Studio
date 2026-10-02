package com.marcosmoreiradev.docupodcaststudio.domain.video;

/** Resolution presets exposed to users for the simple video export. */
public enum SimpleVideoResolutionPreset {
    LOW_VERTICAL_540X960("Vertical 540p", 540, 960),
    HD_VERTICAL_720X1280("Vertical 720p", 720, 1280),
    FULL_HD_VERTICAL_1080X1920("Vertical 1080p", 1080, 1920),
    LOW_540("540p", 960, 540),
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

    public static SimpleVideoResolutionPreset fromDimensions(int width, int height) {
        for (SimpleVideoResolutionPreset preset : values()) {
            if (preset.width == width && preset.height == height) {
                return preset;
            }
        }
        boolean vertical = height > width;
        if (vertical) {
            if (height >= FULL_HD_VERTICAL_1080X1920.height) return FULL_HD_VERTICAL_1080X1920;
            if (height >= HD_VERTICAL_720X1280.height) return HD_VERTICAL_720X1280;
            return LOW_VERTICAL_540X960;
        }
        if (width >= UHD_4K.width) return UHD_4K;
        if (width >= QHD_2K.width) return QHD_2K;
        if (width >= FULL_HD_1080.width) return FULL_HD_1080;
        if (width >= HD_720.width) return HD_720;
        return LOW_540;
    }

    public com.marcosmoreiradev.docupodcaststudio.domain.video.VisualResolutionProfile visualResolutionProfile() {
        return switch (this) {
            case LOW_VERTICAL_540X960, LOW_540 ->
                    com.marcosmoreiradev.docupodcaststudio.domain.video.VisualResolutionProfile.P540;
            case HD_VERTICAL_720X1280, HD_720 ->
                    com.marcosmoreiradev.docupodcaststudio.domain.video.VisualResolutionProfile.P720;
            case FULL_HD_VERTICAL_1080X1920, FULL_HD_1080 ->
                    com.marcosmoreiradev.docupodcaststudio.domain.video.VisualResolutionProfile.P1080;
            case QHD_2K -> com.marcosmoreiradev.docupodcaststudio.domain.video.VisualResolutionProfile.QHD_2K;
            case UHD_4K -> com.marcosmoreiradev.docupodcaststudio.domain.video.VisualResolutionProfile.UHD_4K;
        };
    }
}
