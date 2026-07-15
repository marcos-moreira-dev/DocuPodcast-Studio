package com.marcosmoreiradev.docupodcaststudio.application.image;

import java.util.Locale;

/** Target delivery profile for enhanced theatrical images. */
public enum ImageEnhancementOutputProfile {
    HD_720("720p", 1280, 720, ImageEnhancementPipelineProfile.LIGHT_UPSCALE, false, 512, 64),
    FHD_1080("1080p", 1920, 1080, ImageEnhancementPipelineProfile.RESTORE_UPSCALE, false, 768, 96),
    QHD_2K("2K/QHD", 2560, 1440, ImageEnhancementPipelineProfile.TILED_PROFESSIONAL, true, 1024, 128),
    UHD_4K("4K/UHD", 3840, 2160, ImageEnhancementPipelineProfile.TILED_PROFESSIONAL, true, 1024, 128);

    private final String displayName;
    private final int width;
    private final int height;
    private final ImageEnhancementPipelineProfile pipelineProfile;
    private final boolean professional;
    private final int tileSize;
    private final int tileOverlap;

    ImageEnhancementOutputProfile(String displayName,
                                  int width,
                                  int height,
                                  ImageEnhancementPipelineProfile pipelineProfile,
                                  boolean professional,
                                  int tileSize,
                                  int tileOverlap) {
        this.displayName = displayName;
        this.width = width;
        this.height = height;
        this.pipelineProfile = pipelineProfile;
        this.professional = professional;
        this.tileSize = tileSize;
        this.tileOverlap = tileOverlap;
    }

    public String displayName() {
        return displayName;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public ImageEnhancementPipelineProfile pipelineProfile() {
        return pipelineProfile;
    }

    public boolean professional() {
        return professional;
    }

    public int tileSize() {
        return tileSize;
    }

    public int tileOverlap() {
        return tileOverlap;
    }

    public String workflowId() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
