package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.LinkedHashMap;
import java.util.Map;

/** Detected capacity and application budgets used for atomic multidimensional admission. */
public record ComputeResourceBudget(
        Map<ResourceId, Integer> legacyCapacities,
        long physicalHostMemoryBytes,
        long hostMemoryBudgetBytes,
        Map<ComputeDeviceId, ComputeDeviceBudget> devices,
        long fallbackDeviceVramBudgetBytes,
        int fallbackGpuComputeUnits,
        int fallbackEncoderUnits) {
    private static final long GIB = 1024L * 1024L * 1024L;

    public ComputeResourceBudget {
        LinkedHashMap<ResourceId, Integer> capacities = new LinkedHashMap<>();
        if (legacyCapacities != null) legacyCapacities.forEach((id, units) -> {
            if (id != null && units != null && units > 0) capacities.put(id, units);
        });
        legacyCapacities = Map.copyOf(capacities);
        physicalHostMemoryBytes = Math.max(0L, physicalHostMemoryBytes);
        hostMemoryBudgetBytes = Math.max(0L, hostMemoryBudgetBytes);
        if (physicalHostMemoryBytes > 0L) {
            hostMemoryBudgetBytes = Math.min(hostMemoryBudgetBytes,
                    physicalHostMemoryBytes);
        }
        devices = devices == null ? Map.of() : Map.copyOf(devices);
        fallbackDeviceVramBudgetBytes = Math.max(0L,
                fallbackDeviceVramBudgetBytes);
        fallbackGpuComputeUnits = Math.max(0, fallbackGpuComputeUnits);
        fallbackEncoderUnits = Math.max(0, fallbackEncoderUnits);
    }

    public static ComputeResourceBudget safeDefaults() {
        long detectedHost = 0L;
        long hostBudget = 8L * GIB;
        return new ComputeResourceBudget(Map.of(
                ResourceId.MODEL_MEMORY, 1,
                ResourceId.GPU, 1,
                ResourceId.CPU_HEAVY, 1,
                ResourceId.VIDEO_ENCODER, 1,
                ResourceId.QWEN_INFERENCE, 1),
                detectedHost, hostBudget, Map.of(),
                3L * GIB, 1, 1);
    }

    /**
     * Conservative reader budget: Qwen remains strictly serial while one
     * lightweight CPU TTS request may overlap its GPU-first inference.
     */
    public static ComputeResourceBudget incrementalReaderDefaults() {
        ComputeResourceBudget defaults = safeDefaults();
        return new ComputeResourceBudget(Map.of(
                ResourceId.MODEL_MEMORY, 1,
                ResourceId.GPU, 1,
                ResourceId.CPU_HEAVY, 2,
                ResourceId.VIDEO_ENCODER, 1,
                ResourceId.QWEN_INFERENCE, 1),
                defaults.physicalHostMemoryBytes(),
                defaults.hostMemoryBudgetBytes(),
                defaults.devices(),
                defaults.fallbackDeviceVramBudgetBytes(),
                defaults.fallbackGpuComputeUnits(),
                defaults.fallbackEncoderUnits());
    }

    public static ComputeResourceBudget legacy(Map<ResourceId, Integer> capacities) {
        ComputeResourceBudget defaults = safeDefaults();
        return new ComputeResourceBudget(capacities,
                defaults.physicalHostMemoryBytes(), defaults.hostMemoryBudgetBytes(),
                Map.of(), defaults.fallbackDeviceVramBudgetBytes(),
                defaults.fallbackGpuComputeUnits(), defaults.fallbackEncoderUnits());
    }

    public ComputeDeviceBudget deviceBudget(ComputeDeviceId device) {
        ComputeDeviceBudget exact = devices.get(device);
        if (exact != null) return exact;
        return new ComputeDeviceBudget(device, 0L,
                fallbackDeviceVramBudgetBytes, fallbackGpuComputeUnits,
                Map.of(VideoEncoderKind.NVIDIA_NVENC, fallbackEncoderUnits,
                        VideoEncoderKind.INTEL_QSV, fallbackEncoderUnits,
                        VideoEncoderKind.AMD_AMF, fallbackEncoderUnits));
    }

}
