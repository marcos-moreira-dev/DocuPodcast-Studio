package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.media.api.AnalysisVisualInput;
import com.marcosmoreiradev.docupodcaststudio.media.api.ComputeJobPriority;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisOperation;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.ContentAnalysisResult;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineId;
import com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * PDF adapter for the provider-neutral table-structure operation.
 *
 * <p>The PP-Structure payload is retained as a reviewable derivative. It never
 * replaces the region text, geometry, reading order, or automatic
 * narratability.</p>
 */
public final class TransversalPpTableStructurePdfTreatmentEngine
        implements PdfDerivedTreatmentEngine {
    public static final String ID = "pdf-table-structure-via-pp";
    private static final Pattern TABLE_OPERATION = Pattern.compile(
            "\"operation\"\\s*:\\s*\"table\"");
    private static final Pattern RESULTS = Pattern.compile(
            "\"results\"\\s*:\\s*\\[");
    private final MediaCapabilityService media;

    public TransversalPpTableStructurePdfTreatmentEngine(
            MediaCapabilityService media) {
        this.media = java.util.Objects.requireNonNull(media, "media");
    }

    @Override public String id() { return ID; }
    @Override public String version() { return "1.0"; }
    @Override public PdfDerivedTreatmentKind kind() {
        return PdfDerivedTreatmentKind.TABLE_STRUCTURE;
    }

    @Override
    public PdfDerivedTreatment generate(
            PreparedPdfPage page,
            List<PdfRegion> sourceRegions,
            PdfDerivedTreatmentGenerationRequest request)
            throws IOException, InterruptedException {
        Path roi = requiredImage(request.options().get("roiImage"));
        ContentAnalysisResult result = media.analyzeContent(
                optionalEngine(request.options().get("recognitionEngineId")),
                new ContentAnalysisRequest(
                        ContentAnalysisOperation.TABLE_STRUCTURE_RECOGNITION,
                        List.of(new AnalysisVisualInput(
                                roi, "table-roi", "image/png")),
                        "Reconoce únicamente la estructura y las celdas de la tabla visible.",
                        request.options().getOrDefault("nearbyContext", ""),
                        request.options().getOrDefault("language", "und"),
                        "",
                        Map.of("computePriority",
                                ComputeJobPriority.INTERACTIVE_ANALYSIS.name())),
                ExecutionContext.defaults(
                        "pdf-table-structure-page-" + page.pageNumber()));
        String structureJson = result.structuredJson();
        validateStructurePayload(structureJson);

        long revision = sourceRegions.stream().mapToLong(PdfRegion::revision)
                .max().orElse(1L);
        String fingerprint = fingerprint(
                page.pageNumber(), sourceRegions, structureJson);
        String id = "PDF-DER-" + UUID.nameUUIDFromBytes(
                (page.pageNumber() + "|" + fingerprint)
                        .getBytes(StandardCharsets.UTF_8));
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        metadata.put("local", "true");
        metadata.put("structuredJson", structureJson);
        metadata.put("engineId",
                result.diagnostics().getOrDefault("engineId", ""));
        metadata.put("requestedDevice",
                result.diagnostics().getOrDefault("requestedDevice", ""));
        metadata.put("effectiveDevice",
                result.diagnostics().getOrDefault("effectiveDevice", ""));
        metadata.put("canonicalTextModified", "false");
        metadata.put("requiresApproval", "true");
        metadata.put("roi", roi.toString());
        return new PdfDerivedTreatment(
                id, kind(), request.sourceRegionIds(),
                "Estructura avanzada de tabla reconocida. "
                        + "Revise las celdas antes de preparar su lectura.",
                result.diagnostics().getOrDefault(
                        "engineId", "pp-structure-v3-local"),
                version(), result.confidence(), Instant.now(),
                PdfDerivedTreatmentState.DRAFT, revision, fingerprint,
                "Reconocimiento local de estructura sobre el ROI renderizado.",
                metadata);
    }

    static void validateStructurePayload(String value) throws IOException {
        String json = value == null ? "" : value.strip();
        if (json.isBlank()
                || !TABLE_OPERATION.matcher(json).find()
                || !RESULTS.matcher(json).find()) {
            throw new IOException(
                    "PP-StructureV3 devolvió una estructura de tabla inválida.");
        }
    }

    private static Path requiredImage(String value) throws IOException {
        if (value == null || value.isBlank()) {
            throw new IOException(
                    "El reconocimiento de tabla requiere un recorte visual.");
        }
        Path file = Path.of(value).toAbsolutePath().normalize();
        if (!Files.isRegularFile(file)) {
            throw new IOException(
                    "No existe el recorte visual de la tabla: " + file);
        }
        return file;
    }

    private static EngineId optionalEngine(String value) {
        return value == null || value.isBlank()
                ? null : new EngineId(value);
    }

    private static String fingerprint(
            int page, List<PdfRegion> regions, String structureJson) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Integer.toString(page)
                    .getBytes(StandardCharsets.UTF_8));
            for (PdfRegion region : regions) {
                digest.update(region.id().getBytes(StandardCharsets.UTF_8));
                digest.update(Long.toString(region.revision())
                        .getBytes(StandardCharsets.UTF_8));
            }
            digest.update(structureJson.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
