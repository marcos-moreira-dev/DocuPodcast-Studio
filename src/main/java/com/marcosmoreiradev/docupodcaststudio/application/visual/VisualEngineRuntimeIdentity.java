package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.time.Instant;
import java.util.List;

/** Auditable identity of the currently verified managed visual runtime. */
public record VisualEngineRuntimeIdentity(
        long pid,
        String endpoint,
        VisualComputeBinding computeBinding,
        List<String> launchArguments,
        String pythonVersion,
        String pytorchVersion,
        Instant verifiedAt
) {
    public VisualEngineRuntimeIdentity {
        pid = Math.max(-1L, pid);
        endpoint = endpoint == null ? "" : endpoint.strip();
        launchArguments = launchArguments == null ? List.of() : List.copyOf(launchArguments);
        pythonVersion = pythonVersion == null ? "" : pythonVersion.strip();
        pytorchVersion = pytorchVersion == null ? "" : pytorchVersion.strip();
        verifiedAt = verifiedAt == null ? Instant.now() : verifiedAt;
    }
}
