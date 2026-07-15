package com.marcosmoreiradev.docupodcaststudio.application.runtime;

import java.nio.file.Path;

/** Result of resolving the application runtime root. */
public record RuntimePathResolution(ApplicationRuntimeLayout layout, String source, boolean explicit) {
    public Path applicationRoot() {
        return layout.applicationRoot();
    }
}
