package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisOperation;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/** Central, engine-neutral hardware/capability detection for the PDF pipeline. */
public final class PdfProcessingCapabilityService {
    private static final long LIMITED_MEMORY_BYTES = 12L * 1024 * 1024 * 1024;
    private final MediaCapabilityService media;
    private final BooleanSupplier ocrAvailable;
    private final LongSupplier memoryBytes;

    public PdfProcessingCapabilityService(
            MediaCapabilityService media, BooleanSupplier ocrAvailable) {
        this(media, ocrAvailable, () -> Runtime.getRuntime().maxMemory());
    }

    PdfProcessingCapabilityService(
            MediaCapabilityService media, BooleanSupplier ocrAvailable,
            LongSupplier memoryBytes) {
        this.media = Objects.requireNonNull(media, "media");
        this.ocrAvailable = ocrAvailable == null ? () -> false : ocrAvailable;
        this.memoryBytes = memoryBytes == null
                ? () -> LIMITED_MEMORY_BYTES : memoryBytes;
    }

    public PdfProcessingCapabilityProfile detect() {
        EnumSet<ContentAnalysisOperation> operations =
                EnumSet.noneOf(ContentAnalysisOperation.class);
        media.platform().contentAnalysisEngines().engines()
                .forEach(engine -> operations.addAll(engine.operations()));
        return profile(operations, safeOcr(), Math.max(0, memoryBytes.getAsLong()));
    }

    static PdfProcessingCapabilityProfile profile(
            Set<ContentAnalysisOperation> operations,
            boolean ocrAvailable, long memoryBytes) {
        Set<ContentAnalysisOperation> safe = operations == null
                ? Set.of() : Set.copyOf(operations);
        boolean limited = memoryBytes < LIMITED_MEMORY_BYTES;
        return new PdfProcessingCapabilityProfile(
                limited
                        ? PdfProcessingCapabilityProfile.HardwareClass.LIMITED
                        : PdfProcessingCapabilityProfile.HardwareClass.STANDARD,
                ocrAvailable,
                safe.contains(ContentAnalysisOperation.IMAGE_DESCRIPTION),
                safe.contains(ContentAnalysisOperation.TABLE_STRUCTURE_RECOGNITION),
                safe.contains(ContentAnalysisOperation.MATH_RECOGNITION),
                safe.contains(ContentAnalysisOperation.MATH_SPEECH),
                limited ? 1 : 2,
                limited ? 1 : 3,
                limited);
    }

    private boolean safeOcr() {
        try { return ocrAvailable.getAsBoolean(); }
        catch (RuntimeException unavailable) { return false; }
    }
}
