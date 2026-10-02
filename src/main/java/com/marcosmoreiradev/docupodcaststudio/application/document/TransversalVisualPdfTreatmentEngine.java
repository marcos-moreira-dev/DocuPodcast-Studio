package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.media.api.AnalysisVisualInput;
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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Grounded PDF client: Qwen interprets one delimited ROI and never owns geometry. */
public final class TransversalVisualPdfTreatmentEngine implements PdfDerivedTreatmentEngine {
    public static final String ID = "pdf-via-transversal-visual-analysis";
    private static final String Q8_MODEL = "qwen3-vl:4b-instruct-q8_0";
    static final String GROUNDED_SCHEMA = """
            {"type":"object","properties":{"status":{"type":"string","enum":["OK","INSUFFICIENT_EVIDENCE"]},
            "objectId":{"type":"string"},"narrationText":{"type":"string"},"claims":{"type":"array","items":{
            "type":"object","properties":{"text":{"type":"string"},"evidenceIds":{"type":"array","items":{"type":"string"}}},
            "required":["text","evidenceIds"]}},"recognizedLabels":{"type":"array","items":{"type":"string"}},
            "uncertainties":{"type":"array","items":{"type":"string"}},"confidence":{"type":"number","minimum":0,"maximum":1}},
            "required":["status","objectId","narrationText","claims","recognizedLabels","uncertainties","confidence"]}
            """;

    private final MediaCapabilityService media;

    public TransversalVisualPdfTreatmentEngine(MediaCapabilityService media) {
        this.media = java.util.Objects.requireNonNull(media, "media");
    }

    @Override public String id() { return ID; }
    @Override public String version() { return "3.1-grounded-q8"; }
    @Override public PdfDerivedTreatmentKind kind() { return PdfDerivedTreatmentKind.IMAGE_DESCRIPTION; }

    @Override
    public PdfDerivedTreatment generate(PreparedPdfPage page, List<PdfRegion> sourceRegions,
                                        PdfDerivedTreatmentGenerationRequest request)
            throws IOException, InterruptedException {
        List<AnalysisVisualInput> visuals = visualInputs(request.options());
        if (visuals.isEmpty() || !"high-resolution-roi".equals(visuals.getFirst().role())) {
            throw new IOException("La descripción grounded requiere el ROI como evidencia principal.");
        }
        String objectId = request.options().getOrDefault("objectId", "").strip();
        if (objectId.isBlank()) throw new IOException("La descripción grounded requiere objectId.");
        String nearby = limited(request.options().getOrDefault("nearbyContext",
                sourceRegions.stream().map(PdfRegion::effectiveText).filter(value -> !value.isBlank())
                        .reduce("", (a, b) -> (a + "\n" + b).strip())), 1200);
        String language = request.options().getOrDefault(
                "language", page.analysisProfile().language().effectiveLanguage());
        String instruction = request.options().getOrDefault("instruction",
                "Describe exclusivamente el objeto " + objectId + " delimitado por ROI. "
                        + "Redacta entre dos y cuatro frases naturales en español, idealmente "
                        + "entre 45 y 90 palabras cuando la evidencia lo permita. Explica qué "
                        + "muestra, las relaciones visuales relevantes, sus etiquetas o leyenda "
                        + "y su función probable en el argumento del documento. El contexto "
                        + "cercano es solo una pista narrativa: la evidencia visual prevalece "
                        + "si existe cualquier contradicción. No resumas la página ni "
                        + "inventes detalles. "
                        + "Usa INSUFFICIENT_EVIDENCE si no puedes fundamentarlo.");
        LinkedHashMap<String, String> options = new LinkedHashMap<>(request.options());
        options.put("model", Q8_MODEL);
        options.put("objectId", objectId);
        options.put("groundingMode", "roi-primary");
        options.putIfAbsent("semanticKind", "IMAGE");
        options.putIfAbsent("maxOutputTokens", "320");
        ContentAnalysisResult result = media.analyzeContent(engineId(
                        request.options().getOrDefault("analysisEngineId", request.engineId())),
                new ContentAnalysisRequest(ContentAnalysisOperation.IMAGE_DESCRIPTION,
                        visuals, instruction, nearby, language, GROUNDED_SCHEMA, options),
                ExecutionContext.defaults("pdf-image-description-page-" + page.pageNumber()));

        String status = property(result.structuredJson(), "status");
        String returnedObject = property(result.structuredJson(), "objectId");
        ArrayList<String> failures = new ArrayList<>();
        if (!"OK".equals(status) && !"INSUFFICIENT_EVIDENCE".equals(status)) failures.add("missing-status");
        if (!objectId.equals(returnedObject)) failures.add("object-id-mismatch");
        String evidenceText = nearby + "\n" + request.options().getOrDefault("recognizedLabels", "");
        if (containsUnsupportedNumbers(result.text(), evidenceText)) failures.add("unsupported-number");
        if (looksGlobal(result.text())) failures.add("global-page-summary");
        boolean insufficient = "INSUFFICIENT_EVIDENCE".equals(status) || !failures.isEmpty() || result.text().isBlank();
        String derivedText = insufficient
                ? "Evidencia visual insuficiente para describir este objeto." : result.text();
        PdfSemanticNarrationSafetyValidator safety =
                new PdfSemanticNarrationSafetyValidator();
        boolean ttsSafe = !insufficient
                && safety.safeText(kind(), derivedText, "BRIEF_DESCRIPTION");
        if (!insufficient && !ttsSafe) {
            throw new com.marcosmoreiradev.docupodcaststudio.media.api.EngineExecutionException(
                    com.marcosmoreiradev.docupodcaststudio.media.api.EngineDiagnosticCode.INVALID_OUTPUT,
                    "La descripción visual no es apta para TTS.");
        }

        long sourceRevision = sourceRegions.stream().mapToLong(PdfRegion::revision).max().orElse(1L);
        String fingerprint = fingerprint(page.pageNumber(), sourceRegions, nearby,
                request.options().get("roiImage"));
        String stable = page.pageNumber() + "|" + objectId + "|" + fingerprint;
        String id = "PDF-DER-" + UUID.nameUUIDFromBytes(stable.getBytes(StandardCharsets.UTF_8));
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        metadata.put("local", "true");
        metadata.put("structuredJson", result.structuredJson());
        metadata.put("engineId", result.diagnostics().getOrDefault("engineId", ""));
        metadata.put("objectId", objectId);
        metadata.put("groundingStatus", insufficient ? "INSUFFICIENT_EVIDENCE" : "OK");
        metadata.put("groundingFailures", String.join(",", failures));
        metadata.put("visualInputCount", Integer.toString(visuals.size()));
        metadata.put("roiSha256", fileHash(Path.of(request.options().get("roiImage"))));
        String context = request.options().getOrDefault("contextImage", "");
        metadata.put("contextSha256", context.isBlank() ? "" : fileHash(Path.of(context)));
        metadata.put("schemaVersion", "grounded-visual-v1");
        metadata.put("semanticStrategy", "BRIEF_DESCRIPTION");
        metadata.put("semanticPolicy",
                request.options().getOrDefault("semanticPolicy", ""));
        metadata.put("ttsSafetyValidated", Boolean.toString(ttsSafe));
        metadata.put("ttsValidatorVersion", PdfSemanticNarrationSafetyValidator.VERSION);
        metadata.put("sourceFingerprintValidated", "true");
        metadata.put("automaticAdmission", ttsSafe
                ? PdfSemanticNarrationSafetyValidator.AUTOMATIC_ADMISSION : "");
        metadata.put("prompt", instruction);
        metadata.put("nearbyContextChars", Integer.toString(nearby.length()));
        metadata.put("numPredict", request.options().getOrDefault("maxOutputTokens", "220"));
        for (String key : List.of("model", "durationMs", "doneReason", "promptTokens", "outputTokens")) {
            metadata.put(key, result.diagnostics().getOrDefault(key, ""));
        }
        return new PdfDerivedTreatment(id, kind(), request.sourceRegionIds(), derivedText,
                Q8_MODEL, version(), result.confidence(),
                Instant.now(), PdfDerivedTreatmentState.DRAFT, sourceRevision, fingerprint,
                instruction, metadata);
    }

    private static List<AnalysisVisualInput> visualInputs(Map<String, String> options) {
        ArrayList<AnalysisVisualInput> result = new ArrayList<>();
        add(result, options, "roiImage", "high-resolution-roi");
        if (Boolean.parseBoolean(options.getOrDefault("includeContextPage", "false"))) {
            add(result, options, "contextImage", "page-with-marked-roi");
        }
        return List.copyOf(result);
    }

    private static void add(List<AnalysisVisualInput> result, Map<String, String> options,
                            String key, String role) {
        String value = options.getOrDefault(key, "").strip();
        if (!value.isBlank()) result.add(new AnalysisVisualInput(Path.of(value), role, ""));
    }

    private static EngineId engineId(String value) {
        return value == null || value.isBlank() || ID.equals(value) ? null : new EngineId(value);
    }

    private static String fingerprint(int page, List<PdfRegion> regions, String context, String roiPath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Integer.toString(page).getBytes(StandardCharsets.UTF_8));
            for (PdfRegion region : regions) {
                digest.update(region.id().getBytes(StandardCharsets.UTF_8));
                digest.update(Long.toString(region.revision()).getBytes(StandardCharsets.UTF_8));
                digest.update(region.effectiveText().getBytes(StandardCharsets.UTF_8));
            }
            digest.update(context.getBytes(StandardCharsets.UTF_8));
            digest.update(Files.readAllBytes(Path.of(roiPath)));
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String limited(String value, int maximum) {
        String safe = value == null ? "" : value.strip();
        return safe.length() <= maximum ? safe : safe.substring(0, maximum);
    }

    private static String property(String json, String name) {
        if (json == null) return "";
        var matcher = java.util.regex.Pattern.compile("\\\"" + java.util.regex.Pattern.quote(name)
                + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static boolean containsUnsupportedNumbers(String output, String evidence) {
        java.util.Set<String> available = numbers(evidence);
        return numbers(output).stream().anyMatch(value -> !available.contains(value));
    }

    private static java.util.Set<String> numbers(String value) {
        java.util.HashSet<String> result = new java.util.HashSet<>();
        var matcher = java.util.regex.Pattern.compile("(?<![\\p{L}\\p{N}])[-+]?\\d+(?:[.,]\\d+)?")
                .matcher(value == null ? "" : value);
        while (matcher.find()) result.add(matcher.group().replace(',', '.'));
        return result;
    }

    private static boolean looksGlobal(String text) {
        String value = text == null ? "" : text.toLowerCase(java.util.Locale.ROOT);
        return value.matches("(?s).*(esta|la) (página|pagina|documento) (explica|resume|presenta|trata).*");
    }

    private static String fileHash(Path file) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
