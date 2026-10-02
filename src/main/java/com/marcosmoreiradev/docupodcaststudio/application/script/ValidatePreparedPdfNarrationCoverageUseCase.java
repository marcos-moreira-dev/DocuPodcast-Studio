package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.application.document.SecondarySemanticComponentClassifier;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfRegionContentSignals;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Detects canonical PDF regions that vanished without narration or an explicit reason. */
public final class ValidatePreparedPdfNarrationCoverageUseCase {
    public static final String COVERED_REGION_IDS = "pdfCoveredRegionIds";
    public static final String COVERAGE_REASON = "pdfCoverageReason";
    private final SecondarySemanticComponentClassifier classifier = new SecondarySemanticComponentClassifier();

    public PdfNarrationCoverageReport validate(List<PreparedPdfPage> pages,
                                               NarrationScriptDocument script,
                                               SecondarySemanticReadingPolicy policy) {
        Map<String, List<String>> narrated = new LinkedHashMap<>();
        Map<String, List<String>> covered = new LinkedHashMap<>();
        Map<String, String> coveredReasons = new LinkedHashMap<>();
        if (script != null) for (NarrationSegment segment : script.segments()) {
            segment.sourceBlockIds().forEach(id -> narrated.computeIfAbsent(id, ignored -> new ArrayList<>()).add(segment.id()));
            Arrays.stream(segment.metadata().getOrDefault(COVERED_REGION_IDS, "").split(","))
                    .map(String::strip).filter(id -> !id.isBlank()).forEach(id -> {
                        covered.computeIfAbsent(id, ignored -> new ArrayList<>()).add(segment.id());
                        coveredReasons.put(id, segment.metadata().getOrDefault(COVERAGE_REASON, "equivalent-content"));
                    });
        }
        SecondarySemanticReadingPolicy effective = policy == null
                ? SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF : policy;
        ArrayList<PdfNarrationCoverageItem> result = new ArrayList<>();
        if (pages != null) for (PreparedPdfPage page : pages) for (PdfRegion region : page.regions()) {
            List<String> segmentIds = narrated.get(region.id());
            if (segmentIds != null && !segmentIds.isEmpty()) {
                result.add(item(region, PdfNarrationCoverageStatus.NARRATED, segmentIds, "segment-created"));
                continue;
            }
            segmentIds = covered.get(region.id());
            if (segmentIds != null && !segmentIds.isEmpty()) {
                result.add(item(region, PdfNarrationCoverageStatus.COVERED_BY_EQUIVALENT, segmentIds,
                        coveredReasons.get(region.id())));
                continue;
            }
            String skip = intentionalSkipReason(page, region, effective);
            result.add(item(region, skip.isBlank() ? PdfNarrationCoverageStatus.INCOMPLETE
                            : PdfNarrationCoverageStatus.INTENTIONALLY_SKIPPED,
                    List.of(), skip.isBlank() ? "expected-narration-without-segment" : skip));
        }
        return new PdfNarrationCoverageReport(result);
    }

    /** Inferred NON_NARRATABLE must not erase substantive semantic containers. */
    public static boolean expectedToNarrate(PdfRegion region) {
        if (region == null || region.effectiveText().isBlank()) return false;
        if (region.container() || !region.playbackTarget()) return false;
        if (region.override().narratability() != null) {
            return region.effectiveNarratability() == PdfNarratability.NARRATABLE;
        }
        if (region.effectiveNarratability() == PdfNarratability.NARRATABLE) return true;
        return switch (region.effectiveType()) {
            case SIDEBAR, CAPTION, TABLE, MATH -> substantive(region.effectiveText());
            default -> false;
        };
    }

    private String intentionalSkipReason(PreparedPdfPage page, PdfRegion region,
                                         SecondarySemanticReadingPolicy policy) {
        if (region.container()) return "structural-container";
        if (!region.playbackTarget()) return "not-a-playback-target";
        if (region.effectiveText().isBlank() && region.effectiveType() != PdfRegionType.IMAGE) return "empty-region";
        if (region.override().narratability() != null
                && region.effectiveNarratability() != PdfNarratability.NARRATABLE) return "manual-non-narratable";
        if (switch (region.effectiveType()) {
            case HEADER, FOOTER, PAGE_NUMBER -> true;
            default -> false;
        }) return "editorial-" + region.effectiveType().name().toLowerCase(java.util.Locale.ROOT);
        if (region.effectiveType() == PdfRegionType.UNKNOWN) return "unknown-region";
        if (region.override().narratability() == null
                && PdfRegionContentSignals.looksLikeSparseTechnicalLabel(page, region)) {
            return "sparse-technical-label";
        }
        if (classifier.classify(region).secondary() && !policy.includes(classifier.classify(region))) {
            return "excluded-by-secondary-semantic-policy";
        }
        if (region.effectiveType() == PdfRegionType.IMAGE) return "image-without-admitted-treatment";
        if (region.effectiveType() == PdfRegionType.MATH
                && !new PdfMathSpeechNormalizer().safeForTts(
                new PdfMathSpeechNormalizer().normalize(region, region.effectiveText()))) {
            return "unsafe-math-for-tts";
        }
        if (!expectedToNarrate(region)) return "inferred-non-narratable";
        return "";
    }

    private static boolean substantive(String text) {
        String normalized = text == null ? "" : text.replaceAll("[^\\p{L}\\p{N}]", "");
        return normalized.length() >= 3;
    }

    private static PdfNarrationCoverageItem item(PdfRegion region, PdfNarrationCoverageStatus status,
                                                  List<String> segmentIds, String reason) {
        return new PdfNarrationCoverageItem(region.pageNumber(), region.id(), region.effectiveReadingOrder(),
                region.effectiveType().name(), status, segmentIds, reason);
    }
}
