package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;

/** Atomic priority/FIFO admission across legacy lanes and physical budgets. */
public final class PriorityResourceScheduler implements ResourceScheduler {
    private static final System.Logger LOGGER = System.getLogger(
            PriorityResourceScheduler.class.getName());
    private static final Duration DEFAULT_AGING_INTERVAL = Duration.ofMinutes(1);
    private static final long WAIT_LOG_INTERVAL_NANOS = Duration.ofSeconds(10).toNanos();
    private final Object monitor = new Object();
    private final ComputeResourceBudget budget;
    private final Map<ResourceId, Integer> capacities;
    private final Map<ResourceId, Integer> available;
    private final Map<ComputeDeviceId, MutableDeviceUsage> deviceUsage =
            new LinkedHashMap<>();
    private final Map<ModelResidencyKey, MutableResidency> residencies =
            new LinkedHashMap<>();
    private final List<Pending> queued = new ArrayList<>();
    private final Map<String, Active> active = new LinkedHashMap<>();
    private final long agingIntervalNanos;
    private final LongSupplier nanoClock;
    private long reservedHostMemoryBytes;
    private long sequence;

    public PriorityResourceScheduler(Map<ResourceId, Integer> capacities) {
        this(ComputeResourceBudget.legacy(capacities),
                DEFAULT_AGING_INTERVAL, System::nanoTime);
    }

    public PriorityResourceScheduler(Map<ResourceId, Integer> capacities,
                                     Duration agingInterval,
                                     LongSupplier nanoClock) {
        this(ComputeResourceBudget.legacy(capacities), agingInterval, nanoClock);
    }

    public PriorityResourceScheduler(ComputeResourceBudget budget) {
        this(budget, DEFAULT_AGING_INTERVAL, System::nanoTime);
    }

    public PriorityResourceScheduler(ComputeResourceBudget budget,
                                     Duration agingInterval,
                                     LongSupplier nanoClock) {
        this.budget = java.util.Objects.requireNonNullElseGet(
                budget, ComputeResourceBudget::safeDefaults);
        LinkedHashMap<ResourceId, Integer> configured = new LinkedHashMap<>();
        this.budget.legacyCapacities().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> configured.put(entry.getKey(),
                        Math.max(1, entry.getValue())));
        this.capacities = Map.copyOf(configured);
        this.available = new LinkedHashMap<>(configured);
        this.agingIntervalNanos = Math.max(1L,
                (agingInterval == null ? DEFAULT_AGING_INTERVAL
                        : agingInterval).toNanos());
        this.nanoClock = java.util.Objects.requireNonNullElse(
                nanoClock, System::nanoTime);
    }

    public static PriorityResourceScheduler safeDefaults() {
        return new PriorityResourceScheduler(ComputeResourceBudget.safeDefaults());
    }

    public static PriorityResourceScheduler incrementalReaderDefaults() {
        return new PriorityResourceScheduler(
                ComputeResourceBudget.incrementalReaderDefaults());
    }

    @Override
    public ResourceLease acquire(ComputeAdmissionRequest request)
            throws InterruptedException {
        ComputeAdmissionRequest admission = java.util.Objects.requireNonNull(request);
        if (empty(admission.demand())) return ResourceLease.NONE;
        validateDemand(admission.demand());
        Pending pending;
        synchronized (monitor) {
            pending = new Pending(admission, sequence++, nanoClock.getAsLong(),
                    new AtomicBoolean(), new java.util.concurrent.atomic.AtomicLong());
            queued.add(pending);
            monitor.notifyAll();
            try {
                while (true) {
                    admission.cancellation().throwIfCancellationRequested();
                    if (pending.cancelRequested().get()) {
                        throw new InterruptedException("compute admission cancelled");
                    }
                    FitResult fit = fit(admission.demand());
                    if (admission.deadline().expired()) {
                        throw new ResourceAdmissionTimeoutException(
                                admission.admissionId(), admission.operationId(),
                                fit.reason());
                    }
                    if (eligible(pending, fit) && fit.fits()) {
                        recordBypasses(pending);
                        queued.remove(pending);
                        reserve(admission.demand());
                        Active granted = new Active(pending, nanoClock.getAsLong());
                        active.put(admission.admissionId(), granted);
                        return lease(granted);
                    }
                    logWaitIfDue(pending, fit);
                    long waitMillis = admission.deadline().bounded()
                            ? Math.min(100L, admission.deadline().remainingMillis())
                            : 100L;
                    if (waitMillis <= 0L) continue;
                    monitor.wait(waitMillis);
                }
            } catch (InterruptedException cancelled) {
                queued.remove(pending);
                monitor.notifyAll();
                throw cancelled;
            }
        }
    }

    @Override
    public ComputeQueueSnapshot snapshot() {
        synchronized (monitor) {
            long now = nanoClock.getAsLong();
            List<ComputeQueueSnapshot.Entry> activeEntries = active.values().stream()
                    .map(value -> entry(value.pending(), now, true)).toList();
            List<ComputeQueueSnapshot.Entry> queuedEntries = queued.stream()
                    .sorted(orderAt(now)).map(value -> entry(value, now, false)).toList();
            LinkedHashMap<ComputeDeviceId, ComputeQueueSnapshot.DeviceReservation>
                    devices = new LinkedHashMap<>();
            deviceUsage.forEach((id, usage) -> devices.put(id,
                    new ComputeQueueSnapshot.DeviceReservation(
                            usage.reservedVramBytes, usage.gpuComputeUnits,
                            usage.encoderUnits, usage.activeConsumers)));
            List<ComputeQueueSnapshot.ModelResidency> models = residencies.values()
                    .stream().sorted(Comparator.comparing(value -> value.demand.key()))
                    .map(value -> new ComputeQueueSnapshot.ModelResidency(
                            value.demand.key(), value.demand.hostMemoryBytes(),
                            value.demand.vramBytes(), value.activeConsumers,
                            value.runtimeConfirmed, value.unloadPending)).toList();
            return new ComputeQueueSnapshot(capacities, available,
                    activeEntries, queuedEntries, budget,
                    reservedHostMemoryBytes, devices, models);
        }
    }

    @Override
    public boolean cancel(String admissionId) {
        String normalized = admissionId == null ? "" : admissionId.strip();
        if (normalized.isBlank()) return false;
        synchronized (monitor) {
            for (Pending pending : List.copyOf(queued)) {
                if (pending.request().admissionId().equals(normalized)) {
                    pending.cancelRequested().set(true);
                    queued.remove(pending);
                    monitor.notifyAll();
                    return true;
                }
            }
            Active granted = active.get(normalized);
            if (granted == null) return false;
            granted.pending().cancelRequested().set(true);
            monitor.notifyAll();
            return true;
        }
    }

    private ResourceLease lease(Active granted) {
        AtomicBoolean closed = new AtomicBoolean();
        return new ResourceLease() {
            @Override public boolean yieldRequested() {
                synchronized (monitor) {
                    if (granted.pending().cancelRequested().get()) return true;
                    long now = nanoClock.getAsLong();
                    ComputeJobPriority current = effectivePriority(
                            granted.pending(), now);
                    return queued.stream().filter(item -> item.request().demand()
                                    .conflictsWith(granted.pending().request().demand()))
                            .anyMatch(item -> effectivePriority(item, now).rank()
                                    < current.rank());
                }
            }

            @Override public boolean cancellationRequested() {
                return granted.pending().cancelRequested().get();
            }

            @Override public String admissionId() {
                return granted.pending().request().admissionId();
            }

            @Override public void confirmModelResident() {
                synchronized (monitor) {
                    ModelResidencyDemand demand = granted.pending().request()
                            .demand().modelResidency();
                    if (demand == null) return;
                    MutableResidency state = residencies.get(demand.key());
                    if (state != null) {
                        state.runtimeConfirmed = true;
                        state.unloadPending = false;
                    }
                }
            }

            @Override public void confirmModelUnloaded() {
                synchronized (monitor) {
                    ModelResidencyDemand demand = granted.pending().request()
                            .demand().modelResidency();
                    if (demand == null) return;
                    MutableResidency state = residencies.get(demand.key());
                    if (state == null) return;
                    state.runtimeConfirmed = false;
                    state.unloadPending = true;
                    removeResidencyIfUnused(state);
                }
            }

            @Override public void close() {
                if (!closed.compareAndSet(false, true)) return;
                synchronized (monitor) {
                    if (active.remove(granted.pending().request().admissionId()) != null) {
                        release(granted.pending().request().demand());
                    }
                    monitor.notifyAll();
                }
            }
        };
    }

    private boolean eligible(Pending candidate, FitResult candidateFit) {
        if (!candidateFit.fits()) return false;
        long now = nanoClock.getAsLong();
        Comparator<Pending> order = orderAt(now);
        return queued.stream().filter(other -> other != candidate)
                .filter(other -> other.request().demand()
                        .conflictsWith(candidate.request().demand()))
                .filter(other -> fit(other.request().demand()).fits())
                .noneMatch(other -> order.compare(other, candidate) < 0);
    }

    private Comparator<Pending> orderAt(long now) {
        return Comparator.comparingInt((Pending item) ->
                        effectivePriority(item, now).rank())
                .thenComparingLong(Pending::sequence);
    }

    private ComputeJobPriority effectivePriority(Pending pending, long now) {
        ComputeJobPriority priority = pending.request().priority();
        long promotions = Math.max(0L, now - pending.enqueuedNanos())
                / agingIntervalNanos;
        while (promotions-- > 0
                && priority.rank() > ComputeJobPriority.INTERACTIVE_ANALYSIS.rank()) {
            priority = priority.agedOneLevel();
        }
        return priority;
    }

    private ComputeQueueSnapshot.Entry entry(Pending pending, long now,
                                             boolean activeEntry) {
        FitResult fit = activeEntry ? FitResult.fit()
                : fit(pending.request().demand());
        return new ComputeQueueSnapshot.Entry(
                pending.request().admissionId(), pending.request().operationId(),
                pending.request().priority(), effectivePriority(pending, now),
                pending.request().workload(), pending.request().demand(),
                Duration.ofNanos(Math.max(0L, now - pending.enqueuedNanos())),
                fit.fits(), fit.reason(), fit.details(),
                pending.bypassCount().get(),
                pending.request().deadline().remainingMillis());
    }

    private FitResult fit(ComputeResourceDemand demand) {
        for (Map.Entry<ResourceId, Integer> entry : demand.units().entrySet()) {
            int free = available.getOrDefault(entry.getKey(), 0);
            if (free < entry.getValue()) {
                return FitResult.notFit(entry.getKey().value()
                                .replace('-', '_').toUpperCase(java.util.Locale.ROOT)
                                + "_CAPACITY",
                        Map.of("requiredUnits", (long) entry.getValue(),
                                "availableUnits", (long) free));
            }
        }
        ModelResidencyDemand newResidency = demand.modelResidency() != null
                && !residencies.containsKey(demand.modelResidency().key())
                ? demand.modelResidency() : null;
        long requestedHost = demand.estimatedHostMemoryBytes()
                + (newResidency == null ? 0L : newResidency.hostMemoryBytes());
        if (reservedHostMemoryBytes + requestedHost
                > budget.hostMemoryBudgetBytes()) {
            return FitResult.notFit("HOST_MEMORY_BUDGET", Map.of(
                    "requiredBytes", requestedHost,
                    "reservedBytes", reservedHostMemoryBytes,
                    "projectedBytes", reservedHostMemoryBytes + requestedHost,
                    "budgetBytes", budget.hostMemoryBudgetBytes()));
        }
        ComputeDeviceBudget deviceBudget = budget.deviceBudget(demand.device());
        MutableDeviceUsage usage = deviceUsage.getOrDefault(
                demand.device(), new MutableDeviceUsage());
        long requestedVram = demand.estimatedVramBytes()
                + (newResidency == null ? 0L : newResidency.vramBytes());
        if (usage.reservedVramBytes + requestedVram
                > deviceBudget.vramBudgetBytes()) {
            return FitResult.notFit("VRAM_BUDGET", Map.of(
                    "requiredBytes", requestedVram,
                    "reservedBytes", usage.reservedVramBytes,
                    "projectedBytes", usage.reservedVramBytes + requestedVram,
                    "budgetBytes", deviceBudget.vramBudgetBytes()));
        }
        if (usage.gpuComputeUnits + demand.gpuComputeUnits()
                > deviceBudget.gpuComputeUnits()) {
            return FitResult.notFit("GPU_COMPUTE_CAPACITY", Map.of(
                    "requiredUnits", (long) demand.gpuComputeUnits(),
                    "reservedUnits", (long) usage.gpuComputeUnits,
                    "capacityUnits", (long) deviceBudget.gpuComputeUnits()));
        }
        EncoderResourceDemand encoder = demand.encoder();
        if (encoder.units() > 0) {
            MutableDeviceUsage encoderUsage = deviceUsage.getOrDefault(
                    encoder.device(), new MutableDeviceUsage());
            int used = encoderUsage.encoderUnits.getOrDefault(encoder.kind(), 0);
            int capacity = budget.deviceBudget(encoder.device()).encoderUnits()
                    .getOrDefault(encoder.kind(), budget.fallbackEncoderUnits());
            if (used + encoder.units() > capacity) {
                return FitResult.notFit("ENCODER_CAPACITY", Map.of(
                        "requiredUnits", (long) encoder.units(),
                        "reservedUnits", (long) used,
                        "capacityUnits", (long) capacity));
            }
        }
        return FitResult.fit();
    }

    private void recordBypasses(Pending admitted) {
        long now = nanoClock.getAsLong();
        Comparator<Pending> order = orderAt(now);
        queued.stream().filter(blocked -> blocked != admitted)
                .filter(blocked -> order.compare(blocked, admitted) < 0)
                .filter(blocked -> blocked.request().demand()
                        .conflictsWith(admitted.request().demand()))
                .forEach(blocked -> {
                    FitResult result = fit(blocked.request().demand());
                    if (result.fits()) return;
                    long count = blocked.bypassCount().incrementAndGet();
                    LOGGER.log(System.Logger.Level.INFO,
                            "SCHEDULER_BYPASS blockedRequest={0} blockedReason={1} "
                                    + "blockedDetails={2} admittedRequest={3} bypassCount={4} "
                                    + "reason=BLOCKED_REQUEST_CURRENTLY_INELIGIBLE",
                            blocked.request().operationId(), result.reason(),
                            result.details(), admitted.request().operationId(), count);
                });
    }

    private void logWaitIfDue(Pending pending, FitResult result) {
        long now = nanoClock.getAsLong();
        long previous = pending.lastLogNanos;
        if (now - previous < WAIT_LOG_INTERVAL_NANOS) return;
        pending.lastLogNanos = now;
        List<Pending> ordered = queued.stream().sorted(orderAt(now)).toList();
        int queuePosition = ordered.indexOf(pending) + 1;
        LOGGER.log(System.Logger.Level.INFO,
                "SCHEDULER_WAIT operationId={0} requestId={1} priority={2} waitingMs={3} "
                        + "fits={4} fitFailureReason={5} fitDetails={6} demand={7} "
                        + "currentAvailable={8} reservedHostBytes={9} devices={10} "
                        + "residencies={11} queuePosition={12} bypassCount={13} "
                        + "deadlineRemainingMs={14}",
                pending.request().operationId(), pending.request().admissionId(),
                pending.request().priority(),
                Duration.ofNanos(Math.max(0L, now - pending.enqueuedNanos())).toMillis(),
                result.fits(), result.reason(), result.details(),
                pending.request().demand(), available, reservedHostMemoryBytes,
                deviceUsage, residencies.keySet(), queuePosition,
                pending.bypassCount().get(),
                pending.request().deadline().remainingMillis());
    }

    private void reserve(ComputeResourceDemand demand) {
        demand.units().forEach((resource, units) -> available.compute(resource,
                (ignored, current) -> java.util.Objects.requireNonNullElse(current, 0) - units));
        ModelResidencyDemand residencyDemand = demand.modelResidency();
        if (residencyDemand != null) {
            MutableResidency residency = residencies.get(residencyDemand.key());
            if (residency == null) {
                residency = new MutableResidency(residencyDemand);
                residencies.put(residencyDemand.key(), residency);
                reservedHostMemoryBytes += residencyDemand.hostMemoryBytes();
                usage(residencyDemand.key().device()).reservedVramBytes +=
                        residencyDemand.vramBytes();
            }
            residency.activeConsumers++;
            residency.unloadPending = false;
        }
        reservedHostMemoryBytes += demand.estimatedHostMemoryBytes();
        MutableDeviceUsage device = usage(demand.device());
        device.reservedVramBytes += demand.estimatedVramBytes();
        device.gpuComputeUnits += demand.gpuComputeUnits();
        if (physicalConsumer(demand)) device.activeConsumers++;
        EncoderResourceDemand encoder = demand.encoder();
        if (encoder.units() > 0) usage(encoder.device()).encoderUnits.merge(
                encoder.kind(), encoder.units(), Integer::sum);
    }

    private void release(ComputeResourceDemand demand) {
        demand.units().forEach((resource, units) -> available.compute(resource,
                (ignored, current) -> Math.min(capacities.getOrDefault(resource, units),
                        java.util.Objects.requireNonNullElse(current, 0) + units)));
        reservedHostMemoryBytes = Math.max(0L,
                reservedHostMemoryBytes - demand.estimatedHostMemoryBytes());
        MutableDeviceUsage device = usage(demand.device());
        device.reservedVramBytes = Math.max(0L,
                device.reservedVramBytes - demand.estimatedVramBytes());
        device.gpuComputeUnits = Math.max(0,
                device.gpuComputeUnits - demand.gpuComputeUnits());
        if (physicalConsumer(demand)) {
            device.activeConsumers = Math.max(0, device.activeConsumers - 1);
        }
        EncoderResourceDemand encoder = demand.encoder();
        if (encoder.units() > 0) usage(encoder.device()).encoderUnits.compute(
                encoder.kind(), (ignored, current) -> Math.max(0,
                        java.util.Objects.requireNonNullElse(current, 0) - encoder.units()));
        ModelResidencyDemand residencyDemand = demand.modelResidency();
        if (residencyDemand != null) {
            MutableResidency residency = residencies.get(residencyDemand.key());
            if (residency != null) {
                residency.activeConsumers = Math.max(0, residency.activeConsumers - 1);
                removeResidencyIfUnused(residency);
            }
        }
        pruneUsage();
    }

    private void removeResidencyIfUnused(MutableResidency residency) {
        if (residency.activeConsumers > 0
                || (residency.runtimeConfirmed && !residency.unloadPending)) return;
        if (residencies.remove(residency.demand.key()) == null) return;
        reservedHostMemoryBytes = Math.max(0L, reservedHostMemoryBytes
                - residency.demand.hostMemoryBytes());
        MutableDeviceUsage usage = usage(residency.demand.key().device());
        usage.reservedVramBytes = Math.max(0L, usage.reservedVramBytes
                - residency.demand.vramBytes());
    }

    private void validateDemand(ComputeResourceDemand demand) {
        demand.units().forEach((resource, units) -> {
            int capacity = capacities.getOrDefault(resource, 1);
            if (units > capacity) throw new IllegalArgumentException(
                    "resource demand exceeds configured capacity: " + resource.value());
            synchronized (monitor) { available.putIfAbsent(resource, capacity); }
        });
        long host = demand.estimatedHostMemoryBytes()
                + (demand.modelResidency() == null ? 0L
                : demand.modelResidency().hostMemoryBytes());
        if (host > budget.hostMemoryBudgetBytes()) {
            throw new IllegalArgumentException("host memory demand exceeds budget");
        }
        long vram = demand.estimatedVramBytes()
                + (demand.modelResidency() == null ? 0L
                : demand.modelResidency().vramBytes());
        if (vram > budget.deviceBudget(demand.device()).vramBudgetBytes()) {
            throw new IllegalArgumentException("VRAM demand exceeds device budget: "
                    + demand.device());
        }
    }

    private MutableDeviceUsage usage(ComputeDeviceId device) {
        return deviceUsage.computeIfAbsent(device,
                ignored -> new MutableDeviceUsage());
    }

    private void pruneUsage() {
        deviceUsage.entrySet().removeIf(entry -> entry.getValue().empty());
    }

    private static boolean physicalConsumer(ComputeResourceDemand demand) {
        return demand.estimatedVramBytes() > 0L || demand.gpuComputeUnits() > 0
                || demand.encoder().units() > 0;
    }

    private static boolean empty(ComputeResourceDemand demand) {
        return demand.units().isEmpty() && demand.estimatedHostMemoryBytes() == 0L
                && demand.estimatedVramBytes() == 0L
                && demand.gpuComputeUnits() == 0
                && demand.modelResidency() == null
                && demand.encoder().units() == 0;
    }

    private static final class Pending {
        private final ComputeAdmissionRequest request;
        private final long sequence;
        private final long enqueuedNanos;
        private final AtomicBoolean cancelRequested;
        private final java.util.concurrent.atomic.AtomicLong bypassCount;
        private long lastLogNanos;

        private Pending(ComputeAdmissionRequest request, long sequence,
                        long enqueuedNanos, AtomicBoolean cancelRequested,
                        java.util.concurrent.atomic.AtomicLong bypassCount) {
            this.request = request;
            this.sequence = sequence;
            this.enqueuedNanos = enqueuedNanos;
            this.cancelRequested = cancelRequested;
            this.bypassCount = bypassCount;
            this.lastLogNanos = enqueuedNanos;
        }
        private ComputeAdmissionRequest request() { return request; }
        private long sequence() { return sequence; }
        private long enqueuedNanos() { return enqueuedNanos; }
        private AtomicBoolean cancelRequested() { return cancelRequested; }
        private java.util.concurrent.atomic.AtomicLong bypassCount() { return bypassCount; }
    }
    private record Active(Pending pending, long grantedNanos) { }

    private record FitResult(boolean fits, String reason,
                             Map<String, Long> details) {
        private static FitResult fit() {
            return new FitResult(true, "FIT", Map.of());
        }
        private static FitResult notFit(String reason, Map<String, Long> details) {
            return new FitResult(false, reason, details);
        }
    }

    private static final class MutableDeviceUsage {
        private long reservedVramBytes;
        private int gpuComputeUnits;
        private int activeConsumers;
        private final EnumMap<VideoEncoderKind, Integer> encoderUnits =
                new EnumMap<>(VideoEncoderKind.class);
        private boolean empty() {
            return reservedVramBytes == 0L && gpuComputeUnits == 0
                    && activeConsumers == 0
                    && encoderUnits.values().stream().allMatch(value -> value == 0);
        }
    }

    private static final class MutableResidency {
        private final ModelResidencyDemand demand;
        private int activeConsumers;
        private boolean runtimeConfirmed;
        private boolean unloadPending;
        private MutableResidency(ModelResidencyDemand demand) {
            this.demand = demand;
        }
    }
}
