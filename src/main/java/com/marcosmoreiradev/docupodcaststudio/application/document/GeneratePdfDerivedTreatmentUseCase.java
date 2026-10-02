package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Executes, audits and safely resumes one explicitly requested PDF derivative. */
public final class GeneratePdfDerivedTreatmentUseCase {
    private final PreparedPdfDocumentRepository repository;
    private final PdfDerivedTreatmentEngineRegistry engines;
    private final UpdatePdfDerivedTreatmentUseCase update;
    private final PdfOperationAttemptRepository attempts;

    public GeneratePdfDerivedTreatmentUseCase(
            PreparedPdfDocumentRepository repository,
            PdfDerivedTreatmentEngineRegistry engines) {
        this(repository, engines, PdfOperationAttemptRepository.disabled());
    }

    public GeneratePdfDerivedTreatmentUseCase(
            PreparedPdfDocumentRepository repository,
            PdfDerivedTreatmentEngineRegistry engines,
            PdfOperationAttemptRepository attempts) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.engines = Objects.requireNonNull(engines, "engines");
        this.update = new UpdatePdfDerivedTreatmentUseCase(repository);
        this.attempts = Objects.requireNonNullElseGet(
                attempts, PdfOperationAttemptRepository::disabled);
    }

    public PdfDerivedTreatment execute(PdfDerivedTreatmentGenerationRequest request)
            throws IOException {
        Objects.requireNonNull(request, "request");
        if (!request.explicitlyEnabled()) {
            throw new IllegalStateException(
                    "El tratamiento PDF requiere activación explícita.");
        }
        PreparedPdfPage page = repository.loadPage(
                        request.projectRoot(), request.pageNumber())
                .orElseThrow(() -> new IOException(
                        "La página PDF aún no está preparada."));
        Set<String> requestedIds;
        if (request.kind() == PdfDerivedTreatmentKind.NARRATABILITY_REVIEW) {
            requestedIds = request.sourceRegionIds().isEmpty()
                    ? page.regions().stream()
                    .filter(region -> PdfRegionReviewStatus.requiresReview(page, region))
                    .map(PdfRegion::id).collect(Collectors.toSet())
                    : Set.copyOf(request.sourceRegionIds());
        } else {
            requestedIds = Set.copyOf(request.sourceRegionIds());
        }
        if (requestedIds.isEmpty()) {
            throw new IOException("No hay regiones dudosas pendientes de revisión.");
        }
        List<PdfRegion> regions = page.regions().stream()
                .filter(region -> requestedIds.contains(region.id())).toList();
        Set<String> foundIds = regions.stream().map(PdfRegion::id)
                .collect(Collectors.toSet());
        if (!foundIds.containsAll(requestedIds)) {
            throw new IOException(
                    "Una o más regiones fuente ya no existen en la página.");
        }
        boolean forceRegenerate = Boolean.parseBoolean(
                request.options().getOrDefault("forceRegenerate", "false"));
        if (request.kind() == PdfDerivedTreatmentKind.NARRATABILITY_REVIEW
                && !forceRegenerate) {
            long sourceRevision = regions.stream().mapToLong(PdfRegion::revision)
                    .max().orElse(1L);
            Set<String> regionIds = regions.stream().map(PdfRegion::id)
                    .collect(Collectors.toSet());
            var cached = page.derivedTreatments().stream()
                    .filter(value -> value.kind() == request.kind())
                    .filter(value -> value.sourceRevision() == sourceRevision)
                    .filter(value -> Set.copyOf(value.sourceRegionIds()).equals(regionIds))
                    .max(java.util.Comparator.comparing(
                            PdfDerivedTreatment::createdAt));
            if (cached.isPresent()) return cached.get();
        }

        String evidenceFingerprint = fingerprint(regions.stream()
                .map(region -> region.id() + "|" + region.revision() + "|"
                        + region.evidence().origin() + "|"
                        + region.evidence().extractorVersion()).sorted().toList());
        String parameterFingerprint = fingerprint(parameters(request));
        String operationKey = fingerprint(List.of(
                Integer.toString(request.pageNumber()), request.kind().name(),
                evidenceFingerprint, parameterFingerprint));
        if (!forceRegenerate) {
            PdfDerivedTreatment resumed = reusableAttempt(
                    request, page, operationKey);
            if (resumed != null) return resumed;
        }

        PdfDerivedTreatmentEngine engine = engines.resolve(
                        request.kind(), request.engineId())
                .orElseThrow(() -> new IllegalStateException(
                        "No hay un motor local preparado para "
                                + request.kind() + "."));
        Instant startedAt = Instant.now();
        long startedNanos = System.nanoTime();
        long startedCpu = cpuNanos();
        long startedRam = usedHeapBytes();
        PdfDerivedTreatment treatment;
        try {
            treatment = engine.generate(page, regions, request)
                    .withState(PdfDerivedTreatmentState.DRAFT);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            saveAttempt(request, operationKey, evidenceFingerprint,
                    parameterFingerprint, startedAt, startedNanos, startedCpu,
                    startedRam, engine.id(), null,
                    PdfOperationAttemptState.CANCELLED,
                    "La operación fue interrumpida.");
            throw new IOException(
                    "La generación del derivado fue cancelada.", interrupted);
        } catch (IOException failure) {
            saveAttempt(request, operationKey, evidenceFingerprint,
                    parameterFingerprint, startedAt, startedNanos, startedCpu,
                    startedRam, engine.id(), null,
                    PdfOperationAttemptState.FAILED, failure.getMessage());
            throw failure;
        } catch (RuntimeException failure) {
            saveAttempt(request, operationKey, evidenceFingerprint,
                    parameterFingerprint, startedAt, startedNanos, startedCpu,
                    startedRam, engine.id(), null,
                    PdfOperationAttemptState.FAILED, failure.getMessage());
            throw failure;
        }
        try {
            update.upsert(request.projectRoot(), request.pageNumber(), treatment);
        } catch (IOException failure) {
            saveAttempt(request, operationKey, evidenceFingerprint,
                    parameterFingerprint, startedAt, startedNanos, startedCpu,
                    startedRam, engine.id(), treatment,
                    PdfOperationAttemptState.FAILED, failure.getMessage());
            throw failure;
        }
        String grounding = treatment.metadata().getOrDefault(
                "groundingStatus",
                treatment.metadata().getOrDefault("status", ""));
        String doneReason = treatment.metadata().getOrDefault("doneReason", "");
        PdfOperationAttemptState state = "INSUFFICIENT_EVIDENCE"
                .equalsIgnoreCase(grounding)
                ? PdfOperationAttemptState.INSUFFICIENT_EVIDENCE
                : ("length".equalsIgnoreCase(doneReason)
                || "truncated".equalsIgnoreCase(doneReason))
                ? PdfOperationAttemptState.TRUNCATED
                : PdfOperationAttemptState.COMPLETED;
        saveAttempt(request, operationKey, evidenceFingerprint,
                parameterFingerprint, startedAt, startedNanos, startedCpu,
                startedRam, engine.id(), treatment, state, "");
        return treatment;
    }

    private PdfDerivedTreatment reusableAttempt(
            PdfDerivedTreatmentGenerationRequest request,
            PreparedPdfPage page, String operationKey) throws IOException {
        var reusable = attempts.list(request.projectRoot()).stream()
                .filter(value -> value.operationKey().equals(operationKey))
                .filter(value -> value.state() == PdfOperationAttemptState.COMPLETED
                        || value.state()
                        == PdfOperationAttemptState.INSUFFICIENT_EVIDENCE)
                .filter(value -> !value.treatmentId().isBlank())
                .max(java.util.Comparator.comparing(PdfOperationAttempt::finishedAt));
        if (reusable.isEmpty()) return null;
        return page.derivedTreatments().stream()
                .filter(value -> value.id().equals(reusable.get().treatmentId()))
                .findFirst().orElse(null);
    }

    private void saveAttempt(
            PdfDerivedTreatmentGenerationRequest request, String operationKey,
            String evidenceFingerprint, String parameterFingerprint,
            Instant startedAt, long startedNanos, long startedCpu,
            long startedRam, String engineId, PdfDerivedTreatment treatment,
            PdfOperationAttemptState state, String diagnostic)
            throws IOException {
        long cpu = cpuNanos();
        Map<String, String> metadata = treatment == null
                ? Map.of() : treatment.metadata();
        PdfOperationMetrics metrics = new PdfOperationMetrics(
                millis(metadata, "durationMs", System.nanoTime() - startedNanos),
                number(metadata, "ttftMs"), number(metadata, "promptTokens"),
                number(metadata, "outputTokens"),
                number(metadata, "contextChars"),
                Math.max(startedRam, usedHeapBytes()),
                number(metadata, "vramBytes"),
                startedCpu < 0 || cpu < 0 ? 0
                        : java.util.concurrent.TimeUnit.NANOSECONDS
                        .toMillis(Math.max(0, cpu - startedCpu)),
                integer(request.options().get("roiWidth")),
                integer(request.options().get("roiHeight")),
                engineId, treatment == null ? "" : treatment.modelId(),
                metadata.getOrDefault("doneReason", ""));
        String id = "PDF-ATTEMPT-" + java.util.UUID.randomUUID()
                .toString().replace("-", "");
        attempts.save(request.projectRoot(), new PdfOperationAttempt(
                PdfOperationAttempt.CURRENT_SCHEMA_VERSION, id, operationKey,
                request.pageNumber(), request.kind().name(),
                request.sourceRegionIds(), evidenceFingerprint,
                parameterFingerprint, state, startedAt, Instant.now(), metrics,
                treatment == null ? "" : treatment.id(), diagnostic));
    }

    private static List<String> parameters(
            PdfDerivedTreatmentGenerationRequest request) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("kind", request.kind().name());
        values.put("engine", request.engineId());
        request.options().entrySet().stream()
                .filter(entry -> !entry.getKey().equals("forceRegenerate"))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> values.put(entry.getKey(), entry.getValue()));
        return values.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue()).toList();
    }

    private static String fingerprint(List<String> values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            values.forEach(value -> digest.update(
                    value.getBytes(StandardCharsets.UTF_8)));
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static long millis(Map<String, String> metadata, String key,
                               long fallbackNanos) {
        long value = number(metadata, key);
        return value > 0 ? value : java.util.concurrent.TimeUnit.NANOSECONDS
                .toMillis(Math.max(0, fallbackNanos));
    }

    private static long number(Map<String, String> values, String key) {
        try { return Long.parseLong(values.getOrDefault(key, "0")); }
        catch (NumberFormatException invalid) { return 0; }
    }

    private static int integer(String value) {
        try { return Math.max(0, Integer.parseInt(value)); }
        catch (RuntimeException invalid) { return 0; }
    }

    private static long cpuNanos() {
        var bean = java.lang.management.ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported()
                ? bean.getCurrentThreadCpuTime() : -1;
    }

    private static long usedHeapBytes() {
        Runtime runtime = Runtime.getRuntime();
        return Math.max(0, runtime.totalMemory() - runtime.freeMemory());
    }
}
