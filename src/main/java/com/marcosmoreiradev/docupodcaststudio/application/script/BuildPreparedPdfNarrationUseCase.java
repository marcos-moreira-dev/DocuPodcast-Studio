package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfDocumentRepository;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfWorkspaceRef;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNarrationFocusMetadata;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNarrationBindingMetadata;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNarrationFragmentBindingMetadata;
import com.marcosmoreiradev.docupodcaststudio.application.document.BuildPdfNarrationFragmentBindingsUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfReadingOrderResolver;
import com.marcosmoreiradev.docupodcaststudio.application.document.ResolvePdfPageMapUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRegionContentSignals;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfSemanticNarrationSafetyValidator;
import com.marcosmoreiradev.docupodcaststudio.application.document.SecondarySemanticComponentClassifier;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationTextLayers;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfObjectNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationFocusRef;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfSemanticTextLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentListeningPreferences;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.TableNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticComponentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Builds narration directly from NARRATABLE canonical PDF V2 regions. */
public final class BuildPreparedPdfNarrationUseCase {
    private final PreparedPdfDocumentRepository repository;
    private final ResolvePdfPageMapUseCase pageMapResolver;
    private final SecondarySemanticComponentClassifier semanticClassifier =
            new SecondarySemanticComponentClassifier();
    private final PdfSemanticNarrationSafetyValidator narrationSafety =
            new PdfSemanticNarrationSafetyValidator();
    private final PdfMathSpeechNormalizer mathSpeech = new PdfMathSpeechNormalizer();
    private final PdfTableNarrationTextBuilder tableSpeech =
            new PdfTableNarrationTextBuilder();

    public BuildPreparedPdfNarrationUseCase(PreparedPdfDocumentRepository repository) {
        this(repository, null);
    }

    public BuildPreparedPdfNarrationUseCase(PreparedPdfDocumentRepository repository,
                                             ResolvePdfPageMapUseCase pageMapResolver) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.pageMapResolver = pageMapResolver;
    }

    public NarrationScriptDocument build(PreparedPdfWorkspaceRef workspace, String title,
                                         String language, boolean readAfterColon,
                                         ReadingProfile profile) {
        SecondarySemanticReadingPolicy semanticPolicy = profile == null
                ? SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF
                : SecondarySemanticReadingPolicy.fromLegacy(
                        profile.imagePolicy(), profile.tablePolicy());
        return build(workspace, title, language, readAfterColon, profile,
                semanticPolicy);
    }

    public NarrationScriptDocument build(PreparedPdfWorkspaceRef workspace, String title,
                                         String language, boolean readAfterColon,
                                         ReadingProfile profile,
                                         SecondarySemanticReadingPolicy semanticPolicy) {
        try {
            return buildPages(workspace, title, language, readAfterColon, profile,
                    semanticPolicy, repository.loadPages(workspace.projectRoot()));
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo construir la narración desde PDF V2.", ex);
        }
    }

    /** Builds only one already-published canonical page for incremental audio production. */
    public NarrationScriptDocument buildPage(PreparedPdfWorkspaceRef workspace, int pageNumber,
                                             String title, String language, boolean readAfterColon,
                                             ReadingProfile profile,
                                             SecondarySemanticReadingPolicy semanticPolicy) {
        try {
            PreparedPdfPage page = repository.loadPage(workspace.projectRoot(), pageNumber)
                    .orElseThrow(() -> new IllegalStateException(
                            "La página PDF " + pageNumber + " todavía no está publicada."));
            return buildPages(workspace, title, language, readAfterColon, profile,
                    semanticPolicy, List.of(page));
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo construir la narración incremental de la página "
                    + pageNumber + ".", ex);
        }
    }

    /** Replaces one PDF page in an existing script while preserving its stable script identity. */
    public NarrationScriptDocument mergePage(NarrationScriptDocument current,
                                             NarrationScriptDocument pageScript,
                                             int pageNumber) {
        Objects.requireNonNull(pageScript, "pageScript");
        if (current == null || current.empty()) return pageScript;
        java.util.TreeMap<Integer, List<NarrationSegment>> byPage = new java.util.TreeMap<>();
        current.segments().stream()
                .filter(segment -> pdfPage(segment) != pageNumber)
                .forEach(segment -> byPage.computeIfAbsent(pdfPage(segment), ignored -> new ArrayList<>())
                        .add(segment));
        byPage.put(pageNumber, pageScript.segments());
        List<NarrationSegment> merged = byPage.values().stream().flatMap(List::stream).toList();
        return new NarrationScriptDocument(current.id(), current.title(), current.language(),
                current.sourceDocumentTitle(), merged, current.createdAt(),
                java.time.Instant.now(), current.notes());
    }

    private NarrationScriptDocument buildPages(PreparedPdfWorkspaceRef workspace, String title,
                                                String language, boolean readAfterColon,
                                                ReadingProfile profile,
                                                SecondarySemanticReadingPolicy semanticPolicy,
                                                List<PreparedPdfPage> pages) throws IOException {
        TableNarrationPolicy tablePolicy = profile == null
                ? TableNarrationPolicy.IGNORE_TABLES : profile.tablePolicy();
        SecondarySemanticReadingPolicy selectedSemanticPolicy =
                Objects.requireNonNullElse(semanticPolicy,
                        SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF);
            List<NarrationSegment> segments = new ArrayList<>();
            for (PreparedPdfPage page : pages) {
                java.util.Map<String, PdfNarrationBinding> pageMapBindings = pageMapBindings(workspace, page);
                java.util.HashSet<String> emittedTreatments = new java.util.HashSet<>();
                String latestImageNarration = "";
                int latestImageOrder = -2;
                for (PdfRegion region :
                        new PdfReadingOrderResolver().resolve(page)) {
                    if (region.container() || !region.playbackTarget()) continue;
                    SecondarySemanticComponentKind componentKind =
                            semanticClassifier.classify(region);
                    if (componentKind.secondary()
                            && !selectedSemanticPolicy.includes(componentKind)) {
                        continue;
                    }
                    PdfDerivedTreatment derived = narratableTreatment(
                            page, region, selectedSemanticPolicy);
                    if (derived == null
                            && componentKind == SecondarySemanticComponentKind.IMAGE) continue;
                    if (derived == null
                            && region.effectiveType() == PdfRegionType.UNKNOWN) continue;
                    boolean manualNarratability = region.override().narratability() != null;
                    if (derived == null && manualNarratability
                            && region.effectiveNarratability() != PdfNarratability.NARRATABLE) continue;
                    if (!manualNarratability && derived == null
                            && PdfRegionContentSignals
                            .looksLikeSparseTechnicalLabel(page, region)) continue;
                    if (!manualNarratability && derived == null
                            && !ValidatePreparedPdfNarrationCoverageUseCase.expectedToNarrate(region)) continue;
                    if (derived != null && !emittedTreatments.add(derived.id())) continue;
                    String selectedText = derived == null
                            ? narrationText(page, region, tablePolicy)
                            : mathSpeech.normalizeSpeech(region, derived.derivedText());
                    if (region.effectiveType() == PdfRegionType.MATH
                            && !mathSpeech.safeForTts(selectedText)) continue;
                    if (region.effectiveType() == PdfRegionType.CAPTION
                            && region.effectiveReadingOrder() == latestImageOrder + 1
                            && redundantCaption(selectedText, latestImageNarration)) {
                        markLatestEquivalent(segments, page.pageNumber(), region.id(), "redundant-image-caption");
                        continue;
                    }
                    String narrationText = ReadAfterColonTextPolicy.narrationText(
                            selectedText, readAfterColon);
                    if (narrationText.isBlank()) continue;
                    PdfSemanticTextLayer sourceLayer = narrationSourceLayer(region, derived);
                    PdfObjectNarrationPolicy narrationPolicy = narrationPolicy(region, derived);
                    PdfNarrationTextLayers layers = new PdfNarrationTextLayers(
                            region.effectiveText(),
                            derived == null ? "" : derived.derivedText(),
                            narrationText,
                            sourceLayer);
                    LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
                    metadata.put("sourceBlockType", region.effectiveType().name());
                    metadata.put("sourceBlockId", region.id());
                    metadata.put("pdfRegionRole", region.regionRole().name());
                    metadata.put("pdfPlaybackTarget",
                            Boolean.toString(region.playbackTarget()));
                    metadata.put("pdfParentRegionId", region.parentId());
                    metadata.put("pdfContentRoute", region.contentRoute().name());
                    metadata.put("sourceRegionIds", region.id());
                    boolean usesPageMapBinding = derived == null && pageMapBindings.containsKey(region.id());
                    metadata.put("generationSource", usesPageMapBinding
                            ? "pdf-page-map-v1" : "prepared-pdf-v2");
                    metadata.put("pdfSourcePage", Integer.toString(page.pageNumber()));
                    metadata.put("pdfRegionRevision", Long.toString(region.revision()));
                    metadata.put("pdfReadingOrder",
                            Integer.toString(region.effectiveReadingOrder()));
                    metadata.put("pdfGroupingRule", "region-v2");
                    PdfNarrationBinding binding = usesPageMapBinding
                            ? pageMapBindings.get(region.id())
                            : narrationBinding(region, derived, sourceLayer, narrationPolicy);
                    metadata.put(PdfNarrationBindingMetadata.KEY,
                            PdfNarrationBindingMetadata.encode(binding));
                    String stableSegmentId = segmentId(region.id());
                    metadata.put(PdfNarrationFragmentBindingMetadata.KEY,
                            PdfNarrationFragmentBindingMetadata.encode(
                                    new BuildPdfNarrationFragmentBindingsUseCase().build(
                                            stableSegmentId, region, layers.narrationText(), sourceLayer,
                                            page.widthPoints(), page.heightPoints())));
                    metadata.put("pdfNarrationSourceLayer", sourceLayer.name());
                    metadata.put("pdfNarrationPolicy", narrationPolicy.name());
                    metadata.put("pdfMathSourceConvention",
                            mathSpeech.convention(region, region.effectiveText()));
                    metadata.put("pdfSecondarySemanticPolicy",
                            selectedSemanticPolicy.name());
                    metadata.put("sourceLanguage",
                            page.analysisProfile().language().effectiveLanguage());
                    metadata.put("pdfLiteralSource",
                            region.override().text() == null
                                    ? "extracted" : "human-override");
                    if (derived != null) {
                        PdfDerivedTreatment selectedDerived = derived;
                        metadata.put("pdfDerivedTreatmentId", derived.id());
                        metadata.put("pdfDerivedTreatmentFingerprint",
                                derived.sourceFingerprint() + "|" + derived.sourceRevision()
                                        + "|" + derived.modelId() + "|" + derived.modelVersion());
                        metadata.put("pdfTreatmentAdmission",
                                derived.approved() ? "MANUAL_APPROVAL"
                                        : PdfSemanticNarrationSafetyValidator.AUTOMATIC_ADMISSION);
                        List<PdfRegion> focused = page.regions().stream()
                                .filter(candidate -> selectedDerived.sourceRegionIds()
                                        .contains(candidate.id())).toList();
                        PdfNarrationFocusRef focus = new PdfNarrationFocusRef(page.pageNumber(),
                                derived.sourceRegionIds(),
                                focusBoxes(derived, focused),
                                derived.kind() == PdfDerivedTreatmentKind.TABLE_NARRATION
                                        ? "Celdas narradas" : "Zona narrada");
                        metadata.put(PdfNarrationFocusMetadata.KEY,
                                PdfNarrationFocusMetadata.encode(focus));
                    }
                    if (region.effectiveType() == PdfRegionType.IMAGE) {
                        latestImageNarration = narrationText;
                        latestImageOrder = region.effectiveReadingOrder();
                    } else if (region.effectiveType() != PdfRegionType.CAPTION) {
                        latestImageNarration = "";
                        latestImageOrder = -2;
                    }
                    NarrationSegment candidate = new NarrationSegment(stableSegmentId,
                            segmentType(region.effectiveType()),
                            preview(layers.narrationText()), layers.narrationText(),
                            binding.sourceRegionIds(), characterIdFor(layers.narrationText()),
                            "VOC-NARRATOR", "STY-NEUTRAL", metadata);
                    appendWithDuplicateAudit(segments, page, region, candidate);
                }
            }
            String safeTitle = title == null || title.isBlank() ? "Documento PDF" : title.strip();
            NarrationScriptDocument script = NarrationScriptDocument.create(safeTitle, language, safeTitle, segments);
            PdfNarrationCoverageReport coverage = new ValidatePreparedPdfNarrationCoverageUseCase()
                    .validate(pages, script, selectedSemanticPolicy);
            if (!coverage.complete()) {
                System.getLogger(BuildPreparedPdfNarrationUseCase.class.getName()).log(
                        System.Logger.Level.WARNING,
                        "Cobertura PDF incompleta: " + coverage.incompleteItems());
            }
            return script;
    }

    /**
     * Duplicate semantic regions are reconciled while SOURCE is still available.
     * The richer current region replaces a truncated recovery prefix; export never
     * deduplicates or fuzzy-matches speech.
     */
    private static void appendWithDuplicateAudit(List<NarrationSegment> segments,
                                                 PreparedPdfPage page,
                                                 PdfRegion region,
                                                 NarrationSegment candidate) {
        java.util.Set<String> candidateTokens = significantTokens(candidate.narrationText());
        for (int index = segments.size() - 1; index >= 0; index--) {
            NarrationSegment existing = segments.get(index);
            if (!Integer.toString(page.pageNumber()).equals(
                    existing.metadata().get("pdfSourcePage"))) continue;
            PdfRegion existingRegion = page.regions().stream()
                    .filter(value -> existing.sourceBlockIds().contains(value.id()))
                    .findFirst().orElse(null);
            if (existingRegion == null || !sameReadingColumnAndTouching(existingRegion, region)) continue;
            java.util.Set<String> existingTokens = significantTokens(existing.narrationText());
            if (candidateTokens.isEmpty() || existingTokens.isEmpty()) continue;
            long shared = candidateTokens.stream().filter(existingTokens::contains).count();
            double overlap = shared / (double) Math.min(candidateTokens.size(), existingTokens.size());
            if (overlap < 0.88) continue;
            boolean candidateRicher = candidateTokens.size() > existingTokens.size()
                    || candidate.narrationText().length() > existing.narrationText().length();
            if (candidateRicher) {
                segments.remove(index);
                segments.add(withCoveredRegion(candidate, existingRegion.id(),
                        "duplicate-overlap-richer-source"));
            } else {
                markEquivalent(segments, existing.id(), region.id(),
                        "duplicate-overlap-existing-source");
            }
            return;
        }
        segments.add(candidate);
    }

    private static NarrationSegment withCoveredRegion(NarrationSegment segment,
                                                       String coveredRegionId,
                                                       String reason) {
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>(segment.metadata());
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        java.util.Arrays.stream(metadata.getOrDefault(
                        ValidatePreparedPdfNarrationCoverageUseCase.COVERED_REGION_IDS, "").split(","))
                .map(String::strip).filter(value -> !value.isBlank()).forEach(ids::add);
        ids.add(coveredRegionId);
        metadata.put(ValidatePreparedPdfNarrationCoverageUseCase.COVERED_REGION_IDS,
                String.join(",", ids));
        metadata.put(ValidatePreparedPdfNarrationCoverageUseCase.COVERAGE_REASON, reason);
        metadata.put("pdfDuplicateNarrationResolved", "true");
        return new NarrationSegment(segment.id(), segment.type(), segment.title(),
                segment.narrationText(), segment.sourceBlockIds(), segment.characterId(),
                segment.voiceProfileId(), segment.performanceStyleId(), metadata);
    }

    private static boolean sameReadingColumnAndTouching(PdfRegion left, PdfRegion right) {
        double horizontalIntersection = Math.max(0.0,
                Math.min(left.xMax(), right.xMax()) - Math.max(left.xMin(), right.xMin()));
        double minimumWidth = Math.min(left.xMax() - left.xMin(), right.xMax() - right.xMin());
        if (minimumWidth <= 0.0 || horizontalIntersection / minimumWidth < 0.70) return false;
        double verticalGap = Math.max(0.0,
                Math.max(left.yMin(), right.yMin()) - Math.min(left.yMax(), right.yMax()));
        return verticalGap <= 12.0 || containsEither(left, right);
    }

    private static boolean containsEither(PdfRegion left, PdfRegion right) {
        return containment(left, right) >= 0.90 || containment(right, left) >= 0.90;
    }

    private static double containment(PdfRegion outer, PdfRegion inner) {
        double x1 = Math.max(outer.xMin(), inner.xMin());
        double y1 = Math.max(outer.yMin(), inner.yMin());
        double x2 = Math.min(outer.xMax(), inner.xMax());
        double y2 = Math.min(outer.yMax(), inner.yMax());
        double intersection = Math.max(0.0, x2 - x1) * Math.max(0.0, y2 - y1);
        double innerArea = (inner.xMax() - inner.xMin()) * (inner.yMax() - inner.yMin());
        return innerArea <= 0.0 ? 0.0 : intersection / innerArea;
    }

    private static void markLatestEquivalent(List<NarrationSegment> segments, int pageNumber,
                                             String coveredRegionId, String reason) {
        for (int index = segments.size() - 1; index >= 0; index--) {
            NarrationSegment segment = segments.get(index);
            if (Integer.toString(pageNumber).equals(segment.metadata().get("pdfSourcePage"))) {
                markEquivalent(segments, segment.id(), coveredRegionId, reason);
                return;
            }
        }
    }

    private static void markEquivalent(List<NarrationSegment> segments, String segmentId,
                                       String coveredRegionId, String reason) {
        for (int index = segments.size() - 1; index >= 0; index--) {
            NarrationSegment current = segments.get(index);
            if (!current.id().equals(segmentId)) continue;
            LinkedHashMap<String, String> metadata = new LinkedHashMap<>(current.metadata());
            java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
            java.util.Arrays.stream(metadata.getOrDefault(
                            ValidatePreparedPdfNarrationCoverageUseCase.COVERED_REGION_IDS, "").split(","))
                    .map(String::strip).filter(value -> !value.isBlank()).forEach(ids::add);
            ids.add(coveredRegionId);
            metadata.put(ValidatePreparedPdfNarrationCoverageUseCase.COVERED_REGION_IDS,
                    String.join(",", ids));
            metadata.put(ValidatePreparedPdfNarrationCoverageUseCase.COVERAGE_REASON, reason);
            segments.set(index, new NarrationSegment(current.id(), current.type(), current.title(),
                    current.narrationText(), current.sourceBlockIds(), current.characterId(),
                    current.voiceProfileId(), current.performanceStyleId(), metadata));
            return;
        }
    }

    private static int pdfPage(NarrationSegment segment) {
        try {
            return Integer.parseInt(segment.metadata().getOrDefault("pdfSourcePage", "0"));
        } catch (NumberFormatException invalid) {
            return 0;
        }
    }

    private java.util.Map<String, PdfNarrationBinding> pageMapBindings(
            PreparedPdfWorkspaceRef workspace, PreparedPdfPage page) throws IOException {
        if (pageMapResolver == null) return java.util.Map.of();
        java.util.LinkedHashMap<String, PdfNarrationBinding> bindings = new java.util.LinkedHashMap<>();
        pageMapResolver.resolve(workspace, page.pageNumber()).pageMap().narrationBindings()
                .forEach(binding -> binding.sourceRegionIds().forEach(id -> bindings.putIfAbsent(id, binding)));
        return java.util.Map.copyOf(bindings);
    }

    private PdfDerivedTreatment narratableTreatment(
            PreparedPdfPage page, PdfRegion region,
            SecondarySemanticReadingPolicy policy) {
        boolean manuallyRejected = page.derivedTreatments().stream()
                .filter(value -> treatmentCurrent(page, value))
                .filter(value -> value.sourceRegionIds().contains(region.id()))
                .anyMatch(value -> value.state()
                        == com.marcosmoreiradev.docupodcaststudio.domain.document.pdf
                        .PdfDerivedTreatmentState.REJECTED);
        if (manuallyRejected) return null;
        return page.derivedTreatments().stream()
                .filter(value -> treatmentCurrent(page, value))
                .filter(value -> value.sourceRegionIds().contains(region.id()))
                .filter(value -> value.kind() == PdfDerivedTreatmentKind.IMAGE_DESCRIPTION
                        || value.kind() == PdfDerivedTreatmentKind.MATHEMATICAL_READING
                        || value.kind() == PdfDerivedTreatmentKind.TABLE_NARRATION
                        || value.kind() == PdfDerivedTreatmentKind.SMALL_TABLE_NARRATION
                        || value.kind() == PdfDerivedTreatmentKind.LIGHTWEIGHT_LANGUAGE_MODEL)
                .filter(value -> policy.includes(
                        semanticClassifier.classify(value.kind())))
                .filter(value -> value.approved()
                        ? narrationSafety.safeApprovedText(value)
                        : value.state() == com.marcosmoreiradev.docupodcaststudio.domain.document.pdf
                                .PdfDerivedTreatmentState.DRAFT
                                && DocumentListeningPreferences.QUALITY_MODEL.equals(value.modelId())
                                && narrationSafety.safeForAutomaticAdmission(value))
                .sorted(java.util.Comparator
                        .comparingInt(BuildPreparedPdfNarrationUseCase::treatmentPriority)
                        .reversed()
                        .thenComparing(PdfDerivedTreatment::createdAt,
                                java.util.Comparator.reverseOrder()))
                .findFirst().orElse(null);
    }

    private static boolean treatmentCurrent(
            PreparedPdfPage page, PdfDerivedTreatment treatment) {
        long currentRevision = page.regions().stream()
                .filter(region -> treatment.sourceRegionIds().contains(region.id()))
                .mapToLong(PdfRegion::revision).max().orElse(-1L);
        return currentRevision == treatment.sourceRevision()
                || "legacy-source".equals(treatment.sourceFingerprint());
    }

    private static int treatmentPriority(PdfDerivedTreatment treatment) {
        if ("manual-user".equals(treatment.modelId())
                || "manual".equals(treatment.metadata().get("source"))) {
            return 3;
        }
        if (!treatment.metadata().getOrDefault(
                "technicalElementId", "").isBlank()) {
            return 2;
        }
        return 1;
    }

    private static PdfSemanticTextLayer narrationSourceLayer(
            PdfRegion region, PdfDerivedTreatment treatment) {
        if (treatment != null) return PdfSemanticTextLayer.INTERPRETATION;
        if (region.effectiveType() == PdfRegionType.TABLE
                || region.effectiveType() == PdfRegionType.MATH) {
            return PdfSemanticTextLayer.NARRATION;
        }
        return PdfSemanticTextLayer.LITERAL;
    }

    private static PdfObjectNarrationPolicy narrationPolicy(
            PdfRegion region, PdfDerivedTreatment treatment) {
        if (treatment == null) {
            return switch (region.effectiveType()) {
                case TABLE -> PdfObjectNarrationPolicy.READ_ALL;
                case MATH -> PdfObjectNarrationPolicy.READ_EXACT;
                default -> PdfObjectNarrationPolicy.READ_EXACT;
            };
        }
        return switch (treatment.kind()) {
            case IMAGE_DESCRIPTION -> PdfObjectNarrationPolicy.DESCRIBE_BRIEFLY;
            case MATHEMATICAL_READING -> PdfObjectNarrationPolicy.READ_EXACT;
            case SMALL_TABLE_NARRATION -> PdfObjectNarrationPolicy.READ_ALL;
            case TABLE_NARRATION -> PdfObjectNarrationPolicy.SUMMARIZE;
            default -> PdfObjectNarrationPolicy.READ_EXACT;
        };
    }

    private static PdfNarrationBinding narrationBinding(
            PdfRegion region,
            PdfDerivedTreatment treatment,
            PdfSemanticTextLayer sourceLayer,
            PdfObjectNarrationPolicy policy) {
        if (treatment != null) {
            return new PdfNarrationBinding(
                    region.pageNumber(), treatment.sourceRegionIds(), sourceLayer,
                    policy, treatment.id(), treatment.sourceRevision(),
                    treatment.sourceFingerprint());
        }
        return new PdfNarrationBinding(
                region.pageNumber(), List.of(region.id()), sourceLayer, policy,
                "", region.revision(), literalFingerprint(region));
    }

    private static String literalFingerprint(PdfRegion region) {
        String authority = region.override().text() == null
                ? "extracted" : "human-override";
        return String.join("|",
                authority,
                region.id(),
                Long.toString(region.revision()),
                region.evidence().origin().name(),
                region.evidence().extractorVersion(),
                region.evidence().parserVersion(),
                region.evidence().groupingVersion(),
                region.evidence().classifierVersion());
    }

    private static List<PdfNarrationFocusRef.FocusBox> focusBoxes(
            PdfDerivedTreatment treatment, List<PdfRegion> fallback) {
        java.util.Optional<PdfNarrationFocusRef.FocusBox> complete =
                focusBox(treatment.metadata().get("focusBounds"));
        if (complete.isPresent()) return List.of(complete.get());
        if (treatment.kind() == PdfDerivedTreatmentKind.TABLE_NARRATION
                || treatment.kind()
                == PdfDerivedTreatmentKind.SMALL_TABLE_NARRATION) {
            List<PdfNarrationFocusRef.FocusBox> cells =
                    java.util.Arrays.stream(treatment.metadata()
                                    .getOrDefault("focusCellIds", "").split(","))
                            .map(String::strip)
                            .filter(value -> !value.isBlank())
                            .map(id -> treatment.metadata().get(
                                    "focusCell." + id))
                            .filter(java.util.Objects::nonNull)
                            .map(BuildPreparedPdfNarrationUseCase::focusBox)
                            .flatMap(java.util.Optional::stream)
                            .toList();
            if (!cells.isEmpty()) return cells;
        }
        return fallback.stream()
                .map(candidate -> new PdfNarrationFocusRef.FocusBox(
                        candidate.xMin(), candidate.yMin(),
                        candidate.xMax(), candidate.yMax()))
                .toList();
    }

    private static java.util.Optional<PdfNarrationFocusRef.FocusBox> focusBox(
            String value) {
        try {
            String[] parts = value.split(",");
            if (parts.length != 4) return java.util.Optional.empty();
            return java.util.Optional.of(new PdfNarrationFocusRef.FocusBox(
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3])));
        } catch (RuntimeException invalid) {
            return java.util.Optional.empty();
        }
    }

    private String narrationText(PreparedPdfPage page, PdfRegion region,
                                 TableNarrationPolicy tablePolicy) {
        if (region.effectiveType() == PdfRegionType.TABLE) {
            if (tablePolicy.canonical()
                    == TableNarrationPolicy.ANNOUNCE_ONLY) {
                return "Tabla detectada.";
            }
            return page.derivedTreatments().stream()
                    .filter(value -> (value.kind() == PdfDerivedTreatmentKind.TABLE_NARRATION
                            || value.kind() == PdfDerivedTreatmentKind.SMALL_TABLE_NARRATION)
                            && value.approved()
                            && treatmentCurrent(page, value)
                            && value.sourceRegionIds().contains(region.id()))
                    .sorted(java.util.Comparator.comparing(
                            PdfDerivedTreatment::createdAt).reversed())
                    .map(PdfDerivedTreatment::derivedText).findFirst()
                    .orElseGet(() -> tableSpeech.build(region));
        }
        String literal = switch (region.effectiveType()) {
            case TITLE -> "Tema principal: " + region.effectiveText() + ".";
            case HEADING -> "Nuevo tema: " + region.effectiveText() + ".";
            case SUBHEADING -> "Ahora veremos: " + region.effectiveText() + ".";
            case SIDEBAR -> "Recuadro: " + region.effectiveText();
            default -> region.effectiveText();
        };
        return mathSpeech.normalize(region, literal);
    }

    private static boolean redundantCaption(String caption, String imageSpeech) {
        java.util.Set<String> left = significantTokens(caption);
        java.util.Set<String> right = significantTokens(imageSpeech);
        if (left.isEmpty() || right.isEmpty()) return false;
        long shared = left.stream().filter(right::contains).count();
        return shared / (double) Math.min(left.size(), right.size()) >= 0.88;
    }

    private static java.util.Set<String> significantTokens(String value) {
        return java.util.Arrays.stream((value == null ? "" : value)
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^\\p{L}\\p{N}]+", " ").split("\\s+"))
                .map(String::strip).filter(token -> token.length() >= 3)
                .filter(token -> !java.util.Set.of("una", "uno", "del", "los",
                        "las", "que", "con", "para", "por", "figura", "imagen")
                        .contains(token))
                .collect(java.util.stream.Collectors.toCollection(
                        java.util.LinkedHashSet::new));
    }

    private static NarrationSegmentType segmentType(PdfRegionType type) {
        return switch (type) {
            case TITLE -> NarrationSegmentType.TITLE;
            case HEADING -> NarrationSegmentType.HEADING;
            case SUBHEADING -> NarrationSegmentType.SUBHEADING;
            case LIST -> NarrationSegmentType.LIST_ITEM;
            case TABLE -> NarrationSegmentType.TABLE_NOTICE;
            case IMAGE, CAPTION -> NarrationSegmentType.IMAGE_NOTICE;
            default -> NarrationSegmentType.PARAGRAPH;
        };
    }

    private static String segmentId(String regionId) {
        UUID uuid = UUID.nameUUIDFromBytes(
                (regionId + "|region-v2").getBytes(StandardCharsets.UTF_8));
        return "PDFSEG-" + uuid.toString().replace("-", "").substring(0, 16)
                .toUpperCase(Locale.ROOT);
    }

    private static String preview(String text) {
        String safe = text == null ? "" : text.strip();
        return safe.length() <= 60 ? safe : safe.substring(0, 57).strip() + "...";
    }

    private static String characterIdFor(String text) {
        String normalized = text == null ? "" : text.strip();
        int colon = normalized.indexOf(':');
        if (colon < 2 || colon > 42) return "CHR-NARRATOR";
        String speaker = normalized.substring(0, colon).strip();
        if (speaker.isBlank() || speaker.equalsIgnoreCase("Escena")
                || speaker.toUpperCase(Locale.ROOT).startsWith("ESCENA ")) return "CHR-NARRATOR";
        String token = java.text.Normalizer.normalize(speaker, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-").replaceAll("^-|-$", "");
        return token.isBlank() ? "CHR-NARRATOR" : "CHR-" + token;
    }
}
