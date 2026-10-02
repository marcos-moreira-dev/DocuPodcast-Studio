package com.marcosmoreiradev.docupodcaststudio.infrastructure.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfDocumentRepository;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfRegionIndex;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentAnalysisSummary;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentLanguageProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageAnalysisProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationMetrics;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPreparationProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.json.SimpleJsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.stream.Stream;

/** JSON implementation of the canonical page-oriented PDF workspace. */
public final class JsonPreparedPdfDocumentRepository implements PreparedPdfDocumentRepository {
    public static final String MANIFEST_RELATIVE_PATH = "document/manifest.json";
    private final AtomicUtf8JsonFileWriter writer = new AtomicUtf8JsonFileWriter();
    private final Map<Path, PreparedPdfRegionIndex> regionIndexes = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public synchronized void initialize(Path projectRoot, PdfDocumentManifest requested) throws IOException {
        Path root = root(projectRoot);
        Optional<PdfDocumentManifest> existing = loadManifest(root);
        PdfDocumentManifest manifest = requested;
        if (existing.isPresent() && existing.get().sourceSha256().equals(requested.sourceSha256())) {
            PdfDocumentManifest current = existing.get();
            manifest = new PdfDocumentManifest(PdfDocumentManifest.CURRENT_SCHEMA_VERSION,
                    requested.title(), requested.sourceFile(), requested.sourceSha256(), requested.pageCount(),
                    requested.preparationSignature(), current.analysisSummary(),
                    current.createdAt(), Instant.now(), current.readingStrategy(), current.nativeTextProvider());
        } else if (existing.isPresent()) {
            clearPreparedPages(root);
        }
        writer.write(manifestPath(root), manifestJson(manifest));
        regionIndexes.remove(root);
    }

    @Override
    public Optional<PdfDocumentManifest> loadManifest(Path projectRoot) throws IOException {
        Path path = manifestPath(root(projectRoot));
        if (!Files.isRegularFile(path)) return Optional.empty();
        return Optional.of(readManifest(path));
    }

    @Override
    public void saveManifest(Path projectRoot, PdfDocumentManifest manifest) throws IOException {
        Path root = root(projectRoot);
        Objects.requireNonNull(manifest, "manifest");
        Files.createDirectories(manifestPath(root).getParent());
        writer.write(manifestPath(root), manifestJson(manifest));
        regionIndexes.remove(root);
    }

    @Override
    public Optional<PreparedPdfPage> loadPage(Path projectRoot, int pageNumber) throws IOException {
        Path path = pagePath(root(projectRoot), pageNumber);
        if (!Files.isRegularFile(path)) return Optional.empty();
        return Optional.of(readPage(path));
    }

    @Override
    public List<PreparedPdfPage> loadPages(Path projectRoot) throws IOException {
        Path directory = pagesDirectory(root(projectRoot));
        if (!Files.isDirectory(directory)) return List.of();
        ArrayList<PreparedPdfPage> pages = new ArrayList<>();
        try (Stream<Path> stream = Files.list(directory)) {
            for (Path path : stream.filter(Files::isRegularFile)
                    .filter(candidate -> candidate.getFileName().toString().matches("page-\\d{6}\\.json"))
                    .sorted()
                    .toList()) {
                pages.add(readPage(path));
            }
        }
        pages.sort(Comparator.comparingInt(PreparedPdfPage::pageNumber));
        return List.copyOf(pages);
    }

    @Override
    public PreparedPdfRegionIndex loadRegionIndex(Path projectRoot) throws IOException {
        Path root = root(projectRoot);
        PreparedPdfRegionIndex current = regionIndexes.get(root);
        if (current != null) return current;
        PreparedPdfRegionIndex built = PreparedPdfRegionIndex.from(loadPages(root));
        regionIndexes.put(root, built);
        return built;
    }

    @Override
    public synchronized void savePage(Path projectRoot, PreparedPdfPage page) throws IOException {
        Path root = root(projectRoot);
        PdfDocumentManifest manifest = loadManifest(root)
                .orElseThrow(() -> new IOException("El PDF no tiene document/manifest.json V2."));
        if (page.pageNumber() > manifest.pageCount()) {
            throw new IOException("La página " + page.pageNumber() + " excede el total " + manifest.pageCount() + ".");
        }
        writer.write(pagePath(root, page.pageNumber()), pageJson(page));
        PdfDocumentManifest updated = new PdfDocumentManifest(manifest.schemaVersion(), manifest.title(),
                manifest.sourceFile(), manifest.sourceSha256(), manifest.pageCount(),
                manifest.preparationSignature(), manifest.analysisSummary(),
                manifest.createdAt(), Instant.now(), manifest.readingStrategy(), manifest.nativeTextProvider());
        writer.write(manifestPath(root), manifestJson(updated));
        regionIndexes.remove(root);
    }

    private static Path root(Path projectRoot) {
        if (projectRoot == null) throw new IllegalArgumentException("projectRoot is required");
        return projectRoot.toAbsolutePath().normalize();
    }

    private static Path manifestPath(Path root) {
        return root.resolve("document").resolve("manifest.json").normalize();
    }

    private static Path pagesDirectory(Path root) {
        return root.resolve("document").resolve("pages").normalize();
    }

    private static Path pagePath(Path root, int pageNumber) {
        if (pageNumber <= 0) throw new IllegalArgumentException("pageNumber must be 1-based");
        return pagesDirectory(root).resolve("page-%06d.json".formatted(pageNumber)).normalize();
    }

    private static void clearPreparedPages(Path root) throws IOException {
        Path directory = pagesDirectory(root);
        if (!Files.isDirectory(directory)) return;
        try (Stream<Path> stream = Files.list(directory)) {
            for (Path path : stream.filter(Files::isRegularFile)
                    .filter(candidate -> candidate.getFileName().toString().matches("page-\\d{6}\\.json(?:\\.tmp)?"))
                    .toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static PdfDocumentManifest readManifest(Path path) throws IOException {
        Map<String, Object> map = object(parse(path), "manifest");
        PdfDocumentManifest manifest = new PdfDocumentManifest(
                integer(map.get("schemaVersion"), "schemaVersion"),
                string(map.get("title"), "title"),
                string(map.get("sourceFile"), "sourceFile"),
                string(map.get("sourceSha256"), "sourceSha256"),
                integer(map.get("pageCount"), "pageCount"),
                string(map.get("preparationSignature"), "preparationSignature"),
                readAnalysisSummary(map.get("analysisSummary")),
                instant(map.get("createdAt"), "createdAt"),
                instant(map.get("updatedAt"), "updatedAt"),
                enumeration(com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfReadingStrategy.class,
                        map.getOrDefault("readingStrategy", "SEMANTIC"), "readingStrategy"),
                enumeration(com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNativeTextProvider.class,
                        map.getOrDefault("nativeTextProvider", "PDFBOX"), "nativeTextProvider"));
        if (manifest.schemaVersion() != PdfDocumentManifest.CURRENT_SCHEMA_VERSION) {
            throw new IOException("Versión de manifest PDF no compatible: " + manifest.schemaVersion());
        }
        return manifest;
    }

    private static PreparedPdfPage readPage(Path path) throws IOException {
        Map<String, Object> map = object(parse(path), "page");
        ArrayList<PdfRegion> regions = new ArrayList<>();
        for (Object item : array(map.get("regions"), "regions")) {
            Map<String, Object> region = object(item, "region");
            Map<String, Object> evidence = object(region.get("evidence"), "evidence");
            Map<String, Object> override = optionalObject(region.get("override"));
            regions.add(new PdfRegion(
                    string(region.get("id"), "region.id"),
                    integer(region.get("pageNumber"), "region.pageNumber"),
                    decimal(region.get("xMin"), "region.xMin"),
                    decimal(region.get("yMin"), "region.yMin"),
                    decimal(region.get("xMax"), "region.xMax"),
                    decimal(region.get("yMax"), "region.yMax"),
                    integer(region.get("columnIndex"), "region.columnIndex"),
                    integer(region.get("readingOrder"), "region.readingOrder"),
                    string(region.get("text"), "region.text"),
                    enumeration(PdfRegionType.class, region.get("automaticType"), "region.automaticType"),
                    enumeration(PdfNarratability.class, region.get("automaticNarratability"), "region.automaticNarratability"),
                    strings(region.get("reasons"), "region.reasons"),
                    new PdfRegionEvidence(
                            enumeration(PdfRegionOrigin.class, evidence.get("origin"), "evidence.origin"),
                            decimal(evidence.get("confidence"), "evidence.confidence"),
                            string(evidence.get("extractorVersion"), "evidence.extractorVersion"),
                            string(evidence.get("parserVersion"), "evidence.parserVersion"),
                            string(evidence.get("groupingVersion"), "evidence.groupingVersion"),
                            string(evidence.get("classifierVersion"), "evidence.classifierVersion")),
                    evidenceCandidates(region.get("evidenceCandidates")),
                    new PdfRegionOverride(
                            nullableString(override.get("text")),
                            nullableEnum(PdfRegionType.class, override.get("type")),
                            nullableEnum(PdfNarratability.class, override.get("narratability")),
                            nullableInteger(override.get("readingOrder"))),
                    stringMap(optionalObject(region.get("attributes"))),
                    longNumber(region.get("revision"), "region.revision")));
        }
        PreparedPdfPage page = new PreparedPdfPage(
                integer(map.get("schemaVersion"), "schemaVersion"),
                integer(map.get("pageNumber"), "pageNumber"),
                decimal(map.get("widthPoints"), "widthPoints"),
                decimal(map.get("heightPoints"), "heightPoints"),
                enumeration(PdfPagePreparationStatus.class, map.get("status"), "status"),
                longNumber(map.get("revision"), "revision"),
                regions,
                derivedTreatments(map.get("derivedTreatments")),
                preparationMetrics(map.get("preparationMetrics")),
                pageAnalysisProfile(map.get("analysisProfile")),
                strings(map.get("warnings"), "warnings"),
                nullableString(map.get("lastAttemptError")));
        if (page.schemaVersion() != PreparedPdfPage.CURRENT_SCHEMA_VERSION) {
            throw new IOException("Versión de página PDF no compatible: " + page.schemaVersion());
        }
        return page;
    }

    private static Object parse(Path path) throws IOException {
        try {
            return SimpleJsonParser.parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (RuntimeException ex) {
            throw new IOException("JSON PDF inválido: " + path, ex);
        }
    }

    private static String manifestJson(PdfDocumentManifest manifest) {
        return "{\n"
                + field("schemaVersion", Integer.toString(manifest.schemaVersion()), true)
                + field("title", quote(manifest.title()), true)
                + field("sourceFile", quote(manifest.sourceFile()), true)
                + field("sourceSha256", quote(manifest.sourceSha256()), true)
                + field("pageCount", Integer.toString(manifest.pageCount()), true)
                + field("preparationSignature", quote(manifest.preparationSignature()), true)
                + field("readingStrategy", quote(manifest.readingStrategy().name()), true)
                + field("nativeTextProvider", quote(manifest.nativeTextProvider().name()), true)
                + field("analysisSummary", analysisSummaryJson(manifest.analysisSummary()), true)
                + field("createdAt", quote(manifest.createdAt().toString()), true)
                + field("updatedAt", quote(manifest.updatedAt().toString()), false)
                + "}\n";
    }

    private static PdfDocumentAnalysisSummary readAnalysisSummary(Object raw) throws IOException {
        if (!(raw instanceof Map<?, ?>)) return PdfDocumentAnalysisSummary.empty();
        Map<String, Object> map = object(raw, "analysisSummary");
        return new PdfDocumentAnalysisSummary(
                longNumber(map.getOrDefault("revision", 0L), "analysisSummary.revision"),
                integer(map.getOrDefault("observedPages", 0), "analysisSummary.observedPages"),
                integer(map.getOrDefault("maximumColumnCount", 0), "analysisSummary.maximumColumnCount"),
                strings(map.getOrDefault("repeatedHeaders", List.of()), "analysisSummary.repeatedHeaders"),
                strings(map.getOrDefault("repeatedFooters", List.of()), "analysisSummary.repeatedFooters"),
                map.containsKey("analyzedAt")
                        ? instant(map.get("analyzedAt"), "analysisSummary.analyzedAt")
                        : Instant.EPOCH);
    }

    private static String analysisSummaryJson(PdfDocumentAnalysisSummary summary) {
        PdfDocumentAnalysisSummary safe = summary == null
                ? PdfDocumentAnalysisSummary.empty() : summary;
        return "{"
                + quote("revision") + ": " + safe.revision() + ", "
                + quote("observedPages") + ": " + safe.observedPages() + ", "
                + quote("maximumColumnCount") + ": " + safe.maximumColumnCount() + ", "
                + quote("repeatedHeaders") + ": " + stringArray(safe.repeatedHeaders()) + ", "
                + quote("repeatedFooters") + ": " + stringArray(safe.repeatedFooters()) + ", "
                + quote("analyzedAt") + ": " + quote(safe.analyzedAt().toString())
                + "}";
    }

    private static String pageJson(PreparedPdfPage page) {
        StringBuilder out = new StringBuilder("{\n");
        out.append(field("schemaVersion", Integer.toString(page.schemaVersion()), true));
        out.append(field("pageNumber", Integer.toString(page.pageNumber()), true));
        out.append(field("widthPoints", number(page.widthPoints()), true));
        out.append(field("heightPoints", number(page.heightPoints()), true));
        out.append(field("status", quote(page.status().name()), true));
        out.append(field("revision", Long.toString(page.revision()), true));
        out.append("  \"warnings\": ").append(stringArray(page.warnings())).append(",\n");
        out.append(field("lastAttemptError", quote(page.lastAttemptError()), true));
        out.append(field("preparationMetrics", preparationMetricsJson(page.preparationMetrics()), true));
        out.append(field("analysisProfile", pageAnalysisProfileJson(page.analysisProfile()), true));
        out.append("  \"derivedTreatments\": ").append(derivedTreatmentArray(page.derivedTreatments())).append(",\n");
        out.append("  \"regions\": [");
        if (!page.regions().isEmpty()) out.append('\n');
        for (int index = 0; index < page.regions().size(); index++) {
            out.append(regionJson(page.regions().get(index), "    "));
            if (index < page.regions().size() - 1) out.append(',');
            out.append('\n');
        }
        out.append("  ]\n}\n");
        return out.toString();
    }

    private static PdfPagePreparationMetrics preparationMetrics(Object raw) throws IOException {
        if (!(raw instanceof Map<?, ?>)) return PdfPagePreparationMetrics.empty();
        Map<String, Object> map = object(raw, "preparationMetrics");
        return new PdfPagePreparationMetrics(
                longNumber(map.getOrDefault("elapsedMillis", 0L), "preparationMetrics.elapsedMillis"),
                longNumber(map.getOrDefault("cpuMillis", 0L), "preparationMetrics.cpuMillis"),
                longNumber(map.getOrDefault("observedHeapBytes", 0L), "preparationMetrics.observedHeapBytes"),
                Boolean.TRUE.equals(map.get("nativeExtractionUsed")),
                Boolean.TRUE.equals(map.get("ocrUsed")));
    }

    private static String preparationMetricsJson(PdfPagePreparationMetrics metrics) {
        PdfPagePreparationMetrics safe = metrics == null
                ? PdfPagePreparationMetrics.empty() : metrics;
        return "{"
                + quote("elapsedMillis") + ": " + safe.elapsedMillis() + ", "
                + quote("cpuMillis") + ": " + safe.cpuMillis() + ", "
                + quote("observedHeapBytes") + ": " + safe.observedHeapBytes() + ", "
                + quote("nativeExtractionUsed") + ": " + safe.nativeExtractionUsed() + ", "
                + quote("ocrUsed") + ": " + safe.ocrUsed()
                + "}";
    }

    private static PdfPageAnalysisProfile pageAnalysisProfile(Object raw) throws IOException {
        if (!(raw instanceof Map<?, ?>)) return PdfPageAnalysisProfile.defaults();
        Map<String, Object> map = object(raw, "analysisProfile");
        Map<String, Object> language = optionalObject(map.get("language"));
        return new PdfPageAnalysisProfile(
                enumeration(PdfPageRole.class,
                        map.getOrDefault("automaticRole", PdfPageRole.UNKNOWN.name()),
                        "analysisProfile.automaticRole"),
                nullableEnum(PdfPageRole.class, map.get("roleOverride")),
                new PdfDocumentLanguageProfile(
                        string(language.getOrDefault("detectedLanguage", "und"),
                                "analysisProfile.language.detectedLanguage"),
                        decimal(language.getOrDefault("confidence", 0.0),
                                "analysisProfile.language.confidence"),
                        nullableString(language.get("overrideLanguage"))),
                enumeration(PdfPreparationProfile.class,
                        map.getOrDefault("preparationProfile", PdfPreparationProfile.STANDARD.name()),
                        "analysisProfile.preparationProfile"),
                integer(map.getOrDefault("normalizedRotationDegrees", 0),
                        "analysisProfile.normalizedRotationDegrees"),
                strings(map.getOrDefault("analysisEngineIds", List.of()),
                        "analysisProfile.analysisEngineIds"));
    }

    private static String pageAnalysisProfileJson(PdfPageAnalysisProfile profile) {
        PdfPageAnalysisProfile safe = profile == null
                ? PdfPageAnalysisProfile.defaults() : profile;
        PdfDocumentLanguageProfile language = safe.language();
        return "{"
                + quote("automaticRole") + ": " + quote(safe.automaticRole().name()) + ", "
                + quote("roleOverride") + ": " + nullableEnum(safe.roleOverride()) + ", "
                + quote("language") + ": {"
                + quote("detectedLanguage") + ": " + quote(language.detectedLanguage()) + ", "
                + quote("confidence") + ": " + number(language.confidence()) + ", "
                + quote("overrideLanguage") + ": " + nullableQuote(language.overrideLanguage())
                + "}, "
                + quote("preparationProfile") + ": " + quote(safe.preparationProfile().name()) + ", "
                + quote("normalizedRotationDegrees") + ": " + safe.normalizedRotationDegrees() + ", "
                + quote("analysisEngineIds") + ": " + stringArray(safe.analysisEngineIds())
                + "}";
    }

    private static List<PdfDerivedTreatment> derivedTreatments(Object raw) throws IOException {
        if (!(raw instanceof List<?> values)) return List.of();
        ArrayList<PdfDerivedTreatment> result = new ArrayList<>();
        for (Object value : values) {
            Map<String, Object> treatment = object(value, "derivedTreatment");
            result.add(new PdfDerivedTreatment(
                    string(treatment.get("id"), "derivedTreatment.id"),
                    enumeration(PdfDerivedTreatmentKind.class, treatment.get("kind"), "derivedTreatment.kind"),
                    strings(treatment.get("sourceRegionIds"), "derivedTreatment.sourceRegionIds"),
                    string(treatment.get("derivedText"), "derivedTreatment.derivedText"),
                    string(treatment.get("modelId"), "derivedTreatment.modelId"),
                    string(treatment.get("modelVersion"), "derivedTreatment.modelVersion"),
                    decimal(treatment.get("confidence"), "derivedTreatment.confidence"),
                    instant(treatment.get("createdAt"), "derivedTreatment.createdAt"),
                    enumeration(PdfDerivedTreatmentState.class,
                            treatment.getOrDefault("state", PdfDerivedTreatmentState.DRAFT.name()),
                            "derivedTreatment.state"),
                    longNumber(treatment.getOrDefault("sourceRevision", 1L),
                            "derivedTreatment.sourceRevision"),
                    string(treatment.getOrDefault("sourceFingerprint", "unknown-source"),
                            "derivedTreatment.sourceFingerprint"),
                    string(treatment.getOrDefault("prompt", ""), "derivedTreatment.prompt"),
                    stringMap(optionalObject(treatment.get("metadata")))));
        }
        return List.copyOf(result);
    }

    private static String derivedTreatmentArray(List<PdfDerivedTreatment> values) {
        List<PdfDerivedTreatment> safe = values == null ? List.of() : values;
        return "[" + safe.stream().map(value -> "{"
                        + quote("id") + ": " + quote(value.id()) + ", "
                        + quote("kind") + ": " + quote(value.kind().name()) + ", "
                        + quote("sourceRegionIds") + ": " + stringArray(value.sourceRegionIds()) + ", "
                        + quote("derivedText") + ": " + quote(value.derivedText()) + ", "
                        + quote("modelId") + ": " + quote(value.modelId()) + ", "
                        + quote("modelVersion") + ": " + quote(value.modelVersion()) + ", "
                        + quote("confidence") + ": " + number(value.confidence()) + ", "
                        + quote("createdAt") + ": " + quote(value.createdAt().toString()) + ", "
                        + quote("state") + ": " + quote(value.state().name()) + ", "
                        + quote("sourceRevision") + ": " + value.sourceRevision() + ", "
                        + quote("sourceFingerprint") + ": " + quote(value.sourceFingerprint()) + ", "
                        + quote("prompt") + ": " + quote(value.prompt()) + ", "
                        + quote("metadata") + ": " + stringMapJson(value.metadata())
                        + "}")
                .collect(java.util.stream.Collectors.joining(", ")) + "]";
    }

    private static String regionJson(PdfRegion region, String indent) {
        PdfRegionEvidence evidence = region.evidence();
        PdfRegionOverride override = region.override();
        return indent + "{\n"
                + indent + "  \"id\": " + quote(region.id()) + ",\n"
                + indent + "  \"pageNumber\": " + region.pageNumber() + ",\n"
                + indent + "  \"xMin\": " + number(region.xMin()) + ",\n"
                + indent + "  \"yMin\": " + number(region.yMin()) + ",\n"
                + indent + "  \"xMax\": " + number(region.xMax()) + ",\n"
                + indent + "  \"yMax\": " + number(region.yMax()) + ",\n"
                + indent + "  \"columnIndex\": " + region.columnIndex() + ",\n"
                + indent + "  \"readingOrder\": " + region.readingOrder() + ",\n"
                + indent + "  \"text\": " + quote(region.text()) + ",\n"
                + indent + "  \"automaticType\": " + quote(region.automaticType().name()) + ",\n"
                + indent + "  \"automaticNarratability\": " + quote(region.automaticNarratability().name()) + ",\n"
                + indent + "  \"reasons\": " + stringArray(region.reasons()) + ",\n"
                + indent + "  \"evidence\": {\n"
                + indent + "    \"origin\": " + quote(evidence.origin().name()) + ",\n"
                + indent + "    \"confidence\": " + number(evidence.confidence()) + ",\n"
                + indent + "    \"extractorVersion\": " + quote(evidence.extractorVersion()) + ",\n"
                + indent + "    \"parserVersion\": " + quote(evidence.parserVersion()) + ",\n"
                + indent + "    \"groupingVersion\": " + quote(evidence.groupingVersion()) + ",\n"
                + indent + "    \"classifierVersion\": " + quote(evidence.classifierVersion()) + "\n"
                + indent + "  },\n"
                + indent + "  \"evidenceCandidates\": " + evidenceArray(region.evidenceCandidates()) + ",\n"
                + indent + "  \"override\": {\n"
                + indent + "    \"text\": " + nullableQuote(override.text()) + ",\n"
                + indent + "    \"type\": " + nullableEnum(override.type()) + ",\n"
                + indent + "    \"narratability\": " + nullableEnum(override.narratability()) + ",\n"
                + indent + "    \"readingOrder\": " + (override.readingOrder() == null ? "null" : override.readingOrder()) + "\n"
                + indent + "  },\n"
                + indent + "  \"attributes\": " + stringMapJson(region.attributes()) + ",\n"
                + indent + "  \"revision\": " + region.revision() + "\n"
                + indent + "}";
    }

    private static List<PdfRegionEvidence> evidenceCandidates(Object raw) throws IOException {
        if (!(raw instanceof List<?> values)) return List.of();
        ArrayList<PdfRegionEvidence> result = new ArrayList<>();
        for (Object value : values) {
            Map<String, Object> evidence = object(value, "evidenceCandidate");
            result.add(new PdfRegionEvidence(
                    enumeration(PdfRegionOrigin.class, evidence.get("origin"), "evidenceCandidate.origin"),
                    decimal(evidence.get("confidence"), "evidenceCandidate.confidence"),
                    string(evidence.get("extractorVersion"), "evidenceCandidate.extractorVersion"),
                    string(evidence.get("parserVersion"), "evidenceCandidate.parserVersion"),
                    string(evidence.get("groupingVersion"), "evidenceCandidate.groupingVersion"),
                    string(evidence.get("classifierVersion"), "evidenceCandidate.classifierVersion")));
        }
        return List.copyOf(result);
    }

    private static String evidenceArray(List<PdfRegionEvidence> values) {
        return "[" + (values == null ? List.<PdfRegionEvidence>of() : values).stream()
                .map(evidence -> "{"
                        + quote("origin") + ": " + quote(evidence.origin().name()) + ", "
                        + quote("confidence") + ": " + number(evidence.confidence()) + ", "
                        + quote("extractorVersion") + ": " + quote(evidence.extractorVersion()) + ", "
                        + quote("parserVersion") + ": " + quote(evidence.parserVersion()) + ", "
                        + quote("groupingVersion") + ": " + quote(evidence.groupingVersion()) + ", "
                        + quote("classifierVersion") + ": " + quote(evidence.classifierVersion())
                        + "}")
                .collect(java.util.stream.Collectors.joining(", ")) + "]";
    }

    private static String field(String name, String value, boolean comma) {
        return "  " + quote(name) + ": " + value + (comma ? "," : "") + "\n";
    }

    private static String stringArray(List<String> values) {
        return "[" + (values == null ? List.<String>of() : values).stream()
                .map(JsonPreparedPdfDocumentRepository::quote)
                .collect(java.util.stream.Collectors.joining(", ")) + "]";
    }

    private static String stringMapJson(Map<String, String> values) {
        return "{" + (values == null ? Map.<String, String>of() : values).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> quote(entry.getKey()) + ": " + quote(entry.getValue()))
                .collect(java.util.stream.Collectors.joining(", ")) + "}";
    }

    private static String nullableQuote(String value) {
        return value == null ? "null" : quote(value);
    }

    private static String nullableEnum(Enum<?> value) {
        return value == null ? "null" : quote(value.name());
    }

    private static String number(double value) {
        return String.format(java.util.Locale.ROOT, "%.6f", value);
    }

    private static String quote(String value) {
        String safe = value == null ? "" : value;
        StringBuilder out = new StringBuilder(safe.length() + 8).append('"');
        for (int index = 0; index < safe.length(); index++) {
            char character = safe.charAt(index);
            switch (character) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (character < 0x20) out.append(String.format("\\u%04x", (int) character));
                    else out.append(character);
                }
            }
        }
        return out.append('"').toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value, String field) throws IOException {
        if (!(value instanceof Map<?, ?> map)) throw new IOException(field + " must be an object");
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> optionalObject(Object value) throws IOException {
        if (value == null) return Map.of();
        if (!(value instanceof Map<?, ?> map)) throw new IOException("Expected object");
        return (Map<String, Object>) map;
    }

    private static List<?> array(Object value, String field) throws IOException {
        if (!(value instanceof List<?> list)) throw new IOException(field + " must be an array");
        return list;
    }

    private static List<String> strings(Object value, String field) throws IOException {
        ArrayList<String> result = new ArrayList<>();
        for (Object item : array(value, field)) {
            if (!(item instanceof String text)) throw new IOException(field + " entries must be strings");
            result.add(text);
        }
        return List.copyOf(result);
    }

    private static Map<String, String> stringMap(Map<String, Object> value) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        value.forEach((key, item) -> {
            if (item != null) result.put(key, String.valueOf(item));
        });
        return Map.copyOf(result);
    }

    private static String string(Object value, String field) throws IOException {
        if (!(value instanceof String text)) throw new IOException(field + " must be a string");
        return text;
    }

    private static String nullableString(Object value) throws IOException {
        return value == null ? null : string(value, "nullable string");
    }

    private static int integer(Object value, String field) throws IOException {
        if (!(value instanceof Number number)) throw new IOException(field + " must be a number");
        return number.intValue();
    }

    private static Integer nullableInteger(Object value) throws IOException {
        return value == null ? null : integer(value, "nullable integer");
    }

    private static long longNumber(Object value, String field) throws IOException {
        if (!(value instanceof Number number)) throw new IOException(field + " must be a number");
        return number.longValue();
    }

    private static double decimal(Object value, String field) throws IOException {
        if (!(value instanceof Number number)) throw new IOException(field + " must be a number");
        return number.doubleValue();
    }

    private static Instant instant(Object value, String field) throws IOException {
        try {
            return Instant.parse(string(value, field));
        } catch (RuntimeException ex) {
            throw new IOException(field + " must be an ISO-8601 instant", ex);
        }
    }

    private static <E extends Enum<E>> E enumeration(Class<E> type, Object value, String field) throws IOException {
        try {
            return Enum.valueOf(type, string(value, field));
        } catch (RuntimeException ex) {
            throw new IOException(field + " contains an unsupported value", ex);
        }
    }

    private static <E extends Enum<E>> E nullableEnum(Class<E> type, Object value) throws IOException {
        return value == null ? null : enumeration(type, value, "nullable enum");
    }
}
