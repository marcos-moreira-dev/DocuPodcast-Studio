package com.marcosmoreiradev.docupodcaststudio.application.compute;

import java.nio.file.Path;

/** Executes a small Python-side CUDA probe inside the self-contained XTTS runtime. */
@FunctionalInterface
public interface XttsCudaRuntimeProbeGateway {
    XttsCudaRuntimeProbeResult probe(Path pythonExecutable, String deviceArgument, int timeoutSeconds);
}
