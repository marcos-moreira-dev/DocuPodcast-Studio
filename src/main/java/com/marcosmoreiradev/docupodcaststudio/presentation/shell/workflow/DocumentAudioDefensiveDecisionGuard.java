package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.compute.InspectXttsGpuFallbackDecisionUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.compute.XttsCudaSmokeReport;
import com.marcosmoreiradev.docupodcaststudio.application.decisions.UserVisibleDecision;
import com.marcosmoreiradev.docupodcaststudio.application.runtime.RuntimePathResolver;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Presentation-side guard for document audio actions that can continue after a defensive fallback.
 *
 * <p>The guard keeps JavaFX out of application code: it only returns decisions. The shell decides
 * whether to show a message box.</p>
 */
public final class DocumentAudioDefensiveDecisionGuard {
    private final ApplicationServices services;
    private final InspectXttsGpuFallbackDecisionUseCase gpuFallbackDecision = new InspectXttsGpuFallbackDecisionUseCase();

    public DocumentAudioDefensiveDecisionGuard(ApplicationServices services) {
        this.services = Objects.requireNonNull(services, "services");
    }

    public List<UserVisibleDecision> decisionsBeforeDocumentGeneration() {
        OperationalSettings settings = loadSettings();
        Path applicationRoot = RuntimePathResolver.defaultResolver().resolve().layout().applicationRoot();
        XttsCudaSmokeReport cudaSmoke = services.settings().inspectXttsCudaSmoke().inspect(applicationRoot);
        return gpuFallbackDecision.inspect(settings, cudaSmoke)
                .map(List::of)
                .orElseGet(List::of);
    }

    private OperationalSettings loadSettings() {
        try {
            return services.settings().loadOperationalSettings().load();
        } catch (IOException ex) {
            return OperationalSettings.defaults();
        }
    }
}
