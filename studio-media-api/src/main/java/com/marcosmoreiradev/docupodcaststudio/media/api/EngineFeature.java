package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Locale;

/** Extensible feature flag advertised by an engine descriptor. */
public record EngineFeature(String value) {
    public static final EngineFeature REFERENCE_VOICE = new EngineFeature("reference-voice");
    public static final EngineFeature EXPRESSIVE_STYLE = new EngineFeature("expressive-style");
    public static final EngineFeature BATCH = new EngineFeature("batch");
    public static final EngineFeature CONDITIONING_IMAGE = new EngineFeature("conditioning-image");
    public static final EngineFeature FRAME_INTERPOLATION = new EngineFeature("frame-interpolation");
    public static final EngineFeature HARDWARE_ACCELERATION = new EngineFeature("hardware-acceleration");
    public static final EngineFeature TEXT_TO_VIDEO = new EngineFeature("text-to-video");
    public static final EngineFeature IMAGE_TO_VIDEO = new EngineFeature("image-to-video");

    public EngineFeature {
        value = value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replace('_', '-');
        if (value.isBlank()) throw new IllegalArgumentException("engine feature is required");
    }
}
