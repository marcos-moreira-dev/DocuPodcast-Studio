package com.marcosmoreiradev.docupodcaststudio.bootstrap;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ComfyUiLaunchArgumentPlanner;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ComfyUiRuntimeCapabilities;
import com.marcosmoreiradev.docupodcaststudio.application.modelsetup.ComfyUiRuntimeCapabilityProbe;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationMemoryProfile;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualComputeBackendResolver;
import com.marcosmoreiradev.docupodcaststudio.application.visual.VisualComputeBinding;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.compute.WindowsComputeDeviceDiscoveryGateway;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.settings.PropertiesOperationalSettingsRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Connects persisted compute priority and real hardware discovery to the shared ComfyUI launcher. */
public final class VisualEngineLaunchPolicyResolver {
    private final WindowsComputeDeviceDiscoveryGateway discovery =
            new WindowsComputeDeviceDiscoveryGateway();
    private final DefaultExternalProcessRunner processRunner = new DefaultExternalProcessRunner();
    private final ComfyUiRuntimeCapabilityProbe capabilityProbe = new ComfyUiRuntimeCapabilityProbe();
    private final VisualComputeBackendResolver backendResolver = new VisualComputeBackendResolver();
    private final ComfyUiLaunchArgumentPlanner memoryPlanner = new ComfyUiLaunchArgumentPlanner();

    public ResolvedVisualEngineLaunchPolicy resolve(Path runtimeRoot, String requestedMemoryProfile) {
        OperationalSettings settings = loadSettings();
        ImageGenerationSettings image = withMemoryOverride(
                settings.imageGeneration(), requestedMemoryProfile);
        Path visualRuntime = runtimeRoot.toAbsolutePath().normalize().resolve("tools/image");
        ComfyUiRuntimeCapabilities capabilities = capabilityProbe.probe(visualRuntime, processRunner);
        List<ComputeDeviceDescriptor> devices = discovery.discover(
                System.getProperties(), System.getenv(), Runtime.getRuntime().availableProcessors());
        VisualComputeBinding binding = backendResolver.resolve(settings.compute(), devices, capabilities);
        return new ResolvedVisualEngineLaunchPolicy(
                binding, memoryPlanner.memoryArguments(image, capabilities));
    }

    private static OperationalSettings loadSettings() {
        try {
            return PropertiesOperationalSettingsRepository.defaultRepository().load();
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "No se pudo cargar la prioridad de cómputo guardada en Configuración: "
                            + failure.getMessage(), failure);
        }
    }

    private static ImageGenerationSettings withMemoryOverride(
            ImageGenerationSettings current, String requestedMemoryProfile) {
        String systemOverride = firstNonBlank(
                requestedMemoryProfile,
                System.getProperty("docupodcast.comfy.memoryProfile"),
                System.getenv("DOCUPODCAST_COMFY_MEMORY_PROFILE"));
        if (systemOverride.isBlank()) return current;
        ImageGenerationMemoryProfile memory = ImageGenerationMemoryProfile.from(
                systemOverride, current.lowVram());
        return new ImageGenerationSettings(
                current.engineMode(), current.baseUrl(), current.devicePolicy(),
                current.preset(), current.modelName(), current.adaptersDirectory(),
                current.timeoutSeconds(), memory.legacyLowVram(), memory.name(), current.maxAttempts());
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.strip();
        }
        return "";
    }
}
