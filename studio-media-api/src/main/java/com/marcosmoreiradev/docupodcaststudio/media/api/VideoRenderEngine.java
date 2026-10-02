package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.io.IOException;

public interface VideoRenderEngine extends MediaEngine {
    VideoRenderResult render(VideoRenderRequest request, ExecutionContext context)
            throws IOException, InterruptedException;
}
