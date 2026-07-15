package com.marcosmoreiradev.docupodcaststudio.application.compute;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Safe fallback discovery based on Java properties and environment signals.
 * Platform-specific gateways may add real hardware discovery on top.
 */
public class EnvironmentComputeDeviceDiscoveryGateway implements ComputeDeviceDiscoveryGateway {
    @Override
    public List<ComputeDeviceDescriptor> discover(Properties properties, Map<String, String> environment, int availableProcessors) {
        Properties props = properties == null ? new Properties() : properties;
        Map<String, String> env = environment == null ? Map.of() : environment;
        LinkedHashMap<String, ComputeDeviceDescriptor> devices = new LinkedHashMap<>();
        String arch = props.getProperty("os.arch", "arquitectura desconocida");
        String cpuName = firstNonBlank(env.get("PROCESSOR_IDENTIFIER"), env.get("PROCESSOR_ARCHITECTURE"), arch);
        devices.put("cpu", ComputeDeviceDescriptor.cpu("CPU local · " + Math.max(1, availableProcessors) + " hilos · " + cpuName));
        detectGpuFromEnvironment(env, devices);
        return List.copyOf(devices.values());
    }

    protected final void detectGpuFromEnvironment(Map<String, String> env, Map<String, ComputeDeviceDescriptor> devices) {
        String cuda = value(env, "CUDA_VISIBLE_DEVICES");
        String nvidiaVisible = value(env, "NVIDIA_VISIBLE_DEVICES");
        String hip = value(env, "HIP_VISIBLE_DEVICES");
        String rocr = value(env, "ROCR_VISIBLE_DEVICES");
        String oneapi = value(env, "ONEAPI_DEVICE_SELECTOR");
        if (presentGpu(cuda) || presentGpu(nvidiaVisible)) {
            devices.putIfAbsent("gpu-nvidia-0", ComputeDeviceDescriptor.gpu("gpu-nvidia-0", "GPU NVIDIA visible", "NVIDIA"));
        }
        if (presentGpu(hip) || presentGpu(rocr)) {
            devices.putIfAbsent("gpu-amd-0", ComputeDeviceDescriptor.gpu("gpu-amd-0", "GPU AMD visible", "AMD"));
        }
        if (presentGpu(oneapi)) {
            devices.putIfAbsent("gpu-intel-0", ComputeDeviceDescriptor.gpu("gpu-intel-0", "GPU Intel visible", "Intel"));
        }
    }

    protected static boolean presentGpu(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        return !normalized.isBlank() && !"none".equals(normalized) && !"void".equals(normalized) && !"-1".equals(normalized);
    }

    protected static String value(Map<String, String> env, String key) {
        return env == null ? "" : env.getOrDefault(key, "");
    }

    protected static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String normalized = value == null ? "" : value.strip();
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "";
    }
}
