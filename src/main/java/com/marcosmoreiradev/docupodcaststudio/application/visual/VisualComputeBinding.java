package com.marcosmoreiradev.docupodcaststudio.application.visual;

import java.util.List;

/** Exact device/backend binding used to launch and verify the local engine. */
public record VisualComputeBinding(
        String selectedDeviceId,
        String displayName,
        VisualComputeBackend backend,
        int deviceIndex,
        List<String> launchArguments
) {
    public VisualComputeBinding {
        selectedDeviceId = clean(selectedDeviceId);
        displayName = clean(displayName);
        backend = backend == null ? VisualComputeBackend.CPU : backend;
        deviceIndex = backend == VisualComputeBackend.CPU ? -1 : Math.max(0, deviceIndex);
        launchArguments = launchArguments == null ? List.of() : List.copyOf(launchArguments);
        if (selectedDeviceId.isBlank()) {
            throw new IllegalArgumentException("Falta el identificador del dispositivo visual.");
        }
    }

    public boolean gpu() {
        return backend != VisualComputeBackend.CPU;
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
