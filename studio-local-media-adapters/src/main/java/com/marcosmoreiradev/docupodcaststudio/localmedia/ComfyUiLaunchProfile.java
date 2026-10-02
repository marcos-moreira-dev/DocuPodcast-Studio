package com.marcosmoreiradev.docupodcaststudio.localmedia;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolved ComfyUI launch configuration supplied by the composition root.
 *
 * <p>Hardware discovery and device policy remain outside the provider adapter. The adapter only
 * executes the exact arguments and verifier resolved by the application's existing compute
 * infrastructure.</p>
 */
public record ComfyUiLaunchProfile(
        String selectedDeviceId,
        String displayName,
        String backend,
        boolean gpu,
        List<String> launchArguments,
        ComfyUiSystemStatsVerifier verifier
) {
    public ComfyUiLaunchProfile {
        selectedDeviceId = clean(selectedDeviceId, "auto");
        displayName = clean(displayName, "Selección automática de ComfyUI");
        backend = clean(backend, "AUTO");
        launchArguments = launchArguments == null ? List.of() : List.copyOf(launchArguments);
        verifier = verifier == null ? ComfyUiSystemStatsVerifier.NONE : verifier;
    }

    public static ComfyUiLaunchProfile automatic(ComfyUiMemoryProfile memoryProfile) {
        ComfyUiMemoryProfile memory = memoryProfile == null
                ? ComfyUiMemoryProfile.SAFE_LOW_VRAM : memoryProfile;
        return new ComfyUiLaunchProfile(
                "auto", "Selección automática de ComfyUI", "AUTO", true,
                memory.launchArguments(), ComfyUiSystemStatsVerifier.NONE);
    }

    public static ComfyUiLaunchProfile resolved(
            String selectedDeviceId,
            String displayName,
            String backend,
            boolean gpu,
            List<String> deviceArguments,
            List<String> memoryArguments,
            ComfyUiSystemStatsVerifier verifier
    ) {
        ArrayList<String> arguments = new ArrayList<>();
        if (deviceArguments != null) arguments.addAll(deviceArguments);
        if (memoryArguments != null) arguments.addAll(memoryArguments);
        return new ComfyUiLaunchProfile(selectedDeviceId, displayName, backend, gpu, arguments, verifier);
    }

    String verificationFailure(String systemStatsJson) {
        String result = verifier.verify(systemStatsJson);
        return result == null ? "" : result.strip();
    }

    private static String clean(String value, String fallback) {
        String normalized = value == null ? "" : value.strip();
        return normalized.isBlank() ? fallback : normalized;
    }
}
