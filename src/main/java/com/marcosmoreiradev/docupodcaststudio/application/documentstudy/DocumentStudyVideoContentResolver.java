package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentItem;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentKind;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentProjection;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentPresentationMode;
import com.marcosmoreiradev.docupodcaststudio.application.document.WordContentAnchor;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentParagraphVisualAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyClosingSlide;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVideoSlideConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVisualSource;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.study.SecondarySlideInclusionMode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves documentary video content without depending on a source format. */
public final class DocumentStudyVideoContentResolver {
    public enum Kind { COVER, PARAGRAPH, TABLE, EQUATION, IMAGE, EXTRA, CLOSING }

    public record Item(Kind kind, DocumentContentItem content,
                       DocumentParagraphVisualAssignment paragraphVisual,
                       double durationSeconds,
                       boolean enabled,
                       DocumentBlock sourceBlock) {
        public DocumentPresentationMode presentationMode() {
            return content.presentationMode();
        }
        /** Compatibility view for the existing slide editor and compositor. */
        public DocumentBlock block() {
            if (sourceBlock != null) return sourceBlock;
            DocumentBlockType type = switch (kind) {
                case COVER -> DocumentBlockType.HEADING;
                case TABLE -> DocumentBlockType.TABLE_NOTICE;
                case EQUATION -> DocumentBlockType.MATH_NOTICE;
                case IMAGE, EXTRA -> DocumentBlockType.IMAGE_NOTICE;
                default -> DocumentBlockType.PARAGRAPH;
            };
            java.util.LinkedHashMap<String, String> metadata = new java.util.LinkedHashMap<>();
            content.wordAnchor().ifPresent(anchor -> metadata.putAll(anchor.metadata()));
            metadata.put("contentKind", content.kind().name());
            metadata.put("contentFingerprint", content.fingerprint());
            return DocumentBlock.of(content.contentId(), type, content.narrationText(),
                    "document-content-projection", metadata);
        }
    }

    public List<Item> resolve(DocumentContentProjection projection,
                              DocumentStudyVideoConfiguration configuration) {
        return resolve(projection, configuration,
                SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF);
    }

    public List<Item> resolve(DocumentContentProjection projection,
                              DocumentStudyVideoConfiguration configuration,
                              SecondarySemanticReadingPolicy readingPolicy) {
        if (projection == null) return List.of();
        DocumentStudyVideoConfiguration safe = configuration == null
                ? DocumentStudyVideoConfiguration.empty() : configuration;
        SecondarySemanticReadingPolicy policy = readingPolicy == null
                ? SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF : readingPolicy;
        Map<String, DocumentVideoSlideConfiguration> slidesById = safe.contentSlides().stream()
                .collect(Collectors.toMap(DocumentVideoSlideConfiguration::contentId,
                        Function.identity(), (first, ignored) -> first));
        Set<String> disabledIds = Set.copyOf(safe.disabledBlockIds());
        ArrayList<Item> result = new ArrayList<>();
        for (DocumentContentItem content : projection.items()) {
            if (content == null) continue;
            if (content.secondarySemanticComponent()) {
                if (!secondaryContentEnabled(content, safe, policy)
                        || !sourceVisualAvailable(content)) continue;
            } else if (!content.narratable()) {
                continue;
            }
            DocumentVideoSlideConfiguration slide =
                    configuredSlide(content, slidesById)
                            .map(value -> rekey(content, value))
                            .orElseGet(() -> legacySlide(content, safe));
            DocumentParagraphVisualAssignment visual = slide.visual();
            double duration = slide.durationSeconds() > 0.0 ? slide.durationSeconds()
                    : content.secondarySemanticComponent()
                    ? safe.defaultSecondarySemanticDurationSeconds() : 0.0;
            result.add(new Item(kind(content.kind()), content, visual, duration,
                    slide.enabled() && blockEnabled(content, slidesById, disabledIds), null));
        }
        appendClosingSlides(result, safe);
        return List.copyOf(result);
    }

    private static java.util.Optional<DocumentVideoSlideConfiguration> configuredSlide(
            DocumentContentItem content,
            Map<String, DocumentVideoSlideConfiguration> slidesById) {
        DocumentVideoSlideConfiguration canonical = slidesById.get(content.contentId());
        if (canonical != null) return java.util.Optional.of(canonical);
        for (String legacyId : content.legacyContentIds()) {
            DocumentVideoSlideConfiguration legacy = slidesById.get(legacyId);
            if (legacy != null) return java.util.Optional.of(legacy);
        }
        return java.util.Optional.empty();
    }

    private static boolean blockEnabled(
            DocumentContentItem content,
            Map<String, DocumentVideoSlideConfiguration> slidesById,
            Set<String> disabledIds) {
        java.util.Optional<DocumentVideoSlideConfiguration> configured =
                configuredSlide(content, slidesById);
        if (configured.isPresent()) return configured.orElseThrow().enabled();
        if (disabledIds.contains(content.contentId())) return false;
        return content.legacyContentIds().stream().noneMatch(disabledIds::contains);
    }

    private static boolean secondaryContentEnabled(
            DocumentContentItem content,
            DocumentStudyVideoConfiguration configuration,
            SecondarySemanticReadingPolicy policy) {
        return switch (configuration.secondarySlideInclusionMode()) {
            case OMIT -> false;
            // Video inclusion is independent from Qwen/TTS. An admitted source visual may
            // remain on screen silently even when semantic reading is disabled.
            case INCLUDE_ALLOWED -> true;
            // Compatibility value from older projects. The reading policy now controls
            // narration only; it must not remove the original visual from the video.
            case FOLLOW_READING_POLICY -> true;
        };
    }

    private static boolean sourceVisualAvailable(DocumentContentItem content) {
        if (content.pdfAnchor().isPresent()) return true;
        return content.wordAnchor().map(anchor ->
                content.kind() == DocumentContentKind.TABLE
                        || !anchor.metadata().getOrDefault(
                        "embeddedImageBase64", "").isBlank()).orElse(false);
    }

    /** Compatibility path for older callers and Word-specific tests. */
    public List<Item> resolve(ReadableDocument document,
                              DocumentStudyVideoConfiguration configuration) {
        return resolve(document, configuration,
                SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF);
    }

    public List<Item> resolve(ReadableDocument document,
                              DocumentStudyVideoConfiguration configuration,
                              SecondarySemanticReadingPolicy readingPolicy) {
        if (document == null) return List.of();
        List<DocumentContentItem> content = document.blocks().stream().map(block -> {
            DocumentContentKind kind = switch (block.type()) {
                case TITLE, HEADING, SUBHEADING -> DocumentContentKind.COVER;
                case TABLE_NOTICE -> DocumentContentKind.TABLE;
                case MATH_NOTICE -> DocumentContentKind.PROSE;
                case IMAGE_NOTICE -> DocumentContentKind.IMAGE;
                case IGNORED, EMPTY -> DocumentContentKind.EXTRA;
                default -> DocumentContentKind.PROSE;
            };
            String fingerprint = HexFormat.of().formatHex(sha256(
                    block.id() + "|" + block.type() + "|" + block.text()));
            return new DocumentContentItem(block.id(), kind, block.text(), block.text(),
                    block.narratable() || block.type() == DocumentBlockType.TABLE_NOTICE
                            ? List.of("LEGACY-" + block.id()) : List.of(),
                    List.of(block.id()), fingerprint, 1L,
                    new WordContentAnchor(block.id(), block.metadata()));
        }).toList();
        Map<String, DocumentBlock> sourceBlocks = document.blocks().stream().collect(
                java.util.stream.Collectors.toMap(DocumentBlock::id, block -> block));
        return resolve(new DocumentContentProjection(document.title(), document.format(),
                document.sourcePath(), content), configuration, readingPolicy).stream()
                .map(item -> new Item(item.kind(), item.content(), item.paragraphVisual(),
                        item.durationSeconds(), item.enabled(),
                        sourceBlocks.get(item.content().contentId())))
                .toList();
    }

    private static DocumentVideoSlideConfiguration legacySlide(
            DocumentContentItem content, DocumentStudyVideoConfiguration safe) {
        DocumentParagraphVisualAssignment visual = safe.paragraph(content.contentId())
                .orElseGet(() -> DocumentParagraphVisualAssignment.empty(content.contentId()));
        double duration = content.kind() == DocumentContentKind.TABLE
                ? safe.tableDuration(content.contentId()) : 0.0;
        return new DocumentVideoSlideConfiguration(content.contentId(), content.fingerprint(),
                visual, duration, safe.blockEnabled(content.contentId()), "", "");
    }

    private static DocumentVideoSlideConfiguration rekey(
            DocumentContentItem content, DocumentVideoSlideConfiguration slide) {
        if (slide.contentId().equals(content.contentId())) return slide;
        return new DocumentVideoSlideConfiguration(content.contentId(),
                slide.sourceFingerprint(), slide.visual(), slide.durationSeconds(),
                slide.enabled(), slide.sourceVisualAssetId(),
                slide.sourceVisualFingerprint());
    }

    private static Kind kind(DocumentContentKind kind) {
        return switch (kind) {
            case COVER -> Kind.COVER;
            case TABLE -> Kind.TABLE;
            case EQUATION -> Kind.EQUATION;
            case IMAGE -> Kind.IMAGE;
            case EXTRA -> Kind.EXTRA;
            case PROSE -> Kind.PARAGRAPH;
        };
    }

    private static void appendClosingSlides(ArrayList<Item> result,
                                            DocumentStudyVideoConfiguration safe) {
        for (DocumentStudyClosingSlide slide : safe.closingSlides()) {
            DocumentParagraphVisualAssignment visual = new DocumentParagraphVisualAssignment(
                    slide.id(), "", slide.imageAssetId(), "", "",
                    slide.imageAssetId().isBlank() ? DocumentVisualSource.NONE
                            : DocumentVisualSource.IMPORTED,
                    "", com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentMascotPosition.BOTTOM_RIGHT);
            DocumentContentItem content = new DocumentContentItem(slide.id(),
                    DocumentContentKind.EXTRA, slide.title(), slide.title(),
                    List.of("CLOSING-" + slide.id()), List.of(slide.id()),
                    HexFormat.of().formatHex(sha256(slide.id() + "|" + slide.title())),
                    1L, new WordContentAnchor(slide.id()));
            result.add(new Item(Kind.CLOSING, content, visual,
                    slide.durationSeconds(), safe.blockEnabled(slide.id()), null));
        }
    }

    private static byte[] sha256(String text) {
        try {
            return java.security.MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
