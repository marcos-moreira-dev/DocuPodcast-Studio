package com.marcosmoreiradev.docupodcaststudio.application.image;

import java.io.IOException;

/** Provider abstraction for local image improvement, upscale and adaptation. */
public interface ImageEnhancementProvider {
    String id();

    String displayName();

    boolean supports(ImageEnhancementRequest request);

    ImageEnhancementResult enhance(ImageEnhancementRequest request) throws IOException;
}
