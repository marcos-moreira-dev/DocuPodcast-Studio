package com.marcosmoreiradev.docupodcaststudio.application.compute;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/** Detects CPU/GPU context so the user can choose where the advanced AI voice should run. */
public final class InspectComputeEnvironmentUseCase {
    private final ComputeDeviceDiscoveryGateway discoveryGateway;

    public InspectComputeEnvironmentUseCase() {
        this(new EnvironmentComputeDeviceDiscoveryGateway());
    }

    public InspectComputeEnvironmentUseCase(ComputeDeviceDiscoveryGateway discoveryGateway) {
        this.discoveryGateway = Objects.requireNonNull(discoveryGateway, "discoveryGateway");
    }

    public ComputeEnvironmentReport inspect(OperationalSettings settings) {
        return inspect(settings, System.getProperties(), System.getenv(), Runtime.getRuntime().availableProcessors());
    }

    public ComputeEnvironmentReport inspect(
            OperationalSettings settings,
            Properties properties,
            Map<String, String> environment,
            int availableProcessors
    ) {
        OperationalSettings current = settings == null ? OperationalSettings.defaults() : settings;
        OperationalSettings.ComputeSettings compute = current.compute();
        Properties props = properties == null ? new Properties() : properties;
        Map<String, String> env = environment == null ? Map.of() : environment;
        ArrayList<String> warnings = new ArrayList<>();
        ArrayList<String> info = new ArrayList<>();
        List<ComputeDeviceDescriptor> devices = discoveryGateway.discover(props, env, availableProcessors);
        boolean gpuDetected = devices.stream().anyMatch(ComputeDeviceDescriptor::gpu);

        String os = props.getProperty("os.name", "sistema operativo desconocido");
        String arch = props.getProperty("os.arch", "arquitectura desconocida");
        info.add("Sistema: " + os + " · " + arch + " · CPU/GPU detectadas para selección de motores de voz y video.");
        devices.stream().filter(ComputeDeviceDescriptor::gpu)
                .map(device -> "GPU detectada: " + device.displayName() + " · " + device.vendor() + ".")
                .forEach(info::add);
        if (!gpuDetected) {
            info.add("No se detectó GPU dedicada; los motores de voz pueden trabajar en CPU.");
        }
        if (compute.policy() == ComputeDevicePolicy.SPECIFIC_DEVICE) {
            if (compute.selectedDeviceId().isBlank() || "auto".equalsIgnoreCase(compute.selectedDeviceId())) {
                warnings.add("La política exige dispositivo específico, pero no hay selectedDeviceId configurado.");
            } else if (devices.stream().noneMatch(device -> device.id().equalsIgnoreCase(compute.selectedDeviceId()))) {
                warnings.add("El dispositivo específico '" + compute.selectedDeviceId() + "' no fue detectado; se debe caer a automático/CPU.");
            }
        }
        if (compute.policy() == ComputeDevicePolicy.PREFER_GPU && !gpuDetected) {
            warnings.add("Se pidió preferir GPU, pero no se detectó una GPU disponible; se usará CPU.");
        }
        if (!compute.allowGpuForTts() && compute.policy().canUseGpu()) {
            warnings.add("La voz tiene GPU desactivada en Configuración; se usará CPU aunque haya GPU.");
        }
        if (!compute.allowGpuForTts() && !compute.allowGpuForVideo() && compute.policy().canUseGpu()) {
            warnings.add("La política permite GPU, pero voz y video tienen uso de GPU desactivado.");
        }
        String voiceDevice = ComputeDeviceArgumentMapper.toProcessDeviceArgument(compute.policy(), compute.selectedDeviceId());
        info.add("Dispositivo solicitado para voz: " + compute.selectedDeviceId() + " - argumento de proceso: " + voiceDevice + ".");
        info.add("Voz GPU: " + enabled(compute.allowGpuForTts())
                + " · Video GPU: " + enabled(compute.allowGpuForVideo()) + ".");
        info.add("Encoder de video solicitado: " + compute.videoEncoderPolicy().name() + " — " + compute.videoEncoderPolicy().label() + ".");
        info.add("La detección identifica CPU/GPU para selección honesta; no ejecuta benchmarks.");
        return new ComputeEnvironmentReport(compute.policy(), compute.selectedDeviceId(), devices, warnings, info);
    }

    private static String enabled(boolean value) {
        return value ? "permitido" : "desactivado";
    }
}
