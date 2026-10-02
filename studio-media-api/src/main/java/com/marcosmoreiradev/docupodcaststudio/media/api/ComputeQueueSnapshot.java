package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Read-only scheduler state suitable for diagnostics and an accessible queue UI. */
public record ComputeQueueSnapshot(
        Map<ResourceId, Integer> capacities,
        Map<ResourceId, Integer> available,
        List<Entry> active,
        List<Entry> queued,
        ComputeResourceBudget budget,
        long reservedHostMemoryBytes,
        Map<ComputeDeviceId, DeviceReservation> deviceReservations,
        List<ModelResidency> modelResidencies
) {
    public ComputeQueueSnapshot {
        capacities = capacities == null ? Map.of() : Map.copyOf(capacities);
        available = available == null ? Map.of() : Map.copyOf(available);
        active = active == null ? List.of() : List.copyOf(active);
        queued = queued == null ? List.of() : List.copyOf(queued);
        budget = budget == null ? ComputeResourceBudget.safeDefaults() : budget;
        reservedHostMemoryBytes = Math.max(0L, reservedHostMemoryBytes);
        deviceReservations = deviceReservations == null
                ? Map.of() : Map.copyOf(deviceReservations);
        modelResidencies = modelResidencies == null
                ? List.of() : List.copyOf(modelResidencies);
    }

    public ComputeQueueSnapshot(Map<ResourceId, Integer> capacities,
                                Map<ResourceId, Integer> available,
                                List<Entry> active,
                                List<Entry> queued) {
        this(capacities, available, active, queued,
                ComputeResourceBudget.legacy(capacities), 0L,
                Map.of(), List.of());
    }

    public record DeviceReservation(
            long reservedVramBytes,
            int gpuComputeUnits,
            Map<VideoEncoderKind, Integer> encoderUnits,
            int activeConsumers) {
        public DeviceReservation {
            reservedVramBytes = Math.max(0L, reservedVramBytes);
            gpuComputeUnits = Math.max(0, gpuComputeUnits);
            encoderUnits = encoderUnits == null ? Map.of() : Map.copyOf(encoderUnits);
            activeConsumers = Math.max(0, activeConsumers);
        }
    }

    public record ModelResidency(
            ModelResidencyKey key,
            long hostMemoryBytes,
            long vramBytes,
            int activeConsumers,
            boolean runtimeConfirmed,
            boolean unloadPending) { }

    public record Entry(
            String admissionId,
            String operationId,
            ComputeJobPriority requestedPriority,
            ComputeJobPriority effectivePriority,
            ComputeWorkloadKind workload,
            ComputeResourceDemand demand,
            Duration waiting,
            boolean fits,
            String fitFailureReason,
            Map<String, Long> fitDetails,
            long bypassCount,
            long deadlineRemainingMillis
    ) {
        public Entry {
            waiting = waiting == null || waiting.isNegative() ? Duration.ZERO : waiting;
            fitFailureReason = fitFailureReason == null ? "" : fitFailureReason;
            fitDetails = fitDetails == null ? Map.of() : Map.copyOf(fitDetails);
            bypassCount = Math.max(0L, bypassCount);
        }
    }
}
