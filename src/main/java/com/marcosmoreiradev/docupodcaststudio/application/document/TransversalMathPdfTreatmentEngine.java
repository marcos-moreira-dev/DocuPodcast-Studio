package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** One grounded formula ROI interpreted as safe Spanish speech by Qwen Q8. */
public final class TransversalMathPdfTreatmentEngine
        implements PdfDerivedTreatmentEngine {
    public static final String ID = "pdf-via-transversal-math-analysis";
    private static final String Q8_MODEL = "qwen3-vl:4b-instruct-q8_0";
    private static final String SCHEMA = """
            {"type":"object","properties":{
              "status":{"type":"string","enum":["OK","INSUFFICIENT_EVIDENCE"]},
              "objectId":{"type":"string"},
              "semanticStrategy":{"type":"string","enum":["SPOKEN_MATH","BRIEF_DESCRIPTION"]},
              "narrationText":{"type":"string"},
              "claims":{"type":"array","items":{"type":"object","properties":{
                "text":{"type":"string"},"evidenceIds":{"type":"array","items":{"type":"string"}}
              },"required":["text","evidenceIds"]}},
              "uncertainties":{"type":"array","items":{"type":"string"}},
              "confidence":{"type":"number","minimum":0,"maximum":1}
            },"required":["status","objectId","semanticStrategy","narrationText","claims","uncertainties","confidence"]}
            """;
    private final MediaCapabilityService media;

    public TransversalMathPdfTreatmentEngine(MediaCapabilityService media) {
        this.media = java.util.Objects.requireNonNull(media, "media");
    }

    @Override public String id() { return ID; }
    @Override public String version() { return "3.0-qwen-q8"; }
    @Override public PdfDerivedTreatmentKind kind() {
        return PdfDerivedTreatmentKind.MATHEMATICAL_READING;
    }

    @Override
    public PdfDerivedTreatment generate(
            PreparedPdfPage page, List<PdfRegion> sourceRegions,
            PdfDerivedTreatmentGenerationRequest request)
            throws IOException, InterruptedException {
        String language = request.options().getOrDefault(
                "language", page.analysisProfile().language().effectiveLanguage());
        String roi = request.options().getOrDefault("roiImage", "").strip();
        if (roi.isBlank()) {
            return fallback(page, sourceRegions, request,
                    fallbackPolicy(request.options().get("fallbackPolicy")));
        }
        String objectId = request.options().getOrDefault("objectId",
                request.sourceRegionIds().getFirst()).strip();
        String evidence = nearbyContext(sourceRegions, request);
        boolean brief = normalizedMathLength(sourceRegions, request) > 200
                || operatorGroups(sourceRegions, request) > 24;
        String strategy = brief ? "BRIEF_DESCRIPTION" : "SPOKEN_MATH";
        String instruction = "Interpreta solo la fórmula del ROI " + objectId + ". "
                + (brief
                ? "Da una descripción conceptual breve, sin transcribir la notación. "
                : "Léela en prosa matemática natural en español. ")
                + "No devuelvas LaTeX, MathML, sqrt, frac, barras de división, "
                + "exponentes crudos ni cadenas de operadores. Usa " + strategy
                + " y devuelve INSUFFICIENT_EVIDENCE si el ROI no basta.";
        LinkedHashMap<String, String> options = new LinkedHashMap<>(request.options());
        options.put("model", Q8_MODEL);
        options.put("objectId", objectId);
        options.put("semanticKind", "EQUATION");
        options.put("maxOutputTokens", brief ? "120" : "220");
        List<AnalysisVisualInput> visuals = new java.util.ArrayList<>();
        visuals.add(new AnalysisVisualInput(Path.of(roi),
                "high-resolution-roi", "image/png"));
        String contextImage = request.options().getOrDefault("contextImage", "").strip();
        if (!contextImage.isBlank()) {
            visuals.add(new AnalysisVisualInput(Path.of(contextImage),
                    "page-with-marked-roi", "image/png"));
        }
        ContentAnalysisResult speech = media.analyzeContent(null,
                new ContentAnalysisRequest(ContentAnalysisOperation.IMAGE_DESCRIPTION,
                        List.copyOf(visuals), instruction, evidence, language,
                        SCHEMA, options),
                ExecutionContext.defaults(
                        "pdf-math-qwen-page-" + page.pageNumber()));
        String status = property(speech.structuredJson(), "status");
        String returnedObject = property(speech.structuredJson(), "objectId");
        String returnedStrategy = property(
                speech.structuredJson(), "semanticStrategy");
        boolean grounded = "OK".equals(status)
                && objectId.equals(returnedObject)
                && strategy.equals(returnedStrategy);
        String narration = grounded ? speech.text().strip() : "";
        PdfSemanticNarrationSafetyValidator safety =
                new PdfSemanticNarrationSafetyValidator();
        boolean ttsSafe = grounded && safety.safeText(kind(), narration, strategy);
        if (grounded && !ttsSafe) {
            throw new com.marcosmoreiradev.docupodcaststudio.media.api.EngineExecutionException(
                    EngineDiagnosticCode.INVALID_OUTPUT,
                    "La lectura matemática contiene notación cruda no apta para TTS.");
        }
        long revision = sourceRegions.stream().mapToLong(PdfRegion::revision)
                .max().orElse(1L);
        String fingerprint = fingerprint(
                page.pageNumber(), sourceRegions, evidence, strategy);
        String stable = page.pageNumber() + "|"
                + String.join("|", request.sourceRegionIds()) + "|" + fingerprint;
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        metadata.put("local", "true");
        metadata.put("engineId", speech.diagnostics().getOrDefault("engineId", ""));
        metadata.put("model", Q8_MODEL);
        metadata.put("objectId", objectId);
        metadata.put("semanticStrategy", strategy);
        metadata.put("semanticPolicy",
                request.options().getOrDefault("semanticPolicy", ""));
        metadata.put("groundingStatus", grounded ? "OK" : "INSUFFICIENT_EVIDENCE");
        metadata.put("ttsSafetyValidated", Boolean.toString(ttsSafe));
        metadata.put("ttsValidatorVersion", PdfSemanticNarrationSafetyValidator.VERSION);
        metadata.put("sourceFingerprintValidated", "true");
        metadata.put("automaticAdmission", ttsSafe
                ? PdfSemanticNarrationSafetyValidator.AUTOMATIC_ADMISSION : "");
        metadata.put("rawOcrSuppressed", "true");
        metadata.put("structuredJson", speech.structuredJson());
        return new PdfDerivedTreatment(
                "PDF-DER-" + UUID.nameUUIDFromBytes(
                        stable.getBytes(StandardCharsets.UTF_8)),
                kind(), request.sourceRegionIds(), narration,
                Q8_MODEL, version(), speech.confidence(),
                Instant.now(), PdfDerivedTreatmentState.DRAFT, revision,
                fingerprint,
                instruction,
                metadata);
    }

    private PdfDerivedTreatment fallback(
            PreparedPdfPage page, List<PdfRegion> sourceRegions,
            PdfDerivedTreatmentGenerationRequest request,
            PdfMathFallbackPolicy policy) {
        String text = switch (policy) {
            case ANNOUNCE_ONLY -> "Fórmula matemática.";
            case SKIP -> "Fórmula omitida por la política de lectura.";
            case REQUIRE_REVIEW -> "Fórmula pendiente de revisión matemática.";
        };
        long revision = sourceRegions.stream().mapToLong(PdfRegion::revision)
                .max().orElse(1L);
        String fingerprint = fingerprint(
                page.pageNumber(), sourceRegions, "", policy.name());
        String stable = page.pageNumber() + "|"
                + String.join("|", request.sourceRegionIds()) + "|" + fingerprint;
        return new PdfDerivedTreatment(
                "PDF-DER-" + UUID.nameUUIDFromBytes(
                        stable.getBytes(StandardCharsets.UTF_8)),
                kind(), request.sourceRegionIds(), text, id(), version(), 0,
                Instant.now(), PdfDerivedTreatmentState.DRAFT, revision,
                fingerprint, "Fallback seguro sin narrar símbolos OCR.",
                Map.of("local", "true", "fallbackPolicy", policy.name(),
                        "requiresApproval", "true", "mathMlValidated", "false",
                        "rawOcrSuppressed", "true"));
    }

    private static PdfMathFallbackPolicy fallbackPolicy(String value) {
        try {
            return value == null || value.isBlank()
                    ? PdfMathFallbackPolicy.REQUIRE_REVIEW
                    : PdfMathFallbackPolicy.valueOf(
                    value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            return PdfMathFallbackPolicy.REQUIRE_REVIEW;
        }
    }

    static double normalizedEditDistance(String expected, String actual) {
        String left = expected == null ? "" : expected.replaceAll("\\s+", "");
        String right = actual == null ? "" : actual.replaceAll("\\s+", "");
        if (left.isEmpty() && right.isEmpty()) return 0;
        int[] previous = new int[right.length() + 1];
        for (int index = 0; index <= right.length(); index++) previous[index] = index;
        for (int row = 1; row <= left.length(); row++) {
            int[] current = new int[right.length() + 1];
            current[0] = row;
            for (int column = 1; column <= right.length(); column++) {
                int cost = left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1;
                current[column] = Math.min(Math.min(
                        current[column - 1] + 1, previous[column] + 1),
                        previous[column - 1] + cost);
            }
            previous = current;
        }
        return previous[right.length()]
                / (double) Math.max(left.length(), right.length());
    }

    private static String nearbyContext(
            List<PdfRegion> sourceRegions,
            PdfDerivedTreatmentGenerationRequest request) {
        String explicit = request.options().getOrDefault(
                "nearbyContext", "").strip();
        if (!explicit.isBlank()) return explicit;
        return sourceRegions.stream().map(PdfRegion::effectiveText)
                .filter(value -> !value.isBlank())
                .reduce("", (left, right) ->
                        (left + "\n" + right).strip());
    }

    private static int normalizedMathLength(
            List<PdfRegion> regions,
            PdfDerivedTreatmentGenerationRequest request) {
        return mathEvidence(regions, request)
                .replaceAll("\\s+", "").length();
    }

    private static long operatorGroups(
            List<PdfRegion> regions,
            PdfDerivedTreatmentGenerationRequest request) {
        var matcher = java.util.regex.Pattern.compile(
                "(?:[+\\-*/=<>^_]+|\\\\[A-Za-z]+)")
                .matcher(mathEvidence(regions, request));
        long count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private static String mathEvidence(
            List<PdfRegion> regions,
            PdfDerivedTreatmentGenerationRequest request) {
        return request.options().getOrDefault("latex", "") + "\n"
                + request.options().getOrDefault("mathMl", "") + "\n"
                + regions.stream().map(PdfRegion::effectiveText)
                .reduce("", (left, right) -> left + " " + right);
    }

    private static String property(String json, String name) {
        if (json == null) return "";
        var matcher = java.util.regex.Pattern.compile(
                "\\\"" + java.util.regex.Pattern.quote(name)
                        + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"")
                .matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String fingerprint(
            int page, List<PdfRegion> regions, String latex, String mathMl) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Integer.toString(page).getBytes(StandardCharsets.UTF_8));
            for (PdfRegion region : regions) {
                digest.update(region.id().getBytes(StandardCharsets.UTF_8));
                digest.update(Long.toString(region.revision())
                        .getBytes(StandardCharsets.UTF_8));
            }
            digest.update(latex.getBytes(StandardCharsets.UTF_8));
            digest.update(mathMl.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
