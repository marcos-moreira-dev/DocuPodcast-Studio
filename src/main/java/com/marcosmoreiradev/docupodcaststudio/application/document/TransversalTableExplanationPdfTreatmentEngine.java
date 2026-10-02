package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.media.MediaCapabilityService;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.media.api.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Optional contextual table narration. Qwen can verbalize only the exact
 * structure and deterministic statistics supplied by this adapter.
 */
public final class TransversalTableExplanationPdfTreatmentEngine
        implements PdfDerivedTreatmentEngine {
    public static final String ID = "pdf-table-contextual-explanation";
    private static final String Q8_MODEL = "qwen3-vl:4b-instruct-q8_0";
    private static final String SCHEMA = """
            {"type":"object","properties":{
              "status":{"type":"string","enum":["OK","INSUFFICIENT_EVIDENCE"]},
              "objectId":{"type":"string"},
              "semanticStrategy":{"type":"string","enum":["FULL_TEXT","BRIEF_DESCRIPTION"]},
              "narrationText":{"type":"string"},
              "findings":{"type":"array","items":{"type":"object","properties":{
                "text":{"type":"string"},"cellIds":{"type":"array","items":{"type":"string"}}
              },"required":["text","cellIds"]}},
              "uncertainties":{"type":"array","items":{"type":"string"}},
              "confidence":{"type":"number","minimum":0,"maximum":1}
            },"required":["status","objectId","semanticStrategy","narrationText","findings","uncertainties","confidence"]}
            """;
    private static final Pattern CELL_ID =
            Pattern.compile("CELL-[0-9a-fA-F-]{20,}");
    private static final Pattern NUMBER = Pattern.compile(
            "(?<![A-Za-z0-9-])[-+]?(?:\\d+(?:[.,]\\d*)?|[.,]\\d+)"
                    + "(?:[eE][-+]?\\d+)?");
    private final MediaCapabilityService media;
    private final PdfTableStructureAnalyzer analyzer =
            new PdfTableStructureAnalyzer();

    public TransversalTableExplanationPdfTreatmentEngine(
            MediaCapabilityService media) {
        this.media = java.util.Objects.requireNonNull(media, "media");
    }

    @Override public String id() { return ID; }
    @Override public String version() { return "2.0-qwen-q8"; }
    @Override public PdfDerivedTreatmentKind kind() {
        return PdfDerivedTreatmentKind.TABLE_NARRATION;
    }

    @Override
    public PdfDerivedTreatment generate(
            PreparedPdfPage page,
            List<PdfRegion> sourceRegions,
            PdfDerivedTreatmentGenerationRequest request)
            throws IOException, InterruptedException {
        PdfTableStructure table = analyzer.analyze(
                page.pageNumber(), sourceRegions, request.options());
        PdfTableStatistics statistics = analyzer.statistics(table);
        String structure = analyzer.structureJson(table);
        String calculated = analyzer.statisticsJson(statistics);
        String canonicalText = orderedCellText(table);
        String nearby = request.options().getOrDefault(
                "nearbyContext", "").strip();
        String context = "ESTRUCTURA_VERIFICADA=" + structure
                + "\nESTADISTICAS_CALCULADAS=" + calculated
                + "\nTEXTO_CANONICO_ORDENADO=" + canonicalText
                + (nearby.isBlank() ? "" : "\nCONTEXTO_CERCANO=" + nearby);
        List<AnalysisVisualInput> visuals = visualInputs(request.options());
        String objectId = request.options().getOrDefault(
                "objectId", table.id()).strip();
        LinkedHashMap<String, String> options =
                new LinkedHashMap<>(request.options());
        options.put("model", Q8_MODEL);
        options.put("objectId", objectId);
        options.put("semanticKind", "TABLE");
        options.put("maxOutputTokens", "260");
        ContentAnalysisResult result = media.analyzeContent(
                null,
                new ContentAnalysisRequest(
                        ContentAnalysisOperation.TABLE_CONTEXTUAL_EXPLANATION,
                        visuals,
                        """
                                Interpreta exclusivamente la tabla indicada por objectId.
                                Devuelve FULL_TEXT solo si TEXTO_CANONICO_ORDENADO
                                contiene principalmente prosa y no supera 700 caracteres;
                                en ese caso no lo parafrasees. En los demás casos devuelve
                                BRIEF_DESCRIPTION de hasta 60 palabras.
                                Cada hallazgo numérico debe citar cellIds existentes.
                                No calcules valores nuevos: usa únicamente las
                                estadísticas suministradas. Si el contexto no basta,
                                devuelve INSUFFICIENT_EVIDENCE.
                                """,
                        context,
                        request.options().getOrDefault("language", "es"),
                        SCHEMA, options),
                ExecutionContext.defaults(
                        "pdf-table-explanation-page-" + page.pageNumber()));
        String status = property(result.structuredJson(), "status");
        String returnedObject = property(result.structuredJson(), "objectId");
        String strategy = property(
                result.structuredJson(), "semanticStrategy");
        boolean grounded = "OK".equals(status) && objectId.equals(returnedObject);
        if (grounded) validateGrounding(result, table, statistics);
        String narration = "FULL_TEXT".equals(strategy)
                ? canonicalText : result.text().strip();
        if ("FULL_TEXT".equals(strategy)
                && (!mainlyProse(canonicalText) || canonicalText.length() > 700)) {
            throw new EngineExecutionException(EngineDiagnosticCode.INVALID_OUTPUT,
                    "FULL_TEXT no cumple el límite o no es principalmente prosa.");
        }
        PdfSemanticNarrationSafetyValidator safety =
                new PdfSemanticNarrationSafetyValidator();
        boolean ttsSafe = grounded
                && safety.safeText(kind(), narration, strategy);
        if (grounded && !ttsSafe) {
            throw new EngineExecutionException(EngineDiagnosticCode.INVALID_OUTPUT,
                    "La interpretación de tabla no es apta para TTS.");
        }
        if (!grounded) narration = "";
        String fingerprint = analyzer.fingerprint(table, statistics);
        long revision = sourceRegions.stream().mapToLong(PdfRegion::revision)
                .max().orElse(1L);
        String id = "PDF-DER-" + UUID.nameUUIDFromBytes(
                (page.pageNumber() + "|" + fingerprint + "|" + result.text())
                        .getBytes(StandardCharsets.UTF_8));
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        metadata.put("local", "true");
        metadata.put("tableKind", table.kind().name());
        metadata.put("tableStructureJson", structure);
        metadata.put("tableStatisticsJson", calculated);
        metadata.put("structuredJson", result.structuredJson());
        metadata.put("engineId",
                result.diagnostics().getOrDefault("engineId", ""));
        metadata.put("model",
                Q8_MODEL);
        metadata.put("canonicalTextModified", "false");
        metadata.put("groundingValidated", Boolean.toString(grounded));
        metadata.put("groundingStatus", grounded ? "OK" : "INSUFFICIENT_EVIDENCE");
        metadata.put("objectId", objectId);
        metadata.put("semanticStrategy", strategy);
        metadata.put("semanticPolicy",
                request.options().getOrDefault("semanticPolicy", ""));
        metadata.put("ttsSafetyValidated", Boolean.toString(ttsSafe));
        metadata.put("ttsValidatorVersion", PdfSemanticNarrationSafetyValidator.VERSION);
        metadata.put("sourceFingerprintValidated", "true");
        metadata.put("automaticAdmission", ttsSafe
                ? PdfSemanticNarrationSafetyValidator.AUTOMATIC_ADMISSION : "");
        return new PdfDerivedTreatment(
                id, kind(), request.sourceRegionIds(), narration,
                Q8_MODEL,
                version(), result.confidence(), Instant.now(),
                PdfDerivedTreatmentState.DRAFT, revision, fingerprint,
                "Explicación contextual de estructura y estadísticas verificadas.",
                metadata);
    }

    static void validateGrounding(
            ContentAnalysisResult result,
            PdfTableStructure table,
            PdfTableStatistics statistics) throws IOException {
        String json = result.structuredJson();
        Set<String> allowedIds = table.cells().stream()
                .map(PdfTableCell::id)
                .collect(java.util.stream.Collectors.toSet());
        var idMatcher = CELL_ID.matcher(json);
        int citations = 0;
        while (idMatcher.find()) {
            citations++;
            if (!allowedIds.contains(idMatcher.group())) {
                throw new IOException(
                        "La explicación citó una celda inexistente: "
                                + idMatcher.group());
            }
        }
        if (!statistics.columns().isEmpty() && citations == 0) {
            throw new IOException(
                    "La explicación numérica no citó ninguna celda verificable.");
        }
        Set<Double> allowedNumbers = new HashSet<>(
                statistics.numericCellValues().values());
        allowedNumbers.add((double) table.rows());
        allowedNumbers.add((double) table.columns());
        statistics.columns().forEach(column -> {
            allowedNumbers.add(column.minimum());
            allowedNumbers.add(column.maximum());
            allowedNumbers.add(column.variation());
            allowedNumbers.add((double) column.values());
        });
        var numberMatcher = NUMBER.matcher(result.text());
        while (numberMatcher.find()) {
            var value = PdfTableStructureAnalyzer.number(numberMatcher.group());
            if (value.isPresent() && allowedNumbers.stream().noneMatch(
                    allowed -> close(allowed, value.get()))) {
                throw new IOException(
                        "La explicación introdujo una cifra no respaldada: "
                                + numberMatcher.group());
            }
        }
    }

    private static List<AnalysisVisualInput> visualInputs(
            Map<String, String> options) throws IOException {
        ArrayList<AnalysisVisualInput> inputs = new ArrayList<>();
        addVisual(inputs, options.get("roiImage"), "high-resolution-roi");
        addVisual(inputs, options.get("contextImage"), "page-with-marked-roi");
        return List.copyOf(inputs);
    }

    private static void addVisual(
            List<AnalysisVisualInput> inputs,
            String value,
            String role) throws IOException {
        if (value == null || value.isBlank()) return;
        Path file = Path.of(value).toAbsolutePath().normalize();
        if (!Files.isRegularFile(file)) {
            throw new IOException("No existe la evidencia visual de tabla: " + file);
        }
        inputs.add(new AnalysisVisualInput(file, role, "image/png"));
    }

    private static boolean close(double left, double right) {
        double scale = Math.max(1.0, Math.max(Math.abs(left), Math.abs(right)));
        return Math.abs(left - right) <= 1.0e-9 * scale;
    }

    private static String orderedCellText(PdfTableStructure table) {
        return table.cells().stream()
                .sorted(java.util.Comparator.comparingInt(PdfTableCell::row)
                        .thenComparingInt(PdfTableCell::column))
                .map(PdfTableCell::text).filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.joining(". "));
    }

    private static boolean mainlyProse(String value) {
        if (value == null || value.isBlank()) return false;
        long letters = value.codePoints().filter(Character::isLetter).count();
        long digits = value.codePoints().filter(Character::isDigit).count();
        return letters >= 24 && letters >= digits * 2;
    }

    private static String property(String json, String name) {
        if (json == null) return "";
        var matcher = Pattern.compile("\\\"" + Pattern.quote(name)
                + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);
        return matcher.find() ? matcher.group(1) : "";
    }
}
