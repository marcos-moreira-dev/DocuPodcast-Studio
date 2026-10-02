package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationFocusRef;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Builds a cheap projection from source blocks or already-persisted PDF artifacts. */
public final class BuildDocumentContentProjectionUseCase {
    private final OpenPreparedPdfWorkspaceUseCase preparedPdfWorkspace;

    public BuildDocumentContentProjectionUseCase(
            OpenPreparedPdfWorkspaceUseCase preparedPdfWorkspace) {
        this.preparedPdfWorkspace = Objects.requireNonNull(
                preparedPdfWorkspace, "preparedPdfWorkspace");
    }

    public DocumentContentProjection build(
            ProjectDocumentSource source, NarrationScriptDocument narration) {
        Objects.requireNonNull(source, "source");
        if (source instanceof BlockDocumentSource blocks) {
            return fromBlocks(blocks.document(), narration);
        }
        return fromPreparedPdf((PreparedPdfSource) source, narration);
    }

    private DocumentContentProjection fromBlocks(
            ReadableDocument document, NarrationScriptDocument narration) {
        Map<String, List<NarrationSegment>> byBlock = segmentsBySourceId(narration);
        ArrayList<DocumentContentItem> items = new ArrayList<>();
        for (DocumentBlock block : document.blocks()) {
            List<NarrationSegment> segments = byBlock.getOrDefault(block.id(), List.of());
            String narrationText = joinedNarration(segments);
            DocumentContentKind kind = wordKind(block.type());
            // A secondary visual is not narratable merely because the importer
            // gave its notice block some display text.  Only an admitted
            // narration segment may turn it into spoken content; otherwise it
            // remains available to video as a silent source-capture slide.
            String effectiveText = narrationText.isBlank() && block.narratable()
                    && !kind.secondarySemanticComponent()
                    ? block.text() : narrationText;
            String fingerprint = sha256(String.join("|",
                    document.format().name(), block.id(), block.type().name(),
                    block.text(), effectiveText, block.metadata().toString()));
            items.add(new DocumentContentItem(
                    block.id(), kind, block.text(), effectiveText,
                    segments.stream().map(NarrationSegment::id).toList(),
                    List.of(block.id()), List.of(), fingerprint, blockRevision(block),
                    DocumentPresentationPolicy.forWord(block),
                    new WordContentAnchor(block.id(), block.metadata())));
        }
        return new DocumentContentProjection(document.title(), document.format(),
                document.sourcePath(), items);
    }

    private DocumentContentProjection fromPreparedPdf(
            PreparedPdfSource source, NarrationScriptDocument narration) {
        List<PreparedPdfPage> pages = preparedPdfWorkspace.loadPreparedPages(source.workspace());
        Map<Integer, PreparedPdfPage> pageByNumber = pages.stream().collect(
                java.util.stream.Collectors.toMap(PreparedPdfPage::pageNumber,
                        page -> page, (left, right) -> left, LinkedHashMap::new));
        LinkedHashMap<String, PdfItemBuilder> grouped = new LinkedHashMap<>();
        if (narration != null) {
            for (NarrationSegment segment : narration.segments()) {
                PdfNarrationBinding binding = PdfNarrationBindingMetadata.decode(
                        segment.metadata().get(PdfNarrationBindingMetadata.KEY)).orElse(null);
                if (binding == null) continue;
                PreparedPdfPage page = pageByNumber.get(binding.pageNumber());
                if (page == null) continue;
                String ownerId = semanticContentId(
                        binding.pageNumber(), binding.sourceRegionIds(), segment.id());
                PdfItemBuilder item = grouped.computeIfAbsent(ownerId,
                        ignored -> new PdfItemBuilder(ownerId, pdfKind(segment), page));
                item.acceptLegacyId(legacyOwnerId(segment, binding));
                item.accept(segment, binding);
            }
        }
        DetectPdfVisualObjectsUseCase visualDetector =
                new DetectPdfVisualObjectsUseCase();
        for (PreparedPdfPage page : pages) {
            for (PdfVisualObjectProposal proposal : visualDetector.detect(page)) {
                DocumentContentKind proposalKind = pdfKind(proposal.type());
                if (!proposalKind.secondarySemanticComponent()) continue;
                if (explicitlyRejected(page, proposal)) continue;
                PdfItemBuilder narratedOwner = grouped.values().stream()
                        .filter(item -> item.pageNumber() == page.pageNumber())
                        .filter(item -> item.overlaps(proposal))
                        .findFirst().orElse(null);
                if (narratedOwner != null) {
                    narratedOwner.acceptLegacyId(
                            "PDF-COMP-" + safeToken(proposal.id()));
                    narratedOwner.accept(proposal);
                    continue;
                }
                String contentId = semanticContentId(page.pageNumber(),
                        proposal.sourceRegionIds(), proposal.id());
                grouped.computeIfAbsent(contentId,
                                ignored -> new PdfItemBuilder(
                                        contentId, proposalKind, page))
                        .acceptLegacyIdAndProposal(
                                "PDF-COMP-" + safeToken(proposal.id()), proposal);
            }
        }
        List<DocumentContentItem> items = grouped.values().stream()
                .sorted(Comparator.comparingInt(PdfItemBuilder::pageNumber)
                        .thenComparingDouble(PdfItemBuilder::yMin)
                        .thenComparingDouble(PdfItemBuilder::xMin))
                .map(item -> item.build(source.workspace().sourceSha256()))
                .toList();
        return new DocumentContentProjection(source.title(), source.format(),
                source.sourcePath(), items);
    }

    private static boolean explicitlyRejected(
            PreparedPdfPage page, PdfVisualObjectProposal proposal) {
        java.util.Set<String> ownerRegions = new java.util.HashSet<>(
                proposal.sourceRegionIds());
        ownerRegions.addAll(proposal.internalLabelRegionIds());
        if (!proposal.captionRegionId().isBlank()) {
            ownerRegions.add(proposal.captionRegionId());
        }
        return page.derivedTreatments().stream()
                .filter(treatment -> treatment.state()
                        == PdfDerivedTreatmentState.REJECTED)
                .filter(treatment -> treatmentCurrent(page, treatment))
                .map(PdfDerivedTreatment::sourceRegionIds)
                .anyMatch(ids -> ids.stream().anyMatch(ownerRegions::contains));
    }

    private static boolean treatmentCurrent(
            PreparedPdfPage page, PdfDerivedTreatment treatment) {
        long currentRevision = page.regions().stream()
                .filter(region -> treatment.sourceRegionIds().contains(region.id()))
                .mapToLong(PdfRegion::revision).max().orElse(-1L);
        return currentRevision == treatment.sourceRevision()
                || "legacy-source".equals(treatment.sourceFingerprint());
    }

    private static Map<String, List<NarrationSegment>> segmentsBySourceId(
            NarrationScriptDocument narration) {
        LinkedHashMap<String, List<NarrationSegment>> result = new LinkedHashMap<>();
        if (narration == null) return result;
        for (NarrationSegment segment : narration.segments()) {
            for (String sourceId : segment.sourceBlockIds()) {
                ArrayList<NarrationSegment> next = new ArrayList<>(
                        result.getOrDefault(sourceId, List.of()));
                next.add(segment);
                result.put(sourceId, List.copyOf(next));
            }
        }
        return result;
    }

    private static String legacyOwnerId(NarrationSegment segment, PdfNarrationBinding binding) {
        String interpretation = binding.interpretationId();
        if (interpretation != null && !interpretation.isBlank()) {
            return "PDF-COMP-" + safeToken(interpretation);
        }
        String explicit = segment.metadata().getOrDefault("pdfPageMapOwnerId", "").strip();
        if (!explicit.isBlank()) return "PDF-COMP-" + safeToken(explicit);
        String first = binding.sourceRegionIds().isEmpty()
                ? segment.metadata().getOrDefault("sourceBlockId", segment.id())
                : binding.sourceRegionIds().getFirst();
        return "PDF-REGION-" + safeToken(first);
    }

    private static String semanticContentId(
            int pageNumber, List<String> sourceRegionIds, String fallback) {
        List<String> stableSources = (sourceRegionIds == null
                ? List.<String>of() : sourceRegionIds).stream()
                .filter(Objects::nonNull).map(String::strip)
                .filter(value -> !value.isBlank()).distinct().sorted().toList();
        if (stableSources.isEmpty()) {
            stableSources = List.of(fallback == null ? "content" : fallback);
        }
        if (stableSources.size() == 1) {
            return "PDF-REGION-" + safeToken(stableSources.getFirst());
        }
        String identity = pageNumber + "|" + String.join("|", stableSources);
        return "PDF-CONTENT-" + sha256(identity).substring(0, 20)
                .toUpperCase(Locale.ROOT);
    }

    private static DocumentContentKind wordKind(DocumentBlockType type) {
        return switch (type) {
            case TITLE, HEADING, SUBHEADING -> DocumentContentKind.COVER;
            case TABLE_NOTICE -> DocumentContentKind.TABLE;
            case MATH_NOTICE -> DocumentContentKind.PROSE;
            case IMAGE_NOTICE -> DocumentContentKind.IMAGE;
            case IGNORED, EMPTY -> DocumentContentKind.EXTRA;
            default -> DocumentContentKind.PROSE;
        };
    }

    private static DocumentContentKind pdfKind(NarrationSegment segment) {
        String raw = segment.metadata().getOrDefault("sourceBlockType", "PARAGRAPH");
        PdfRegionType type;
        try {
            type = PdfRegionType.valueOf(raw);
        } catch (IllegalArgumentException invalid) {
            type = PdfRegionType.UNKNOWN;
        }
        return switch (type) {
            case TITLE, HEADING, SUBHEADING -> DocumentContentKind.COVER;
            case TABLE -> DocumentContentKind.TABLE;
            case MATH -> DocumentContentKind.EQUATION;
            case IMAGE -> DocumentContentKind.IMAGE;
            case UNKNOWN, CODE, SIDEBAR, CAPTION ->
                    segment.metadata().containsKey("pdfDerivedTreatmentId")
                            ? DocumentContentKind.EXTRA : DocumentContentKind.PROSE;
            default -> DocumentContentKind.PROSE;
        };
    }

    private static DocumentContentKind pdfKind(
            PdfVisualObjectProposal.Type type) {
        return switch (type) {
            case TABLE -> DocumentContentKind.TABLE;
            case FORMULA -> DocumentContentKind.EQUATION;
            case FIGURE, DIAGRAM, GRAPH -> DocumentContentKind.IMAGE;
            case UNKNOWN -> DocumentContentKind.EXTRA;
            case DECORATION -> DocumentContentKind.PROSE;
        };
    }

    private static long blockRevision(DocumentBlock block) {
        try {
            return Math.max(1L, Long.parseLong(
                    block.metadata().getOrDefault("revision", "1")));
        } catch (NumberFormatException ignored) {
            return 1L;
        }
    }

    private static String joinedNarration(List<NarrationSegment> segments) {
        return segments.stream().map(NarrationSegment::narrationText)
                .filter(text -> text != null && !text.isBlank())
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private static String safeToken(String value) {
        String normalized = value == null ? "" : value.strip();
        String token = normalized.replaceAll("\\s+", "-")
                .replaceAll("[^A-Za-z0-9._:-]", "-")
                .replaceAll("-+", "-");
        return token.isBlank() ? sha256(normalized).substring(0, 16) : token;
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest).toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static final class PdfItemBuilder {
        private final String contentId;
        private DocumentContentKind kind;
        private final PreparedPdfPage page;
        private final ArrayList<NarrationSegment> segments = new ArrayList<>();
        private final java.util.LinkedHashSet<String> sourceIds = new java.util.LinkedHashSet<>();
        private final java.util.LinkedHashSet<String> legacyContentIds =
                new java.util.LinkedHashSet<>();
        private DocumentContentRectangle roi;
        private DocumentContentRectangle semanticVisualRoi;
        private boolean visualProposalAccepted;
        private long revision = 1L;
        private String sourceFingerprint = "";

        private PdfItemBuilder(String contentId, DocumentContentKind kind, PreparedPdfPage page) {
            this.contentId = contentId;
            this.kind = kind;
            this.page = page;
        }

        private void accept(NarrationSegment segment, PdfNarrationBinding binding) {
            segments.add(segment);
            sourceIds.addAll(binding.sourceRegionIds());
            revision = Math.max(revision, binding.sourceRevision());
            if (!binding.sourceFingerprint().isBlank()) sourceFingerprint = binding.sourceFingerprint();
            DocumentContentRectangle focusRoi = PdfNarrationFocusMetadata.decode(
                    segment.metadata().get(PdfNarrationFocusMetadata.KEY))
                    .map(focus -> focus.boxes().stream()
                            .map(box -> new DocumentContentRectangle(box.xMin(), box.yMin(),
                                    box.xMax(), box.yMax()))
                            .reduce(DocumentContentRectangle::union).orElse(null))
                    .orElse(null);
            if (focusRoi != null) include(focusRoi);
            for (PdfRegion region : page.regions()) {
                if (binding.sourceRegionIds().contains(region.id())) {
                    include(new DocumentContentRectangle(region.xMin(), region.yMin(),
                            region.xMax(), region.yMax()));
                    revision = Math.max(revision, region.revision());
                }
            }
            DocumentContentKind segmentKind = pdfKind(segment);
            if (segmentKind.secondarySemanticComponent()) {
                kind = segmentKind;
                if (focusRoi != null) semanticVisualRoi = focusRoi;
            }
        }

        private void accept(PdfVisualObjectProposal proposal) {
            DocumentContentRectangle proposalRoi = new DocumentContentRectangle(
                    proposal.geometry().xMinPoints(),
                    proposal.geometry().yMinPoints(),
                    proposal.geometry().xMaxPoints(),
                    proposal.geometry().yMaxPoints());
            include(proposalRoi);
            semanticVisualRoi = visualProposalAccepted
                    ? DocumentContentRectangle.union(semanticVisualRoi, proposalRoi)
                    : proposalRoi;
            visualProposalAccepted = true;
            sourceIds.addAll(proposal.sourceRegionIds());
            sourceIds.addAll(proposal.internalLabelRegionIds());
            if (!proposal.captionRegionId().isBlank()) {
                sourceIds.add(proposal.captionRegionId());
            }
            if (sourceIds.isEmpty()) sourceIds.add(proposal.id());
            sourceFingerprint = sha256(proposal.id() + "|"
                    + proposal.type().name() + "|" + proposal.geometry()
                    + "|" + proposal.detector());
            for (PdfRegion region : page.regions()) {
                if (sourceIds.contains(region.id())) {
                    revision = Math.max(revision, region.revision());
                }
            }
            kind = pdfKind(proposal.type());
        }

        private void acceptLegacyId(String value) {
            if (value != null && !value.isBlank() && !contentId.equals(value.strip())) {
                legacyContentIds.add(value.strip());
            }
        }

        private PdfItemBuilder acceptLegacyIdAndProposal(
                String legacyId, PdfVisualObjectProposal proposal) {
            acceptLegacyId(legacyId);
            accept(proposal);
            return this;
        }

        private void include(DocumentContentRectangle next) {
            roi = roi == null ? next : DocumentContentRectangle.union(roi, next);
        }

        private DocumentContentItem build(String sourceSha) {
            DocumentContentRectangle visualRoi = kind.secondarySemanticComponent()
                    && semanticVisualRoi != null ? semanticVisualRoi : roi;
            if (visualRoi == null) {
                throw new IllegalStateException("Admitted PDF content requires source geometry: " + contentId);
            }
            String narration = joinedNarration(segments);
            String fingerprint = sha256(String.join("|", sourceSha, contentId,
                    Long.toString(revision), sourceFingerprint, narration));
            String visualFingerprint = sha256(String.join("|", sourceSha,
                    Integer.toString(page.pageNumber()), Long.toString(revision),
                    visualRoi.toString(), String.join(",", sourceIds)));
            PdfContentAnchor anchor = new PdfContentAnchor(page.pageNumber(),
                    page.widthPoints(), page.heightPoints(), visualRoi, List.copyOf(sourceIds),
                    page.regions().stream().filter(region -> sourceIds.contains(region.id()))
                            .collect(java.util.stream.Collectors.toMap(PdfRegion::id,
                                    region -> new DocumentContentRectangle(region.xMin(), region.yMin(),
                                            region.xMax(), region.yMax()),
                                    (left, right) -> left, LinkedHashMap::new)),
                    page.regions().stream().filter(region -> sourceIds.contains(region.id()))
                            .collect(java.util.stream.Collectors.toMap(PdfRegion::id,
                                    PdfRegion::effectiveText, (left, right) -> left,
                                    LinkedHashMap::new)),
                    page.regions().stream().filter(region -> sourceIds.contains(region.id()))
                            .filter(region -> Boolean.parseBoolean(region.attributes()
                                    .getOrDefault("playbackGeometryFallback", "false")))
                            .map(PdfRegion::id).toList(),
                    revision, visualFingerprint);
            String title = segments.stream().map(NarrationSegment::title)
                    .filter(value -> value != null && !value.isBlank()).findFirst()
                    .orElse("Página " + page.pageNumber());
            List<PdfRegion> sourceRegions = page.regions().stream()
                    .filter(region -> sourceIds.contains(region.id())).toList();
            DocumentPresentationMode presentationMode =
                    DocumentPresentationPolicy.forPdf(
                            kind, sourceRegions, visualProposalAccepted);
            return new DocumentContentItem(contentId, kind, title, narration,
                    segments.stream().map(NarrationSegment::id).toList(),
                    List.copyOf(sourceIds), List.copyOf(legacyContentIds),
                    fingerprint, revision, presentationMode, anchor);
        }

        private int pageNumber() { return page.pageNumber(); }
        private boolean overlaps(PdfVisualObjectProposal proposal) {
            return proposal.sourceRegionIds().stream().anyMatch(sourceIds::contains)
                    || proposal.internalLabelRegionIds().stream().anyMatch(sourceIds::contains)
                    || !proposal.captionRegionId().isBlank()
                    && sourceIds.contains(proposal.captionRegionId());
        }
        private double xMin() { return semanticVisualRoi != null
                ? semanticVisualRoi.xMin() : roi == null ? 0.0 : roi.xMin(); }
        private double yMin() { return semanticVisualRoi != null
                ? semanticVisualRoi.yMin() : roi == null ? 0.0 : roi.yMin(); }
    }
}
