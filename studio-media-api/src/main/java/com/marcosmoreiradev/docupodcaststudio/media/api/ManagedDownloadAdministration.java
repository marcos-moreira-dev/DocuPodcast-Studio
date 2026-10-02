package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.io.IOException;

/**
 * Administrative adapter that publishes a real managed download.
 *
 * <p>The preflight is local-only. Network access and replacement of an existing
 * valid resource require an explicit execution decision.</p>
 */
public interface ManagedDownloadAdministration extends EngineAdministration {
    ManagedDownloadPreflight inspectDownload(EngineActionRequest request) throws IOException;

    EngineActionResult executeDownload(
            EngineActionRequest request,
            ManagedDownloadDecision decision,
            ExecutionContext context) throws IOException, InterruptedException;
}
