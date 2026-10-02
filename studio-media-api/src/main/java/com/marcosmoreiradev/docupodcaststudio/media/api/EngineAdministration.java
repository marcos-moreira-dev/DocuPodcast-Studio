package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.io.IOException;
import java.util.List;

/** Optional maintenance surface implemented by local adapters, never by product UI. */
public interface EngineAdministration {
    EngineId engineId();
    List<EngineActionDescriptor> actions();
    EngineActionResult execute(EngineActionRequest request, ExecutionContext context)
            throws IOException, InterruptedException;
}
