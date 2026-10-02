package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisOperation;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisResult;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF client for the shared context-analysis capability. The adapter remains
 * product-neutral; this class assembles regions and protects source facts.
 */
public final class TransversalContextCorrectionPdfTreatmentEngine
        implements PdfDerivedTreatmentEngine {
    public static final String ID = "pdf-via-transversal-context-correction";
    private static final Pattern PROTECTED_TOKEN = Pattern.compile(
            "(?iu)(?:\\b\\d+(?:[.,]\\d+)?(?:\\s?(?:%|cm|mm|m|km|kg|g|s|ms|hz|v|a))?\\b)"
                    + "|(?:\\b[A-ZÁÉÍÓÚÑ][\\p{L}'’-]{2,}\\b)"
                    + "|(?:[=+\\-×÷<>≤≥∑√∞π])");
    private final MediaCapabilityService media;

    public TransversalContextCorrectionPdfTreatmentEngine(MediaCapabilityService media) {
        this.media = java.util.Objects.requireNonNull(media, "media");
    }

    @Override public String id() { return ID; }
    @Override public String version() { return "1.0"; }
    @Override public PdfDerivedTreatmentKind kind() {
        return PdfDerivedTreatmentKind.CONTEXTUAL_CORRECTION;
    }

    @Override
    public PdfDerivedTreatment generate(PreparedPdfPage page,
                                        List<PdfRegion> sourceRegions,
                                        PdfDerivedTreatmentGenerationRequest request)
            throws IOException, InterruptedException {
        String source = sourceRegions.stream().map(PdfRegion::effectiveText)
                .filter(value -> !value.isBlank())
                .reduce("", (left, right) -> (left + "\n" + right).strip());
        if (source.isBlank()) {
            throw new IOException("La corrección contextual requiere texto de origen.");
        }
        String nearby = request.options().getOrDefault("nearbyContext", source);
        String language = request.options().getOrDefault(
                "language", page.analysisProfile().language().effectiveLanguage());
        String instruction = request.options().getOrDefault("instruction",
                "Propón una corrección mínima del texto dañado. Conserva todo el contenido, "
                        + "las cifras, unidades, nombres, variables y operadores.");
        ContentAnalysisResult result = media.analyzeContent(
                engineId(request.engineId()),
                new ContentAnalysisRequest(ContentAnalysisOperation.CONTEXT_CORRECTION,
                        List.of(), instruction, nearby, language, "", request.options()),
                ExecutionContext.defaults("pdf-context-correction-page-" + page.pageNumber()));
        String corrected = result.text();
        if (corrected.isBlank()) {
            throw new IOException("El motor local no devolvió una propuesta de corrección.");
        }
        Set<String> missing = new LinkedHashSet<>(protectedTokens(source));
        missing.removeAll(protectedTokens(corrected));
        if (!missing.isEmpty()) {
            throw new IOException("La propuesta se rechazó porque alteraba contenido protegido: "
                    + String.join(", ", missing) + ".");
        }
        long sourceRevision = sourceRegions.stream().mapToLong(PdfRegion::revision)
                .max().orElse(1L);
        String fingerprint = fingerprint(page.pageNumber(), sourceRegions, nearby);
        String stable = page.pageNumber() + "|" + String.join("|", request.sourceRegionIds())
                + "|" + fingerprint;
        String id = "PDF-DER-" + UUID.nameUUIDFromBytes(stable.getBytes(StandardCharsets.UTF_8));
        return new PdfDerivedTreatment(id, kind(), request.sourceRegionIds(), corrected,
                result.diagnostics().getOrDefault("model", "qwen3-vl"),
                version(), result.confidence(), Instant.now(), PdfDerivedTreatmentState.DRAFT,
                sourceRevision, fingerprint, instruction,
                Map.of("local", "true",
                        "structuredJson", result.structuredJson(),
                        "engineId", result.diagnostics().getOrDefault("engineId", ""),
                        "protectedTokenCount",
                        Integer.toString(protectedTokens(source).size())));
    }

    static List<String> protectedTokens(String value) {
        ArrayList<String> result = new ArrayList<>();
        Matcher matcher = PROTECTED_TOKEN.matcher(value == null ? "" : value);
        while (matcher.find()) result.add(matcher.group().strip());
        return result.stream().distinct().toList();
    }

    private static EngineId engineId(String value) {
        return value == null || value.isBlank() || ID.equals(value)
                ? null : new EngineId(value);
    }

    private static String fingerprint(int page, List<PdfRegion> regions, String context) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Integer.toString(page).getBytes(StandardCharsets.UTF_8));
            for (PdfRegion region : regions) {
                digest.update(region.id().getBytes(StandardCharsets.UTF_8));
                digest.update(Long.toString(region.revision()).getBytes(StandardCharsets.UTF_8));
                digest.update(region.effectiveText().getBytes(StandardCharsets.UTF_8));
            }
            digest.update(context.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
