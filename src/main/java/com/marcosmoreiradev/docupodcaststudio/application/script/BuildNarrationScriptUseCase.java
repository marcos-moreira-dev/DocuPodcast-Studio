package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.fragment.FragmentId;
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

    public NarrationScriptDocument build(ReadableDocument document, String language) {
        return build(document, language, false);
    }

    public NarrationScriptDocument build(ReadableDocument document, String language, boolean readAfterColon) {
        return build(document, language, readAfterColon, TableNarrationPolicy.IGNORE_TABLES);
    }

    public NarrationScriptDocument build(ReadableDocument document, String language, boolean readAfterColon, ReadingProfile profile) {
        TableNarrationPolicy tablePolicy = profile == null ? TableNarrationPolicy.IGNORE_TABLES : profile.tablePolicy();
        return build(document, language, readAfterColon, tablePolicy);
    }

    public NarrationScriptDocument build(ReadableDocument document, String language, boolean readAfterColon, TableNarrationPolicy tablePolicy) {
        Objects.requireNonNull(document, "document");
        TableNarrationPolicy safeTablePolicy = tablePolicy == null ? TableNarrationPolicy.IGNORE_TABLES : tablePolicy;
        List<NarrationSegment> segments = new ArrayList<>();
        int index = 1;
        for (DocumentBlock block : document.blocks()) {
            if (document.format() == SourceDocumentFormat.PDF && !pdfOcrNarratable(block)) {
                continue;
            }
            if (!includedInNarration(block, safeTablePolicy)) {
                continue;
            }
            String speakerId = characterIdFor(block.text());
            String narrationText = narrationTextFor(block, readAfterColon, safeTablePolicy);
            if (narrationText.isBlank()) {
                continue;
            }
            boolean secondary = block.type() == DocumentBlockType.TABLE_NOTICE;
            segments.add(new NarrationSegment(
                    "SEG-%03d".formatted(index++),
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

    private static boolean pdfOcrNarratable(DocumentBlock block) {
        return block != null
                && Boolean.parseBoolean(block.metadata().getOrDefault("ocr", "false"))
                && "ocr-local".equalsIgnoreCase(block.metadata().getOrDefault("extractionMode", ""));
    }

    private static boolean includedInNarration(DocumentBlock block, TableNarrationPolicy tablePolicy) {
        if (block == null || block.type() == DocumentBlockType.EMPTY || block.type() == DocumentBlockType.IGNORED) {
            return false;
        }
        if (block.type() == DocumentBlockType.TABLE_NOTICE) {
            return tablePolicy != TableNarrationPolicy.IGNORE_TABLES;
        }
        return block.narratable();
    }

    private String narrationTextFor(DocumentBlock block, boolean readAfterColon, TableNarrationPolicy tablePolicy) {
        if (block.type() == DocumentBlockType.TABLE_NOTICE) {
            return tablePolicy == TableNarrationPolicy.READ_STRUCTURED
                    ? tableNarration.structuredText(block)
                    : tableNarration.summaryText(block);
        }
        String text = ReadAfterColonTextPolicy.narrationText(block.text(), readAfterColon);
        return switch (block.type()) {
            case TITLE -> "Tema principal: " + text + ".";
            case HEADING -> "Nuevo tema: " + text + ".";
            case SUBHEADING -> "Ahora veremos: " + text + ".";
            case IMAGE_NOTICE -> text.startsWith("Imagen") ? text : "Imagen: " + text;
            case MATH_NOTICE -> "";
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
        if (secondary) {
            metadata.put("secondaryReadUnit", "true");
            metadata.put("sourceVisualReadUnit", "true");
            metadata.put("tableNarrationPolicy", tablePolicy.name());
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
