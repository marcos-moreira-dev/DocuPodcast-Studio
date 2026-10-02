package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Detects material omissions without requiring literal transcription equality. */
public final class PdfSemanticCoverageValidator {
    static final double REQUIRED_TOKEN_COVERAGE = 0.84;
    static final double REQUIRED_BLOCK_COVERAGE = 0.58;
    static final double REQUIRED_SHORT_BLOCK_COVERAGE = 0.80;
    static final double REPEATED_MARGIN_RATIO = 0.06;
    static final double EDITORIAL_MARGIN_RATIO = 0.08;
    private static final double EDITORIAL_MAX_SEMANTIC_OVERLAP = 0.18;
    private static final Set<String> CONTENT_STOPWORDS = Set.of(
            "a", "al", "de", "del", "el", "en", "es", "la", "las", "lo",
            "los", "no", "o", "para", "por", "que", "se", "su", "un", "una",
            "and", "for", "in", "is", "of", "on", "or", "the", "to", "with");
    private static final Pattern BODY_CONTINUATION_SIGNAL = Pattern.compile(
            "(?iu)\\b(?:conclusion|por consiguiente|por tanto|resultado|teorema|"
                    + "demostracion|prueba|limite|ecuacion|therefore|thus|result|"
                    + "theorem|proof|equation)\\b");
    private static final Pattern PAGE_NUMBER = Pattern.compile("^(?:pagina\\s*)?\\d{1,4}$");
    private static final Pattern EMPTY_VISUAL_SIGNAL = Pattern.compile(
            "(?i)(pagina en blanco|sin contenido (?:textual|visible)|no hay (?:texto|rotulos|elementos)|blank page|no visible content|no text)");
    private static final Pattern MATH_SIGNAL = Pattern.compile(
            "(?i)(\\\\(?:frac|sqrt|lim|sum|int|sin|cos|tan|log|leftarrow|lor)|[=<>≤≥∑∫√^_])");

    private static final Pattern SPARSE_MATH_SIGNAL = Pattern.compile(
            "(?iu)(\\\\|[=<>+*/^_\\u2192\\u2212\\u2264\\u2265\\u2211\\u222b\\u221a\\u00b1]"
                    + "|\\b(?:sin|cos|tan|lim|sqrt|log|frac)\\b)");

    public PdfSemanticCoverageResult validate(
            PdfTextLayer nativeLayer,
            PdfNativeTextQualityReport nativeQuality,
            PdfSemanticPageAnalysis candidate,
            boolean verifierAttempted) {
        ArrayList<String> reasons = new ArrayList<>();
        ArrayList<String> missing = new ArrayList<>();
        if (candidate == null || candidate.elements().isEmpty()) {
            return new PdfSemanticCoverageResult(
                    PdfSemanticCoverageStatus.REJECTED, false, 0.0,
                    List.of("missingRequiredContent"), List.of());
        }
        validateSemanticCoherence(candidate, reasons);
        boolean reliable = nativeLayer != null && nativeQuality != null
                && nativeQuality.quality() == PdfNativeTextQuality.RELIABLE;
        if (!reliable) {
            reasons.add("noReliableCoverageEvidence");
            PdfSemanticCoverageStatus status;
            if (!verifierAttempted) {
                status = PdfSemanticCoverageStatus.NEEDS_VERIFICATION;
            } else if (hasBlockingCoherenceReason(reasons)) {
                reasons.add("verifierDisagreement");
                status = PdfSemanticCoverageStatus.REJECTED;
            } else {
                reasons.add("visualVerifierNoMissingRegions");
                status = PdfSemanticCoverageStatus.ACCEPTED;
            }
            return new PdfSemanticCoverageResult(status, false, 0.0,
                    reasons, List.of());
        }

        String semantic = candidate.elements().stream()
                .map(PdfSemanticPageAnalysis.Element::sourceText)
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
        List<String> semanticTokens = tokens(semantic);
        long evidenceTokens = 0L;
        long coveredTokens = 0L;
        List<PdfTextLine> evidenceLines = meaningfulLines(nativeLayer,
                semanticTokens);
        List<PdfSemanticCoverageEvidenceAudit> evidenceAudit = audit(
                nativeLayer, candidate);
        Map<String, PdfSemanticCoverageEvidenceAudit> auditById = evidenceAudit.stream()
                .collect(java.util.stream.Collectors.toMap(
                        PdfSemanticCoverageEvidenceAudit::evidenceId,
                        value -> value, (left, right) -> left));
        for (int lineIndex = 0; lineIndex < evidenceLines.size(); lineIndex++) {
            PdfTextLine line = evidenceLines.get(lineIndex);
            List<String> block = tokens(line.text());
            if (block.size() < 3) continue;
            PdfSemanticCoverageEvidenceAudit decision = auditById.get(
                    evidenceId(line));
            if (decision != null && switch (decision.classification()) {
                case MATH_FRAGMENT, HEADER_FOOTER, OCR_NOISE, EDITORIAL_MARGIN -> true;
                default -> false;
            }) continue;
            double ratio = decision != null
                    && decision.semanticRegionType() == PdfRegionType.TABLE
                    && !decision.significantMissingContent()
                    ? 1.0 : tokenRecall(block, semanticTokens);
            evidenceTokens += block.size();
            coveredTokens += Math.round(ratio * block.size());
            double threshold = block.size() <= 8
                    ? REQUIRED_SHORT_BLOCK_COVERAGE : REQUIRED_BLOCK_COVERAGE;
            // PDFBox commonly emits one displayed formula as several narrow
            // lines (operator, limit subscript, numerator). Keep those tokens
            // in the aggregate score, but do not require 80% recall from each
            // lossy glyph fragment independently. Ordinary short prose and
            // headings still use the strict short-block threshold.
            if (ratio < threshold
                    && (decision == null || decision.significantMissingContent())) {
                missing.add(line.text());
            }
        }
        if (evidenceTokens == 0L) {
            List<String> aggregate = tokens(coverageLines(nativeLayer,
                            semanticTokens).stream().filter(line -> {
                        PdfSemanticCoverageEvidenceAudit decision = auditById.get(
                                evidenceId(line));
                        return decision == null || switch (decision.classification()) {
                            case MATH_FRAGMENT, HEADER_FOOTER, OCR_NOISE,
                                    EDITORIAL_MARGIN -> false;
                            default -> true;
                        };
                    })
                    .map(PdfTextLine::text)
                    .collect(java.util.stream.Collectors.joining(" ")));
            if (aggregate.size() >= 8) {
                evidenceTokens = aggregate.size();
                coveredTokens = Math.round(tokenRecall(
                        aggregate, semanticTokens) * aggregate.size());
            } else if (!evidenceAudit.isEmpty()
                    && evidenceAudit.stream().noneMatch(
                    PdfSemanticCoverageEvidenceAudit::significantMissingContent)) {
                // A page made only of already-covered structured math/table
                // fragments must not fall back to literal OCR equality.
                evidenceTokens = 1L;
                coveredTokens = 1L;
            }
        }
        double overall = coveredTokens / (double) Math.max(1L, evidenceTokens);
        if (overall < REQUIRED_TOKEN_COVERAGE) reasons.add("nativeTextCoverageGap");
        if (!missing.isEmpty()) reasons.add("missingSignificantTextBlocks");
        boolean gap = overall < REQUIRED_TOKEN_COVERAGE || !missing.isEmpty();
        PdfSemanticCoverageStatus status;
        if (gap || hasBlockingCoherenceReason(reasons)) {
            status = verifierAttempted
                    ? PdfSemanticCoverageStatus.REJECTED
                    : PdfSemanticCoverageStatus.NEEDS_VERIFICATION;
            if (verifierAttempted) reasons.add("verifierDisagreement");
        } else {
            status = PdfSemanticCoverageStatus.ACCEPTED;
        }
        return new PdfSemanticCoverageResult(status, true, overall,
                reasons, missing.stream().limit(12).toList());
    }

    /** Produces the same per-line classification consumed by validation. */
    public List<PdfSemanticCoverageEvidenceAudit> audit(
            PdfTextLayer layer, PdfSemanticPageAnalysis candidate) {
        if (layer == null || candidate == null) return List.of();
        List<String> semanticTokens = tokens(candidate.elements().stream()
                .map(PdfSemanticPageAnalysis.Element::sourceText)
                .collect(java.util.stream.Collectors.joining(" ")));
        List<PdfTextLine> all = layer.lines().stream().filter(line ->
                line != null && line.region() != null
                        && !normalized(line.text()).isBlank()).toList();
        Set<PdfTextLine> editorial = editorialMarginLines(all, semanticTokens);
        ArrayList<PdfSemanticCoverageEvidenceAudit> result = new ArrayList<>();
        for (PdfTextLine line : all) {
            List<String> block = tokens(line.text());
            boolean margin = outerMargin(line);
            StructuredMatch structured = bestStructuredMatch(line, candidate);
            double globalSimilarity = tokenRecall(block, semanticTokens);
            boolean mathematical = sparseMathematicalFragment(line, block)
                    || SPARSE_MATH_SIGNAL.matcher(line.text()).find()
                    || structured.type() == PdfRegionType.MATH;
            PdfSemanticCoverageEvidenceAudit.Classification classification;
            boolean missing;
            if (margin) {
                classification = PdfSemanticCoverageEvidenceAudit.Classification.HEADER_FOOTER;
                missing = false;
            } else if (editorial.contains(line)) {
                classification = PdfSemanticCoverageEvidenceAudit.Classification.EDITORIAL_MARGIN;
                missing = false;
            } else if (structured.covered()) {
                classification = structured.type() == PdfRegionType.MATH
                        ? PdfSemanticCoverageEvidenceAudit.Classification.MATH_FRAGMENT
                        : PdfSemanticCoverageEvidenceAudit.Classification.COVERED_DIFFERENT_REPRESENTATION;
                missing = false;
            } else if (sparseMathematicalFragment(line, block)) {
                classification = PdfSemanticCoverageEvidenceAudit.Classification.MATH_FRAGMENT;
                missing = false;
            } else if (likelyOcrNoise(line, block)) {
                classification = PdfSemanticCoverageEvidenceAudit.Classification.OCR_NOISE;
                missing = false;
            } else {
                double threshold = block.size() <= 8
                        ? REQUIRED_SHORT_BLOCK_COVERAGE : REQUIRED_BLOCK_COVERAGE;
                missing = block.size() >= 3 && globalSimilarity < threshold;
                classification = missing
                        ? PdfSemanticCoverageEvidenceAudit.Classification.TRUE_MISSING_CONTENT
                        : PdfSemanticCoverageEvidenceAudit.Classification.COVERED_DIFFERENT_REPRESENTATION;
            }
            result.add(new PdfSemanticCoverageEvidenceAudit(
                    evidenceId(line), line.text(), line.region(), area(line.region()),
                    evidenceSource(layer, line), structured.regionId(), structured.type(),
                    structured.overlap(), Math.max(globalSimilarity,
                    structured.textSimilarity()), mathematical, margin,
                    classification, missing));
        }
        return List.copyOf(result);
    }

    private static double area(PdfPageRegion box) {
        return Math.max(0.0, box.xMaxPoints() - box.xMinPoints())
                * Math.max(0.0, box.yMaxPoints() - box.yMinPoints());
    }

    private static String evidenceSource(PdfTextLayer layer, PdfTextLine line) {
        if (layer.origin() != PdfTextLayerOrigin.OCR_LOCAL) {
            return layer.origin().name();
        }
        boolean rasterConfidence = line.confidence() < 0.999
                || line.tokens().stream().anyMatch(token -> token.confidence() < 0.999);
        return rasterConfidence ? "OCR_LINE" : "PDF_NATIVE_TEXT";
    }

    private static void validateSemanticCoherence(
            PdfSemanticPageAnalysis candidate, List<String> reasons) {
        long proseTokens = candidate.elements().stream()
                .filter(element -> element.type() == PdfRegionType.PARAGRAPH)
                .mapToLong(element -> tokens(element.sourceText()).size())
                .sum();
        PdfPageRole role = candidate.pageRole();
        long totalTokens = candidate.elements().stream()
                .mapToLong(element -> tokens(element.sourceText()).size())
                .sum();
        if ((role == PdfPageRole.INDEX || role == PdfPageRole.CATALOG
                || role == PdfPageRole.BIBLIOGRAPHY)
                && totalTokens < 12) {
            reasons.add("suspiciousSparseStructuredPage");
        }
        boolean onlyImages = candidate.elements().stream().allMatch(element ->
                element.type() == PdfRegionType.IMAGE);
        long visibleSourceTokens = candidate.elements().stream()
                .map(PdfSemanticPageAnalysis.Element::sourceText)
                .map(PdfSemanticCoverageValidator::tokens)
                .mapToLong(List::size).sum();
        String visualNarration = candidate.elements().stream()
                .map(PdfSemanticPageAnalysis.Element::narrationText)
                .collect(java.util.stream.Collectors.joining(" "));
        if (onlyImages && visibleSourceTokens <= 2
                && EMPTY_VISUAL_SIGNAL.matcher(normalized(visualNarration)).find()) {
            reasons.add("noSubstantiveVisibleContent");
        }
        if ((role == PdfPageRole.INDEX || role == PdfPageRole.CATALOG
                || role == PdfPageRole.VISUAL_REFERENCE)
                && proseTokens >= 80) {
            reasons.add("suspiciousPageRole");
        }
        for (PdfSemanticPageAnalysis.Element element : candidate.elements()) {
            String source = element.sourceText();
            if (element.type() == PdfRegionType.TABLE
                    && !hasObservableTableCells(source)) {
                reasons.add("tableMissingVisibleCells");
            }
            if (element.type() == PdfRegionType.IMAGE
                    && element.narrationText().isBlank()) {
                reasons.add("imageMissingVisualExplanation");
            }
            if (element.type() == PdfRegionType.MATH
                    && tokens(source).size() >= 12
                    && !MATH_SIGNAL.matcher(source).find()) {
                reasons.add("suspiciousMathType");
            }
        }
    }

    private static boolean hasObservableTableCells(String source) {
        List<String> lines = source == null ? List.of() : source.lines()
                .map(String::strip).filter(value -> !value.isBlank()).toList();
        if (lines.size() < 2) return false;
        if (source.contains(";") || source.contains("|") || source.contains("\t")) {
            return true;
        }
        long labeledCells = lines.stream().filter(line -> line.contains(":")).count();
        long listedRows = lines.stream().filter(line -> line.startsWith("-")
                || line.matches("^\\d+[.)].*")).count();
        return labeledCells >= 3 || listedRows >= 2;
    }

    private static boolean hasBlockingCoherenceReason(List<String> reasons) {
        return reasons.stream().anyMatch(value -> value.equals("tableMissingVisibleCells")
                || value.equals("imageMissingVisualExplanation")
                || value.equals("suspiciousMathType")
                || value.equals("suspiciousPageRole")
                || value.equals("suspiciousSparseStructuredPage")
                || value.equals("noSubstantiveVisibleContent"));
    }

    private static List<PdfTextLine> meaningfulLines(PdfTextLayer layer,
                                                     List<String> semanticTokens) {
        return coverageLines(layer, semanticTokens).stream().filter(line -> {
            String normalized = normalized(line.text());
            return tokens(normalized).size() >= 3 || normalized.length() >= 18;
        }).toList();
    }

    private static List<PdfTextLine> coverageLines(PdfTextLayer layer,
                                                   List<String> semanticTokens) {
        List<PdfTextLine> candidates = layer.lines().stream().filter(line -> {
            String normalized = normalized(line.text());
            return !normalized.isBlank() && !PAGE_NUMBER.matcher(normalized).matches();
        }).toList();
        Set<PdfTextLine> editorial = editorialMarginLines(candidates,
                semanticTokens);
        return candidates.stream().filter(line -> {
            String normalized = normalized(line.text());
            PdfPageRegion box = line.region();
            double top = box.yMinPoints() / box.pageHeightPoints();
            double bottom = box.yMaxPoints() / box.pageHeightPoints();
            if ((top < REPEATED_MARGIN_RATIO
                    || bottom > 1.0 - REPEATED_MARGIN_RATIO)
                    && tokens(normalized).size() <= 12) {
                return false;
            }
            return !editorial.contains(line);
        }).toList();
    }

    private static Set<PdfTextLine> editorialMarginLines(
            List<PdfTextLine> lines, List<String> semanticTokens) {
        if (lines.isEmpty()) return Set.of();
        List<Double> heights = lines.stream().map(PdfTextLine::region)
                .map(region -> region.yMaxPoints() - region.yMinPoints())
                .filter(value -> value > 0.0).sorted().toList();
        double medianHeight = heights.get(heights.size() / 2);
        HashSet<PdfTextLine> editorial = new HashSet<>();
        classifyMarginSide(lines, semanticTokens, medianHeight, true, editorial);
        classifyMarginSide(lines, semanticTokens, medianHeight, false, editorial);
        return Set.copyOf(editorial);
    }

    private static void classifyMarginSide(
            List<PdfTextLine> all, List<String> semanticTokens,
            double medianHeight, boolean top, Set<PdfTextLine> editorial) {
        List<PdfTextLine> margin = all.stream().filter(line -> {
            PdfPageRegion box = line.region();
            return top
                    ? box.yMaxPoints() / box.pageHeightPoints()
                    < EDITORIAL_MARGIN_RATIO
                    : box.yMinPoints() / box.pageHeightPoints()
                    > 1.0 - EDITORIAL_MARGIN_RATIO;
        }).sorted(java.util.Comparator.comparingDouble(
                line -> line.region().yMinPoints())).toList();
        ArrayList<PdfTextLine> cluster = new ArrayList<>();
        for (PdfTextLine line : margin) {
            if (!cluster.isEmpty()) {
                PdfTextLine previous = cluster.getLast();
                double gap = line.region().yMinPoints()
                        - previous.region().yMaxPoints();
                if (gap > medianHeight * 2.25) {
                    classifyEditorialCluster(all, semanticTokens, medianHeight,
                            top, cluster, editorial);
                    cluster.clear();
                }
            }
            cluster.add(line);
        }
        classifyEditorialCluster(all, semanticTokens, medianHeight,
                top, cluster, editorial);
    }

    private static void classifyEditorialCluster(
            List<PdfTextLine> all, List<String> semanticTokens,
            double medianHeight, boolean top, List<PdfTextLine> cluster,
            Set<PdfTextLine> editorial) {
        if (cluster.isEmpty()) return;
        double start = cluster.stream().map(PdfTextLine::region)
                .mapToDouble(PdfPageRegion::yMinPoints).min().orElse(0.0);
        double end = cluster.stream().map(PdfTextLine::region)
                .mapToDouble(PdfPageRegion::yMaxPoints).max().orElse(0.0);
        if (end - start > Math.max(18.0, medianHeight * 3.0)) return;
        String text = cluster.stream().map(PdfTextLine::text)
                .collect(java.util.stream.Collectors.joining(" "));
        List<String> contentTokens = contentTokens(text);
        if (contentTokens.isEmpty() || contentTokens.size() > 48) return;
        if (BODY_CONTINUATION_SIGNAL.matcher(normalized(text)).find()) return;
        List<String> semanticContent = semanticTokens.stream()
                .filter(token -> !CONTENT_STOPWORDS.contains(token)).toList();
        if (tokenRecall(contentTokens, semanticContent)
                >= EDITORIAL_MAX_SEMANTIC_OVERLAP) return;
        double isolation = top
                ? all.stream().filter(line -> !cluster.contains(line))
                .map(PdfTextLine::region)
                .mapToDouble(PdfPageRegion::yMinPoints)
                .filter(value -> value >= end).min().orElse(Double.POSITIVE_INFINITY) - end
                : start - all.stream().filter(line -> !cluster.contains(line))
                .map(PdfTextLine::region)
                .mapToDouble(PdfPageRegion::yMaxPoints)
                .filter(value -> value <= start).max().orElse(Double.NEGATIVE_INFINITY);
        if (isolation >= medianHeight * 6.0) editorial.addAll(cluster);
    }

    private static List<String> contentTokens(String value) {
        return tokens(value).stream()
                .filter(token -> !CONTENT_STOPWORDS.contains(token)).toList();
    }

    private static double tokenRecall(List<String> evidence, List<String> candidate) {
        Map<String, Integer> available = new HashMap<>();
        candidate.forEach(token -> available.merge(token, 1, Integer::sum));
        int matched = 0;
        for (String token : evidence) {
            int count = available.getOrDefault(token, 0);
            if (count <= 0) continue;
            matched++;
            available.put(token, count - 1);
        }
        return matched / (double) Math.max(1, evidence.size());
    }

    private static boolean sparseMathematicalFragment(PdfTextLine line,
                                                       List<String> block) {
        if (line == null || line.region() == null || block.size() > 5) return false;
        PdfPageRegion box = line.region();
        double widthRatio = (box.xMaxPoints() - box.xMinPoints())
                / box.pageWidthPoints();
        return widthRatio <= 0.40
                && SPARSE_MATH_SIGNAL.matcher(line.text()).find();
    }

    private static boolean likelyOcrNoise(PdfTextLine line, List<String> block) {
        // Deliberately narrow: low-confidence OCR is not enough by itself.
        // Require a very short line dominated by one/two-character glyphs so
        // ordinary short prose (for example "el resultado es") still blocks.
        if (line == null || block.isEmpty() || block.size() > 6
                || line.confidence() >= 0.55) return false;
        long weak = block.stream().filter(token -> token.length() <= 2
                || token.chars().allMatch(Character::isDigit)).count();
        return weak / (double) block.size() >= 0.75;
    }

    private static boolean outerMargin(PdfTextLine line) {
        PdfPageRegion box = line.region();
        String value = normalized(line.text());
        return (box.yMinPoints() / box.pageHeightPoints() < REPEATED_MARGIN_RATIO
                || box.yMaxPoints() / box.pageHeightPoints()
                > 1.0 - REPEATED_MARGIN_RATIO)
                && tokens(value).size() <= 12;
    }

    private static StructuredMatch bestStructuredMatch(
            PdfTextLine line, PdfSemanticPageAnalysis candidate) {
        PdfPageRegion source = line.region();
        double x1 = source.xMinPoints() / source.pageWidthPoints() * 1000.0;
        double y1 = source.yMinPoints() / source.pageHeightPoints() * 1000.0;
        double x2 = source.xMaxPoints() / source.pageWidthPoints() * 1000.0;
        double y2 = source.yMaxPoints() / source.pageHeightPoints() * 1000.0;
        PdfSemanticPageAnalysis.NormalizedBox evidence =
                new PdfSemanticPageAnalysis.NormalizedBox(x1, y1, x2, y2);
        StructuredMatch best = StructuredMatch.none();
        for (PdfSemanticPageAnalysis.Element element : candidate.elements()) {
            if (element.type() != PdfRegionType.MATH
                    && element.type() != PdfRegionType.TABLE) continue;
            double overlap = containment(evidence, element.box());
            double verticalGap = verticalGap(evidence, element.box());
            boolean weakFragment = tokens(line.text()).size() <= 8;
            boolean covered = overlap >= 0.30
                    || element.type() == PdfRegionType.MATH
                    && weakFragment && verticalGap <= 24.0;
            double similarity = tokenRecall(tokens(line.text()),
                    tokens(element.sourceText()));
            double score = Math.max(overlap, covered ? 0.31 : 0.0);
            if (score > best.score()) {
                best = new StructuredMatch(element.responseId(), element.type(),
                        overlap, similarity, covered, score);
            }
        }
        return best;
    }

    private static double containment(PdfSemanticPageAnalysis.NormalizedBox line,
                                      PdfSemanticPageAnalysis.NormalizedBox region) {
        double intersection = Math.max(0.0,
                Math.min(line.xMax(), region.xMax()) - Math.max(line.xMin(), region.xMin()))
                * Math.max(0.0,
                Math.min(line.yMax(), region.yMax()) - Math.max(line.yMin(), region.yMin()));
        double area = Math.max(1.0, (line.xMax() - line.xMin())
                * (line.yMax() - line.yMin()));
        return intersection / area;
    }

    private static double verticalGap(PdfSemanticPageAnalysis.NormalizedBox left,
                                      PdfSemanticPageAnalysis.NormalizedBox right) {
        if (left.yMax() < right.yMin()) return right.yMin() - left.yMax();
        if (right.yMax() < left.yMin()) return left.yMin() - right.yMax();
        return 0.0;
    }

    private static String evidenceId(PdfTextLine line) {
        PdfPageRegion box = line.region();
        return Integer.toHexString((normalized(line.text()) + '|'
                + Math.round(box.xMinPoints() * 10.0) + '|'
                + Math.round(box.yMinPoints() * 10.0) + '|'
                + Math.round(box.xMaxPoints() * 10.0) + '|'
                + Math.round(box.yMaxPoints() * 10.0)).hashCode());
    }

    private record StructuredMatch(String regionId, PdfRegionType type,
                                   double overlap, double textSimilarity,
                                   boolean covered, double score) {
        static StructuredMatch none() {
            return new StructuredMatch("", null, 0.0, 0.0, false, 0.0);
        }
    }

    static List<String> tokens(String value) {
        String normalized = normalized(value)
                .replaceAll("(?<=\\p{L})-\\s+(?=\\p{L})", "")
                .replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
        return normalized.isBlank() ? List.of()
                : List.of(normalized.split("\\s+"));
    }

    static String normalized(String value) {
        return Normalizer.normalize(value == null ? "" : value,
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('\u00ad', '-')
                .replaceAll("\\s+", " ").strip();
    }
}
