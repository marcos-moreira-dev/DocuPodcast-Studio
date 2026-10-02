package com.marcosmoreiradev.docupodcaststudio.bootstrap;

import com.marcosmoreiradev.docupodcaststudio.application.visual.ComfyUiSystemStats;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualComputeBinding;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualComputeBindingVerifier;

import java.util.List;

/**
 * Public composition-boundary view of the existing visual compute policy.
 *
 * <p>It intentionally does not expose application-internal compute types to provider modules.</p>
 */
public final class ResolvedVisualEngineLaunchPolicy {
    private final VisualComputeBinding binding;
    private final List<String> memoryArguments;
    private final VisualComputeBindingVerifier verifier = new VisualComputeBindingVerifier();

    ResolvedVisualEngineLaunchPolicy(VisualComputeBinding binding, List<String> memoryArguments) {
        this.binding = java.util.Objects.requireNonNull(binding, "visual compute binding");
        this.memoryArguments = memoryArguments == null ? List.of() : List.copyOf(memoryArguments);
    }

    public String selectedDeviceId() {
        return binding.selectedDeviceId();
    }

    public String displayName() {
        return binding.displayName();
    }

    public String backend() {
        return binding.backend().name();
    }

    public boolean gpu() {
        return binding.gpu();
    }

    public List<String> deviceArguments() {
        return binding.launchArguments();
    }

    public List<String> memoryArguments() {
        return memoryArguments;
    }

    /** Returns an empty string only when the live ComfyUI device matches the configured binding. */
    public String verifySystemStats(String json) {
        try {
            VisualComputeBindingVerifier.Verification result =
                    verifier.verify(binding, ComfyUiSystemStats.parse(json));
            return result.matches() ? "" : result.message();
        } catch (RuntimeException invalidResponse) {
            return "No se pudo validar /system_stats: " + invalidResponse.getMessage();
        }
    }
}
