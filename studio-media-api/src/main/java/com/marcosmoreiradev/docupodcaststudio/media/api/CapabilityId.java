package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Locale;

/** Extensible capability identifier. It intentionally is not an enum. */
public record CapabilityId(String value) {
    public static final CapabilityId VOICE_SYNTHESIS = new CapabilityId("voice-synthesis");
    public static final CapabilityId IMAGE_GENERATION = new CapabilityId("image-generation");
    public static final CapabilityId IMAGE_SUPER_RESOLUTION = new CapabilityId("image-super-resolution");
    public static final CapabilityId IMAGE_REFINEMENT = new CapabilityId("image-refinement");
    public static final CapabilityId VIDEO_GENERATION = new CapabilityId("video-generation");
    public static final CapabilityId VIDEO_RENDERING = new CapabilityId("video-rendering");
    public static final CapabilityId CONTENT_LAYOUT_ANALYSIS =
            new CapabilityId("content-layout-analysis");
    public static final CapabilityId CONTENT_MATH_RECOGNITION =
            new CapabilityId("content-math-recognition");
    public static final CapabilityId MATH_SPEECH =
            new CapabilityId("math-speech");
    public static final CapabilityId VISUAL_CONTENT_DESCRIPTION =
            new CapabilityId("visual-content-description");
    public static final CapabilityId CONTENT_CONTEXT_CORRECTION =
            new CapabilityId("content-context-correction");
    public static final CapabilityId CONTENT_NARRATABILITY_ANALYSIS =
            new CapabilityId("content-narratability-analysis");
    public static final CapabilityId CONTENT_TABLE_ANALYSIS =
            new CapabilityId("content-table-analysis");
    public static final CapabilityId NARRATION_TRANSLATION =
            new CapabilityId("narration-translation");

    public CapabilityId {
        value = normalize(value);
        if (value.isBlank()) throw new IllegalArgumentException("capability id is required");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
