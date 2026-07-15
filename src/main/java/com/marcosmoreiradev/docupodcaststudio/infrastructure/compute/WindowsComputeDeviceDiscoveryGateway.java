package com.marcosmoreiradev.docupodcaststudio.infrastructure.compute;

import com.marcosmoreiradev.docupodcaststudio.application.compute.ComputeDeviceDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.process.DefaultExternalProcessRunner;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/** Windows-focused CPU/GPU discovery using CIM/Win32_VideoController when available. */
public final class WindowsComputeDeviceDiscoveryGateway extends LocalComputeDeviceDiscoveryGateway {
    private final ExternalProcessRunner runner;

    public WindowsComputeDeviceDiscoveryGateway() {
        this(new DefaultExternalProcessRunner());
    }

    public WindowsComputeDeviceDiscoveryGateway(ExternalProcessRunner runner) {
        this.runner = runner == null ? new DefaultExternalProcessRunner() : runner;
    }

    @Override
    public List<ComputeDeviceDescriptor> discover(Properties properties, Map<String, String> environment, int availableProcessors) {
        LinkedHashMap<String, ComputeDeviceDescriptor> devices = new LinkedHashMap<>();
        for (ComputeDeviceDescriptor descriptor : super.discover(properties, environment, availableProcessors)) {
            devices.put(descriptor.id(), descriptor);
        }
        String os = properties == null ? "" : properties.getProperty("os.name", "");
        if (os.toLowerCase(Locale.ROOT).contains("windows")) {
            for (ComputeDeviceDescriptor gpu : parseWindowsVideoControllerOutput(queryWindowsVideoControllers())) {
                devices.putIfAbsent(gpu.id(), gpu);
            }
        }
        return List.copyOf(devices.values());
    }

    public List<ComputeDeviceDescriptor> parseWindowsVideoControllerOutput(String output) {
        String text = output == null ? "" : output.strip();
        if (text.isBlank()) {
            return List.of();
        }
        ArrayList<ComputeDeviceDescriptor> result = new ArrayList<>();
        int nvidia = 0;
        int amd = 0;
        int intel = 0;
        int unknown = 0;
        for (String rawLine : text.split("\\R")) {
            String name = extractName(rawLine);
            if (name.isBlank() || "Name".equalsIgnoreCase(name)) {
                continue;
            }
            String displayName = name + extractAdapterRamSuffix(rawLine);
            String lower = name.toLowerCase(Locale.ROOT);
            String vendor;
            String id;
            if (lower.contains("nvidia") || lower.contains("geforce") || lower.contains("rtx") || lower.contains("gtx")) {
                vendor = "NVIDIA";
                id = "gpu-nvidia-" + nvidia++;
            } else if (lower.contains("amd") || lower.contains("radeon")) {
                vendor = "AMD";
                id = "gpu-amd-" + amd++;
            } else if (lower.contains("intel") || lower.contains("iris") || lower.contains("arc") || lower.contains("uhd")) {
                vendor = "Intel";
                id = "gpu-intel-" + intel++;
            } else {
                vendor = "desconocido";
                id = "gpu-unknown-" + unknown++;
            }
            result.add(ComputeDeviceDescriptor.gpu(id, "GPU " + vendor + " - " + displayName, vendor));
        }
        return List.copyOf(result);
    }

    private String queryWindowsVideoControllers() {
        try {
            ExternalProcessRequest request = ExternalProcessRequest.of(
                            List.of("powershell", "-NoProfile", "-Command",
                                    "Get-CimInstance Win32_VideoController | Select-Object Name,AdapterRAM | ConvertTo-Csv -NoTypeInformation"),
                            "windows-video-controller-discovery",
                            Duration.ofSeconds(4))
                    .redirectingErrorStream();
            ExternalProcessResult result = runner.run(request);
            return result.succeeded() ? result.stdout() : "";
        } catch (Exception ex) {
            return "";
        }
    }

    private static String extractName(String line) {
        String normalized = line == null ? "" : line.strip();
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() > 1) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.contains(",")) {
            String[] parts = normalized.split(",");
            normalized = parts[0].replace("\"", "").strip();
        }
        return normalized;
    }

    private static String extractAdapterRamSuffix(String line) {
        String normalized = line == null ? "" : line.strip();
        if (!normalized.contains(",")) {
            return "";
        }
        String[] parts = normalized.split(",");
        if (parts.length < 2) {
            return "";
        }
        String raw = parts[1].replace("\"", "").strip();
        try {
            long bytes = Long.parseLong(raw);
            if (bytes <= 0) {
                return "";
            }
            double gb = bytes / 1024.0 / 1024.0 / 1024.0;
            return String.format(Locale.ROOT, " - VRAM %.1f GB", gb);
        } catch (NumberFormatException ex) {
            return "";
        }
    }
}
