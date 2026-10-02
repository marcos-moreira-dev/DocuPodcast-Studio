package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resources that must be granted atomically to one workload. Memory values are
 * estimates for admission diagnostics and future capacity certification.
 */
public record ComputeResourceDemand(
        Map<ResourceId, Integer> units,
        long estimatedHostMemoryBytes,
        long estimatedVramBytes,
        boolean hostMemoryOffloadAllowed,
        ComputeDeviceId device,
        int gpuComputeUnits,
        ModelResidencyDemand modelResidency,
        EncoderResourceDemand encoder
) {
    public static final ComputeResourceDemand NONE =
            new ComputeResourceDemand(Map.of(), 0L, 0L, false,
                    ComputeDeviceId.CPU_0, 0, null,
                    EncoderResourceDemand.none());

    public ComputeResourceDemand {
        LinkedHashMap<ResourceId, Integer> normalized = new LinkedHashMap<>();
        if (units != null) {
            units.entrySet().stream()
                    .filter(entry -> entry.getKey() != null)
                    .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> normalized.put(entry.getKey(), entry.getValue()));
        }
        units = Map.copyOf(normalized);
        estimatedHostMemoryBytes = Math.max(0L, estimatedHostMemoryBytes);
        estimatedVramBytes = Math.max(0L, estimatedVramBytes);
        device = java.util.Objects.requireNonNullElse(
                device, ComputeDeviceId.CPU_0);
        gpuComputeUnits = Math.max(0, gpuComputeUnits);
        encoder = java.util.Objects.requireNonNullElseGet(
                encoder, EncoderResourceDemand::none);
    }

    /** Compatibility constructor: memory values represent marginal request cost. */
    public ComputeResourceDemand(Map<ResourceId, Integer> units,
                                 long estimatedHostMemoryBytes,
                                 long estimatedVramBytes,
                                 boolean hostMemoryOffloadAllowed) {
        this(units, estimatedHostMemoryBytes, estimatedVramBytes,
                hostMemoryOffloadAllowed, ComputeDeviceId.CPU_0,
                0, null, EncoderResourceDemand.none());
    }

    public static ComputeResourceDemand of(ResourceId... resources) {
        LinkedHashMap<ResourceId, Integer> units = new LinkedHashMap<>();
        if (resources != null) {
            for (ResourceId resource : resources) {
                if (resource != null) units.merge(resource, 1, Integer::sum);
            }
        }
        return new ComputeResourceDemand(units, 0L, 0L, true);
    }

    public static ComputeResourceDemand from(ResourceRequirement requirement) {
        if (requirement == null) return NONE;
        return of(requirement.resources().toArray(ResourceId[]::new));
    }

    public boolean conflictsWith(ComputeResourceDemand other) {
        if (other == null) return false;
        if (units.keySet().stream().anyMatch(other.units::containsKey)) return true;
        long host = estimatedHostMemoryBytes
                + (modelResidency == null ? 0L : modelResidency.hostMemoryBytes());
        long otherHost = other.estimatedHostMemoryBytes
                + (other.modelResidency == null ? 0L
                : other.modelResidency.hostMemoryBytes());
        if (host > 0L && otherHost > 0L) return true;
        long vram = estimatedVramBytes
                + (modelResidency == null ? 0L : modelResidency.vramBytes());
        long otherVram = other.estimatedVramBytes
                + (other.modelResidency == null ? 0L
                : other.modelResidency.vramBytes());
        if (vram > 0L && otherVram > 0L && device.equals(other.device)) return true;
        if (gpuComputeUnits > 0 && other.gpuComputeUnits > 0
                && device.equals(other.device)) return true;
        return encoder.units() > 0 && other.encoder.units() > 0
                && encoder.device().equals(other.encoder.device())
                && encoder.kind() == other.encoder.kind();
    }
}
