package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.Map;

/** Typed, provider-neutral request payload stored with generation jobs. */
public sealed interface GenerationPayload permits VoiceJobPayload, ImageJobPayload,
        VideoGenerationJobPayload, VideoRenderJobPayload, EmptyGenerationPayload {
    String kind();
    Map<String, String> portableFields();
}
