package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ImageNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.DocumentListeningPreferences;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.TableNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compatibility builder for the internal segment-based prepared-reading payload.
 *
 * <p>Product flows should prefer {@code BuildPreparedReadingProjectionUseCase}. This class remains
 * because existing audio, render, playback and persisted-project contracts still consume
 * {@code NarrationScriptDocument} while the project migrates away from script as a user-facing
 * concept.</p>
 */
public final class BuildNarrationScriptUseCase {
    private final TableNarrationTextBuilder tableNarration = new TableNarrationTextBuilder();
    public BuildNarrationScriptUseCase() {
    }

    public NarrationScriptDocument build(ReadableDocument document, String language) {
        return build(document, language, false);
    }

    public NarrationScriptDocument build(ReadableDocument document, String language, boolean readAfterColon) {
        return build(document, language, readAfterColon, TableNarrationPolicy.IGNORE_TABLES);
    }

    public NarrationScriptDocument build(ReadableDocument document, String language, boolean readAfterColon, ReadingProfile profile) {
        TableNarrationPolicy tablePolicy = profile == null ? TableNarrationPolicy.IGNORE_TABLES : profile.tablePolicy();
        ImageNarrationPolicy imagePolicy = profile == null
                ? ImageNarrationPolicy.IGNORE_IMAGES
                : profile.imagePolicy();
        return build(document, language, readAfterColon, tablePolicy, imagePolicy);
    }

    public NarrationScriptDocument build(ReadableDocument document, String language, boolean readAfterColon, TableNarrationPolicy tablePolicy) {
        return build(document, language, readAfterColon, tablePolicy, ImageNarrationPolicy.IGNORE_IMAGES);
    }

    private NarrationScriptDocument build(
            ReadableDocument document,
            String language,
            boolean readAfterColon,
            TableNarrationPolicy tablePolicy,
            ImageNarrationPolicy imagePolicy
    ) {
        Objects.requireNonNull(document, "document");
        if (document.format() == SourceDocumentFormat.PDF) {
            throw new IllegalArgumentException(
                    "PDF narration must use BuildPreparedPdfNarrationUseCase");
        }
        TableNarrationPolicy safeTablePolicy = tablePolicy == null ? TableNarrationPolicy.IGNORE_TABLES : tablePolicy;
        ImageNarrationPolicy safeImagePolicy = imagePolicy == null
                ? ImageNarrationPolicy.IGNORE_IMAGES
                : imagePolicy;
        List<NarrationSegment> segments = new ArrayList<>();
        int index = 1;
        for (DocumentBlock block : document.blocks()) {
            if (block.metadata().containsKey(com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.TheatreGrammarDocumentBuilder.INTERVENTION_ID)) {
                segments.add(com.marcosmoreiradev.docupodcaststudio.application.theatre.grammar.TheatreGrammarDocumentBuilder.segment(block));
                continue;
            }
            if (!includedInNarration(block, safeTablePolicy, safeImagePolicy)) {
                continue;
            }
            String speakerId = characterIdFor(block.text());
            String narrationText = narrationTextFor(block, readAfterColon, safeTablePolicy, safeImagePolicy);
            if (narrationText.isBlank()) {
                continue;
            }
            boolean secondary = block.type() == DocumentBlockType.TABLE_NOTICE
                    || block.type() == DocumentBlockType.IMAGE_NOTICE;
            String segmentId = "SEG-%03d".formatted(index++);
            segments.add(new NarrationSegment(
                    segmentId,
                    segmentTypeFor(block.type()),
                    titleFor(block),
                    narrationText,
                    List.of(block.id()),
                    speakerId,
                    "VOC-NARRATOR",
                    "STY-NEUTRAL",
                    metadataFor(block, secondary, safeTablePolicy)
            ));
        }
        return NarrationScriptDocument.create(document.title(), language, document.title(), segments);
    }

    private static boolean includedInNarration(
            DocumentBlock block,
            TableNarrationPolicy tablePolicy,
            ImageNarrationPolicy imagePolicy
    ) {
        if (block == null || block.type() == DocumentBlockType.EMPTY || block.type() == DocumentBlockType.IGNORED) {
            return false;
        }
        if (block.type() == DocumentBlockType.TABLE_NOTICE) {
            return !tablePolicy.skips();
        }
        if (block.type() == DocumentBlockType.IMAGE_NOTICE) {
            return switch (imagePolicy) {
                case IGNORE_IMAGES -> false;
                case READ_DESCRIPTION_OR_OMIT ->
                        imageDescriptionAdmitted(block);
                case ANNOUNCE_IMAGE_WITHOUT_DESCRIPTION -> true;
            };
        }
        if (block.type() == DocumentBlockType.MATH_NOTICE) {
            return !tablePolicy.skips() && (block.metadataValue("sourceMathText")
                    .filter(value -> !value.isBlank())
                    .or(() -> block.metadataValue("plainText"))
                    .isPresent() || !block.text().isBlank());
        }
        return block.narratable();
    }

    private static boolean imageDescriptionAdmitted(DocumentBlock block) {
        if (block.metadataValue("description").filter(value -> !value.isBlank()).isEmpty()) {
            return false;
        }
        String state = block.metadata().getOrDefault("descriptionState", "").strip();
        if ("REJECTED".equalsIgnoreCase(state) || "STALE".equalsIgnoreCase(state)) {
            return false;
        }
        if (!"DRAFT".equalsIgnoreCase(state)) return true;
        return DocumentListeningPreferences.QUALITY_MODEL.equals(
                block.metadata().getOrDefault("descriptionSource", ""))
                && Boolean.parseBoolean(block.metadata().getOrDefault(
                "ttsSafetyValidated", "false"))
                && "DRAFT_POLICY_VALIDATED".equals(block.metadata().getOrDefault(
                "automaticAdmission", ""));
    }

    private String narrationTextFor(
            DocumentBlock block,
            boolean readAfterColon,
            TableNarrationPolicy tablePolicy,
            ImageNarrationPolicy imagePolicy
    ) {
        if (block.type() == DocumentBlockType.TABLE_NOTICE) {
            return switch (tablePolicy.canonical()) {
                case READ_ALL, READ_TEXTUAL_CONTENT ->
                        tableNarration.structuredText(block);
                case SUMMARIZE -> tableNarration.summaryText(block);
                case ANNOUNCE_ONLY -> "Tabla detectada.";
                case SKIP -> "";
                default -> throw new IllegalStateException(
                        "Política de tabla no normalizada");
            };
        }
        if (block.type() == DocumentBlockType.IMAGE_NOTICE) {
            String description = block.metadataValue("description").orElse("").strip();
            if (!description.isBlank()) {
                return "Imagen: " + description + ".";
            }
            return imagePolicy == ImageNarrationPolicy.ANNOUNCE_IMAGE_WITHOUT_DESCRIPTION
                    ? "Imagen sin descripción textual."
                    : "";
        }
        if (block.type() == DocumentBlockType.TITLE
                || block.type() == DocumentBlockType.HEADING
                || block.type() == DocumentBlockType.SUBHEADING) {
            // Speaker-prefix trimming is useful for dialogue paragraphs, never
            // for structural titles such as "0.9 Algoritmo: la película".
            return block.text().strip();
        }
        String text = ReadAfterColonTextPolicy.narrationText(block.text(), readAfterColon);
        return switch (block.type()) {
            case TITLE, HEADING, SUBHEADING -> throw new IllegalStateException(
                    "Los títulos se resuelven antes de aplicar reglas de diálogo");
            case IMAGE_NOTICE -> text.startsWith("Imagen") ? text : "Imagen: " + text;
            case MATH_NOTICE -> block.metadataValue("sourceMathText")
                    .orElse(text);
            case LIST_ITEM, PARAGRAPH -> text;
            case TABLE_NOTICE -> "";
            case IGNORED, EMPTY -> "";
        };
    }

    private static Map<String, String> metadataFor(DocumentBlock block, boolean secondary, TableNarrationPolicy tablePolicy) {
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        metadata.put("sourceBlockType", block.type().name());
        metadata.put("sourceBlockId", block.id());
        metadata.put("generationSource", "document-workspace");
        if (block.type() == DocumentBlockType.TITLE
                || block.type() == DocumentBlockType.HEADING
                || block.type() == DocumentBlockType.SUBHEADING) {
            // A heading is both document structure and spoken content. Preserve
            // its exact source text: presentation may style it as a title, while
            // TTS, playback and export still receive a normal narratable segment.
            metadata.put("semanticRole", "DOCUMENT_TITLE");
            metadata.put("audioNarration", "EXACT_SOURCE_TEXT");
        }
        if (secondary) {
            metadata.put("secondaryReadUnit", "true");
            metadata.put("sourceVisualReadUnit", "true");
            if (block.type() == DocumentBlockType.TABLE_NOTICE) {
                metadata.put("tableNarrationPolicy", tablePolicy.name());
            }
        } else {
            metadata.put("fragmentId", FragmentId.fromBlockId(block.id()).value());
        }
        return Map.copyOf(metadata);
    }

    private static String characterIdFor(String text) {
        String normalized = text == null ? "" : text.strip();
        int colon = normalized.indexOf(':');
        if (colon < 2 || colon > 42) {
            return "CHR-NARRATOR";
        }
        String speaker = normalized.substring(0, colon).strip();
        if (speaker.isBlank() || speaker.equalsIgnoreCase("Escena") || speaker.toUpperCase(java.util.Locale.ROOT).startsWith("ESCENA ")) {
            return "CHR-NARRATOR";
        }
        String token = java.text.Normalizer.normalize(speaker, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return token.isBlank() ? "CHR-NARRATOR" : "CHR-" + token;
    }

    private static String titleFor(DocumentBlock block) {
        String preview = block.preview(60);
        if (preview.isBlank()) {
            return block.type().displayName();
        }
        return preview;
    }

    private static NarrationSegmentType segmentTypeFor(DocumentBlockType type) {
        return switch (type) {
            case TITLE -> NarrationSegmentType.TITLE;
            case HEADING -> NarrationSegmentType.HEADING;
            case SUBHEADING -> NarrationSegmentType.SUBHEADING;
            case LIST_ITEM -> NarrationSegmentType.LIST_ITEM;
            case TABLE_NOTICE -> NarrationSegmentType.TABLE_NOTICE;
            case IMAGE_NOTICE -> NarrationSegmentType.IMAGE_NOTICE;
            case MATH_NOTICE -> NarrationSegmentType.PARAGRAPH;
            case PARAGRAPH, IGNORED, EMPTY -> NarrationSegmentType.PARAGRAPH;
        };
    }

}
