package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.io.IOException;

public interface VoiceSynthesisEngine extends MediaEngine {
    VoiceSynthesisResult synthesize(VoiceSynthesisRequest request, ExecutionContext context)
            throws IOException, InterruptedException;
}
