package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
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
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Page-level semantic filter. It proposes narratability by stable region ID
 * and never edits or removes the extracted text.
 */
public final class TransversalNarratabilityReviewPdfTreatmentEngine
        implements PdfDerivedTreatmentEngine {
    public static final String ID = "pdf-via-transversal-narratability-review";
    static final int DEFAULT_CONTEXT_CHAR_LIMIT = 12_000;
    private static final Pattern OBJECT = Pattern.compile("\\{[^{}]*}", Pattern.DOTALL);
    private final MediaCapabilityService media;

    public TransversalNarratabilityReviewPdfTreatmentEngine(MediaCapabilityService media) {
        this.media = java.util.Objects.requireNonNull(media, "media");
    }

    @Override public String id() { return ID; }
    @Override public String version() { return "1.0"; }
    @Override public PdfDerivedTreatmentKind kind() {
        return PdfDerivedTreatmentKind.NARRATABILITY_REVIEW;
    }

    @Override
    public PdfDerivedTreatment generate(PreparedPdfPage page, List<PdfRegion> sourceRegions,
                                        PdfDerivedTreatmentGenerationRequest request)
            throws IOException, InterruptedException {
        if (sourceRegions.isEmpty()) throw new IOException("La página no contiene regiones preparadas.");
        String language = request.options().getOrDefault(
                "language", page.analysisProfile().language().effectiveLanguage());
        String instruction = """
                Clasifica cada región por su ID sin reescribir ni omitir ninguna.
                NARRATABLE significa prosa comprensible que puede leerse en voz alta.
                NON_NARRATABLE significa galimatías inequívoco, cabecera, pie, número de página,
                residuo OCR, fragmento de tabla o fórmula que no debe narrarse como prosa.
                UNCERTAIN significa evidencia ambigua que requiere revisión humana.
                Conserva cifras, nombres y símbolos en tu razonamiento. No corrijas el texto.
                Usa en reason exactamente uno de estos códigos breves:
                CLEAR_PROSE, STRUCTURAL_METADATA, OCR_GIBBERISH, TABLE_FRAGMENT,
                FORMULA_FRAGMENT, AMBIGUOUS o OTHER.
                CLEAR_PROSE corresponde a NARRATABLE. AMBIGUOUS corresponde a UNCERTAIN.
                Para NON_NARRATABLE usa STRUCTURAL_METADATA en cabeceras, pies o números
                de página; OCR_GIBBERISH en texto ilegible; TABLE_FRAGMENT solo en tablas
                y FORMULA_FRAGMENT solo en matemáticas.
                """;
        int contextLimit = positiveInt(request.options().get("maxContextChars"),
                DEFAULT_CONTEXT_CHAR_LIMIT);
        List<List<PdfRegion>> groups = contextGroups(sourceRegions, contextLimit);
        String commonContext = commonContext(sourceRegions);
        LinkedHashMap<String, Decision> mergedDecisions = new LinkedHashMap<>();
        List<String> structuredResponses = new ArrayList<>();
        LinkedHashMap<String, String> diagnostics = new LinkedHashMap<>();
        for (int index = 0; index < groups.size(); index++) {
            List<PdfRegion> group = groups.get(index);
            LinkedHashMap<String, String> analysisOptions =
                    new LinkedHashMap<>(request.options());
            analysisOptions.put("maxOutputTokens", Integer.toString(
                    Math.min(4096, 128 + group.size() * 24)));
            analysisOptions.put("fragmentIndex", Integer.toString(index + 1));
            analysisOptions.put("fragmentCount", Integer.toString(groups.size()));
            String source = commonContext
                    + "\nREGIONES_A_CLASIFICAR:\n" + structuredSource(group);
            ContentAnalysisResult result = media.analyzeContent(
                    engineId(request.engineId()),
                    new ContentAnalysisRequest(
                            ContentAnalysisOperation.NARRATABILITY_CLASSIFICATION,
                            List.of(), instruction, source, language, reviewSchema(),
                            analysisOptions),
                    ExecutionContext.defaults("pdf-narratability-review-page-"
                            + page.pageNumber() + "-fragment-" + (index + 1)));
            Map<String, Decision> fragment = decisions(
                    result.structuredJson(), group);
            if (fragment.size() != group.size()) {
                throw new IOException(
                        "El filtro local devolvió un fragmento incompleto.");
            }
            mergedDecisions.putAll(fragment);
            structuredResponses.add(result.structuredJson());
            diagnostics.putAll(result.diagnostics());
        }
        Map<String, Decision> decisions =
                java.util.Collections.unmodifiableMap(mergedDecisions);
        if (decisions.size() != sourceRegions.size()) {
            throw new IOException("El filtro local no devolvió una decisión para cada región.");
        }
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        int excluded = 0;
        int uncertain = 0;
        double confidenceSum = 0.0;
        StringBuilder review = new StringBuilder();
        for (PdfRegion region : sourceRegions) {
            Decision decision = decisions.get(region.id());
            metadata.put("decision." + region.id(), decision.narratability().name());
            metadata.put("reason." + region.id(), decision.reason());
            metadata.put("confidence." + region.id(), Double.toString(decision.confidence()));
            if (decision.narratability() == PdfNarratability.NON_NARRATABLE) excluded++;
            if (decision.narratability() == PdfNarratability.UNCERTAIN) uncertain++;
            confidenceSum += decision.confidence();
            review.append(region.id()).append(" · ")
                    .append(decision.narratability()).append(" · ")
                    .append(Math.round(decision.confidence() * 100.0)).append("%\n")
                    .append(humanReason(decision.reason()))
                    .append("\nTexto conservado: ")
                    .append(preview(region.effectiveText())).append("\n\n");
        }
        metadata.put("local", "true");
        metadata.put("structuredJsonFragments", String.join("\n", structuredResponses));
        metadata.put("fragmentCount", Integer.toString(groups.size()));
        metadata.put("engineId", diagnostics.getOrDefault("engineId", ""));
        metadata.put("pageNumber", Integer.toString(page.pageNumber()));
        metadata.put("regionCount", Integer.toString(sourceRegions.size()));
        String summary = "Revisión de página: " + sourceRegions.size() + " regiones; "
                + excluded + " propuestas como no narrables y " + uncertain
                + " requieren revisión. El texto original permanece intacto.";
        long revision = sourceRegions.stream().mapToLong(PdfRegion::revision).max().orElse(1L);
        String fingerprint = fingerprint(page.pageNumber(), sourceRegions);
        String id = "PDF-DER-" + UUID.nameUUIDFromBytes(
                (page.pageNumber() + "|" + fingerprint).getBytes(StandardCharsets.UTF_8));
        return new PdfDerivedTreatment(id, kind(),
                sourceRegions.stream().map(PdfRegion::id).toList(),
                summary + "\n\n" + review.toString().strip(),
                diagnostics.getOrDefault("model", "qwen3-vl"),
                version(), confidenceSum / sourceRegions.size(), Instant.now(),
                PdfDerivedTreatmentState.DRAFT, revision, fingerprint, instruction,
                metadata);
    }

    private static String structuredSource(List<PdfRegion> regions) {
        StringBuilder value = new StringBuilder();
        for (PdfRegion region : regions) {
            value.append("ID=").append(region.id())
                    .append("\nTYPE=").append(region.effectiveType())
                    .append("\nAUTO=").append(region.automaticNarratability())
                    .append("\nTEXT=").append(region.effectiveText().replace('\n', ' '))
                    .append("\n---\n");
        }
        return value.toString();
    }

    static List<List<PdfRegion>> contextGroups(List<PdfRegion> regions,
                                               int characterLimit) {
        int safeLimit = Math.max(512, characterLimit);
        List<List<PdfRegion>> result = new ArrayList<>();
        List<PdfRegion> current = new ArrayList<>();
        int currentLength = 0;
        for (PdfRegion region : regions) {
            int regionLength = structuredSource(List.of(region)).length();
            if (!current.isEmpty() && currentLength + regionLength > safeLimit) {
                result.add(List.copyOf(current));
                current.clear();
                currentLength = 0;
            }
            current.add(region);
            currentLength += regionLength;
        }
        if (!current.isEmpty()) {
            result.add(List.copyOf(current));
        }
        return List.copyOf(result);
    }

    private static String commonContext(List<PdfRegion> regions) {
        StringBuilder value = new StringBuilder(
                "CONTEXTO_COMUN_NO_CLASIFICAR:\n");
        regions.stream()
                .filter(region -> switch (region.effectiveType()) {
                    case TITLE, HEADING, SUBHEADING, CAPTION -> true;
                    default -> false;
                })
                .limit(6)
                .forEach(region -> value.append(region.effectiveType())
                        .append('=').append(preview(region.effectiveText()))
                        .append('\n'));
        if (value.toString().equals("CONTEXTO_COMUN_NO_CLASIFICAR:\n")) {
            value.append("Sin encabezado o pie de figura confiable.\n");
        }
        return value.toString();
    }

    private static int positiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value == null ? "" : value);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String reviewSchema() {
        return """
                {"type":"object","properties":{
                  "decisions":{"type":"array","items":{"type":"object","properties":{
                    "regionId":{"type":"string"},
                    "narratability":{"type":"string","enum":["NARRATABLE","NON_NARRATABLE","UNCERTAIN"]},
                    "reason":{"type":"string","enum":["CLEAR_PROSE","STRUCTURAL_METADATA",
                      "OCR_GIBBERISH","TABLE_FRAGMENT","FORMULA_FRAGMENT","AMBIGUOUS","OTHER"]},
                    "confidence":{"type":"number","minimum":0,"maximum":1}
                  },"required":["regionId","narratability","reason","confidence"]}},
                  "uncertainties":{"type":"array","items":{"type":"string"}},
                  "confidence":{"type":"number","minimum":0,"maximum":1}
                },"required":["decisions","uncertainties","confidence"]}
                """;
    }

    static Map<String, Decision> decisions(String json, List<PdfRegion> regions)
            throws IOException {
        java.util.Set<String> allowed = regions.stream().map(PdfRegion::id)
                .collect(java.util.stream.Collectors.toSet());
        LinkedHashMap<String, Decision> result = new LinkedHashMap<>();
        java.util.Set<String> duplicates = new java.util.HashSet<>();
        var matcher = OBJECT.matcher(json == null ? "" : json);
        while (matcher.find()) {
            String object = matcher.group();
            String id = property(object, "regionId");
            String label = property(object, "narratability");
            if (id.isBlank()) continue;
            if (!allowed.contains(id)) {
                throw new IOException(
                        "El filtro local inventó un ID de región: " + id);
            }
            if (result.containsKey(id)) {
                duplicates.add(id);
                continue;
            }
            PdfNarratability narratability;
            try {
                narratability = PdfNarratability.valueOf(label.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException invalid) {
                narratability = PdfNarratability.UNCERTAIN;
            }
            String reason = property(object, "reason");
            double confidence = numberProperty(object, "confidence");
            if (confidence < 0.65
                    || contradictory(narratability, reason)) {
                narratability = PdfNarratability.UNCERTAIN;
                reason = "AMBIGUOUS";
            }
            result.put(id, new Decision(narratability, reason, confidence));
        }
        LinkedHashMap<String, Decision> complete = new LinkedHashMap<>();
        for (PdfRegion region : regions) {
            Decision decision = result.get(region.id());
            if (decision == null || duplicates.contains(region.id())) {
                decision = new Decision(
                        PdfNarratability.UNCERTAIN, "AMBIGUOUS", 0.0);
            }
            complete.put(region.id(), decision);
        }
        return java.util.Collections.unmodifiableMap(complete);
    }

    private static boolean contradictory(
            PdfNarratability narratability, String reason) {
        return switch (narratability) {
            case NARRATABLE -> !"CLEAR_PROSE".equals(reason);
            case NON_NARRATABLE -> !java.util.Set.of(
                    "STRUCTURAL_METADATA", "OCR_GIBBERISH",
                    "TABLE_FRAGMENT", "FORMULA_FRAGMENT")
                    .contains(reason);
            case UNCERTAIN -> !java.util.Set.of(
                    "AMBIGUOUS", "OTHER").contains(reason);
        };
    }

    private static String property(String json, String name) {
        var matcher = Pattern.compile("\"" + Pattern.quote(name)
                + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL).matcher(json);
        if (!matcher.find()) return "";
        return matcher.group(1).replace("\\\"", "\"").replace("\\n", "\n")
                .replace("\\\\", "\\").strip();
    }

    private static double numberProperty(String json, String name) {
        var matcher = Pattern.compile("\"" + Pattern.quote(name)
                + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(json);
        if (!matcher.find()) return 0.0;
        try {
            return Math.max(0.0, Math.min(1.0, Double.parseDouble(matcher.group(1))));
        } catch (NumberFormatException invalid) {
            return 0.0;
        }
    }

    private static EngineId engineId(String value) {
        return value == null || value.isBlank() || ID.equals(value)
                ? null : new EngineId(value);
    }

    private static String preview(String text) {
        String normalized = text == null ? "" : text.replace('\n', ' ').strip();
        return normalized.length() <= 180
                ? normalized : normalized.substring(0, 177).strip() + "...";
    }

    private static String humanReason(String code) {
        return switch (code) {
            case "CLEAR_PROSE" -> "Prosa comprensible.";
            case "STRUCTURAL_METADATA" -> "Metadato estructural.";
            case "OCR_GIBBERISH" -> "Residuo o galimatías de OCR.";
            case "TABLE_FRAGMENT" -> "Fragmento de tabla.";
            case "FORMULA_FRAGMENT" -> "Fragmento de fórmula.";
            case "AMBIGUOUS" -> "Evidencia ambigua; requiere revisión.";
            case "OTHER" -> "Otra razón conservadora.";
            default -> "Razón no reconocida; requiere revisión.";
        };
    }

    private static String fingerprint(int page, List<PdfRegion> regions) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Integer.toString(page).getBytes(StandardCharsets.UTF_8));
            for (PdfRegion region : regions) {
                digest.update(region.id().getBytes(StandardCharsets.UTF_8));
                digest.update(Long.toString(region.revision()).getBytes(StandardCharsets.UTF_8));
                digest.update(region.effectiveText().getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    record Decision(PdfNarratability narratability, String reason,
                    double confidence) { }
}
