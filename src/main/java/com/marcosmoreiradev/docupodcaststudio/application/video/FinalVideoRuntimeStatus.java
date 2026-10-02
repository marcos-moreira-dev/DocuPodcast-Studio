package com.marcosmoreiradev.docupodcaststudio.application.video;

import java.nio.file.Path;
import java.util.List;

/** Provider-neutral presentation data for the configured final-video runtime. */
public record FinalVideoRuntimeStatus(
        String rendererName,
        String inspectorName,
        Path rendererPath,
        Path inspectorPath,
        boolean ready,
        String stateMessage,
        String rendererVersion,
        String inspectorVersion,
        String effectiveEncoder,
        String requestedEncoder,
        List<String> availableEncoders,
        String administrationHint
) {
    public FinalVideoRuntimeStatus {
        rendererName = label(rendererName, "Renderizador");
        inspectorName = label(inspectorName, "Inspector");
        stateMessage = stateMessage == null ? "" : stateMessage.strip();
        rendererVersion = rendererVersion == null ? "" : rendererVersion.strip();
        inspectorVersion = inspectorVersion == null ? "" : inspectorVersion.strip();
        effectiveEncoder = effectiveEncoder == null ? "" : effectiveEncoder.strip();
        requestedEncoder = requestedEncoder == null ? "" : requestedEncoder.strip();
        availableEncoders = availableEncoders == null ? List.of() : List.copyOf(availableEncoders);
        administrationHint = administrationHint == null ? "" : administrationHint.strip();
    }

    private static String label(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
