package com.marcosmoreiradev.docupodcaststudio.application.process;

import java.io.IOException;

/** Runs local external processes through a common auditable contract. */
@FunctionalInterface
public interface ExternalProcessRunner {
    ExternalProcessResult run(ExternalProcessRequest request) throws IOException, InterruptedException;

    default ExternalProcessResult run(ExternalProcessRequest request, ExternalProcessObserver observer)
            throws IOException, InterruptedException {
        return run(request);
    }

    static ExternalProcessRunner unavailable(String owner) {
        return request -> {
            throw new IOException(owner + " requiere un ExternalProcessRunner inyectado desde infraestructura.");
        };
    }
}
